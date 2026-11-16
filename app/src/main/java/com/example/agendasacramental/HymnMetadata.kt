package com.example.agendasacramental

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
    val sacramental: Boolean,
    val temas: List<String> = emptyList()
)

fun metadataHimno(numero: Int, nombre: String, cambios: Map<String, List<String>> = emptyMap(), idioma: String = idiomaHimno(numero, nombre)): MetadataHimno {
    val temas = temasEfectivosHimno(numero, idioma, cambios)
    val normalizados = temas.map(::normalizarNombre).toSet()
    val categorias = CategoriaHimno.entries.filter { categoria ->
        categoria.aliases.any { normalizarNombre(it) in normalizados }
    }.toSet()
    // Topic edits do not alter sacrament-slot suitability.
    val sacramental = (if (idioma == "en") numero in 169..196 else numero in 101..120) ||
        numero in setOf(1007, 1008, 1009, 1015, 1016, 1017, 1041, 1043, 1072, 1206, 1210)
    return MetadataHimno(if (sacramental) categorias + CategoriaHimno.SACRAMENTO else categorias, numero >= 1000, sacramental, temas)
}

fun temasDeAgenda(agenda: Agenda?): List<String> = agenda?.mensajesEvangelio.orEmpty()
    .filter { it.tipo == TipoMensaje.DISCURSO }
    .flatMap { etiquetasTemaDesdeTexto(it.etiquetaTema, it.tema) }
    .distinctBy { normalizarNombre(it) }

fun coincidenciasTematicas(metadata: MetadataHimno, temas: List<String>): Int {
    if (temas.isEmpty()) return 0
    val temasNormalizados = temas.map(::normalizarNombre)
    return metadata.temas.count { etiqueta ->
        val normalizado = normalizarNombre(etiqueta)
        temasNormalizados.any { tema -> tema.isNotEmpty() && (tema.contains(normalizado) || normalizado.contains(tema)) }
    }
}
