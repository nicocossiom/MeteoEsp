package com.mssdepas.meteoesp.data.remote

import androidx.compose.ui.input.key.type
import com.google.gson.*
import java.lang.reflect.Type

class ListOrStringDeserializer : JsonDeserializer<List<String>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String>? {
        return json?.let {
            if (it.isJsonArray) {
                // If it's an array, deserialize it as a List<String>
                // We need to handle potential nulls if context is null, though unlikely in standard Gson setup
                val listType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, String::class.java).type
                context?.deserialize<List<String>>(it.asJsonArray, listType)
            } else if (it.isJsonPrimitive && it.asJsonPrimitive.isString) {
                // If it's a string, wrap it in a single-element list
                listOf(it.asString)
            } else if (it.isJsonObject && it.asJsonObject.entrySet().isEmpty()) {
                // Handle case like "prob_precipitacion": {} which might mean no data or 0%
                // Decide if this should be null, emptyList(), or perhaps listOf("0")
                emptyList() // Or null, depending on how you want to interpret an empty object
            }
            else {
                // Unknown type or null, return null or an empty list as appropriate
                null // Or emptyList()
            }
        }
    }
}

class RachaMaxDeserializer : JsonDeserializer<List<Periodo>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Periodo>? {
        return json?.let {
            if (it.isJsonArray) {
                val resultList = mutableListOf<Periodo>()
                it.asJsonArray.forEach { element ->
                    if (element.isJsonObject) {
                        val attributesObject = element.asJsonObject.getAsJsonObject("@attributes")
                        if (attributesObject != null && context != null) {
                            val periodo = context.deserialize<Periodo>(attributesObject, Periodo::class.java)
                            if (periodo != null) {
                                resultList.add(periodo)
                            }
                        }
                        // If there's no "@attributes" or it's not an object, we might skip it or add a default Periodo
                    }
                }
                resultList
            } else if (it.isJsonObject && it.asJsonObject.size() == 0) {
                // It's an empty object {}
                emptyList() // Or return null if that's more appropriate
            } else {
                // Unexpected type
                null
            }
        }
    }
}

class VientoListDeserializer : JsonDeserializer<List<VientoDia>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<VientoDia>? {
        return json?.let {
            if (it.isJsonArray) {
                // If it's an array, deserialize it as a List<VientoDia>
                val listType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, VientoDia::class.java).type
                context?.deserialize<List<VientoDia>>(it.asJsonArray, listType)
            } else if (it.isJsonObject) {
                // If it's a single object, deserialize it as a VientoDia and wrap it in a list
                val vientoDia = context?.deserialize<VientoDia>(it.asJsonObject, VientoDia::class.java)
                if (vientoDia != null) {
                    listOf(vientoDia)
                } else {
                    null
                }
            } else {
                null // Or emptyList()
            }
        }
    }
}