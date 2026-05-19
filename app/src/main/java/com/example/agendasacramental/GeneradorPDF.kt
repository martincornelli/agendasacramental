package com.example.agendasacramental

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object GeneradorPDF {

    private const val PAGE_WIDTH = 595   // A4 en puntos (72dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 555f
    private const val COL_WIDTH = PAGE_WIDTH - 80f
    private val FONT_SCALES = listOf(1.18f, 1.14f, 1.10f, 1.06f, 1.02f, 1.0f, 0.96f, 0.92f)

    private fun Canvas?.drawText(text: String, x: Float, y: Float, paint: Paint) {
        this?.drawText(text, x, y, paint)
    }

    private fun Canvas?.drawLine(startX: Float, startY: Float, stopX: Float, stopY: Float, paint: Paint) {
        this?.drawLine(startX, startY, stopX, stopY, paint)
    }

    private fun Canvas?.drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        this?.drawRect(left, top, right, bottom, paint)
    }

    fun generarYCompartir(context: Context, agenda: Agenda) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fontScale = seleccionarEscala(agenda, dateFormat, context)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        dibujarContenido(canvas, agenda, dateFormat, context, fontScale)

        document.finishPage(page)

        if (agenda.reunionTestimonios) {
            val pageInfoTestimonios = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
            val pageTestimonios = document.startPage(pageInfoTestimonios)
            dibujarPaginaTestimonios(pageTestimonios.canvas, agenda, dateFormat, context)
            document.finishPage(pageTestimonios)
        }

        // Guardar en cache
        val fileName = "agenda_${dateFormat.format(agenda.fecha.toDate()).replace("/", "-")}.pdf"
        val file = File(context.cacheDir, fileName)
        document.writeTo(FileOutputStream(file))
        document.close()

        // Abrir en lector de PDF (el usuario puede compartir desde ahí)
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.editar_exportar_pdf)))
        }
    }

    private fun seleccionarEscala(agenda: Agenda, dateFormat: SimpleDateFormat, context: Context): Float {
        return FONT_SCALES.firstOrNull { scale ->
            dibujarContenido(null, agenda, dateFormat, context, scale) <= PAGE_HEIGHT - 24f
        } ?: FONT_SCALES.last()
    }

    private fun dibujarContenido(canvas: Canvas?, agenda: Agenda, dateFormat: SimpleDateFormat, context: Context, fontScale: Float): Float {
        fun sp(value: Float) = value * fontScale
        var y = 50f

        // --- Título principal ---
        val paintTitulo = Paint().apply {
            color = Color.BLACK
            textSize = sp(14f)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.pdf_titulo), PAGE_WIDTH / 2f, y, paintTitulo)
        y += sp(16f)

        // Subtítulo cursiva
        val paintSubtitulo = Paint().apply {
            color = Color.BLACK
            textSize = sp(10f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(context.getString(R.string.pdf_subtitulo), PAGE_WIDTH / 2f, y, paintSubtitulo)
        y += sp(8f)

        // --- Asistencia arriba a la derecha ---
        val paintLabel = Paint().apply {
            color = Color.BLACK; textSize = sp(10f); isAntiAlias = true
        }
        val paintLinea = Paint().apply {
            color = Color.BLACK; strokeWidth = 0.5f
        }

        val paintAsistencia = Paint(paintLabel).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(context.getString(R.string.pdf_asistencia), 484f, 50f, paintAsistencia)
        // Cuadro de asistencia
        val asistenciaVal = if (agenda.asistencia > 0) agenda.asistencia.toString() else ""
        canvas.drawRect(490f, 38f, 555f, 55f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })
        if (asistenciaVal.isNotBlank()) {
            val paintVal = Paint().apply { color = Color.BLACK; textSize = sp(9f); isAntiAlias = true }
            canvas.drawText(asistenciaVal, 518f, 51f, paintVal.apply { textAlign = Paint.Align.CENTER })
        }

        // Línea separadora
        y += sp(4f)
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += sp(12f)

        // --- Campos principales ---
        fun campo(label: String, valor: String, x: Float, yPos: Float, width: Float) {
            canvas.drawText(label, x, yPos, paintLabel)
            val xValor = x + paintLabel.measureText(label) + 4f
            canvas.drawText(valor, xValor, yPos, paintLabel)
            canvas.drawLine(xValor, yPos + 2f, x + width, yPos + 2f, paintLinea)
        }

        // Fecha | Dirige
        campo(context.getString(R.string.pdf_fecha), dateFormat.format(agenda.fecha.toDate()), MARGIN_LEFT, y, 240f)
        campo(context.getString(R.string.pdf_dirige), agenda.dirige, 300f, y, 255f)
        y += sp(16f)

        // Preside
        campo(context.getString(R.string.pdf_preside), agenda.preside, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // Preludio
        canvas.drawText(context.getString(R.string.pdf_preludio), MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = sp(9f) })
        y += sp(14f)
        paintLabel.apply { color = Color.BLACK; textSize = sp(10f) }

        // Bienvenida/reconocimientos
        val reconocimientosItems = agenda.reconocimientos.split(",").map { it.trim() }.filter { it.isNotBlank() }
        canvas.drawText(context.getString(R.string.pdf_bienvenida), MARGIN_LEFT, y, paintLabel)
        y += sp(13f)
        if (reconocimientosItems.isEmpty()) {
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += sp(14f)
        } else {
            reconocimientosItems.forEach { item ->
                canvas.drawText("• $item", MARGIN_LEFT + 8f, y, paintLabel)
                y += sp(13f)
            }
        }
        y += sp(6f)

        // Anuncios
        canvas.drawText(context.getString(R.string.pdf_anuncios), MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = sp(9f) })
        y += sp(13f)
        paintLabel.apply { color = Color.BLACK; textSize = sp(10f) }
        val anunciosItems = agenda.anuncios.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (anunciosItems.isEmpty()) {
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += sp(12f)
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += sp(14f)
        } else {
            anunciosItems.forEach { item ->
                canvas.drawText("• $item", MARGIN_LEFT + 8f, y, paintLabel)
                y += sp(13f)
            }
            y += sp(6f)
        }

        // Himno apertura
        val himnoAp = if (agenda.primerHimnoNumero > 0) "${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}" else ""
        campo(context.getString(R.string.pdf_himno_apertura), himnoAp, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // Director/a | Pianista
        canvas.drawText(context.getString(R.string.pdf_director), MARGIN_LEFT, y, paintLabel)
        val xDirVal = MARGIN_LEFT + paintLabel.measureText(context.getString(R.string.pdf_director)) + 4f
        canvas.drawText(agenda.directorMusica, xDirVal, y, paintLabel)
        canvas.drawLine(xDirVal, y + 2f, 270f, y + 2f, paintLinea)
        canvas.drawText(context.getString(R.string.pdf_pianista), 290f, y, paintLabel)
        val xPiVal = 290f + paintLabel.measureText(context.getString(R.string.pdf_pianista)) + 4f
        canvas.drawText(agenda.pianista, xPiVal, y, paintLabel)
        canvas.drawLine(xPiVal, y + 2f, MARGIN_RIGHT, y + 2f, paintLinea)
        y += sp(16f)

        // Primera oración
        campo(context.getString(R.string.pdf_primera_oracion), agenda.primeraOracion, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // --- Asuntos: fórmulas litúrgicas ---
        val paintBold = Paint().apply { color = Color.BLACK; textSize = sp(10f); isFakeBoldText = true; isAntiAlias = true }
        val paintTextoAsunto = Paint().apply { color = Color.BLACK; textSize = sp(9.5f); isAntiAlias = true }
        val paintLineaGris = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        // Si no hay asuntos, dejar espacio en blanco similar a tabla vacía
        if (agenda.asuntosEstacaBarrio.isEmpty()) {
            // Espacio equivalente a tabla vacía
            y += sp(6f)
            canvas.drawText(context.getString(R.string.lectura_asuntos), MARGIN_LEFT, y, paintBold)
            y += sp(14f)
            repeat(3) {
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLineaGris)
                y += sp(16f)
            }
            y += sp(6f)
        } else {
            val grupos = agenda.asuntosEstacaBarrio.groupBy { it.tipo }

            listOf(TipoAsunto.RELEVO, TipoAsunto.SOSTENIMIENTO, TipoAsunto.OTROS).forEach { tipo ->
                val asuntosDelTipo = grupos[tipo] ?: return@forEach
                if (tipo == TipoAsunto.OTROS && asuntosDelTipo.all { it.columna2.isBlank() }) return@forEach

                val tipoLabel = when (tipo) {
                    TipoAsunto.RELEVO -> context.getString(R.string.formula_relevo)
                    TipoAsunto.SOSTENIMIENTO -> context.getString(R.string.formula_sostenimiento)
                    TipoAsunto.OTROS -> context.getString(R.string.formula_otros)
                }
                canvas.drawText(tipoLabel, MARGIN_LEFT, y, paintBold)
                y += sp(13f)

                if (tipo == TipoAsunto.OTROS) {
                    asuntosDelTipo.filter { it.columna2.isNotBlank() }.forEachIndexed { index, asunto ->
                        if (index > 0) y += sp(4f)
                        y = dibujarTextoAjustado(
                            canvas = canvas,
                            texto = asunto.columna2,
                            x = MARGIN_LEFT + 8f,
                            yInicial = y,
                            anchoMaximo = COL_WIDTH - 8f,
                            paint = paintTextoAsunto,
                            altoLinea = sp(12f)
                        )
                    }
                } else {
                    val formula = generarFormulaLiturgica(tipo, asuntosDelTipo, context)
                    y = dibujarTextoAjustado(
                        canvas = canvas,
                        texto = formula,
                        x = MARGIN_LEFT + 8f,
                        yInicial = y,
                        anchoMaximo = COL_WIDTH - 8f,
                        paint = paintTextoAsunto,
                        altoLinea = sp(12f)
                    )
                }
                y += sp(8f)
            }
        }

        // Margen antes del Himno Sacramental
        y += sp(8f)

        // --- Himno Sacramental ---
        val himnoSac = if (agenda.himnoSacramentalNumero > 0) "${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}" else ""
        campo(context.getString(R.string.pdf_himno_sacramental), himnoSac, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // Bendición y Reparto de la Santa Cena
        canvas.drawText(context.getString(R.string.pdf_bendicion), MARGIN_LEFT, y,
            Paint().apply { color = Color.GRAY; textSize = sp(8f); isAntiAlias = true; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC) })
        y += sp(20f)

        // --- Mensajes del Evangelio ---
        canvas.drawText(context.getString(R.string.pdf_mensajes), MARGIN_LEFT, y, paintBold)
        y += sp(8f)

        // Caja grande para mensajes
        val msgBoxH = (if (agenda.mensajesEvangelio.isEmpty()) 60f else (agenda.mensajesEvangelio.size * 16f + 16f).coerceAtLeast(60f)) * fontScale
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + msgBoxH, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })

        val paintMsg = Paint().apply { color = Color.BLACK; textSize = sp(9.5f); isAntiAlias = true }
        var msgY = y + sp(14f)
        agenda.mensajesEvangelio.forEach { msg ->
            val texto = when (msg.tipo) {
                TipoMensaje.HIMNO_INTERMEDIO -> "🎵 Himno: ${msg.himnoNumero} - ${msg.himnoNombre}"
                TipoMensaje.TESTIMONIO -> "Testimonio: ${msg.nombre}"
                else -> "${context.getString(R.string.lectura_discurso)}: ${msg.nombre}"
            }
            canvas.drawText(texto, MARGIN_LEFT + 6f, msgY, paintMsg)
            msgY += sp(16f)
        }
        y += msgBoxH + sp(12f)

        // --- Himno Final ---
        val himnoFin = if (agenda.himnoFinalNumero > 0) "${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}" else ""
        campo(context.getString(R.string.pdf_himno_final), himnoFin, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // Oración final
        campo(context.getString(R.string.pdf_oracion_final), agenda.oracionFinal, MARGIN_LEFT, y, COL_WIDTH)
        y += sp(16f)

        // Postludio
        canvas.drawText(context.getString(R.string.pdf_postludio), MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = sp(9f) })
        y += sp(20f)

        // Línea separadora
        paintLabel.apply { color = Color.BLACK; textSize = sp(9f) }
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += sp(12f)

        // Cita D&C 46:2
        val paintCita = Paint().apply { color = Color.BLACK; textSize = sp(7.5f); isAntiAlias = true }
        val citaCompleta = context.getString(R.string.pdf_cita)
        // Split at ~half for two-line display
        val splitIdx = citaCompleta.lastIndexOf(' ', citaCompleta.length / 2 + 20)
            .takeIf { it > 0 } ?: (citaCompleta.length / 2)
        val cita = citaCompleta.take(splitIdx)
        val cita2 = citaCompleta.drop(splitIdx).trim()
        canvas.drawText(cita, MARGIN_LEFT, y, paintCita)
        y += sp(11f)
        canvas.drawText(cita2, MARGIN_LEFT, y, paintCita)
        y += sp(14f)

        // Área Sudamérica Sur — solo en español
        val lang = context.resources.configuration.locales[0].language
        if (lang != "en") {
            val paintAreaRight = Paint().apply { color = Color.BLACK; textSize = sp(8f); isAntiAlias = true; textAlign = Paint.Align.RIGHT }
            canvas.drawText(context.getString(R.string.pdf_area), MARGIN_RIGHT, y, paintAreaRight)
        }

        return y
    }

    private fun dibujarPaginaTestimonios(
        canvas: Canvas,
        agenda: Agenda,
        dateFormat: SimpleDateFormat,
        context: Context
    ) {
        var y = 64f
        val paintTitulo = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val paintFecha = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val paintNumero = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val paintNombre = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val paintLinea = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.7f
        }

        canvas.drawText(context.getString(R.string.pdf_reunion_testimonios).uppercase(), PAGE_WIDTH / 2f, y, paintTitulo)
        y += 20f
        canvas.drawText(dateFormat.format(agenda.fecha.toDate()), PAGE_WIDTH / 2f, y, paintFecha)
        y += 24f
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += 34f

        val nombres = agenda.testimonios.filter { it.isNotBlank() }
        val cantidadNecesaria = maxOf(28, nombres.size)
        val altoLinea = ((PAGE_HEIGHT - y - 48f) / cantidadNecesaria).coerceIn(16f, 22f)
        val lineasDisponibles = ((PAGE_HEIGHT - y - 36f) / altoLinea).toInt()
        val cantidadLineas = cantidadNecesaria.coerceAtMost(lineasDisponibles)

        repeat(cantidadLineas) { index ->
            val numero = "${index + 1}."
            val nombre = nombres.getOrNull(index).orEmpty()
            val baseline = y + (altoLinea * 0.55f)
            canvas.drawText(numero, MARGIN_LEFT + 20f, baseline, paintNumero)
            if (nombre.isNotBlank()) {
                canvas.drawText(nombre, MARGIN_LEFT + 34f, baseline, paintNombre)
            }
            canvas.drawLine(MARGIN_LEFT + 34f, baseline + 3f, MARGIN_RIGHT, baseline + 3f, paintLinea)
            y += altoLinea
        }
    }

    private fun dibujarTextoAjustado(
        canvas: Canvas?,
        texto: String,
        x: Float,
        yInicial: Float,
        anchoMaximo: Float,
        paint: Paint,
        altoLinea: Float
    ): Float {
        var y = yInicial
        val lineasOriginales = texto.replace("\r\n", "\n").replace('\r', '\n').split('\n')

        lineasOriginales.forEach { lineaOriginal ->
            if (lineaOriginal.isBlank()) {
                y += altoLinea
                return@forEach
            }

            var restante = lineaOriginal
            while (paint.measureText(restante) > anchoMaximo && restante.length > 1) {
                var corte = restante.indices.firstOrNull { index ->
                    index > 0 && paint.measureText(restante.substring(0, index + 1)) > anchoMaximo
                } ?: restante.lastIndex

                val corteEnEspacio = restante.lastIndexOf(' ', corte).takeIf { it > 0 }
                    ?: restante.lastIndexOf('\t', corte).takeIf { it > 0 }
                if (corteEnEspacio != null) {
                    corte = corteEnEspacio + 1
                }

                canvas.drawText(restante.take(corte).trimEnd(), x, y, paint)
                y += altoLinea
                restante = restante.drop(corte).trimStart()
            }

            if (restante.isNotEmpty()) {
                canvas.drawText(restante, x, y, paint)
                y += altoLinea
            }
        }

        return y
    }

    private fun dibujarFormulasLiturgicas(canvas: Canvas, agenda: Agenda, context: Context) {
        var y = 50f
        val paintTitulo = Paint().apply {
            color = Color.BLACK; textSize = 13f; isFakeBoldText = true
            textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        val paintSeccion = Paint().apply {
            color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true
        }
        val paintTexto = Paint().apply {
            color = Color.BLACK; textSize = 9f; isAntiAlias = true
        }
        val paintGris = Paint().apply {
            color = Color.GRAY; textSize = 8f; isAntiAlias = true
        }
        val paintLinea = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        canvas.drawText(context.getString(R.string.pdf_formulas_titulo), PAGE_WIDTH / 2f, y, paintTitulo)
        y += 8f
        canvas.drawText(context.getString(R.string.pdf_formulas_subtitulo), PAGE_WIDTH / 2f, y,
            Paint().apply { color = Color.GRAY; textSize = 8f; textAlign = Paint.Align.CENTER; isAntiAlias = true })
        y += 6f
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += 16f

        // Agrupar por tipo — Relevos primero, luego Sostenimientos
        val grupos = agenda.asuntosEstacaBarrio.groupBy { it.tipo }
        var grupoIndex = 0

        listOf(TipoAsunto.RELEVO, TipoAsunto.SOSTENIMIENTO).forEach { tipo ->
            val asuntosDelTipo = grupos[tipo] ?: return@forEach
            if (y > PAGE_HEIGHT - 80f) return@forEach

            val tipoLabel = if (tipo == TipoAsunto.RELEVO) context.getString(R.string.formula_relevo) else context.getString(R.string.formula_sostenimiento)
            canvas.drawText("${grupoIndex + 1}. $tipoLabel", MARGIN_LEFT, y, paintSeccion)
            y += 14f

            // Generar fórmula agrupada
            val formula = generarFormulaLiturgica(tipo, asuntosDelTipo, context)

            // Dibujar línea por línea (wrap manual cada ~90 chars)
            val palabras = formula.split(" ")
            var lineaActual = ""
            palabras.forEach { palabra ->
                val candidata = if (lineaActual.isEmpty()) palabra else "$lineaActual $palabra"
                if (candidata.length > 90) {
                    canvas.drawText(lineaActual, MARGIN_LEFT + 12f, y, paintTexto)
                    y += 13f
                    lineaActual = palabra
                } else {
                    lineaActual = candidata
                }
            }
            if (lineaActual.isNotEmpty()) {
                canvas.drawText(lineaActual, MARGIN_LEFT + 12f, y, paintTexto)
                y += 13f
            }

            y += 6f

            if (grupoIndex < grupos.size - 1) {
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
                y += 10f
            }
            grupoIndex++
        }

        // Nota al pie
        canvas.drawLine(MARGIN_LEFT, PAGE_HEIGHT - 40f, MARGIN_RIGHT, PAGE_HEIGHT - 40f, paintLinea)
        canvas.drawText(
            context.getString(R.string.pdf_formulas_nota),
            MARGIN_LEFT, PAGE_HEIGHT - 28f, paintGris
        )
    }
}
