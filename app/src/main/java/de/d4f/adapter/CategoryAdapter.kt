package de.d4f.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import de.d4f.R
import de.d4f.model.Category

class CategoryAdapter (private val context: Context,
                       private val dataSource: ArrayList<Category>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.item_category, parent, false)

        val titleTextView = rowView.findViewById(R.id.it_ca_title) as TextView
        val drawable = rowView.findViewById(R.id.it_ca_image) as ImageView

        val category = getItem(position) as Category
        titleTextView.text = category.name
        drawable.setImageDrawable(category.drawable)



        return rowView
    }
}