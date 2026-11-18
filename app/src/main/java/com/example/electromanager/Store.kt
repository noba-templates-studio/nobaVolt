package com.example.electromanager

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda y carga documentos, inventario y ajustes en SharedPreferences como JSON.
 * Usa org.json (incluido en Android), sin dependencias extra.
 */
object Store {
    private const val PREFS = "electromanager"
    private const val KEY_DOCS = "documentos"
    private const val KEY_INV = "inventario"
    private const val KEY_AJUSTES = "ajustes"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Documentos ----------

    fun cargarDocumentos(ctx: Context): List<Documento> {
        val json = prefs(ctx).getString(KEY_DOCS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i -> arr.getJSONObject(i).toDocumento() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun guardarDocumentos(ctx: Context, docs: List<Documento>) {
        val arr = JSONArray()
        docs.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString(KEY_DOCS, arr.toString()).apply()
    }

    // ---------- Inventario ----------

    /** Si todavía no hay inventario guardado, devuelve el de ejemplo. */
    fun cargarInventario(ctx: Context): List<Articulo> {
        val json = prefs(ctx).getString(KEY_INV, null) ?: return inventarioInicial()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Articulo(
                    id = o.getLong("id"),
                    nombre = o.optString("nombre"),
                    precio = o.optDouble("precio", 0.0),
                    unidad = o.optString("unidad", "ud"),
                    categoria = Categoria.de(o.optString("categoria")),
                    stock = if (o.has("stock") && !o.isNull("stock")) o.getDouble("stock") else null
                )
            }
        } catch (e: Exception) {
            inventarioInicial()
        }
    }

    fun guardarInventario(ctx: Context, inventario: List<Articulo>) {
        val arr = JSONArray()
        inventario.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("nombre", a.nombre)
                put("precio", a.precio)
                put("unidad", a.unidad)
                put("categoria", a.categoria.name)
                if (a.stock != null) put("stock", a.stock)
            })
        }
        prefs(ctx).edit().putString(KEY_INV, arr.toString()).apply()
    }

    // ---------- Ajustes ----------

    fun cargarAjustes(ctx: Context): Ajustes {
        val json = prefs(ctx).getString(KEY_AJUSTES, null) ?: return Ajustes()
        return try {
            val o = JSONObject(json)
            val d = Ajustes()
            Ajustes(
                empresa = o.optString("empresa", d.empresa),
                nif = o.optString("nif"),
                telefono = o.optString("telefono"),
                email = o.optString("email"),
                direccion = o.optString("direccion"),
                ivaPct = o.optDouble("ivaPct", d.ivaPct),
                validezDias = o.optInt("validezDias", d.validezDias),
                umbralStock = o.optDouble("umbralStock", d.umbralStock),
                descontarStock = o.optBoolean("descontarStock", d.descontarStock),
                tema = o.optString("tema", d.tema),
                sonidoInicio = o.optBoolean("sonidoInicio", d.sonidoInicio)
            )
        } catch (e: Exception) {
            Ajustes()
        }
    }

    fun guardarAjustes(ctx: Context, a: Ajustes) {
        val o = JSONObject().apply {
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
        prefs(ctx).edit().putString(KEY_AJUSTES, o.toString()).apply()
    }

    // ---------- Copia de seguridad ----------

    /** Claves del antiguo asistente con IA: ya no se usan y no deben exportarse ni importarse. */
    private val CLAVES_ASISTENTE = listOf(
        "motor", "apiKey", "modelo", "ollamaUrl", "ollamaModelo", "modeloMovil", "descargaModeloId"
    )

    /** Borra de los ajustes guardados los datos del antiguo asistente (incluida la clave de API). */
    fun purgarAsistente(ctx: Context) {
        val p = prefs(ctx)
        val raw = p.getString(KEY_AJUSTES, null) ?: return
        try {
            val o = JSONObject(raw)
            if (CLAVES_ASISTENTE.none { o.has(it) }) return
            CLAVES_ASISTENTE.forEach { o.remove(it) }
            p.edit().putString(KEY_AJUSTES, o.toString()).apply()
        } catch (_: Exception) {
            // Ajustes ilegibles: cargarAjustes ya usa los valores por defecto
        }
    }

    /** Todos los datos en un único JSON. */
    fun exportar(ctx: Context): String {
        val p = prefs(ctx)
        val ajustes = JSONObject(p.getString(KEY_AJUSTES, null) ?: "{}").apply {
            CLAVES_ASISTENTE.forEach { remove(it) }
        }
        // Si el inventario aún no se ha guardado nunca, se exporta el de ejemplo
        val inventarioJson = p.getString(KEY_INV, null) ?: run {
            guardarInventario(ctx, inventarioInicial())
            p.getString(KEY_INV, null) ?: "[]"
        }
        return JSONObject()
            .put("app", "Noba Volt")
            .put("formato", 1)
            .put("fecha", hoy())
            .put("documentos", JSONArray(p.getString(KEY_DOCS, null) ?: "[]"))
            .put("inventario", JSONArray(inventarioJson))
            .put("ajustes", ajustes)
            .toString(2)
    }

    /** Restaura una copia creada con [exportar]. Lanza una excepción si el archivo no es válido. */
    fun importar(ctx: Context, texto: String) {
        val o = try { JSONObject(texto) } catch (e: Exception) { throw Exception("el archivo no es una copia válida") }
        val docs = o.optJSONArray("documentos") ?: throw Exception("al archivo le faltan los documentos")
        val inv = o.optJSONArray("inventario") ?: throw Exception("al archivo le falta el inventario")
        // Comprueba que todo se puede leer antes de sustituir nada
        for (i in 0 until docs.length()) docs.getJSONObject(i).toDocumento()
        for (i in 0 until inv.length()) inv.getJSONObject(i).getLong("id")

        val nuevos = o.optJSONObject("ajustes") ?: JSONObject()
        CLAVES_ASISTENTE.forEach { nuevos.remove(it) }
        prefs(ctx).edit()
            .putString(KEY_DOCS, docs.toString())
            .putString(KEY_INV, inv.toString())
            .putString(KEY_AJUSTES, nuevos.toString())
            .commit()
    }

    // ---------- Conversión JSON ----------

    private fun Documento.toJson() = JSONObject().apply {
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
        put("lineas", JSONArray().apply {
            lineas.forEach { l ->
                put(JSONObject().apply {
                    put("concepto", l.concepto)
                    put("cantidad", l.cantidad)
                    put("precio", l.precio)
                    put("unidad", l.unidad)
                    if (l.articuloId != null) put("articuloId", l.articuloId)
                })
            }
        })
    }

    private fun JSONObject.toDocumento(): Documento {
        val ls = optJSONArray("lineas") ?: JSONArray()
        return Documento(
            id = getLong("id"),
            tipo = optString("tipo", PRESUPUESTO),
            numero = optString("numero"),
            fecha = optString("fecha"),
            cliente = optString("cliente"),
            direccion = optString("direccion"),
            instalacion = optString("instalacion"),
            telefono = optString("telefono"),
            origenId = if (has("origenId")) getLong("origenId") else null,
            ivaPct = optDouble("ivaPct", 21.0),
            lineas = List(ls.length()) { i ->
                val o = ls.getJSONObject(i)
                Linea(
                    concepto = o.optString("concepto"),
                    cantidad = o.optDouble("cantidad", 0.0),
                    precio = o.optDouble("precio", 0.0),
                    unidad = o.optString("unidad", "ud"),
                    articuloId = if (o.has("articuloId")) o.getLong("articuloId") else null
                )
            }
        )
    }
}
