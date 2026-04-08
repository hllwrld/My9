package peakchao.com.porn.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.*
import peakchao.com.porn.R
import peakchao.com.porn.adapter.VideoListAdapter
import peakchao.com.porn.model.PornModel
import peakchao.com.porn.network.ApiClient
import peakchao.com.porn.parser.HtmlParser

class SearchResultActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvEmpty: TextView
    private val adapter = VideoListAdapter { model -> openVideo(model) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var query = ""
    private var currentPage = 1
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        query = intent.getStringExtra("query") ?: ""
        title = "搜索: $query"

        tvTitle = findViewById(R.id.tv_search_title)
        tvEmpty = findViewById(R.id.tv_empty)
        recyclerView = findViewById(R.id.recycler_view)
        swipeRefresh = findViewById(R.id.swipe_refresh)

        tvTitle.text = "搜索: $query"
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            currentPage = 1
            adapter.setData(emptyList())
            loadResults()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                if (!isLoading && lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    currentPage++
                    loadResults()
                }
            }
        })

        loadResults()
    }

    private fun loadResults() {
        if (isLoading) return
        isLoading = true
        swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                val html = withContext(Dispatchers.IO) {
                    val base = ApiClient.getBaseUrl(this@SearchResultActivity)
                    ApiClient.ensureCookies(this@SearchResultActivity)
                    val url = "$base/search_result.php?search_id=${java.net.URLEncoder.encode(query, "UTF-8")}&search_type=search_videos&page=$currentPage"
                    ApiClient.fetchHtml(url)
                }
                val items = HtmlParser.parseVideoList(html)
                if (currentPage == 1) {
                    adapter.setData(items)
                } else {
                    adapter.addData(items)
                }
                tvEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(this@SearchResultActivity, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openVideo(model: PornModel) {
        val intent = Intent(this, VideoPlayActivity::class.java)
        intent.putExtra("videoInfo", model)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
