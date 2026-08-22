package app.tastile.android.data.di

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.io.IOException
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Moshi adapters for `java.time` types that the generated v1 DTOs use.
 *
 * `Rfc3339DateJsonAdapter` from `moshi-adapters` only covers `java.util.Date`,
 * which is not what the server emits. These adapters parse the RFC 3339
 * offset-preserving strings (e.g. `2026-08-15T00:43:34.946+09:00`) that the
 * v1 surface returns for `OffsetDateTime` / `OffsetTime` fields.
 */
class OffsetDateTimeJsonAdapter {

    @FromJson
    @Throws(IOException::class)
    fun fromJson(reader: JsonReader): OffsetDateTime =
        OffsetDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    @ToJson
    @Throws(IOException::class)
    fun toJson(writer: JsonWriter, value: OffsetDateTime) {
        writer.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
}

class OffsetTimeJsonAdapter {

    @FromJson
    @Throws(IOException::class)
    fun fromJson(reader: JsonReader): OffsetTime =
        OffsetTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_TIME)

    @ToJson
    @Throws(IOException::class)
    fun toJson(writer: JsonWriter, value: OffsetTime) {
        writer.value(value.format(DateTimeFormatter.ISO_OFFSET_TIME))
    }
}

/**
 * Moshi adapter for `java.util.UUID`. Moshi has no built-in UUID support for
 * platform types (it errors with "Platform class java.util.UUID requires
 * explicit JsonAdapter to be registered"). Generated v1 DTOs expose `ownerId`
 * as `UUID`, so we serialize it as a string in canonical 8-4-4-4-12 form.
 */
class UuidJsonAdapter {

    @FromJson
    @Throws(IOException::class)
    fun fromJson(reader: JsonReader): UUID = UUID.fromString(reader.nextString())

    @ToJson
    @Throws(IOException::class)
    fun toJson(writer: JsonWriter, value: UUID) {
        writer.value(value.toString())
    }
}
