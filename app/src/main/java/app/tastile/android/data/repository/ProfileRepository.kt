package app.tastile.android.data.repository

import app.tastile.android.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val client: SupabaseClient
) {
    companion object {
        private const val TABLE_PROFILES = "profiles"
    }

    suspend fun getProfile(userId: String): Profile? {
        return try {
            client.from(TABLE_PROFILES)
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateDisplayName(userId: String, displayName: String): Profile? {
        return try {
            client.from(TABLE_PROFILES)
                .update({
                    set("display_name", displayName)
                }) {
                    filter {
                        eq("id", userId)
                    }
                    select()
                }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
