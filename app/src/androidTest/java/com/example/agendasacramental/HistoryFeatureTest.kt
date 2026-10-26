package com.example.agendasacramental

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HistoryFeatureTest {
    @Test fun nativePdfWrapsAndContinuesInBothLanguages() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        for (lang in listOf("es", "en")) {
            val context = LocaleHelper.aplicarIdiomaAContexto(base, lang)
            val quote = context.getString(R.string.tiempo_presentacion_primaria)
            assertTrue(quote.contains("29.8"))
            for (long in listOf(false, true)) {
                val text = if (lang == "es") "El sábado, a las 18:00, habrá una actividad. Traer agua, comida y abrigo. " else "On Saturday, at 6 pm, there will be an activity. Bring water, food and a coat. "
                val agenda = Agenda(anuncios = text.repeat(if (long) 160 else 6) + "\n" + "https://example.com/" + "abcdefghij".repeat(20), presentacionPrimaria = true)
                val pdf = GeneradorPDF.crearPdf(context, agenda)
                val output = File(base.cacheDir, "qa-$lang-${if (long) "long" else "normal"}.pdf")
                pdf.copyTo(output, overwrite = true)
                PdfRenderer(ParcelFileDescriptor.open(output, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                    if (long) assertTrue(renderer.pageCount > 1)
                    for (i in 0 until renderer.pageCount) renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        File(base.cacheDir, "qa-$lang-${if (long) "long" else "normal"}-$i.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        // No text may run into the last 20 points on the right edge.
                        for (y in 0 until page.height) for (x in (page.width - 20) until page.width) {
                            assertEquals("Right margin at $x,$y", android.graphics.Color.WHITE, bitmap.getPixel(x, y))
                        }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test fun bundledWebViewCanExportExcelOffline() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val exported = CountDownLatch(1)
        var bytes = byteArrayOf()
        var failure = ""
        lateinit var web: WebView
        val bridge = object {
            @JavascriptInterface fun failed(message: String) { failure = message; exported.countDown() }
            @JavascriptInterface fun request(json: String) {
                val request = JSONObject(json)
                val id = request.getInt("id")
                if (request.getString("action") == "load") instrumentation.runOnMainSync {
                    web.evaluateJavascript("window.nativeResult($id,{unit:'123456',revision:0,existing:[]},null); setTimeout(()=>{document.querySelector('#template').click();const timer=setInterval(()=>{const error=document.querySelector('#error')?.textContent;if(error){clearInterval(timer);AndroidHistory.failed(error);}},100);},100);", null)
                }
                if (request.getString("action") == "download") {
                    bytes = Base64.decode(request.getString("base64"), Base64.DEFAULT)
                    exported.countDown()
                }
            }
        }
        instrumentation.runOnMainSync {
            web = WebView(instrumentation.targetContext)
            configureHistoryWebView(web, bridge)
            web.loadUrl("https://appassets.androidplatform.net/history/index.html?lang=en")
        }
        try {
            assertTrue("Bundled worker must load offline and export an Excel workbook", exported.await(30, TimeUnit.SECONDS))
            assertEquals("", failure)
            assertTrue(bytes.size > 100)
            assertEquals('P'.code.toByte(), bytes[0]); assertEquals('K'.code.toByte(), bytes[1])
        } finally { instrumentation.runOnMainSync { web.destroy() } }
    }
}
