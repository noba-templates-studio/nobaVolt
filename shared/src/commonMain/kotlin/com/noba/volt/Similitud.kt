package com.noba.volt

/**
 * Búsqueda aproximada de materiales: ignora tildes, mayúsculas y palabras vacías,
 * admite palabras a medias ("magnetot" → "magnetotérmico") y exige que los números coincidan
 * ("cable 2.5" no encuentra "cable 1.5").
 */
object Similitud {

    private val VACIAS = setOf(
        "de", "del", "la", "el", "los", "las", "para", "con", "y", "a", "en", "un", "una", "unos", "unas",
        "ud", "uds", "unidad", "unidades", "metro", "metros", "x", "por"
    )

    /** Letras con tilde o diéresis y su equivalente sin ella (lo que antes hacía java.text.Normalizer). */
    private val SIN_TILDE: Map<Char, Char> = buildMap {
        val pares = listOf("áàâäã" to 'a', "éèêë" to 'e', "íìîï" to 'i', "óòôöõ" to 'o', "úùûü" to 'u', "ñ" to 'n', "ç" to 'c', "ý" to 'y')
        for ((letras, base) in pares) letras.forEach { put(it, base) }
        put('²', '2'); put('³', '3'); put('º', 'o'); put('ª', 'a')
    }

    fun normalizar(s: String): String {
        val n = s.lowercase().map { SIN_TILDE[it] ?: it }.joinToString("")
        return n.replace(Regex("(\\d),(\\d)"), "$1.$2")
            .replace(Regex("[^a-z0-9. ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun palabras(s: String) = s.split(' ').filter { it.isNotBlank() && it !in VACIAS }

    private fun numeros(s: String) = Regex("\\d+(?:\\.\\d+)?").findAll(s).map { it.value }.toSet()

    private fun palabraCoincide(a: String, b: String): Boolean =
        a == b || (a.length >= 4 && b.length >= 4 && (a.startsWith(b) || b.startsWith(a)))

    /** Puntuación de parecido entre lo que se busca y un nombre. 0 = nada que ver. */
    fun parecido(consulta: String, nombre: String): Int {
        val q = normalizar(consulta)
        val n = normalizar(nombre)
        if (q.isEmpty() || n.isEmpty()) return 0
        if (q == n) return 1000
        val qp = palabras(q)
        val np = palabras(n)
        val comunes = qp.count { p -> np.any { palabraCoincide(p, it) } }
        if (comunes == 0 && !n.contains(q)) return 0
        var puntos = comunes * 100 - (np.size - comunes) * 5 - (qp.size - comunes) * 30
        if (n.contains(q) || q.contains(n)) puntos += 150
        // Los números deben coincidir; se admite "16a" frente a "16 a"
        val qn = numeros(q)
        if (qn.isNotEmpty() && !numeros(n).containsAll(qn) && !numeros(n.replace(" ", "")).containsAll(qn)) {
            puntos -= 400
        }
        return puntos
    }
}
