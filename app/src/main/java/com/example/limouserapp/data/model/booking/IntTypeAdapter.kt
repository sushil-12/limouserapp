package com.example.limouserapp.data.model.booking

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter for Int? that handles empty strings
 * Converts empty strings to null, allowing the API to send "" for optional Int fields
 * This matches the iOS implementation behavior
 */
class IntTypeAdapter : JsonDeserializer<Int?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Int? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                when {
                    primitive.isNumber -> primitive.asInt
                    primitive.isString -> {
                        val stringValue = primitive.asString.trim()
                        if (stringValue.isEmpty() || stringValue == "null") {
                            null
                        } else {
                            try {
                                stringValue.toInt()
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

