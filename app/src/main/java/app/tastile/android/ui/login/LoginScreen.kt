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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.data.auth.TastileAuthState

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
    val isGoogleSigningIn by viewModel.isGoogleSigningIn.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()

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
            Spacer(modifier = Modifier.weight(0.4f))

            BrandHeader()

            Spacer(modifier = Modifier.height(Grid.blockGap))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Grid.inlineGap),
            ) {
                Text(
                    text = stringResource(R.string.login_subtitle_signin),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.login_subtitle_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(Grid.blockGap))

            EmailPasswordFields(
                email = email,
                password = password,
                enabled = !isSigningIn,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
            )

            Spacer(modifier = Modifier.weight(1f))

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                AssistChip(
                    onClick = viewModel::clearError,
                    label = { Text(message) },
                    modifier = Modifier.padding(bottom = Grid.inlineGap),
                )
            }

            Button(
                onClick = { viewModel.signInWithEmail(context) },
                enabled = !isSigningIn && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = if (isSigningIn) stringResource(R.string.login_button_signing_in) else stringResource(R.string.login_button_signin),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(Grid.blockGap))

            // Subtle divider so the social button reads as a separate path
            // instead of a fourth email-password control.
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Spacer(modifier = Modifier.height(Grid.blockGap))

            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                enabled = !isSigningIn && !isGoogleSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login-google-button"),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(Grid.inlineGap))
                Text(
                    text = if (isGoogleSigningIn) {
                        stringResource(R.string.login_button_continue_google_in_progress)
                    } else {
                        stringResource(R.string.login_button_continue_google)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(Grid.inlineGap))

            TextButton(
                onClick = { viewModel.signUp(context) },
                enabled = !isSigningIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.login_signup_link),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(Grid.blockGap))

            PrivacyFooter()
        }
    }
}

@Composable
private fun EmailPasswordFields(
    email: String,
    password: String,
    enabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Grid.inlineGap),
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.login_email_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            singleLine = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.login_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
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
            contentDescription = stringResource(R.string.login_logo_cd),
            modifier = Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.medium),
        )
        Text(
            text = stringResource(R.string.login_brand_label),
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
    val termsLabel = stringResource(R.string.login_terms_link)
    val privacyLabel = stringResource(R.string.login_privacy_link)
    val footerSeparator = stringResource(R.string.login_footer_separator)
    val links: AnnotatedString = remember(linkColor, termsLabel, privacyLabel, footerSeparator) {
        buildAnnotatedString {
            withLink(
                LinkAnnotation.Clickable(
                    "https://tastile.app/terms",
                    styles = TextLinkStyles(style = linkStyle),
                ) { uriHandler.openUri("https://tastile.app/terms") },
            ) {
                withStyle(linkStyle) { append(termsLabel) }
            }
            append(footerSeparator)
            withLink(
                LinkAnnotation.Clickable(
                    "https://tastile.app/privacy",
                    styles = TextLinkStyles(style = linkStyle),
                ) { uriHandler.openUri("https://tastile.app/privacy") },
            ) {
                withStyle(linkStyle) { append(privacyLabel) }
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
