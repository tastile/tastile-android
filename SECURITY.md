# Security Policy

## Supported Versions

| AAB version | Supported |
| ----------- | --------- |
| latest      | ✅ Active |
| <latest     | ❌ EOL    |

Only the latest AAB (`versionCode` is incremented each release) receives
security backports. Identify your installed version in the Play Store or
APK info; older versions are not maintained.

## Reporting a Vulnerability

**Please do not file public GitHub issues for security vulnerabilities.**

Use one of these channels:

1. **GitHub Security Advisories** (preferred; private thread with the
   maintainers): https://github.com/tastile/tastile-android/security/advisories/new
2. **X (Twitter) DM** to `@361do_sleep` for urgent pre-disclosure matters.

We acknowledge within 2 business days and aim to ship a fix within 30 days
for high-impact issues.

## Build / Local Security Checks

`./gradlew verify` requires JDK 17 and the Android SDK 35 platform. See
`docs/setup.md` (added in a later step). Use `./gradlew --scan` to enable
the Gradle build-scan server, which logs dependency hashes for supply-chain
review.

## Security Scope

This client is a thin shell over the `tastile-core` REST API. Auth,
authorization, and multi-tenant isolation live in `tastile-core` (private);
report API-layer vulnerabilities there instead.

Android-specific concerns (compromised device, malicious intent,
CVE in dependencies) are within scope; please report in the channels
above.
