package com.a5starcompany.topwisemp35p.emvreader

// To parse the JSON, install Klaxon and do:
//
//   val printjson = Printjson.fromJson(jsonString)


import com.beust.klaxon.*

private fun <T> Klaxon.convert(k: kotlin.reflect.KClass<*>, fromJson: (JsonValue) -> T, toJson: (T) -> String, isUnion: Boolean = false) =
    this.converter(object: Converter {
        @Suppress("UNCHECKED_CAST")
        override fun toJson(value: Any)        = toJson(value as T)
        override fun fromJson(jv: JsonValue)   = fromJson(jv) as Any
        override fun canConvert(cls: Class<*>) = cls == k.java || (isUnion && cls.superclass == k.java)
    })

private val klaxon = Klaxon()
    .convert(Data::class, { Data.fromJson(it) }, { it.toJson() }, true)

class Printjson(elements: Collection<PrintjsonElement>) : ArrayList<PrintjsonElement>(elements) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = Printjson(klaxon.parseArray<PrintjsonElement>(json)!!)
    }
}

data class PrintjsonElement (
    val data: Data
)

sealed class Data {
    class DatumArrayValue(val value: List<Datume>) : Data()
    class StringValue(val value: String)          : Data()

    public fun toJson(): String = klaxon.toJsonString(when (this) {
        is DatumArrayValue -> this.value
        is StringValue     -> this.value
        else -> {}
    })

    companion object {
        public fun fromJson(jv: JsonValue): Data = when (jv.inside) {
            is JsonArray<*> -> DatumArrayValue(jv.array?.let { klaxon.parseFromJsonArray<Datume>(it) }!!)
            is String       -> StringValue(jv.string!!)
            else            -> throw IllegalArgumentException()
        }
    }
}

data class Datume (
    val data: String
)
