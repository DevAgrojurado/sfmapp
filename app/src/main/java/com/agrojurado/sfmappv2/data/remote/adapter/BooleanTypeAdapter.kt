package com.agrojurado.sfmappv2.data.remote.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class BooleanTypeAdapter : TypeAdapter<Boolean>() {
    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value)
    }

    override fun read(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            com.google.gson.stream.JsonToken.NUMBER -> reader.nextInt() == 1
            com.google.gson.stream.JsonToken.BOOLEAN -> reader.nextBoolean()
            com.google.gson.stream.JsonToken.STRING -> reader.nextString().toLowerCase() == "true" || reader.nextString() == "1"
            else -> false
        }
    }
}