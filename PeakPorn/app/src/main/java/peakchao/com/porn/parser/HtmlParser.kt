package peakchao.com.porn.parser

import org.jsoup.Jsoup
import peakchao.com.porn.model.PornEvaluator
import peakchao.com.porn.model.PornModel

object HtmlParser {

    fun parseVideoList(html: String): List<PornModel> {
        val doc = Jsoup.parse(html)
        val items = doc.select(".col-xs-12.col-sm-4.col-md-3.col-lg-3")
        return items.mapNotNull { element ->
            try {
                val titleEl = element.selectFirst(".video-title.title-truncate.m-t-5")
                val title = titleEl?.text() ?: return@mapNotNull null

                val linkEl = element.selectFirst("a") ?: return@mapNotNull null
                val imgEl = linkEl.selectFirst("img")
                val imgUrl = imgEl?.attr("src") ?: ""

                val duration = element.selectFirst(".duration")?.text()?.let { "时长：$it" } ?: ""

                val innerHtml = element.html()
                val addTime = extractInfo("添加时间:", innerHtml, "[^<]*")
                val author = extractInfo("作者:", innerHtml, "[^<]*")
                val views = extractInfo("查看:", innerHtml, "[0-9]*")
                val favorites = extractInfo("收藏:", innerHtml, "[0-9]*")
                val info = "添加时间：$addTime\n作者：$author\n热度：$views 收藏：$favorites"

                val href = linkEl.attr("href")
                val viewKey = extractViewKey(href)

                PornModel(
                    title = title,
                    imgUrl = imgUrl,
                    duration = duration,
                    info = info,
                    viewKey = viewKey
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun extractInfo(label: String, html: String, regexSuffix: String): String {
        val pattern = """<span class="info">$label</span>\s*$regexSuffix""".toRegex()
        val match = pattern.find(html) ?: return ""
        return match.value
            .replace("""<span class="info">$label</span>""", "")
            .replace("&nbsp;", "")
            .trim()
    }

    private fun extractViewKey(href: String): String {
        val start = href.indexOf("viewkey=")
        if (start == -1) return ""
        val valueStart = start + "viewkey=".length
        val end = href.indexOf("&", valueStart)
        return if (end == -1) href.substring(valueStart) else href.substring(valueStart, end)
    }

    fun parseVideoUrl(html: String): String {
        // Method 1: Decode from strencode2("hex_encoded") call in raw HTML
        val strencodeRegex = """strencode2?\("([0-9a-fA-F%]+)"\)""".toRegex()
        val match = strencodeRegex.find(html)
        if (match != null) {
            val hex = match.groupValues[1]
            try {
                val decoded = java.net.URLDecoder.decode(hex, "UTF-8")
                // Extract src from decoded <source src='...' type='video/mp4'>
                val srcRegex = """src='([^']+)'""".toRegex()
                val srcMatch = srcRegex.find(decoded)
                if (srcMatch != null) {
                    return srcMatch.groupValues[1]
                }
            } catch (_: Exception) {}
        }
        // Method 2: Direct <source src> match (for already decoded HTML)
        val regex = """<source src=['"]([^'"]+)['"] type=['"]video/mp4['"]>""".toRegex()
        return regex.find(html)?.groupValues?.getOrNull(1) ?: ""
    }

    fun parseEvaluators(html: String): List<PornEvaluator> {
        val doc = Jsoup.parse(html)
        val items = doc.select(".comment-divider")
        return items.mapNotNull { element ->
            try {
                val td = element.selectFirst("td") ?: return@mapNotNull null
                val name = td.selectFirst("a")?.text() ?: ""
                val span = td.selectFirst("span")
                val time = span?.childNodes()?.getOrNull(2)?.toString()?.trim() ?: ""
                val content = element.selectFirst(".comment-body")
                    ?.childNodes()?.getOrNull(0)?.toString()?.trim() ?: ""
                PornEvaluator(name, time, content)
            } catch (e: Exception) {
                null
            }
        }
    }
}
