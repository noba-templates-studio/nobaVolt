package com.noba.volt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Guarda y carga documentos, inventario y ajustes como JSON en el almacenamiento de la plataforma
 * (SharedPreferences en Android, NSUserDefaults en iOS).
 * El formato es el mismo que el de la versión solo-Android: las copias de seguridad son compatibles.
 */
class Store(private val p: Plataforma) {

    private companion object {
        const val KEY_DOCS = "documentos"
        const val KEY_INV = "inventario"
        const val KEY_AJUSTES = "ajustes"

        /** Claves del antiguo asistente con IA: ya no se usan y no deben exportarse ni importarse. */
        val CLAVES_ASISTENTE = listOf(
            "motor", "apiKey", "modelo", "ollamaUrl", "ollamaModelo", "modeloMovil", "descargaModeloId"
        )

        val json = Json { ignoreUnknownKeys = true }
        val jsonBonito = Json { prettyPrint = true }
    }

    // ---------- Documentos ----------

    fun cargarDocumentos(): List<Documento> {
        val texto = p.leer(KEY_DOCS) ?: return emptyList()
        return try {
            json.parseToJsonElement(texto).jsonArray.map { it.jsonObject.aDocumento() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardarDocumentos(docs: List<Documento>) {
        p.guardar(mapOf(KEY_DOCS to JsonArray(docs.map { it.aJson() }).toString()))
    }

    // ---------- Inventario ----------

    /** Si todavía no hay inventario guardado, devuelve el de ejemplo. */
    fun cargarInventario(): List<Articulo> {
        val texto = p.leer(KEY_INV) ?: return inventarioInicial()
        return try {
            json.parseToJsonElement(texto).jsonArray.map { it.jsonObject.aArticulo() }
        } catch (e: Exception) {
            inventarioInicial()
        }
    }

    fun guardarInventario(inventario: List<Articulo>) {
        p.guardar(mapOf(KEY_INV to inventarioJson(inventario).toString()))
    }

    private fun inventarioJson(inventario: List<Articulo>) = buildJsonArray {
        inventario.forEach { a ->
            add(buildJsonObject {
                put("id", a.id)
                put("nombre", a.nombre)
                put("precio", a.precio)
                put("unidad", a.unidad)
                put("categoria", a.categoria.name)
                if (a.stock != null) put("stock", a.stock)
            })
        }
    }

    // ---------- Ajustes ----------

    fun cargarAjustes(): Ajustes {
        val texto = p.leer(KEY_AJUSTES) ?: return Ajustes()
        return try {
            val o = json.parseToJsonElement(texto).jsonObject
            val d = Ajustes()
            Ajustes(
                empresa = o.texto("empresa", d.empresa),
                nif = o.texto("nif"),
                telefono = o.texto("telefono"),
                email = o.texto("email"),
                direccion = o.texto("direccion"),
                ivaPct = o.decimal("ivaPct", d.ivaPct),
                validezDias = o.entero("validezDias", d.validezDias),
                umbralStock = o.decimal("umbralStock", d.umbralStock),
                descontarStock = o.booleano("descontarStock", d.descontarStock),
                tema = o.texto("tema", d.tema),
                sonidoInicio = o.booleano("sonidoInicio", d.sonidoInicio)
            )
        } catch (e: Exception) {
            Ajustes()
        }
    }

    fun guardarAjustes(a: Ajustes) {
        val o = buildJsonObject {
            put("empresa", a.empresa)
            put("nif", a.nif)
            put("telefono", a.telefono)
            put("email", a.email)
            put("direccion", a.direccion)
            put("ivaPct", a.ivaPct)
            put("validezDias", a.validezDias)
            put("umbralStock", a.umbralStock)
            put("descontarStock", a.descontarStock)
            put("tema", a.tema)
            put("sonidoInicio", a.sonidoInicio)
        }
        p.guardar(mapOf(KEY_AJUSTES to o.toString()))
    }

    /** Borra de los ajustes guardados los datos del antiguo asistente (incluida la clave de API). */
    fun purgarAsistente() {
        val texto = p.leer(KEY_AJUSTES) ?: return
        try {
            val o = json.parseToJsonElement(texto).jsonObject
            if (CLAVES_ASISTENTE.none { it in o }) return
            p.guardar(mapOf(KEY_AJUSTES to JsonObject(o - CLAVES_ASISTENTE.toSet()).toString()))
        } catch (_: Exception) {
            // Ajustes ilegibles: cargarAjustes ya usa los valores por defecto
        }
    }

    // ---------- Copia de seguridad ----------

    /** Todos los datos en un único JSON. */
    fun exportar(): String {
        val ajustes = try {
            JsonObject(json.parseToJsonElement(p.leer(KEY_AJUSTES) ?: "{}").jsonObject - CLAVES_ASISTENTE.toSet())
        } catch (e: Exception) {
            JsonObject(emptyMap())
        }
        val docs = try {
            json.parseToJsonElement(p.leer(KEY_DOCS) ?: "[]")
        } catch (e: Exception) {
            JsonArray(emptyList())
        }
        // Si el inventario aún no se ha guardado nunca, se exporta el de ejemplo
        val inventario = p.leer(KEY_INV)?.let {
            try { json.parseToJsonElement(it) } catch (e: Exception) { null }
        } ?: inventarioJson(inventarioInicial())

        val o = buildJsonObject {
            put("app", "Noba Volt")
            put("formato", 1)
            put("fecha", hoy())
            put("documentos", docs)
            put("inventario", inventario)
            put("ajustes", ajustes)
        }
        return jsonBonito.encodeToString(JsonElement.serializer(), o)
    }

    /** Restaura una copia creada con [exportar]. Lanza una excepción si el archivo no es válido. */
    fun importar(texto: String) {
        val o = try {
            json.parseToJsonElement(texto).jsonObject
        } catch (e: Exception) {
            throw Exception("el archivo no es una copia válida")
        }
        val docs = (o["documentos"] as? JsonArray) ?: throw Exception("al archivo le faltan los documentos")
        val inv = (o["inventario"] as? JsonArray) ?: throw Exception("al archivo le falta el inventario")
        // Comprueba que todo se puede leer antes de sustituir nada
        docs.forEach { it.jsonObject.aDocumento() }
        inv.forEach { it.jsonObject.aArticulo() }

        val ajustes = (o["ajustes"] as? JsonObject) ?: JsonObject(emptyMap())
        p.guardar(
            mapOf(
                KEY_DOCS to docs.toString(),
                KEY_INV to inv.toString(),
                KEY_AJUSTES to JsonObject(ajustes - CLAVES_ASISTENTE.toSet()).toString()
            )
        )
    }

    // ---------- Conversión JSON ----------

    private fun Documento.aJson() = buildJsonObject {
        put("id", id)
        put("tipo", tipo)
        put("numero", numero)
        put("fecha", fecha)
        put("cliente", cliente)
        put("telefono", telefono)
        put("direccion", direccion)
        put("instalacion", instalacion)
        put("ivaPct", ivaPct)
        if (origenId != null) put("origenId", origenId)
        put("lineas", buildJsonArray {
            lineas.forEach { l ->
                add(buildJsonObject {
                    put("concepto", l.concepto)
                    put("cantidad", l.cantidad)
                    put("precio", l.precio)
                    put("unidad", l.unidad)
                    if (l.articuloId != null) put("articuloId", l.articuloId)
                })
            }
        })
    }

    private fun JsonObject.aDocumento(): Documento {
        val ls = (this["lineas"] as? JsonArray) ?: JsonArray(emptyList())
        return Documento(
            id = largo("id") ?: throw Exception("documento sin id"),
            tipo = texto("tipo", PRESUPUESTO),
            numero = texto("numero"),
            fecha = texto("fecha"),
            cliente = texto("cliente"),
            direccion = texto("direccion"),
            instalacion = texto("instalacion"),
            telefono = texto("telefono"),
            origenId = largo("origenId"),
            ivaPct = decimal("ivaPct", 21.0),
            lineas = ls.map {
                val o = it.jsonObject
                Linea(
                    concepto = o.texto("concepto"),
                    cantidad = o.decimal("cantidad", 0.0),
                    precio = o.decimal("precio", 0.0),
                    unidad = o.texto("unidad", "ud"),
                    articuloId = o.largo("articuloId")
                )
            }
        )
    }

    private fun JsonObject.aArticulo() = Articulo(
        id = largo("id") ?: throw Exception("artículo sin id"),
        nombre = texto("nombre"),
        precio = decimal("precio", 0.0),
        unidad = texto("unidad", "ud"),
        categoria = Categoria.de(texto("categoria")),
        stock = (this["stock"] as? JsonPrimitive)?.doubleOrNull
    )

    // Lectura tolerante, como optString/optDouble de org.json
    private fun JsonObject.primitivo(k: String): JsonPrimitive? =
        (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }

    private fun JsonObject.texto(k: String, def: String = ""): String = primitivo(k)?.contentOrNull ?: def
    private fun JsonObject.decimal(k: String, def: Double): Double = primitivo(k)?.doubleOrNull ?: def
    private fun JsonObject.entero(k: String, def: Int): Int =
        primitivo(k)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() } ?: def
    private fun JsonObject.largo(k: String): Long? =
        primitivo(k)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
    private fun JsonObject.booleano(k: String, def: Boolean): Boolean =
        primitivo(k)?.let { it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull() } ?: def
}
