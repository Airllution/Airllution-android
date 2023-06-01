package edu.skku.map.airllution

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import edu.skku.map.airllution.location.DbHelper
import edu.skku.map.airllution.location.MyLocation
import edu.skku.map.airllution.location.MyLocationActivity
import edu.skku.map.airllution.map.MyLocationMapActivity
import edu.skku.map.airllution.news.NewsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        const val EXT_LATITUDE = "현재 위도"
        const val EXT_LONGITUDE = "현재 경도"
    }

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    private lateinit var mLocationRequest: LocationRequest // 위치 정보 요청 매개변수
    private val REQUEST_PERMISSION_LOCATION = 10
    private var isLocationInit = false

    // db 관련 설정
    lateinit var db: DbHelper
    var myMyLocations = ArrayList<MyLocation>()

    private var isLikeLocation = false
    private var currentCityName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // tool bar setting
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar!!
        actionBar.setDisplayShowCustomEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.menu_bar)

        // menu bar
        val myMenuBar = findViewById<ImageButton>(R.id.backButton)
        myMenuBar.setOnClickListener {
            findViewById<DrawerLayout>(R.id.drawerLayout).openDrawer(GravityCompat.START)
        }

        // like button
        val likeButton = findViewById<ImageButton>(R.id.likeButton)
        likeButton.setOnClickListener {
            if (isLikeLocation) {
                db.deleteLocation(currentCityName)
                likeButton.setImageResource(R.drawable.empty_heart)
                isLikeLocation = false
            } else {
                db.addLocation(MyLocation(mLastLocation.latitude, mLastLocation.longitude, currentCityName))
                likeButton.setImageResource(R.drawable.full_heart)
                isLikeLocation = true
                Toast.makeText(this, "내 장소에 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }


        // add navigation bar event listener
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.itemIconTintList = null

        // 듀토리얼 화면
        val infoButton = findViewById<ImageButton>(R.id.questionButton)
        infoButton.setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
        }

        // db 관련 설정
        db = DbHelper(this)

        // 현재 위치 가져오기
        mLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(50)
            .setMaxUpdateDelayMillis(100)
            .build()


        val locationJob = CoroutineScope(Dispatchers.Main).launch {
            if (checkPermissionForLocation(applicationContext)) {
                startLocationUpdates()
            }
            while (!isLocationInit) {
                delay(10)
            }
        }

        // 백엔드로부터 현재 위치에 대한 정보 표시
        val geocodingJob = CoroutineScope(Dispatchers.Main).launch {
            locationJob.join()

            // 현재 위치를 기반으로 백엔드 API 호출
            val client = OkHttpClient()

            val host = "http://${BuildConfig.SERVER_BASE_URI}?latitude=${mLastLocation.latitude}&longitude=${mLastLocation.longitude}"

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
                            val cityNameTextView = findViewById<TextView>(R.id.cityTextView)
                            cityNameTextView.text = pollutionInfo.cityName

                            currentCityName = pollutionInfo.cityName.toString()

                            // 내 장소일 경우 하트를 채운다.
                            if (pollutionInfo.cityName?.let { it1 -> db.checkCityName(it1) } == true) {
                                isLikeLocation = true
                                likeButton.setImageResource(R.drawable.full_heart)
                            }

                            val dateTextView = findViewById<TextView>(R.id.timeTextView)
                            dateTextView.text = pollutionInfo.pollutionInfo!!.currentDate

                            val totalTextView = findViewById<TextView>(R.id.statusTextView)         // 전체
                            val totalImageView = findViewById<ImageView>(R.id.statusImageView)
                            val totalLayout = findViewById<ConstraintLayout>(R.id.totalLayout)
                            pollutionInfo.pollutionInfo!!.totalGrade?.let { it1 ->
                                setSinglePollutionInfo(it1, "", totalTextView, totalImageView, totalLayout)
                            }

                            val totalScoreTextView = findViewById<TextView>(R.id.allAirStatusTextView)
                            totalScoreTextView.text = "통합지수: ${pollutionInfo.pollutionInfo!!.totalScore}"

                            val pmTextView = findViewById<TextView>(R.id.fineStatusTextView)              // 미세먼지
                            val pmImageView = findViewById<ImageView>(R.id.fineImageView)
                            val pmLayout = findViewById<ConstraintLayout>(R.id.fineLayout)
                            pollutionInfo.pollutionInfo!!.pmGrade?.let { it1 ->
                                pollutionInfo.pollutionInfo!!.pmScore?.let { it2 ->
                                    setSinglePollutionInfo(
                                        it1, it2, pmTextView, pmImageView, pmLayout)
                                }
                            }

                            val ultraPmTextView = findViewById<TextView>(R.id.ultraFineStatusTextView)    // 초미세먼지
                            val ultraPmImageView = findViewById<ImageView>(R.id.ultraFineImageView)
                            val ultraPmLayout = findViewById<ConstraintLayout>(R.id.ultraFineLayout)
                            pollutionInfo.pollutionInfo!!.ultraPmGrade?.let { it1 ->
                                pollutionInfo.pollutionInfo!!.ultraPmScore?.let { it2 ->
                                    setSinglePollutionInfo(
                                        it1, it2,ultraPmTextView, ultraPmImageView, ultraPmLayout)
                                }
                            }

                            val so2TextView = findViewById<TextView>(R.id.nitrogenStatusTextView)         // 아황산
                            val so2ImageView = findViewById<ImageView>(R.id.nitrogenImageView)
                            val so2Layout = findViewById<ConstraintLayout>(R.id.nitrogenLayout)
                            pollutionInfo.pollutionInfo!!.so2Grade?.let { it1 ->
                                pollutionInfo.pollutionInfo!!.so2Score?.let { it2 ->
                                    setSinglePollutionInfo(
                                        it1, it2, so2TextView, so2ImageView, so2Layout)
                                }
                            }

                            val coTextView = findViewById<TextView>(R.id.sulfurStatusTextView)            // 일산화탄소
                            val coImageView = findViewById<ImageView>(R.id.sulfurImageView)
                            val coLayout = findViewById<ConstraintLayout>(R.id.sulfurLayout)
                            pollutionInfo.pollutionInfo!!.coGrade?.let { it1 ->
                                pollutionInfo.pollutionInfo!!.coScore?.let { it2 ->
                                    setSinglePollutionInfo(
                                        it1, it2, coTextView, coImageView, coLayout)
                                }
                            }

                            val o3TextView = findViewById<TextView>(R.id.ozoneStatusTextView)             // 오존
                            val o3ImageView = findViewById<ImageView>(R.id.ozoneImageView)
                            val o3Layout = findViewById<ConstraintLayout>(R.id.ozoneLayout)
                            pollutionInfo.pollutionInfo!!.o3Grade?.let { it1 ->
                                pollutionInfo.pollutionInfo!!.o3Score?.let { it2 ->
                                    setSinglePollutionInfo(
                                        it1, it2, o3TextView, o3ImageView, o3Layout)
                                }
                            }

                            val no2TextView = findViewById<TextView>(R.id.nitrogen2StatusTextView)        // 이산화질소
                            val no2ImageView = findViewById<ImageView>(R.id.nitrogen2ImageView)
                            val no2Layout = findViewById<ConstraintLayout>(R.id.nitrogen2Layout)
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
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_favorite -> {
                // 좋아요
                val intent = Intent(this, MyLocationActivity::class.java)
                findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawer(GravityCompat.START)
                startActivity(intent)

                true
            }
            R.id.item_favoirte_map -> {
                // 좋아요 (지도)
                val intent = Intent(this, MyLocationMapActivity::class.java).apply {
                    putExtra(EXT_LATITUDE, mLastLocation.latitude)
                    putExtra(EXT_LONGITUDE, mLastLocation.longitude)
                }
                findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawer(GravityCompat.START)
                startActivity(intent)
                true
            }
            R.id.item_news -> {
                // 뉴스
                val intent = Intent(this, NewsActivity::class.java)
                findViewById<DrawerLayout>(R.id.drawerLayout).closeDrawer(GravityCompat.START)
                startActivity(intent)
                true
            }
            else -> {
                Log.d("여기엔", "오면 안됨")
                true
            }
        }
    }

    /**
     * 현재 위치를 불러오는 메서드입니다.
     */
    private fun startLocationUpdates() {

        //FusedLocationProviderClient 인스턴스 생성
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // 기기의 위치에 관한 정기 업데이트를 요청하는 메서드 실행
        // 지정한 루퍼 스레드(Looper.myLooper())에서 콜백(mLocationCallback)으로 위치 업데이트를 요청
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    // 시스템으로 부터 위치 정보를 콜백으로 받음
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 시스템에서 받은 location 정보를 onLocationChanged()에 전달
            locationResult.lastLocation
            locationResult.lastLocation?.let { onLocationChanged(it) }
        }
    }

    /**
     * 시스템으로 부터 받은 위치정보를 화면에 갱신
     */
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        isLocationInit = true;
    }

    /**
     * 위치 권한이 있는지 확인
     */
    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    /**
     * 사용자에게 권한 요청 후 결과에 대한 처리
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()

            } else {
                Log.e("권한 에러", "onRequestPermissionsResult(): 권한 허용 거부")
                Toast.makeText(this, "위치 사용 권한을 체크해주세요!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 특정 대기 정보를 갱신하는 메서드입니다.
     */
    private fun setSinglePollutionInfo(grade: String, score: String, gradeTextView: TextView, imageView: ImageView, layout: ConstraintLayout) {

        if (score != "") {
            gradeTextView.text = grade + "\n" + score

            when (grade) {
                "좋음" -> {
                    imageView.setImageResource(R.drawable.very_good)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_blue))
                }
                "보통" -> {
                    imageView.setImageResource(R.drawable.normal)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_green))
                }
                "나쁨" -> {
                    imageView.setImageResource(R.drawable.bad)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_orange))
                }
                "매우 나쁨" -> {
                    imageView.setImageResource(R.drawable.very_bad)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_red))
                }
                else -> {
                    // 정보를 불러오지 못한 경우
                    imageView.setImageResource(R.drawable.good)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.light_blue))
                }
            }

        } else {
            gradeTextView.text = grade

            when (grade) {
                "좋음" -> {
                    imageView.setImageResource(R.drawable.very_good)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.blue))
                }
                "보통" -> {
                    imageView.setImageResource(R.drawable.normal)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.green))
                }
                "나쁨" -> {
                    imageView.setImageResource(R.drawable.bad)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.orange))
                }
                "매우 나쁨" -> {
                    imageView.setImageResource(R.drawable.very_bad)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.red))
                }
                else -> {
                    // 정보를 불러오지 못한 경우
                    imageView.setImageResource(R.drawable.good)
                    layout.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.blue))
                }
            }
        }


    }
}