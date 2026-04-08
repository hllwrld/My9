package peakchao.com.porn.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import peakchao.com.porn.R
import peakchao.com.porn.activity.VideoPlayActivity
import peakchao.com.porn.adapter.VideoListAdapter
import peakchao.com.porn.model.PornModel
import peakchao.com.porn.network.ApiClient
import peakchao.com.porn.parser.HtmlParser

class HomeFragment : Fragment() {

    private lateinit var adapter: VideoListAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private var category: String = "rf"
    private var page = 1
    private var isLoading = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        fun newInstance(title: String, category: String): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().apply {
                    putString("category", category)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString("category") ?: "rf"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        recyclerView = view.findViewById(R.id.recycler_view)

        adapter = VideoListAdapter { model ->
            val intent = Intent(requireContext(), VideoPlayActivity::class.java)
            intent.putExtra("videoInfo", model)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData(isRefresh = true) }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (isLoading) return
                val lm = rv.layoutManager as LinearLayoutManager
                val total = lm.itemCount
                val lastVisible = lm.findLastVisibleItemPosition()
                if (lastVisible >= total - 3) {
                    loadData(isRefresh = false)
                }
            }
        })

        loadData(isRefresh = true)
    }

    private fun loadData(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (isRefresh) {
            page = 1
            swipeRefresh.isRefreshing = true
        } else {
            page++
        }

        if (category == "sc") {
            loadCollection(isRefresh)
            return
        }

        val cat = if (category.isEmpty()) "hot" else category

        Log.e("HomeFragment", "loadData cat=$cat page=$page isRefresh=$isRefresh")
        scope.launch {
            try {
                val html = withContext(Dispatchers.IO) {
                    ApiClient.fetchVideoList(requireContext(), cat, page)
                }
                Log.e("HomeFragment", "HTML len=${html.length} cat=$cat")
                // Log first 500 chars of HTML for debugging
                Log.e("HomeFragment", "HTML preview cat=$cat: ${html.take(500)}")
                val list = HtmlParser.parseVideoList(html)
                Log.e("HomeFragment", "Parsed ${list.size} items for cat=$cat")
                if (list.size >= 2) {
                    Log.e("HomeFragment", "Item0: ${list[0].title} | Item1: ${list[1].title}")
                } else if (list.isNotEmpty()) {
                    Log.e("HomeFragment", "First: ${list[0].title}")
                }
                if (isRefresh) adapter.setData(list) else adapter.addData(list)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Load error cat=$cat", e)
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh.isRefreshing = false
                isLoading = false
            }
        }
    }

    private fun loadCollection(isRefresh: Boolean) {
        scope.launch {
            delay(200)
            val ctx = context ?: return@launch
            val prefs = ctx.getSharedPreferences("collection", Context.MODE_PRIVATE)
            val json = prefs.getString("collection", "[]") ?: "[]"
            val type = object : TypeToken<List<PornModel>>() {}.type
            val list: List<PornModel> = try {
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
            adapter.setData(list.reversed())
            swipeRefresh.isRefreshing = false
            isLoading = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
