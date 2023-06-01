package edu.skku.map.airllution

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.gson.Gson
import edu.skku.map.airllution.location.DbHelper
import edu.skku.map.airllution.location.MyLocation
import edu.skku.map.airllution.location.MyLocationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

class SinglePollutionActivity : AppCompatActivity() {

    // db 관련 설정
    lateinit var db: DbHelper
    private var isLikeLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_pollution)

        // db 관련 설정
        db = DbHelper(this)

        val latitude = intent.getDoubleExtra(MyLocationActivity.EXT_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(MyLocationActivity.EXT_LONGITUDE, 0.0)
        val cityName = intent.getStringExtra(MyLocationActivity.EXT_CITY_NAME)

        // like button
        val likeButton = findViewById<ImageButton>(R.id.spLikeButton)
        likeButton.setOnClickListener {
            if (isLikeLocation) {
                if (cityName != null) {
                    db.deleteLocation(cityName)
                }
                likeButton.setImageResource(R.drawable.empty_heart)
                isLikeLocation = false
            } else {
                if (cityName != null)
                    db.addLocation(MyLocation(latitude, longitude, cityName))

                likeButton.setImageResource(R.drawable.full_heart)
                isLikeLocation = true
                Toast.makeText(this, "내 장소에 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // back button
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // info button
        val infoButton = findViewById<ImageButton>(R.id.singleInfoButton)
        infoButton.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }


        // 백엔드로부터 현재 위치에 대한 정보 표시
        // 현재 위치를 기반으로 백엔드 API 호출
        val client = OkHttpClient()
        val host = "http://${BuildConfig.SERVER_BASE_URI}?latitude=${latitude}&longitude=${longitude}"
        val req = Request.Builder().url(host).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                response.use{
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val data = response.body!!.string()
                    val pollutionInfo = Gson().fromJson(data, DataModel.MainInfo::class.java)

                    CoroutineScope(Dispatchers.Main).launch {
                        val cityNameTextView = findViewById<TextView>(R.id.spCityTextView)
                        cityNameTextView.text = cityName

                        // 내 장소일 경우 하트를 채운다.
                        if (cityName?.let { it1 -> db.checkCityName(it1) } == true) {
                            isLikeLocation = true
                            likeButton.setImageResource(R.drawable.full_heart)
                        }

                        val dateTextView = findViewById<TextView>(R.id.spTimeTextView)
                        dateTextView.text = pollutionInfo.pollutionInfo!!.currentDate

                        val totalTextView = findViewById<TextView>(R.id.spStatusTextView)         // 전체
                        val totalImageView = findViewById<ImageView>(R.id.spStatusImageView)
                        val totalLayout = findViewById<ConstraintLayout>(R.id.spTotalLayout)
                        pollutionInfo.pollutionInfo!!.totalGrade?.let { it1 ->
                            setSinglePollutionInfo(it1, "", totalTextView, totalImageView, totalLayout)
                        }

                        val totalScoreTextView = findViewById<TextView>(R.id.spAllAirStatusTextView)
                        totalScoreTextView.text = "통합지수: ${pollutionInfo.pollutionInfo!!.totalScore}"

                        val pmTextView = findViewById<TextView>(R.id.spFineStatusTextView)              // 미세먼지
                        val pmImageView = findViewById<ImageView>(R.id.spFineImageView)
                        val pmLayout = findViewById<ConstraintLayout>(R.id.spFineLayout)
                        pollutionInfo.pollutionInfo!!.pmGrade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.pmScore?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2, pmTextView, pmImageView, pmLayout)
                            }
                        }

                        val ultraPmTextView = findViewById<TextView>(R.id.spUltraFineStatusTextView)    // 초미세먼지
                        val ultraPmImageView = findViewById<ImageView>(R.id.spUltraFineImageView)
                        val ultraPmLayout = findViewById<ConstraintLayout>(R.id.spUltraFineLayout)
                        pollutionInfo.pollutionInfo!!.ultraPmGrade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.ultraPmScore?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2,ultraPmTextView, ultraPmImageView, ultraPmLayout)
                            }
                        }

                        val so2TextView = findViewById<TextView>(R.id.spNitrogenStatusTextView)         // 아황산
                        val so2ImageView = findViewById<ImageView>(R.id.spNitrogenImageView)
                        val so2Layout = findViewById<ConstraintLayout>(R.id.spNitrogenLayout)
                        pollutionInfo.pollutionInfo!!.so2Grade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.so2Score?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2, so2TextView, so2ImageView, so2Layout)
                            }
                        }

                        val coTextView = findViewById<TextView>(R.id.spSulfurStatusTextView)            // 일산화탄소
                        val coImageView = findViewById<ImageView>(R.id.spSulfurImageView)
                        val coLayout = findViewById<ConstraintLayout>(R.id.spSulfurLayout)
                        pollutionInfo.pollutionInfo!!.coGrade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.coScore?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2, coTextView, coImageView, coLayout)
                            }
                        }

                        val o3TextView = findViewById<TextView>(R.id.spOzoneStatusTextView)             // 오존
                        val o3ImageView = findViewById<ImageView>(R.id.spOzoneImageView)
                        val o3Layout = findViewById<ConstraintLayout>(R.id.spOzoneLayout)
                        pollutionInfo.pollutionInfo!!.o3Grade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.o3Score?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2, o3TextView, o3ImageView, o3Layout)
                            }
                        }

                        val no2TextView = findViewById<TextView>(R.id.spNitrogen2StatusTextView)        // 이산화질소
                        val no2ImageView = findViewById<ImageView>(R.id.spNitrogen2ImageView)
                        val no2Layout = findViewById<ConstraintLayout>(R.id.spNitrogen2Layout)
                        pollutionInfo.pollutionInfo!!.no2Grade?.let { it1 ->
                            pollutionInfo.pollutionInfo!!.no2Score?.let { it2 ->
                                setSinglePollutionInfo(
                                    it1, it2, no2TextView, no2ImageView, no2Layout)
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * 특정 대기 정보를 갱신하는 메서드입니다.
     */
    private fun setSinglePollutionInfo(
        grade: String,
        score: String,
        gradeTextView: TextView,
        imageView: ImageView,
        layout: ConstraintLayout
    ) {

        if (score != "") {
            gradeTextView.text = grade + "\n" + score

            when (grade) {
                "좋음" -> {
                    imageView.setImageResource(R.drawable.very_good)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.light_blue
                        )
                    )
                }
                "보통" -> {
                    imageView.setImageResource(R.drawable.normal)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.light_green
                        )
                    )
                }
                "나쁨" -> {
                    imageView.setImageResource(R.drawable.bad)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.light_orange
                        )
                    )
                }
                "매우 나쁨" -> {
                    imageView.setImageResource(R.drawable.very_bad)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.light_red
                        )
                    )
                }
                else -> {
                    // 정보를 불러오지 못한 경우
                    imageView.setImageResource(R.drawable.good)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.light_blue
                        )
                    )
                }
            }

        } else {
            gradeTextView.text = grade

            when (grade) {
                "좋음" -> {
                    imageView.setImageResource(R.drawable.very_good)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.blue
                        )
                    )
                }
                "보통" -> {
                    imageView.setImageResource(R.drawable.normal)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.green
                        )
                    )
                }
                "나쁨" -> {
                    imageView.setImageResource(R.drawable.bad)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.orange
                        )
                    )
                }
                "매우 나쁨" -> {
                    imageView.setImageResource(R.drawable.very_bad)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.red
                        )
                    )
                }
                else -> {
                    // 정보를 불러오지 못한 경우
                    imageView.setImageResource(R.drawable.good)
                    layout.setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.blue
                        )
                    )
                }
            }
        }

    }
}