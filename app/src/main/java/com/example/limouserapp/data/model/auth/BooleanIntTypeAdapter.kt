package com.example.limouserapp.data.model.auth

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter to handle fields that can be either Boolean or Int (1/0)
 * This matches the iOS implementation behavior exactly
 */
class BooleanIntTypeAdapter : JsonDeserializer<Boolean?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean? {
        return when {
            json == null || json.isJsonNull -> null
            json.isJsonPrimitive -> {
                val primitive = json.asJsonPrimitive
                when {
                    primitive.isBoolean -> primitive.asBoolean
                    primitive.isNumber -> primitive.asInt == 1
                    else -> null
                }
            }
            else -> null
        }
    }
}
