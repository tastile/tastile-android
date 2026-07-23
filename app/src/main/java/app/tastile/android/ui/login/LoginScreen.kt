package app.tastile.android.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.repository.TastileAuthState

private object Grid {
    val pageGutter = 24.dp
    val topInset = 56.dp
    val blockGap = 16.dp
    val inlineGap = 8.dp
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.error.collectAsStateWithLifecycle()
    val isSigningIn by viewModel.isSigningIn.collectAsStateWithLifecycle()

    if (authState is TastileAuthState.Authenticated) {
        onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .consumeWindowInsets(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Grid.pageGutter)
                .padding(top = Grid.topInset, bottom = Grid.pageGutter),
        ) {
            Spacer(modifier = Modifier.weight(0.7f))

            BrandHeader()

            Spacer(modifier = Modifier.height(Grid.blockGap))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Grid.inlineGap),
            ) {
                Text(
                    text = "Sign in to Tastile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Continue with the account you use to plan your tiles. Your schedule and devices will sync automatically.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                AssistChip(
                    onClick = viewModel::clearError,
                    label = { Text(message) },
                    modifier = Modifier.padding(bottom = Grid.inlineGap),
                )
            }

            Button(
                onClick = { viewModel.signInWithCognito(context) },
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = if (isSigningIn) "Opening sign-in…" else "Continue with Tastile",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(Grid.blockGap))

            PrivacyFooter()
        }
    }
}

@Composable
private fun BrandHeader() {
    val isDarkScheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val markRes = if (isDarkScheme) R.drawable.ic_tastile_icon_dark else R.drawable.ic_tastile_icon
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = Grid.inlineGap,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        Image(
            painter = painterResource(id = markRes),
            contentDescription = "Tastile logo",
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Text(
            text = "Tastile",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun PrivacyFooter() {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
    val links: AnnotatedString = remember(linkColor) {
        buildAnnotatedString {
            withLink(
                LinkAnnotation.Clickable(
                    "https://tastile.app/terms",
                    styles = TextLinkStyles(style = linkStyle),
                ) { uriHandler.openUri("https://tastile.app/terms") },
            ) {
                withStyle(linkStyle) { append("Terms of Service") }
            }
            append("   ·   ")
            withLink(
                LinkAnnotation.Clickable(
                    "https://tastile.app/privacy",
                    styles = TextLinkStyles(style = linkStyle),
                ) { uriHandler.openUri("https://tastile.app/privacy") },
            ) {
                withStyle(linkStyle) { append("Privacy Policy") }
            }
        }
    }

    Text(
        text = links,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}