package com.e16din.screensadapter.helpers

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

import kotlin.reflect.KClass

internal class KClassAdapter : TypeAdapter<KClass<*>>() {
    override fun read(reader: JsonReader): KClass<*> {
        var kclassName = ""

        reader.beginObject()

        while (reader.hasNext()) {
            val token = reader.peek()

            if (token.equals(JsonToken.NAME)) {
                //get the current token
                val name = reader.nextName()
                if (name == "KClassName") {
                    kclassName = reader.nextString()
                }
            }
        }
        reader.endObject()

        val kClass = Class.forName(kclassName).kotlin
        return kClass
    }

    override fun write(writer: JsonWriter, obj: KClass<*>) {
        writer.beginObject()
        writer.name("KClassName")
        writer.value(obj.qualifiedName)
        writer.endObject()
    }

}