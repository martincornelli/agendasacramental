package com.example.agendasacramental

import java.util.concurrent.ConcurrentHashMap

enum class CategoriaHimno(
    val stringResId: Int,
    val aliases: Set<String>
) {
    ALABANZA(R.string.hymn_category_praise, setOf("alabanza", "adoracion", "gloria", "praise", "worship", "glory")),
    JESUCRISTO(R.string.hymn_category_jesus_christ, setOf("jesucristo", "cristo", "jesus", "salvador", "redentor", "christ", "savior", "redeemer")),
    SACRAMENTO(R.string.hymn_category_sacrament, setOf("sacramento", "santa cena", "expiacion", "cruz", "sacrament", "atonement", "cross")),
    FE(R.string.hymn_category_faith, setOf("fe", "testimonio", "confianza", "faith", "testimony", "trust")),
    ORACION(R.string.hymn_category_prayer, setOf("oracion", "orar", "plegaria", "prayer", "pray")),
    ESPIRITU_SANTO(R.string.hymn_category_holy_ghost, setOf("espiritu santo", "espiritu", "holy ghost", "holy spirit")),
    GRATITUD(R.string.hymn_category_gratitude, setOf("gratitud", "gracias", "bendiciones", "gratitude", "thanks", "blessings")),
    SERVICIO(R.string.hymn_category_service, setOf("servicio", "servir", "obra", "ministrar", "service", "serve", "minister")),
    FAMILIA(R.string.hymn_category_family, setOf("familia", "hogar", "padres", "hijos", "family", "home", "parents", "children")),
    TEMPLO(R.string.hymn_category_temple, setOf("templo", "convenios", "temple", "covenants")),
    ESCRITURAS(R.string.hymn_category_scriptures, setOf("escrituras", "libro de mormon", "palabra de dios", "scriptures", "book of mormon", "word of god")),
    RESTAURACION(R.string.hymn_category_restoration, setOf("restauracion", "jose smith", "profeta", "restoration", "joseph smith", "prophet")),
    OBRA_MISIONAL(R.string.hymn_category_missionary, setOf("obra misional", "mision", "predicar", "missionary", "mission", "preach")),
    NAVIDAD(R.string.hymn_category_christmas, setOf("navidad", "belen", "pesebre", "nacio", "christmas", "bethlehem", "manger", "born")),
    PASCUA(R.string.hymn_category_easter, setOf("pascua", "resurreccion", "resucitado", "easter", "resurrection", "risen")),
    PAZ(R.string.hymn_category_peace, setOf("paz", "consuelo", "peace", "comfort")),
    AMOR(R.string.hymn_category_love, setOf("amor", "caridad", "love", "charity")),
    MANDAMIENTOS(R.string.hymn_category_commandments, setOf("mandamientos", "obediencia", "obedecer", "commandments", "obedience", "obey")),
    SION(R.string.hymn_category_zion, setOf("sion", "santos", "zion", "saints"))
}

data class MetadataHimno(
    val categorias: Set<CategoriaHimno>,
    val himnarioNuevo: Boolean,
    val sacramental: Boolean
)

private data class ClaveMetadataHimno(val numero: Int, val nombre: String)

private val cacheMetadataHimnos = ConcurrentHashMap<ClaveMetadataHimno, MetadataHimno>()

fun metadataHimno(numero: Int, nombre: String): MetadataHimno =
    cacheMetadataHimnos.getOrPut(ClaveMetadataHimno(numero, nombre)) {
        calcularMetadataHimno(numero, nombre)
    }

private fun calcularMetadataHimno(numero: Int, nombre: String): MetadataHimno {
    val categorias = linkedSetOf<CategoriaHimno>()
    val nombreNormalizado = normalizarNombre(nombre)
    val coincideCatalogoEspanol = Himnos.lista[numero]
        ?.let { normalizarNombre(it) == nombreNormalizado } == true
    val coincideCatalogoIngles = HimnosEn.himnos[numero]
        ?.let { normalizarNombre(it) == nombreNormalizado } == true

    if (coincideCatalogoEspanol) {
        when (numero) {
            in 1..29 -> categorias += setOf(CategoriaHimno.RESTAURACION, CategoriaHimno.SION)
            in 30..47 -> categorias += CategoriaHimno.ALABANZA
            in 48..82 -> categorias += setOf(CategoriaHimno.FE, CategoriaHimno.ORACION)
            in 83..101 -> categorias += CategoriaHimno.ALABANZA
            in 102..120 -> categorias += setOf(CategoriaHimno.SACRAMENTO, CategoriaHimno.JESUCRISTO)
            in 121..122 -> categorias += setOf(CategoriaHimno.PASCUA, CategoriaHimno.JESUCRISTO)
            in 123..134 -> categorias += setOf(CategoriaHimno.NAVIDAD, CategoriaHimno.JESUCRISTO)
            in 135..160 -> categorias += setOf(CategoriaHimno.SERVICIO, CategoriaHimno.GRATITUD)
            in 161..180 -> categorias += setOf(CategoriaHimno.OBRA_MISIONAL, CategoriaHimno.ESCRITURAS)
            in 181..186 -> categorias += CategoriaHimno.TEMPLO
            in 187..204 -> categorias += setOf(CategoriaHimno.FAMILIA, CategoriaHimno.AMOR)
            in 205..209 -> categorias += CategoriaHimno.SERVICIO
        }
    }

    if (coincideCatalogoIngles) {
        when (numero) {
            in 1..61 -> categorias += setOf(CategoriaHimno.RESTAURACION, CategoriaHimno.SION)
            in 62..96 -> categorias += CategoriaHimno.ALABANZA
            in 97..168 -> categorias += setOf(CategoriaHimno.FE, CategoriaHimno.ORACION)
            in 169..196 -> categorias += setOf(CategoriaHimno.SACRAMENTO, CategoriaHimno.JESUCRISTO)
            in 197..200 -> categorias += setOf(CategoriaHimno.PASCUA, CategoriaHimno.JESUCRISTO)
            in 201..215 -> categorias += setOf(CategoriaHimno.NAVIDAD, CategoriaHimno.JESUCRISTO)
            in 216..252 -> categorias += setOf(CategoriaHimno.SERVICIO, CategoriaHimno.GRATITUD)
            in 253..280 -> categorias += setOf(CategoriaHimno.OBRA_MISIONAL, CategoriaHimno.ESCRITURAS)
            in 281..291 -> categorias += CategoriaHimno.TEMPLO
            in 292..318 -> categorias += setOf(CategoriaHimno.FAMILIA, CategoriaHimno.AMOR)
            in 319..337 -> categorias += CategoriaHimno.SERVICIO
        }
    }

    CategoriaHimno.entries.forEach { categoria ->
        if (categoria.aliases.any { nombreNormalizado.contains(normalizarNombre(it)) }) {
            categorias += categoria
        }
    }

    if (numero >= 1000 && categorias.isEmpty()) {
        categorias += setOf(CategoriaHimno.JESUCRISTO, CategoriaHimno.FE)
    }

    val sacramental = CategoriaHimno.SACRAMENTO in categorias ||
            (coincideCatalogoEspanol && numero == 101) || numero in setOf(
        1007, 1008, 1009, 1015, 1016, 1017, 1041, 1043, 1072, 1206, 1210
    )
    if (sacramental) categorias += setOf(CategoriaHimno.SACRAMENTO, CategoriaHimno.JESUCRISTO)

    return MetadataHimno(
        categorias = categorias,
        himnarioNuevo = numero.toString().length == 4,
        sacramental = sacramental
    )
}

fun temasDeAgenda(agenda: Agenda?): List<String> = agenda?.mensajesEvangelio.orEmpty()
    .filter { it.tipo == TipoMensaje.DISCURSO }
    .flatMap { etiquetasTemaDesdeTexto(it.etiquetaTema, it.tema) }
    .distinctBy { normalizarNombre(it) }

fun coincidenciasTematicas(metadata: MetadataHimno, temas: List<String>): Int {
    if (temas.isEmpty()) return 0
    val temasNormalizados = temas.map(::normalizarNombre)
    return metadata.categorias.count { categoria ->
        val terminos = categoria.aliases + categoria.name.replace('_', ' ')
        terminos.any { termino ->
            val normalizado = normalizarNombre(termino)
            temasNormalizados.any { tema -> tema.contains(normalizado) || normalizado.contains(tema) }
        }
    }
}
