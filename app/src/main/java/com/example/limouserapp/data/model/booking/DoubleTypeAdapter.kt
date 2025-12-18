package com.example.limouserapp.data.model.booking

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter for Double? that handles empty strings
 * Converts empty strings to null, allowing the API to send "" for optional Double fields
 * This matches the iOS implementation behavior
 */
class DoubleTypeAdapter : JsonDeserializer<Double?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Double? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                when {
                    primitive.isNumber -> primitive.asDouble
                    primitive.isString -> {
                        val stringValue = primitive.asString.trim()
                        if (stringValue.isEmpty() || stringValue == "null") {
                            null
                        } else {
                            try {
                                stringValue.toDouble()
                            } catch (e: NumberFormatException) {
                                null
                            }
                        }
                    }
                    else -> null
                }
            }
            else -> null
        }
    }
}

