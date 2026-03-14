package app.tastile.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val plan: String = "free",
)

enum class Plan(val value: String) {
    FREE("free"),
    PRO("pro");

    companion object {
        fun fromString(value: String): Plan = values().find { it.value == value } ?: FREE
    }
}
