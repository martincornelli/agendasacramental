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

    fun generarYCompartir(context: Context, agenda: Agenda) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = dibujarContenido(canvas, agenda, dateFormat)

        document.finishPage(page)

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
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si no hay lector de PDF, ofrecer compartir
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Abrir agenda PDF"))
        }
    }

    private fun dibujarContenido(canvas: Canvas, agenda: Agenda, dateFormat: SimpleDateFormat): Float {
        var y = 50f

        // --- Título principal ---
        val paintTitulo = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AGENDA REUNIÓN SACRAMENTAL", PAGE_WIDTH / 2f, y, paintTitulo)
        y += 16f

        // Subtítulo cursiva
        val paintSubtitulo = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Una Experiencia Espiritual", PAGE_WIDTH / 2f, y, paintSubtitulo)
        y += 8f

        // --- Asistencia arriba a la derecha ---
        val paintLabel = Paint().apply {
            color = Color.BLACK; textSize = 10f; isAntiAlias = true
        }
        val paintLinea = Paint().apply {
            color = Color.BLACK; strokeWidth = 0.5f
        }

        canvas.drawText("Asistencia", 440f, 50f, paintLabel)
        // Cuadro de asistencia
        val asistenciaVal = if (agenda.asistencia > 0) agenda.asistencia.toString() else ""
        canvas.drawRect(490f, 38f, 555f, 55f, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })
        if (asistenciaVal.isNotBlank()) {
            val paintVal = Paint().apply { color = Color.BLACK; textSize = 9f; isAntiAlias = true }
            canvas.drawText(asistenciaVal, 518f, 51f, paintVal.apply { textAlign = Paint.Align.CENTER })
        }

        // Línea separadora
        y += 4f
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += 12f

        // --- Campos principales ---
        fun campo(label: String, valor: String, x: Float, yPos: Float, width: Float) {
            canvas.drawText(label, x, yPos, paintLabel)
            val xValor = x + paintLabel.measureText(label) + 4f
            canvas.drawText(valor, xValor, yPos, paintLabel)
            canvas.drawLine(xValor, yPos + 2f, x + width, yPos + 2f, paintLinea)
        }

        // Fecha | Dirige
        campo("Fecha:", dateFormat.format(agenda.fecha.toDate()), MARGIN_LEFT, y, 240f)
        campo("Dirige:", agenda.dirige, 300f, y, 255f)
        y += 16f

        // Preside
        campo("Preside:", agenda.preside, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // Preludio
        canvas.drawText("Preludio  (10-15' antes del inicio de la reunión)", MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = 9f })
        y += 14f
        paintLabel.apply { color = Color.BLACK; textSize = 10f }

        // Bienvenida/reconocimientos
        val reconocimientosItems = agenda.reconocimientos.split(",").map { it.trim() }.filter { it.isNotBlank() }
        canvas.drawText("Bienvenida y reconocimiento de autoridades:", MARGIN_LEFT, y, paintLabel)
        y += 13f
        if (reconocimientosItems.isEmpty()) {
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += 14f
        } else {
            reconocimientosItems.forEach { item ->
                canvas.drawText("• $item", MARGIN_LEFT + 8f, y, paintLabel)
                y += 13f
            }
        }
        y += 6f

        // Anuncios
        canvas.drawText("Anuncios  (Solamente los más importantes y urgentes):", MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = 9f })
        y += 13f
        paintLabel.apply { color = Color.BLACK; textSize = 10f }
        val anunciosItems = agenda.anuncios.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (anunciosItems.isEmpty()) {
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += 12f
            canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
            y += 14f
        } else {
            anunciosItems.forEach { item ->
                canvas.drawText("• $item", MARGIN_LEFT + 8f, y, paintLabel)
                y += 13f
            }
            y += 6f
        }

        // Himno apertura
        val himnoAp = if (agenda.primerHimnoNumero > 0) "${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}" else ""
        campo("Himno de apertura:", himnoAp, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // Director/a | Pianista
        canvas.drawText("Director/a:", MARGIN_LEFT, y, paintLabel)
        val xDirVal = MARGIN_LEFT + paintLabel.measureText("Director/a:") + 4f
        canvas.drawText(agenda.directorMusica, xDirVal, y, paintLabel)
        canvas.drawLine(xDirVal, y + 2f, 270f, y + 2f, paintLinea)
        canvas.drawText("Pianista:", 290f, y, paintLabel)
        val xPiVal = 290f + paintLabel.measureText("Pianista:") + 4f
        canvas.drawText(agenda.pianista, xPiVal, y, paintLabel)
        canvas.drawLine(xPiVal, y + 2f, MARGIN_RIGHT, y + 2f, paintLinea)
        y += 16f

        // Primera oración
        campo("Primera oración:", agenda.primeraOracion, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // --- Asuntos: fórmulas litúrgicas ---
        val paintBold = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val paintTextoAsunto = Paint().apply { color = Color.BLACK; textSize = 9.5f; isAntiAlias = true }
        val paintLineaGris = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        // Si no hay asuntos, dejar espacio en blanco similar a tabla vacía
        if (agenda.asuntosEstacaBarrio.isEmpty()) {
            // Espacio equivalente a tabla vacía
            y += 6f
            canvas.drawText("Asuntos", MARGIN_LEFT, y, paintBold)
            y += 14f
            repeat(3) {
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLineaGris)
                y += 16f
            }
            y += 6f
        } else {
            val grupos = agenda.asuntosEstacaBarrio.groupBy { it.tipo }

            listOf(TipoAsunto.RELEVO, TipoAsunto.SOSTENIMIENTO).forEach { tipo ->
                val asuntosDelTipo = grupos[tipo] ?: return@forEach

                val tipoLabel = if (tipo == TipoAsunto.RELEVO) "RELEVO" else "SOSTENIMIENTO"
                canvas.drawText(tipoLabel, MARGIN_LEFT, y, paintBold)
                y += 13f

                val formula = generarFormulaLiturgica(tipo, asuntosDelTipo)
                // Wrap manual ~90 chars por línea
                val palabras = formula.split(" ")
                var lineaActual = ""
                palabras.forEach { palabra ->
                    val candidata = if (lineaActual.isEmpty()) palabra else "$lineaActual $palabra"
                    if (candidata.length > 88) {
                        canvas.drawText(lineaActual, MARGIN_LEFT + 8f, y, paintTextoAsunto)
                        y += 12f
                        lineaActual = palabra
                    } else {
                        lineaActual = candidata
                    }
                }
                if (lineaActual.isNotEmpty()) {
                    canvas.drawText(lineaActual, MARGIN_LEFT + 8f, y, paintTextoAsunto)
                    y += 12f
                }
                y += 8f
            }
        }

        // Margen antes del Himno Sacramental
        y += 8f

        // --- Himno Sacramental ---
        val himnoSac = if (agenda.himnoSacramentalNumero > 0) "${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}" else ""
        campo("Himno Sacramental:", himnoSac, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // Bendición y Reparto de la Santa Cena
        canvas.drawText("Bendición y Reparto de la Santa Cena", MARGIN_LEFT, y,
            Paint().apply { color = Color.GRAY; textSize = 8f; isAntiAlias = true; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC) })
        y += 20f

        // --- Mensajes del Evangelio ---
        canvas.drawText("Mensajes del evangelio, canto de la congregación y números musicales especiales", MARGIN_LEFT, y, paintBold)
        y += 8f

        // Caja grande para mensajes
        val msgBoxH = if (agenda.mensajesEvangelio.isEmpty()) 60f else (agenda.mensajesEvangelio.size * 16f + 16f).coerceAtLeast(60f)
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_RIGHT, y + msgBoxH, Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 0.5f })

        val paintMsg = Paint().apply { color = Color.BLACK; textSize = 9.5f; isAntiAlias = true }
        var msgY = y + 14f
        agenda.mensajesEvangelio.forEach { msg ->
            val texto = when (msg.tipo) {
                TipoMensaje.HIMNO_INTERMEDIO -> "🎵 Himno: ${msg.himnoNumero} - ${msg.himnoNombre}"
                TipoMensaje.TESTIMONIO -> "Testimonio: ${msg.nombre}"
                else -> "Discurso: ${msg.nombre}"
            }
            canvas.drawText(texto, MARGIN_LEFT + 6f, msgY, paintMsg)
            msgY += 16f
        }
        y += msgBoxH + 12f

        // --- Himno Final ---
        val himnoFin = if (agenda.himnoFinalNumero > 0) "${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}" else ""
        campo("Himno final:", himnoFin, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // Oración final
        campo("Oración final:", agenda.oracionFinal, MARGIN_LEFT, y, COL_WIDTH)
        y += 16f

        // Postludio
        canvas.drawText("Postludio  (10 minutos - sólo música)", MARGIN_LEFT, y, paintLabel.apply { color = Color.GRAY; textSize = 9f })
        y += 20f

        // Línea separadora
        paintLabel.apply { color = Color.BLACK; textSize = 9f }
        canvas.drawLine(MARGIN_LEFT, y, MARGIN_RIGHT, y, paintLinea)
        y += 12f

        // Cita D&C 46:2
        val paintCita = Paint().apply { color = Color.BLACK; textSize = 7.5f; isAntiAlias = true }
        val cita = "\"Pero a pesar de las cosas que están escritas, siempre se ha concedido a los élderes de mi iglesia desde el principio, y siempre será así, dirigir"
        val cita2 = "todas las reuniones conforme los oriente y los guíe el Santo Espíritu.\"  D y C 46:2"
        canvas.drawText(cita, MARGIN_LEFT, y, paintCita)
        y += 11f
        canvas.drawText(cita2, MARGIN_LEFT, y, paintCita)
        y += 14f

        // Área Sudamérica Sur
        val paintAreaRight = Paint().apply { color = Color.BLACK; textSize = 8f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("Área Sudamérica Sur", MARGIN_RIGHT, y, paintAreaRight)

        return y
    }

    private fun dibujarFormulasLiturgicas(canvas: Canvas, agenda: Agenda) {
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

        canvas.drawText("Fórmulas de Sostenimiento y Relevo", PAGE_WIDTH / 2f, y, paintTitulo)
        y += 8f
        canvas.drawText("Referencia para uso durante la reunión", PAGE_WIDTH / 2f, y,
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

            val tipoLabel = if (tipo == TipoAsunto.RELEVO) "RELEVO" else "SOSTENIMIENTO"
            canvas.drawText("${grupoIndex + 1}. $tipoLabel", MARGIN_LEFT, y, paintSeccion)
            y += 14f

            // Generar fórmula agrupada
            val formula = generarFormulaLiturgica(tipo, asuntosDelTipo)

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
            "Texto sugerido según el Manual General de Instrucciones de La Iglesia de Jesucristo de los Santos de los Últimos Días.",
            MARGIN_LEFT, PAGE_HEIGHT - 28f, paintGris
        )
    }
}