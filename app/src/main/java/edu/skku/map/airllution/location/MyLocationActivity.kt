package edu.skku.map.airllution.location

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ListView
import com.google.gson.Gson
import edu.skku.map.airllution.BuildConfig
import edu.skku.map.airllution.DataModel
import edu.skku.map.airllution.R
import edu.skku.map.airllution.SinglePollutionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

class MyLocationActivity : AppCompatActivity() {

    companion object {
        const val EXT_CITY_NAME = "도시명"
        const val EXT_LATITUDE = "위도"
        const val EXT_LONGITUDE = "경도"
    }

    // db 관련 설정
    lateinit var db: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location)

        // db 관련 설정
        db = DbHelper(this)

        val locations = db.allMyLocations
        val resultLocations = ArrayList<MyLocationPollutionInfo>()

        for (location in locations) {
            resultLocations.add(MyLocationPollutionInfo(location.cityName, location.latitude, location.longitude))
        }

        val myAdapter = MyLocationAdapter(resultLocations, applicationContext)
        val listView = findViewById<ListView>(R.id.listViewLocation)
        listView.adapter = myAdapter

        // API 호출
        for (location in resultLocations) {
            // 현재 위치를 기반으로 백엔드 API 호출
            val client = OkHttpClient()

            val host = "http://${BuildConfig.SERVER_BASE_URI}?latitude=${location.latitude}&longitude=${location.longitude}"
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

                        location.totalScore = pollutionInfo.pollutionInfo?.totalScore.toString()
                        location.pmScore = pollutionInfo.pollutionInfo?.pmScore.toString()
                        location.ultraPmScore = pollutionInfo.pollutionInfo?.ultraPmScore.toString()
                        location.status = pollutionInfo.pollutionInfo?.totalGrade.toString()

                        CoroutineScope(Dispatchers.Main).launch {
                            myAdapter.notifyDataSetChanged()
                        }
                    }
                }
            })
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as MyLocationPollutionInfo

            val intent = Intent(this, SinglePollutionActivity::class.java).apply {
                putExtra(EXT_CITY_NAME, selectedItem.cityName)
                putExtra(EXT_LATITUDE, selectedItem.latitude)
                putExtra(EXT_LONGITUDE, selectedItem.longitude)
            }

            startActivity(intent)
        }

    }
}