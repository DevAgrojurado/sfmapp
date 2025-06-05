package com.agrojurado.sfmappv2.data.remote.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonToken
import android.util.Log

class BooleanTypeAdapter : TypeAdapter<Boolean>() {

    // Escribe un valor Boolean en el JSON
    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value) // Escribe el valor como true, false o null
    }

    // Lee un valor del JSON y lo convierte a Boolean
    override fun read(reader: JsonReader): Boolean {
        when (reader.peek()) {
            // Caso 1: El valor es un String (por ejemplo, "1", "0", "true", "false")
            JsonToken.STRING -> {
                val value = reader.nextString().lowercase() // Lee el String y lo convierte a minúsculas
                return when (value) {
                    "true", "1" -> true // Mapea "true" o "1" a true
                    "false", "0" -> false // Mapea "false" o "0" a false
                    else -> {
                        Log.w("BooleanTypeAdapter", "Valor String inesperado: $value")
                        false // Valor por defecto para valores no reconocidos
                    }
                }
            }
            // Caso 2: El valor es un número (por si la API cambia a 1 o 0 sin comillas)
            JsonToken.NUMBER -> {
                val number = reader.nextInt()
                return number == 1 // Mapea 1 a true, cualquier otro número a false
            }
            // Caso 3: El valor es un Boolean directo (por compatibilidad)
            JsonToken.BOOLEAN -> {
                return reader.nextBoolean() // Lee el valor Boolean directamente
            }
            // Caso 4: El valor es null
            JsonToken.NULL -> {
                reader.nextNull() // Consume el null
                return false // Valor por defecto para null
            }
            // Caso 5: Token inesperado (por ejemplo, objeto {}, array [], etc.)
            else -> {
                Log.w("BooleanTypeAdapter", "Token inesperado: ${reader.peek()}")
                reader.skipValue() // Salta el token para evitar desalinear el parser
                return false // Valor por defecto
            }
        }
    }
}