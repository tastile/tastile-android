package app.tastile.android.ui.avatar

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tastile.android.R
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * AvatarLoader — Display avatar with fallback chain (v1/15 §3).
 *
 * Chain: profile.avatar_url → Gravatar(email) → initials(display_name).
 * Uses Coil for async image loading.
 * Phase A: skeleton. Full Coil integration in Phase X.
 */
@Composable
fun AvatarLoader(
    avatarUrl: String?,
    displayName: String,
    size: Int = 64
) {
    if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.avatar_user_cd, displayName),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        // Initials fallback
        Text(
            text = getInitials(displayName),
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    }
}

private fun getInitials(displayName: String): String {
    val parts = displayName.trim().split("\\s+".toRegex())
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts.last().first()}".uppercase()
    } else {
        parts.firstOrNull()?.take(2)?.uppercase() ?: "?"
    }
}
