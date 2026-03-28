package app.tastile.android.core

class TastileCoreBridge(
    private val libraryName: String = DEFAULT_LIBRARY_NAME,
    private val libraryLoader: (String) -> Unit = { System.loadLibrary(it) },
    private val nativeBindings: NativeBindings = JniNativeBindings()
) {
    @Volatile
    private var libraryLoaded: Boolean = false

    fun applyCommand(command: CoreCommandRequest): CoreCommandAck {
        ensureLibraryLoaded()
        val responseJson = try {
            nativeBindings.dispatchCommand(command.toJson())
        } catch (error: UnsatisfiedLinkError) {
            throw CoreBridgeError.NativeMethodUnavailable(methodName = "dispatchCommand", source = error)
        }

        val response = CoreCommandResponse.fromJson(responseJson)
        if (!response.accepted) {
            val err = response.error
                ?: throw CoreBridgeError.CommandResponseParseFailed(
                    rawPayload = responseJson,
                    source = IllegalStateException("Native response has accepted=false but missing error field")
                )
            throw CoreBridgeError.CommandFailed(
                errorCode = err.code,
                messageText = err.message,
                rawPayload = responseJson
            )
        }
        return response
    }

    fun currentSnapshot(): CoreSnapshot {
        ensureLibraryLoaded()
        val snapshotJson = try {
            nativeBindings.getSnapshot()
        } catch (error: UnsatisfiedLinkError) {
            throw CoreBridgeError.NativeMethodUnavailable(methodName = "getSnapshot", source = error)
        }

        return try {
            CoreSnapshot.fromJson(snapshotJson)
        } catch (error: CoreBridgeError.SnapshotParseFailed) {
            throw error
        } catch (error: Exception) {
            throw CoreBridgeError.SnapshotParseFailed(rawPayload = snapshotJson, source = error)
        }
    }

    fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck {
        ensureLibraryLoaded()
        val responseJson = try {
            nativeBindings.replaceEventLog(coreJson.encodeToString(events))
        } catch (error: UnsatisfiedLinkError) {
            throw CoreBridgeError.NativeMethodUnavailable(methodName = "replaceEventLog", source = error)
        }

        val response = CoreCommandResponse.fromJson(responseJson)
        if (!response.accepted) {
            val err = response.error
                ?: throw CoreBridgeError.CommandResponseParseFailed(
                    rawPayload = responseJson,
                    source = IllegalStateException("Native replaceEventLog response is missing error field")
                )
            throw CoreBridgeError.CommandFailed(
                errorCode = err.code,
                messageText = err.message,
                rawPayload = responseJson
            )
        }
        return response
    }

    private fun ensureLibraryLoaded() {
        if (libraryLoaded) return

        synchronized(this) {
            if (libraryLoaded) return
            try {
                libraryLoader(libraryName)
                libraryLoaded = true
            } catch (error: UnsatisfiedLinkError) {
                throw CoreBridgeError.LibraryLoadFailed(libraryName = libraryName, source = error)
            }
        }
    }

    interface NativeBindings {
        fun dispatchCommand(commandJson: String): String
        fun replaceEventLog(eventsJson: String): String
        fun getSnapshot(): String
    }

    private class JniNativeBindings : NativeBindings {
        external override fun dispatchCommand(commandJson: String): String
        external override fun replaceEventLog(eventsJson: String): String
        external override fun getSnapshot(): String
    }

    private companion object {
        const val DEFAULT_LIBRARY_NAME = "tastile_core"
    }
}

sealed class CoreBridgeError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    data class LibraryLoadFailed(val libraryName: String, val source: Throwable? = null) :
        CoreBridgeError("Failed to load native library '$libraryName'", source)

    data class NativeMethodUnavailable(val methodName: String, val source: Throwable? = null) :
        CoreBridgeError("Native method '$methodName' is unavailable", source)

    data class SnapshotParseFailed(val rawPayload: String, val source: Throwable? = null) :
        CoreBridgeError("Failed to parse core snapshot payload", source)

    data class CommandResponseParseFailed(val rawPayload: String, val source: Throwable? = null) :
        CoreBridgeError("Failed to parse core command response payload", source)

    data class CommandFailed(
        val errorCode: String,
        val messageText: String,
        val rawPayload: String
    ) : CoreBridgeError("Core command failed [$errorCode]: $messageText")
}
