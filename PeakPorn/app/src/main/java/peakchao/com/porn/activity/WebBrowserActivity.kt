package peakchao.com.porn.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import peakchao.com.porn.R
import peakchao.com.porn.network.ApiClient

class WebBrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var fullscreenContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // Ad domains to block (network level)
    private val adDomains = setOf(
        // Site-specific ad image servers
        "fans.91selfie.com", "91selfie.com",
        // Site-specific ad link domains
        "sp2026.com", "goko543.com", "ludu319.com", "hpv112.com",
        "hnijukj.cn", "shikaiqi.com", "315mt.com", "chqya.cn",
        // Generic ad networks
        "googleads", "googlesyndication", "doubleclick", "google-analytics",
        "facebook.com/tr", "fbcdn.net",
        "adservice", "adsense", "adnxs", "adsrvr", "adform",
        "pubmatic", "rubiconproject", "openx", "criteo",
        "taboola", "outbrain", "mgid", "revcontent",
        "popads", "popcash", "propellerads", "trafficjunky",
        "juicyads", "exoclick", "exosrv", "ero-advertising",
        "tsyndicate", "a-ads", "ad-maven", "admaven",
        "clickadu", "clickaine", "hilltopads",
        "track.", "tracker.", "tracking.",
        "counter.", "pixel.", "beacon.",
    )

    // CSS to hide ad elements — targets actual page structure
    private val adBlockCss = """
        .cont6, .ad_img, .top-nav,
        #search_form, .form-inline,
        #footer-container, .footer-container, .footer-links, .footer-row,
        .gotop,
        img.ad_img,
        div[style*="z-index: 9999"], div[style*="z-index:9999"],
        div[style*="z-index: 99999"], div[style*="z-index:99999"],
        div[style*="position: fixed"]:not(.navbar),
        iframe,
        [class*="popup"], [id*="popup"],
        [class*="overlay"]:not(.thumb-overlay), [id*="overlay"]
        { display: none !important; visibility: hidden !important; height: 0 !important; max-height: 0 !important; overflow: hidden !important; padding: 0 !important; margin: 0 !important; }

        .container-minheight { padding-top: 0 !important; margin-top: 0 !important; }
        body { padding-top: 0 !important; margin-top: 0 !important; }
        video { max-width: 100% !important; }
    """.trimIndent().replace("\n", " ")

    // JS to remove ad elements and block popups
    private val adBlockJs = """
        (function() {
            window.open = function() { return null; };

            // Remove site-specific ad containers
            var selectors = [
                '.cont6', '.ad_img', '.top-nav',
                '#search_form', '.form-inline',
                '#footer-container', '.footer-container', '.footer-links',
                '.gotop', '.footer-row',
                'iframe',
                'img[src*="91selfie"]',
                'a[href*="sp2026"]', 'a[href*="goko543"]', 'a[href*="ludu319"]',
                'a[href*="hpv112"]', 'a[href*="hnijukj"]', 'a[href*="shikaiqi"]',
                'a[href*="315mt"]', 'a[href*="chqya"]',
                'script[src*="rotator"]',
                '[class*="popup"]', '[id*="popup"]',
                'div[style*="z-index: 9999"]', 'div[style*="z-index:9999"]',
                'div[style*="z-index: 99999"]'
            ];
            selectors.forEach(function(sel) {
                try {
                    document.querySelectorAll(sel).forEach(function(el) {
                        el.remove();
                    });
                } catch(e) {}
            });

            // Remove the ad wrapper div (div align=center containing .cont6 ads)
            document.querySelectorAll('div[align="center"]').forEach(function(el) {
                // Only remove if it contains ad images, not video content
                if (el.querySelector('.ad_img') || el.querySelector('img[src*="91selfie"]') || el.querySelector('.cont6')) {
                    el.remove();
                }
            });

            // Remove fixed positioned overlays (but keep navbar)
            document.querySelectorAll('div').forEach(function(el) {
                if (el.classList.contains('navbar') || el.closest('.navbar')) return;
                var style = window.getComputedStyle(el);
                if (style.position === 'fixed') {
                    el.remove();
                }
            });

            // Remove navbar links to external ad sites (keep internal sol148 links)
            document.querySelectorAll('.navbar a[target="blank"], .navbar a[target="_blank"]').forEach(function(el) {
                var href = el.getAttribute('href') || '';
                if (!href.includes('sol148.com')) {
                    var li = el.closest('li');
                    if (li) li.remove(); else el.remove();
                }
            });

            // Block onclick handlers that open new windows
            document.addEventListener('click', function(e) {
                var target = e.target.closest('a');
                if (target && target.hostname && target.hostname !== window.location.hostname) {
                    var allowed = ['sol148.com'];
                    var isAllowed = allowed.some(function(d) { return target.hostname.indexOf(d) !== -1; });
                    if (!isAllowed) {
                        e.preventDefault();
                        e.stopPropagation();
                    }
                }
            }, true);
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_browser)

        originalOrientation = requestedOrientation
        progressBar = findViewById(R.id.progress_bar)
        fullscreenContainer = findViewById(R.id.fullscreen_container)
        webView = findViewById(R.id.web_view)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            if (webView.canGoBack()) webView.goBack() else finish()
        }

        setupWebView()

        val baseUrl = ApiClient.getBaseUrl(this).replace("https://", "http://")
        webView.loadUrl("$baseUrl/v.php?category=hot&page=1&viewtype=basic")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString().lowercase()
                if (isAdUrl(url)) {
                    Log.d("AdBlock", "Blocked: $url")
                    return WebResourceResponse("text/plain", "utf-8", "".byteInputStream())
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                var url = request.url.toString()
                val host = request.url.host ?: ""
                // Allow same-site navigation, force HTTP
                if (host.contains("sol148.com")) {
                    if (url.startsWith("https://")) {
                        view.loadUrl(url.replace("https://", "http://"))
                        return true
                    }
                    return false
                }
                // Block external redirects
                Log.d("AdBlock", "Blocked navigation: $url")
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                // Inject ad blocking CSS
                val cssJs = "var style=document.createElement('style');style.textContent='$adBlockCss';document.head.appendChild(style);"
                view.evaluateJavascript(cssJs, null)
                // Inject ad blocking JS
                view.evaluateJavascript(adBlockJs, null)
                // Run again after a delay to catch dynamically loaded ads
                view.postDelayed({
                    view.evaluateJavascript(adBlockJs, null)
                }, 2000)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress >= 100) progressBar.visibility = View.GONE
            }

            // Fullscreen video support
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback

                webView.visibility = View.GONE
                fullscreenContainer.addView(view, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ))
                fullscreenContainer.visibility = View.VISIBLE

                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }

            override fun onHideCustomView() {
                if (customView == null) return

                fullscreenContainer.removeView(customView)
                fullscreenContainer.visibility = View.GONE
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                webView.visibility = View.VISIBLE
                requestedOrientation = originalOrientation
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }

            // Block popup windows
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                return false
            }
        }
    }

    private fun isAdUrl(url: String): Boolean {
        return adDomains.any { url.contains(it) }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (customView != null) {
            webView.webChromeClient?.onHideCustomView()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
