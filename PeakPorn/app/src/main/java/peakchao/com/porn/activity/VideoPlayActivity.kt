package peakchao.com.porn.activity

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import peakchao.com.porn.R
import peakchao.com.porn.adapter.EvaluatorAdapter
import peakchao.com.porn.model.PornEvaluator
import peakchao.com.porn.model.PornModel
import peakchao.com.porn.player.VideoUrlResolver

class VideoPlayActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private lateinit var tvLoading: TextView
    private lateinit var evaluatorTitle: TextView
    private lateinit var rlvEvaluator: RecyclerView
    private lateinit var loadingOverlay: LinearLayout
    private lateinit var bufferProgress: ProgressBar
    private lateinit var tvBufferPercent: TextView
    private lateinit var playerContainer: FrameLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var rootContainer: FrameLayout
    private val evaluatorAdapter = EvaluatorAdapter()
    private var videoModel: PornModel? = null
    private var resolver: VideoUrlResolver? = null
    private var isCollected = false
    private var isFullscreen = false
    private val handler = Handler(Looper.getMainLooper())

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_play)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rootContainer = findViewById(R.id.root_container)
        contentLayout = findViewById(R.id.content_layout)
        playerContainer = findViewById(R.id.player_container)
        playerView = findViewById(R.id.player_view)
        tvTitle = findViewById(R.id.tv_title)
        tvLoading = findViewById(R.id.tv_loading)
        evaluatorTitle = findViewById(R.id.evaluator_title)
        rlvEvaluator = findViewById(R.id.rlv_evaluator)
        loadingOverlay = findViewById(R.id.loading_overlay)
        bufferProgress = findViewById(R.id.buffer_progress)
        tvBufferPercent = findViewById(R.id.tv_buffer_percent)

        rlvEvaluator.layoutManager = LinearLayoutManager(this)
        rlvEvaluator.adapter = evaluatorAdapter

        // Use PlayerView's built-in fullscreen button
        playerView.setFullscreenButtonClickListener { isEnteringFullscreen ->
            if (isEnteringFullscreen) enterFullscreen() else exitFullscreen()
        }

        videoModel = intent.getSerializableExtra("videoInfo") as? PornModel
        val model = videoModel ?: run { finish(); return }

        title = model.title
        tvTitle.text = model.title
        isCollected = isInCollection(model)

        loadingOverlay.visibility = View.VISIBLE
        bufferProgress.isIndeterminate = true
        tvBufferPercent.text = "解析中..."

        resolver = VideoUrlResolver(this)
        resolver?.resolve(model.viewKey ?: "", object : VideoUrlResolver.Callback {
            override fun onResolved(videoUrl: String, evaluators: List<PornEvaluator>) {
                tvLoading.visibility = View.GONE
                bufferProgress.isIndeterminate = false
                tvBufferPercent.text = "缓冲中 0%"
                playVideo(videoUrl)
                if (evaluators.isNotEmpty()) {
                    evaluatorTitle.visibility = View.VISIBLE
                    evaluatorAdapter.setData(evaluators)
                }
            }

            override fun onError(error: String) {
                tvLoading.text = error
                loadingOverlay.visibility = View.GONE
            }
        })
    }

    private fun playVideo(url: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> loadingOverlay.visibility = View.GONE
                        Player.STATE_BUFFERING -> {
                            if (loadingOverlay.visibility != View.VISIBLE) {
                                loadingOverlay.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            })

            val progressRunnable = object : Runnable {
                override fun run() {
                    if (player == null) return
                    val duration = exo.duration
                    val buffered = exo.bufferedPosition
                    if (duration > 0 && loadingOverlay.visibility == View.VISIBLE) {
                        val percent = (buffered * 100 / duration).toInt().coerceIn(0, 100)
                        bufferProgress.progress = percent
                        tvBufferPercent.text = "缓冲中 $percent%"
                    }
                    handler.postDelayed(this, 200)
                }
            }
            handler.post(progressRunnable)

            exo.prepare()
            exo.playWhenReady = true
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        supportActionBar?.hide()

        // Move playerView to root fullscreen
        (playerView.parent as? ViewGroup)?.removeView(playerView)
        rootContainer.addView(playerView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        contentLayout.visibility = View.GONE
    }

    private fun exitFullscreen() {
        isFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        supportActionBar?.show()

        // Move playerView back
        (playerView.parent as? ViewGroup)?.removeView(playerView)
        playerContainer.addView(playerView, 0, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        contentLayout.visibility = View.VISIBLE
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, if (isCollected) "取消收藏" else "收藏")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            1 -> { toggleCollection(); invalidateOptionsMenu(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleCollection() {
        val model = videoModel ?: return
        val list = getCollection().toMutableList()
        if (isCollected) {
            list.removeAll { it.title == model.title }
            Toast.makeText(this, "取消收藏成功!", Toast.LENGTH_SHORT).show()
        } else {
            list.add(model)
            Toast.makeText(this, "收藏成功!", Toast.LENGTH_SHORT).show()
        }
        saveCollection(list)
        isCollected = !isCollected
    }

    private fun getCollection(): List<PornModel> {
        val prefs = getSharedPreferences("collection", Context.MODE_PRIVATE)
        val json = prefs.getString("collection", "[]") ?: "[]"
        return try {
            Gson().fromJson(json, object : TypeToken<List<PornModel>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    private fun saveCollection(list: List<PornModel>) {
        getSharedPreferences("collection", Context.MODE_PRIVATE)
            .edit().putString("collection", Gson().toJson(list)).apply()
    }

    private fun isInCollection(model: PornModel): Boolean {
        return getCollection().any { it.title == model.title }
    }

    override fun onPause() { super.onPause(); player?.pause() }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player?.release(); player = null
        resolver?.destroy()
    }
}
