package peakchao.com.porn.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import peakchao.com.porn.network.ApiClient
import peakchao.com.porn.parser.HtmlParser
import peakchao.com.porn.model.PornEvaluator
import kotlin.concurrent.thread

class VideoUrlResolver(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    interface Callback {
        fun onResolved(videoUrl: String, evaluators: List<PornEvaluator>)
        fun onError(error: String)
    }

    fun resolve(viewKey: String, callback: Callback) {
        thread {
            try {
                ApiClient.ensureCookies(context)
                val html = ApiClient.fetchVideoPage(context, viewKey)
                Log.d("VideoUrlResolver", "Fetched page len=${html.length}")

                val videoUrl = HtmlParser.parseVideoUrl(html)
                Log.d("VideoUrlResolver", "Parsed videoUrl: $videoUrl")

                val evaluators = HtmlParser.parseEvaluators(html)

                handler.post {
                    if (videoUrl.isNotEmpty()) {
                        callback.onResolved(videoUrl, evaluators)
                    } else {
                        callback.onError("无法解析视频地址")
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoUrlResolver", "Resolve failed", e)
                handler.post {
                    callback.onError("加载失败: ${e.message}")
                }
            }
        }
    }

    fun destroy() {
        // No WebView to destroy anymore
    }
}
