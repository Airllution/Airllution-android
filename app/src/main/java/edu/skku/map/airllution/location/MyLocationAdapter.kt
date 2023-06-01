package edu.skku.map.airllution.location

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import edu.skku.map.airllution.R

class MyLocationAdapter(val data: ArrayList<MyLocationPollutionInfo>, val context: Context): BaseAdapter() {
    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(p0: Int): Any {
        return data[p0]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val generatedView = inflater.inflate(R.layout.item_location, null)

        val statusImageView = generatedView.findViewById<ImageView>(R.id.singleStatusImageView)
        val cityNameTextView = generatedView.findViewById<TextView>(R.id.cityNameTextView)
        val statusTextView = generatedView.findViewById<TextView>(R.id.singleStatusTextView)
        val totalScoreTextView = generatedView.findViewById<TextView>(R.id.singleTotalScoreTextView)
        val pmTextView = generatedView.findViewById<TextView>(R.id.singlePmTextView)
        val ultraPmTextView = generatedView.findViewById<TextView>(R.id.singleUltraPmTextView)
        val layout = generatedView.findViewById<ConstraintLayout>(R.id.singleItemLayout)

        cityNameTextView.text = data[p0].cityName
        statusTextView.text = data[p0].status
        totalScoreTextView.text = data[p0].totalScore
        pmTextView.text = "미세: ${data[p0].pmScore}"
        ultraPmTextView.text = "초미세: ${data[p0].ultraPmScore}"

        when (data[p0].status) {
            "좋음" -> {
                statusImageView.setImageResource(R.drawable.very_good)
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
            }
            "보통" -> {
                statusImageView.setImageResource(R.drawable.normal)
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
            }
            "나쁨" -> {
                statusImageView.setImageResource(R.drawable.bad)
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.orange))
            }
            "매우 나쁨" -> {
                statusImageView.setImageResource(R.drawable.very_bad)
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
            }
            else -> {
                statusImageView.setImageResource(R.drawable.good)
                layout.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
            }
        }

        return generatedView
    }
}