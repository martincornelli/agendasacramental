package com.example.agendasacramental

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Offline bundled UI; only this bridge accesses the already selected unit. */
class HistorialImportActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var unit: String
    private val repository = HistorialRepository()
    private var loaded: String? = null
    private var pickerId: Int? = null
    private var downloadId: Int? = null
    private var downloadBytes: ByteArray? = null
    private var saving = false
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pickerId ?: return@registerForActivityResult
        pickerId = null
        val uri = result.data?.data
        if (result.resultCode != RESULT_OK || uri == null) reply(id, null)
        else lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else "history.csv"
                    } ?: "history.csv"
                    val bytes = contentResolver.openInputStream(uri)!!.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val size = input.read(buffer)
                            if (size < 0) break
                            require(output.size() + size <= 2 * 1024 * 1024) { "Maximum 2 MB / Máximo 2 MB" }
                            output.write(buffer, 0, size)
                        }
                        output.toByteArray()
                    }
                    JSONObject().put("name", name).put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                }
                reply(id, file)
            } catch (e: Exception) { fail(id, e) }
        }
    }
    private val downloader = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = downloadId ?: return@registerForActivityResult
        val bytes = downloadBytes
        downloadId = null
        downloadBytes = null
        val uri = result.data?.data
        if (result.resultCode != RESULT_OK || uri == null || bytes == null) reply(id, null)
        else lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { contentResolver.openOutputStream(uri, "wt")!!.use { it.write(bytes) } }
                reply(id, true)
            } catch (e: Exception) { fail(id, e) }
        }
    }

    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(LocaleHelper.aplicarIdioma(newBase))

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        unit = intent.getStringExtra("numeroUnidad").orEmpty()
        if (!unit.matches(Regex("[0-9]+")) || FirebaseAuth.getInstance().currentUser == null) { finish(); return }
        web = WebView(this)
        configureHistoryWebView(web, Bridge())
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val padding = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            insets
        }
        container.addView(Button(this).apply { text = getString(R.string.history_back); setOnClickListener { finish() } })
        container.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(container)
        val lang = if (resources.configuration.locales[0].language == "en") "en" else "es"
        web.loadUrl("https://appassets.androidplatform.net/history/index.html?lang=$lang")
    }


    private inner class Bridge {
        @JavascriptInterface fun request(json: String) {
            if (json.length > 12 * 1024 * 1024) return
            val request = runCatching { JSONObject(json) }.getOrNull() ?: return
            val id = request.optInt("id")
            runOnUiThread {
                lifecycleScope.launch {
                    try {
                        when (request.getString("action")) {
                            "load" -> {
                                val result = repository.payload(unit)
                                loaded = result.getJSONArray("existing").toString()
                                reply(id, result)
                            }
                            "pick" -> {
                                check(pickerId == null)
                                pickerId = id
                                picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain", "text/tab-separated-values", "application/csv", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                })
                            }
                            "download" -> {
                                check(downloadId == null)
                                val filename = request.getString("filename")
                                require(filename.matches(Regex("[a-zA-Z0-9_-]+\\.(csv|xlsx)")))
                                downloadBytes = Base64.decode(request.getString("base64"), Base64.DEFAULT)
                                downloadId = id
                                downloader.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = if (filename.endsWith(".csv")) "text/csv" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_TITLE, filename)
                                })
                            }
                            "save" -> {
                                check(!saving)
                                saving = true
                                try {
                                    val latest = repository.payload(unit)
                                    check(loaded != null && latest.getJSONArray("existing").toString() == loaded) { getString(R.string.history_changed) }
                                    repository.guardar(unit, request.getLong("revision"), request.getJSONArray("changes"))
                                    setResult(RESULT_OK)
                                    reply(id, true)
                                } finally { saving = false }
                            }
                            else -> error("Unsupported operation")
                        }
                    } catch (e: Exception) { fail(id, e) }
                }
            }
        }
    }
    private fun reply(id: Int, value: Any?, error: String? = null) {
        if (!isDestroyed && ::web.isInitialized) web.evaluateJavascript(
            "window.nativeResult($id,${value ?: "null"},${error?.let { JSONObject.quote(it) } ?: "null"});", null)
    }
    private fun fail(id: Int, e: Exception) = reply(id, null, e.message ?: getString(R.string.history_error))
    override fun onDestroy() {
        if (::web.isInitialized) { web.removeJavascriptInterface("AndroidHistory"); web.destroy() }
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal fun configureHistoryWebView(web: WebView, bridge: Any) {
    fun blocked() = WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(byteArrayOf()))
    web.settings.apply {
        javaScriptEnabled = true
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        setSupportMultipleWindows(false)
    }
    web.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
            val uri = request?.url
            val path = uri?.path.orEmpty()
            if (uri?.scheme != "https" || uri.host != "appassets.androidplatform.net" ||
                !path.startsWith("/history/") || path.contains("..")) return blocked()
            return try {
                val mime = when (path.substringAfterLast('.')) {
                    "js", "mjs" -> "application/javascript"
                    "css" -> "text/css"
                    "html" -> "text/html"
                    else -> "application/octet-stream"
                }
                WebResourceResponse(mime, "UTF-8", web.context.assets.open(path.removePrefix("/")))
            } catch (_: Exception) { blocked() }
        }
    }
    web.addJavascriptInterface(bridge, "AndroidHistory")
}
