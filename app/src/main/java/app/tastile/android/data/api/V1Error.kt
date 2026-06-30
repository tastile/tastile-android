package app.tastile.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class V1ApiErrorBody(
    val kind: Short,
    val message: String = "",
    @SerialName("current_revision") val currentRevision: Long? = null,
    val violations: List<ResolutionViolationBody> = emptyList()
)

@Serializable
data class ResolutionViolationBody(
    val path: String = "",
    val code: Int = 0,
    val message: String = ""
)

sealed class V1Error(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : V1Error("network error", cause)
    class Auth : V1Error("auth missing or invalid")
    class Unknown(status: Int, body: String) : V1Error("http $status: $body")
    data class Api(val kindValue: Short, val kindName: String, override val message: String, val currentRevision: Long? = null) : V1Error(message)

    companion object {
        fun fromApiBody(body: V1ApiErrorBody): Api {
            val name = when (body.kind) {
                V1NumericConstants.ApiErrorKind.VALIDATION -> "VALIDATION"
                V1NumericConstants.ApiErrorKind.FORBIDDEN -> "FORBIDDEN"
                V1NumericConstants.ApiErrorKind.STALE_REVISION -> "STALE_REVISION"
                V1NumericConstants.ApiErrorKind.IDEMPOTENCY_KEY_REUSED -> "IDEMPOTENCY_KEY_REUSED"
                V1NumericConstants.ApiErrorKind.NOT_FOUND -> "NOT_FOUND"
                V1NumericConstants.ApiErrorKind.CONFLICT -> "CONFLICT"
                V1NumericConstants.ApiErrorKind.BLOCKED -> "BLOCKED"
                V1NumericConstants.ApiErrorKind.RETRYABLE -> "RETRYABLE"
                else -> "UNKNOWN_${body.kind}"
            }
            return Api(body.kind, name, body.message, body.currentRevision)
        }
    }
}