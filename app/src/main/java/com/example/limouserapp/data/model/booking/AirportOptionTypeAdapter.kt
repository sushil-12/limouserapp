package com.example.limouserapp.data.model.booking

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter for AirportOption
 * Serializes as formatted name string when present, empty string when null
 * This matches web API format exactly
 */
class AirportOptionTypeAdapter : JsonSerializer<AirportOption?>, JsonDeserializer<AirportOption?> {
    override fun serialize(
        src: AirportOption?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src != null) {
            // Serialize as formatted name string (matches web format)
            JsonPrimitive(src.formattedName)
        } else {
            // Serialize as empty string when null (matches web format)
            JsonPrimitive("")
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AirportOption? {
        // For deserialization, if it's a string, we can't reconstruct the full object
        // This is mainly for serialization to match web format
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonObject -> {
                // If it's an object, deserialize normally
                val gson = Gson()
                gson.fromJson(json, AirportOption::class.java)
            }
            else -> null
        }
    }
}

/**
 * Custom Gson TypeAdapter for AirlineOption
 * Serializes as object when present, empty string when null
 * This matches web API format exactly
 */
class AirlineOptionTypeAdapter : JsonSerializer<AirlineOption?>, JsonDeserializer<AirlineOption?> {
    override fun serialize(
        src: AirlineOption?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src != null) {
            // Serialize as object when present (matches web format)
            val gson = Gson()
            gson.toJsonTree(src)
        } else {
            // Serialize as empty string when null (matches web format)
            JsonPrimitive("")
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AirlineOption? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonObject -> {
                // If it's an object, deserialize normally
                val gson = Gson()
                gson.fromJson(json, AirlineOption::class.java)
            }
            else -> null
        }
    }
}

