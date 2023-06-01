package edu.skku.map.airllution.location

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.widget.Toast

object LocationContract {
    const val DATABASE_NAME = "Airllution.db"

    object LocationEntry : BaseColumns {
        const val TABLE_NAME = "LOCATION"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_CITY_NAME = "city_name"
        const val ID = "ID"

        const val CREATE_QUERY = "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_LATITUDE TEXT, " +
                "$COLUMN_LONGITUDE TEXT, " +
                "$COLUMN_CITY_NAME TEXT)"

        const val DROP_QUERY = "DROP TABLE IF EXISTS $TABLE_NAME"
    }
}

/**
 * 내 장소 엔티티에 대한 DB 접근을 다루는 메서드입니다.
 */
class DbHelper(val context: Context?) : SQLiteOpenHelper(context, LocationContract.DATABASE_NAME, null, 1) {

    override fun onCreate(p0: SQLiteDatabase?) {
        p0?.execSQL(LocationContract.LocationEntry.CREATE_QUERY)
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
        p0?.execSQL(LocationContract.LocationEntry.DROP_QUERY)
        onCreate(p0)
    }

    /**
     * 모든 내 장소 엔티티를 반환
     */
    val allMyLocations: ArrayList<MyLocation>
    @SuppressLint("Range")
    get() {
        val myLocations = ArrayList<MyLocation>()
        val selectQueryHandler = "SELECT * FROM ${LocationContract.LocationEntry.TABLE_NAME}"
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQueryHandler, null)

        if (cursor.moveToFirst()) {
            do {
                val myLocation = MyLocation()
                myLocation.latitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_LATITUDE))
                myLocation.longitude = cursor.getDouble(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_LONGITUDE))
                myLocation.cityName = cursor.getString(cursor.getColumnIndex(LocationContract.LocationEntry.COLUMN_CITY_NAME))

                myLocations.add(myLocation)
            } while (cursor.moveToNext())
        }

        db.close()
        return myLocations
    }

    /**
     * 이미 존재하는 내 장소인지를 확인하는 메서드입니다.
     */
    fun checkCityName(cityName: String): Boolean {
        val db = this.readableDatabase

        val projection = arrayOf(LocationContract.LocationEntry.ID)

        val selection = "${LocationContract.LocationEntry.COLUMN_CITY_NAME} = ?"
        val selectionArgs = arrayOf(cityName)

        val cursor = db.query(
            LocationContract.LocationEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        return cursor.count > 0
    }

    /**
     * 내 장소를 추가하는 메서드입니다.
     */
    fun addLocation(myLocation: MyLocation) {
        if (checkCityName(myLocation.cityName)) {
            Toast.makeText(this.context, "이미 추가된 장소입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(LocationContract.LocationEntry.COLUMN_LATITUDE, myLocation.latitude)
        values.put(LocationContract.LocationEntry.COLUMN_LONGITUDE, myLocation.longitude)
        values.put(LocationContract.LocationEntry.COLUMN_CITY_NAME, myLocation.cityName)

        db.insert(LocationContract.LocationEntry.TABLE_NAME, null, values)
        db.close()
        Toast.makeText(this.context, "내 장소에 추가되었습니다.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 내 장소를 삭제하는 메서드입니다.
     */
    fun deleteLocation(myLocation: String) {
        val db = this.writableDatabase

        db.delete(LocationContract.LocationEntry.TABLE_NAME,
            "${LocationContract.LocationEntry.COLUMN_CITY_NAME} = ?", arrayOf(myLocation))
        db.close()
    }
}