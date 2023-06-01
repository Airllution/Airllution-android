package edu.skku.map.airllution.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import edu.skku.map.airllution.BuildConfig
import edu.skku.map.airllution.DataModel
import edu.skku.map.airllution.MainActivity
import edu.skku.map.airllution.R
import edu.skku.map.airllution.SinglePollutionActivity
import edu.skku.map.airllution.location.DbHelper
import edu.skku.map.airllution.location.MyLocationActivity
import edu.skku.map.airllution.location.MyLocationPollutionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException


class MyLocationMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // db 관련 설정
    lateinit var db: DbHelper
    private lateinit var mMap: GoogleMap
    private var currentMarker: Marker ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location_map)

        // db 관련 설정
        db = DbHelper(this)

        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.mapview) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onRestart() {
        addMyLocationToMap()
        super.onRestart()
    }

    override fun onMapReady(p0: GoogleMap) {

        mMap = p0

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        addMyLocationToMap()

        // 구글맵 터치 이벤트 리스너
        mMap.setOnMapClickListener {
            val markerOption = MarkerOptions()

            val latitude = it.latitude
            val longitude = it.longitude

            // 역 지오코딩
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

                        markerOption.title(pollutionInfo.cityName.toString())
                        markerOption.snippet("대기정보 보기")
                        markerOption.position(LatLng(latitude, longitude))
                        CoroutineScope(Dispatchers.Main).launch {
                            currentMarker?.remove()
                            currentMarker = mMap.addMarker(markerOption)
                        }
                    }
                }
            })
        }

        mMap.isMyLocationEnabled = true
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(intent.getDoubleExtra(MainActivity.EXT_LATITUDE, 0.0),
                    intent.getDoubleExtra(MainActivity.EXT_LONGITUDE, 0.0)), 15F))

        mMap.setInfoWindowAdapter(object: InfoWindowAdapter {
            override fun getInfoContents(p0: Marker): View? {

                val v = layoutInflater.inflate(R.layout.info_window, null)

                val title = v.findViewById<TextView>(R.id.mapTitleTextView)
                val snippet = v.findViewById<TextView>(R.id.mapSnippetTextView)
                title.text = p0.title
                snippet.text = p0.snippet

                mMap.setOnInfoWindowClickListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        val intent = Intent(applicationContext, SinglePollutionActivity::class.java).apply {
                            putExtra(MyLocationActivity.EXT_CITY_NAME, p0.title)
                            putExtra(MyLocationActivity.EXT_LATITUDE, p0.position.latitude)
                            putExtra(MyLocationActivity.EXT_LONGITUDE, p0.position.longitude)
                        }
                        startActivity(intent)
                    }
                }
                return v
            }

            override fun getInfoWindow(p0: Marker): View? {
                return null
            }

        })
    }

    private fun addMyLocationToMap() {
        mMap.clear()
        val locations = db.allMyLocations

        for (location in locations) {
            val marker = MarkerOptions()
            marker // LatLng에 대한 어레이를 만들어서 이용할 수도 있다.
                .position(LatLng(location.latitude, location.longitude))
                .title(location.cityName) // 타이틀
                .snippet("대기정보 보기")

            marker.icon(bitmapDescriptorFromVector(this, R.drawable.circle_heart))

            mMap.addMarker(marker)
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        location.latitude,
                        location.longitude
                    ), 10F
                )
            )
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}