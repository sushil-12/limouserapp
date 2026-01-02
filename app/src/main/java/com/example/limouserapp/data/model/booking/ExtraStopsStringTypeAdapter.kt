package com.example.limouserapp.data.model.booking

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter for String fields that can be either a JSON string or a JSON array
 * Handles the case where extra_stops and return_extra_stops can come as either:
 * - A JSON string: "[]" or "[{...}]"
 * - A JSON array: [{...}]
 * 
 * This adapter converts arrays to JSON strings to maintain compatibility with the existing String-based parsing
 */
class ExtraStopsStringTypeAdapter : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive -> {
                // Already a string, return as-is
                val stringValue = json.asString.trim()
                if (stringValue.isEmpty() || stringValue == "null") {
                    null
                } else {
                    stringValue
                }
            }
            json.isJsonArray -> {
                // It's an array, convert to JSON string
                val gson = Gson()
                val jsonString = gson.toJson(json)
                jsonString
            }
            json.isJsonObject -> {
                // Single object, wrap in array and convert to JSON string
                val gson = Gson()
                val array = JsonArray().apply { add(json) }
                val jsonString = gson.toJson(array)
                jsonString
            }
            else -> null
        }
    }
}

