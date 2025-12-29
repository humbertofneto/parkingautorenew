package com.example.parkingautorenew

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var getInfoBtn: Button
    private lateinit var infoText: TextView
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getInfoBtn = findViewById(R.id.getInfoBtn)
        infoText = findViewById(R.id.infoText)

        getInfoBtn.setOnClickListener {
            infoText.text = "Loading page..."
            fetchPageInfo()
        }
    }

    private fun fetchPageInfo() {
        Handler(Looper.getMainLooper()).post {
            try {
                val wv = WebView(this)
                webView = wv
                wv.settings.javaScriptEnabled = true
                wv.settings.domStorageEnabled = true
                wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
                wv.addJavascriptInterface(PageBridge(), "Android")
                
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("MainActivity", "Page loaded: $url")
                    // Pequeno delay para conteúdo dinâmico
                    Handler(Looper.getMainLooper()).postDelayed({
                      extractPageInfo()
                    }, 1500)
                    }
                }
                
                wv.loadUrl("https://www.offstreet.io/location/LWLN9BUO")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error: ${e.message}", e)
                infoText.text = "Error: ${e.message}"
            }
        }
    }

    private fun extractPageInfo() {
        val script = """
            (function(){
              try {
                const inputs = Array.from(document.querySelectorAll('input'));
                const buttons = Array.from(document.querySelectorAll('button, a[role="button"], input[type="submit"], input[type="button"]'));
                const selects = Array.from(document.querySelectorAll('select'));
                
                const info = {
                  title: document.title,
                  url: window.location.href,
                  inputs: inputs.map(i => ({
                    type: i.type || 'text',
                    placeholder: i.placeholder || '',
                    name: i.name || '',
                    id: i.id || '',
                    value: i.value || ''
                  })),
                  buttons: buttons.map(b => ({
                    text: (b.innerText || b.value || b.textContent || '').trim(),
                    id: b.id || '',
                    className: b.className || ''
                  })),
                  selects: selects.map(s => ({
                    name: s.name || '',
                    id: s.id || '',
                    options: Array.from(s.options).map(o => o.text)
                  }))
                };
                
                if (typeof Android !== 'undefined' && Android.onPageInfo) {
                  Android.onPageInfo(JSON.stringify(info, null, 2));
                }
              } catch(e) {
                if (typeof Android !== 'undefined' && Android.onError) {
                  Android.onError(e.message || 'Unknown error');
                }
              }
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(script, null)
    }

    inner class PageBridge {
        @JavascriptInterface
        fun onPageInfo(json: String) {
            Log.d("PageBridge", "Received: $json")
            runOnUiThread {
                infoText.text = json
                webView?.destroy()
                webView = null
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            Log.e("PageBridge", "Error: $message")
            runOnUiThread {
                infoText.text = "Error: $message"
                webView?.destroy()
                webView = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        webView = null
    }
}
