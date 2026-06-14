import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

const packageName = process.env.PLAY_PACKAGE_NAME ?? "app.tastile.android";
const track = process.env.PLAY_TRACK ?? "internal";
const status = process.env.PLAY_RELEASE_STATUS ?? "completed";
const keyFile =
  process.env.PLAY_SERVICE_ACCOUNT_JSON ??
  path.join(process.env.USERPROFILE ?? process.env.HOME ?? "", ".config", "tastile", "play", "tastile-play-publisher.json");
const aabFile =
  process.env.PLAY_AAB_PATH ??
  path.resolve("app", "build", "outputs", "bundle", "release", "app-release.aab");
const releaseName = process.env.PLAY_RELEASE_NAME ?? "0.2.4";
const releaseNotes = [
  {
    language: "ja-JP",
    text: process.env.PLAY_RELEASE_NOTES_JA ?? "認証フローを Cognito ベースに更新しました。",
  },
  {
    language: "en-US",
    text: process.env.PLAY_RELEASE_NOTES_EN ?? "Updated authentication flow to Cognito.",
  },
];

function base64Url(input) {
  return Buffer.from(typeof input === "string" ? input : JSON.stringify(input)).toString("base64url");
}

async function getAccessToken() {
  const serviceAccount = JSON.parse(fs.readFileSync(keyFile, "utf8"));
  const now = Math.floor(Date.now() / 1000);
  const unsignedJwt = [
    base64Url({ alg: "RS256", typ: "JWT" }),
    base64Url({
      iss: serviceAccount.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    }),
  ].join(".");
  const signature = crypto
    .sign("RSA-SHA256", Buffer.from(unsignedJwt), serviceAccount.private_key)
    .toString("base64url");

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsignedJwt}.${signature}`,
    }),
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(`Token request failed: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return body.access_token;
}

async function playFetch(token, url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      authorization: `Bearer ${token}`,
      ...(options.headers ?? {}),
    },
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : {};
  if (!response.ok) {
    throw new Error(`Google Play API failed: HTTP ${response.status} ${JSON.stringify(body)}`);
  }
  return body;
}

async function main() {
  if (!fs.existsSync(keyFile)) {
    throw new Error(`Service account key not found: ${keyFile}`);
  }
  if (!fs.existsSync(aabFile)) {
    throw new Error(`AAB not found: ${aabFile}`);
  }

  const token = await getAccessToken();
  const root = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}`;
  const uploadRoot = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${encodeURIComponent(packageName)}`;

  const edit = await playFetch(token, `${root}/edits`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{}",
  });
  console.log(`Created edit ${edit.id}`);

  const aab = fs.readFileSync(aabFile);
  const bundle = await playFetch(token, `${uploadRoot}/edits/${edit.id}/bundles?uploadType=media`, {
    method: "POST",
    headers: { "content-type": "application/octet-stream" },
    body: aab,
  });
  console.log(`Uploaded versionCode ${bundle.versionCode}`);

  await playFetch(token, `${root}/edits/${edit.id}/tracks/${encodeURIComponent(track)}`, {
    method: "PUT",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      track,
      releases: [
        {
          name: releaseName,
          versionCodes: [String(bundle.versionCode)],
          status,
          releaseNotes,
        },
      ],
    }),
  });
  console.log(`Updated track ${track}`);

  const commit = await playFetch(token, `${root}/edits/${edit.id}:commit`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{}",
  });
  console.log(`Committed edit ${commit.id}`);
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
