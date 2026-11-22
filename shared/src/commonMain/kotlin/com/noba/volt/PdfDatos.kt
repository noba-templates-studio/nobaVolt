package com.noba.volt

// Datos generados (no editar a mano): métricas de Helvetica (Adobe AFM, códigos WinAnsi 32-255)
// y la máscara de transparencia del logo de Noba para la marca de agua del PDF.

internal val ANCHOS_HELVETICA = intArrayOf(
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 350,
        556, 350, 222, 556, 333, 1000, 556, 556, 333, 1000, 667, 333, 1000, 350, 611, 350,
        350, 222, 222, 333, 333, 350, 556, 1000, 333, 1000, 500, 333, 944, 350, 500, 667,
        278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 556, 537, 278, 333, 333, 365, 556, 834, 834, 834, 611,
        667, 667, 667, 667, 667, 667, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278,
        722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 584, 611, 556, 556, 556, 556, 500, 556, 500,
)

internal val ANCHOS_HELVETICA_NEGRITA = intArrayOf(
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584, 350,
        556, 350, 278, 556, 500, 1000, 556, 556, 333, 1000, 667, 333, 1000, 350, 611, 350,
        350, 278, 278, 500, 500, 350, 556, 1000, 333, 1000, 556, 333, 944, 350, 500, 667,
        278, 333, 556, 556, 556, 556, 280, 556, 333, 737, 370, 556, 584, 333, 737, 333,
        400, 584, 333, 333, 333, 611, 556, 278, 333, 333, 365, 556, 834, 834, 834, 611,
        722, 722, 722, 722, 722, 722, 1000, 722, 667, 667, 667, 667, 278, 278, 278, 278,
        722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611,
        556, 556, 556, 556, 556, 556, 889, 556, 556, 556, 556, 556, 278, 278, 278, 278,
        611, 611, 611, 611, 611, 611, 611, 584, 611, 611, 611, 611, 611, 556, 611, 556,
)

internal const val LOGO_ANCHO = 336
internal const val LOGO_ALTO = 88

/** Canal alfa del logo (336x88, 8 bits, comprimido con Deflate) en Base64. */
internal const val LOGO_ALFA_B64 =
        "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"
