package app.tastile.android.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

interface CoreRuntimeService {
    fun applyCommand(command: CoreCommandRequest): CoreCommandAck
    fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck
    fun currentSnapshot(): CoreSnapshot
}

class DefaultCoreRuntimeService(
    private val bridge: TastileCoreBridge = TastileCoreBridge()
) : CoreRuntimeService {
    override fun applyCommand(command: CoreCommandRequest): CoreCommandAck = bridge.applyCommand(command)

    override fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck = bridge.replaceEventLog(events)

    override fun currentSnapshot(): CoreSnapshot = bridge.currentSnapshot()
}

interface CoreCommandStore {
    fun load(): List<CoreCommandRequest>
    fun save(commands: List<CoreCommandRequest>)
    fun loadImportedEvents(): List<CoreEventEnvelopeRecord>
    fun saveImportedEvents(events: List<CoreEventEnvelopeRecord>)
}

class SharedPreferencesCoreCommandStore @Inject constructor(
    @ApplicationContext context: Context
) : CoreCommandStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): List<CoreCommandRequest> {
        val raw = prefs.getString(KEY_COMMANDS, null) ?: return emptyList()
        return try {
            coreJson.decodeFromString(ListSerializer(CoreCommandRequest.serializer()), raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    override fun save(commands: List<CoreCommandRequest>) {
        prefs.edit()
            .putString(KEY_COMMANDS, coreJson.encodeToString(commands))
            .apply()
    }

    override fun loadImportedEvents(): List<CoreEventEnvelopeRecord> {
        val raw = prefs.getString(KEY_IMPORTED_EVENTS, null) ?: return emptyList()
        return try {
            coreJson.decodeFromString(ListSerializer(CoreEventEnvelopeRecord.serializer()), raw)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    override fun saveImportedEvents(events: List<CoreEventEnvelopeRecord>) {
        prefs.edit()
            .putString(KEY_IMPORTED_EVENTS, coreJson.encodeToString(events))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "tastile-core-runtime"
        const val KEY_COMMANDS = "commands"
        const val KEY_IMPORTED_EVENTS = "imported-events"
    }
}

class PersistentCoreRuntimeService(
    private val bridge: TastileCoreBridge = TastileCoreBridge(),
    private val commandStore: CoreCommandStore
) : CoreRuntimeService {
    @Volatile
    private var replayed = false

    override fun applyCommand(command: CoreCommandRequest): CoreCommandAck {
        ensureReplayed()
        val ack = bridge.applyCommand(command)
        if (shouldPersist(command.type)) {
            val commands = commandStore.load().toMutableList()
            commands += normalizeForPersistence(command, ack)
            commandStore.save(commands)
        }
        return ack
    }

    override fun replaceEventLog(events: List<CoreEventEnvelopeRecord>): CoreCommandAck {
        val ack = bridge.replaceEventLog(events)
        commandStore.saveImportedEvents(events)
        commandStore.save(emptyList())
        replayed = true
        return ack
    }

    override fun currentSnapshot(): CoreSnapshot {
        ensureReplayed()
        return bridge.currentSnapshot()
    }

    private fun ensureReplayed() {
        if (replayed) return

        synchronized(this) {
            if (replayed) return
            val importedEvents = commandStore.loadImportedEvents()
            if (importedEvents.isNotEmpty()) {
                bridge.replaceEventLog(importedEvents)
            }
            commandStore.load().forEach { command ->
                bridge.applyCommand(command)
            }
            replayed = true
        }
    }

    private fun normalizeForPersistence(
        command: CoreCommandRequest,
        ack: CoreCommandAck
    ): CoreCommandRequest {
        if (command.type != "tile.create") return command
        if (command.payload["tile_id"] != null) return command

        val tileId = ack.generatedTileId() ?: return command
        val normalizedPayload = buildJsonObject {
            command.payload.forEach { (key, value) -> put(key, value) }
            put("tile_id", tileId)
        }
        return command.copy(payload = normalizedPayload)
    }

    private fun shouldPersist(type: String): Boolean {
        return type !in setOf("auth.set_session", "sync.now")
    }
}
