package edu.skku.map.airllution.news

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import edu.skku.map.airllution.R

class NewsAdapter(val data: ArrayList<News>, val context: Context): BaseAdapter() {
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
        val generatedView = inflater.inflate(R.layout.item_news, null)

        val titleTextView = generatedView.findViewById<TextView>(R.id.newsTitleTextView)
        val imageView = generatedView.findViewById<ImageView>(R.id.newsImageView)
        val timeView = generatedView.findViewById<TextView>(R.id.newsTimeTextView)

        titleTextView.text = data[p0].title
        Glide.with(context).load(data[p0].imageLink).into(imageView)
        timeView.text = data[p0].date

        return generatedView
    }
}