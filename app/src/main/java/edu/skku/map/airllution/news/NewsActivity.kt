package edu.skku.map.airllution.news

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import edu.skku.map.airllution.BuildConfig
import edu.skku.map.airllution.DataModel
import edu.skku.map.airllution.R
import edu.skku.map.airllution.location.MyLocationAdapter
import edu.skku.map.airllution.location.MyLocationPollutionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException

class NewsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        // 0. 어댑터 사전 장착
        val newsList = ArrayList<News>()
        val myAdapter = NewsAdapter(newsList, applicationContext)
        val listView = findViewById<ListView>(R.id.newsListView)
        listView.adapter = myAdapter

        // 1. 뉴스 데이터 불러오기
        // 현재 위치를 기반으로 백엔드 API 호출
        val client = OkHttpClient()
        val host = "https://openapi.naver.com/v1/search/news.json?query=%EB%AF%B8%EC%84%B8%EB%A8%BC%EC%A7%80%20%EC%98%A4%EC%A1%B4&display=30"
        val req = Request.Builder()
            .url(host)
            .header("X-Naver-Client-Id", BuildConfig.NAVER_CLIENT_ID)
            .header("X-Naver-Client-Secret", BuildConfig.NAVER_CLIENT_SECRET)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                response.use{
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val data = response.body!!.string()
                    //Log.d("뉴스 데이터", data)
                    val newsData = Gson().fromJson(data, DataModel.NewsItem::class.java)

                    // 2. 뉴스 데이터들로부터 이미지 URL 파싱
                    var imageLink = ""
                    for (item in newsData.items!!) {
                        // 뉴스 데이터들의 날짜 포맷 변경
                        var date = ""
                        for (i in 0..3) {
                                date += item.pubDate!!.split(" ")[i] + " "
                        }

                        try {
                            val con = Jsoup.connect(item.link!!)
                            val doc = con.get()
                            val ogTags = doc.select("meta[property^=og:]")

                            if (ogTags.size <= 0) {
                                continue
                            }

                            for (i in 0 until ogTags.size) {
                                val tag = ogTags[i]

                                val text = tag.attr("property")
                                if ("og:image" == text) {
                                    imageLink = tag.attr("content")
                                    break
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        // 어댑터 갱신
                        newsList.add(News(Html.fromHtml(item.title!!), item.link!!, imageLink, date))
                        CoroutineScope(Dispatchers.Main).launch {

                            myAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as News

            val uri = Uri.parse(selectedItem.link)
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(webIntent)
        }
    }
}