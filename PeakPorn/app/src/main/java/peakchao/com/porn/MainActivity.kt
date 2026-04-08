package peakchao.com.porn

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import peakchao.com.porn.activity.SearchResultActivity
import peakchao.com.porn.activity.WebBrowserActivity
import peakchao.com.porn.fragment.HomeFragment
import peakchao.com.porn.network.ApiClient

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private val tabNames = arrayOf(
        "91原创", "当前最热", "本月最热", "10分钟以上", "20分钟以上",
        "本月收藏", "收藏最多", "最近加精", "高清", "本月讨论", "我的收藏"
    )
    private val categories = arrayOf(
        "ori", "hot", "top", "long", "longer",
        "tf", "mf", "rf", "hd", "md", "sc"
    )

    private val fragments = mutableListOf<HomeFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        for (i in tabNames.indices) {
            fragments.add(HomeFragment.newInstance(tabNames[i], categories[i]))
        }

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        val etSearch = findViewById<EditText>(R.id.et_search)
        val btnSearch = findViewById<ImageButton>(R.id.btn_search)

        val doSearch = {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                startActivity(Intent(this, SearchResultActivity::class.java).putExtra("query", query))
            }
        }
        btnSearch.setOnClickListener { doSearch() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }

        findViewById<ImageButton>(R.id.btn_browser).setOnClickListener {
            startActivity(Intent(this, WebBrowserActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_switch_source).setOnClickListener {
            showSourceSelector()
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val tab = intent.getIntExtra("tab", -1)
        if (tab in tabNames.indices) {
            viewPager.setCurrentItem(tab, false)
        }
    }

    private val sourceUrls = arrayOf(
        "http://h1014.sol148.com",
        "http://91porn.com"
    )
    private val sourceNames = arrayOf(
        "默认 (sol148.com)",
        "备用 (91porn.com)"
    )

    private fun showSourceSelector() {
        val currentUrl = ApiClient.getBaseUrl(this)
        var checkedIndex = sourceUrls.indexOfFirst { currentUrl.contains(it.removePrefix("http://")) }
        if (checkedIndex < 0) checkedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("选择数据源")
            .setSingleChoiceItems(sourceNames, checkedIndex) { dialog, which ->
                val newUrl = sourceUrls[which]
                if (!currentUrl.contains(newUrl.removePrefix("http://"))) {
                    ApiClient.setBaseUrl(this, newUrl)
                    Toast.makeText(this, "已切换到 ${sourceNames[which]}", Toast.LENGTH_SHORT).show()
                    recreate()
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
