package peakchao.com.porn.network

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val DEFAULT_BASE_URL = "http://h1014.sol148.com"
    private const val PREFS_NAME = "host"
    private const val KEY_ADDRESS = "apiAddress"

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val host = url.host
                    cookieStore.getOrPut(host) { mutableListOf() }.apply {
                        // Replace existing cookies with same name
                        cookies.forEach { newCookie ->
                            removeAll { it.name == newCookie.name }
                            add(newCookie)
                        }
                    }
                    Log.d("CookieJar", "Saved ${cookies.size} cookies for $host: ${cookies.map { "${it.name}=${it.value.take(20)}" }}")
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val cookies = cookieStore[url.host] ?: emptyList()
                    return cookies
                }
            })
            .addInterceptor(headersInterceptor())
            .addInterceptor(loggingInterceptor())
            .build()
    }

    private fun headersInterceptor() = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("upgrade-insecure-requests", "1")
            .header("user-agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .header("sec-fetch-site", "none")
            .header("sec-fetch-mode", "navigate")
            .header("sec-fetch-user", "?1")
            .header("sec-fetch-dest", "document")
            .header("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()
        chain.proceed(request)
    }

    private fun loggingInterceptor() = Interceptor { chain ->
        val req = chain.request()
        Log.e("OkHttp", ">> ${req.method} ${req.url}")
        val resp = chain.proceed(req)
        Log.e("OkHttp", "<< ${resp.code} ${req.url} (${resp.body?.contentLength()} bytes)")
        resp
    }

    fun getBaseUrl(context: Context): String {
        val url = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        // Force HTTP — server rejects HTTPS
        return url.replace("https://", "http://")
    }

    fun setBaseUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ADDRESS, url).apply()
    }

    @Volatile
    private var cookieInitialized = false

    /**
     * Warm up cookies by visiting the site once.
     * The server requires ga/CLIPSHARE cookies to return correct category results.
     */
    fun ensureCookies(context: Context) {
        if (cookieInitialized) return
        synchronized(this) {
            if (cookieInitialized) return
            try {
                val base = getBaseUrl(context)
                val request = Request.Builder().url("$base/v.php?category=hot&viewtype=basic").build()
                client.newCall(request).execute().use { it.body?.string() }
                cookieInitialized = true
                Log.d("ApiClient", "Cookie warmup done, store: ${cookieStore.map { (k, v) -> "$k: ${v.map { c -> c.name }}" }}")
            } catch (e: Exception) {
                Log.e("ApiClient", "Cookie warmup failed", e)
            }
        }
    }

    fun fetchHtml(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }

    fun fetchVideoList(context: Context, category: String, page: Int): String {
        ensureCookies(context)
        val base = getBaseUrl(context)
        val url = "$base/v.php?category=$category&page=$page&viewtype=basic"
        return fetchHtml(url)
    }

    fun fetchVideoPage(context: Context, viewKey: String): String {
        val base = getBaseUrl(context)
        val url = "$base/view_video.php?viewkey=$viewKey"
        return fetchHtml(url)
    }
}
