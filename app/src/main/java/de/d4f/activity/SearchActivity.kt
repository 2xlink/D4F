package de.d4f.activity

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import de.d4f.R
import de.d4f.adapter.CategoryAdapter
import de.d4f.model.Category

class SearchActivity : AppCompatActivity() {

    private lateinit var rootListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        title = resources.getString(R.string.tab_text_2)

        rootListView = findViewById(R.id.categoryListView)

        val categories: ArrayList<Category> = ArrayList()
        categories.add(
            Category(
                resources.getString(R.string.feature_restaurant_title),
                getDrawable(R.drawable.feature_restaurant)!!
            )
        )
        categories.add(
            Category(
                resources.getString(R.string.feature_theatre_title),
                getDrawable(R.drawable.feature_theatre)!!
            )
        )
        categories.add(
            Category(
                resources.getString(R.string.feature_bar_title),
                getDrawable(R.drawable.feature_bar)!!
            )
        )
        categories.add(
            Category(
                resources.getString(R.string.feature_recreation_title),
                getDrawable(R.drawable.feature_recycling)!!
            )
        )
        categories.add(
            Category(
                resources.getString(R.string.feature_fitness_title),
                getDrawable(R.drawable.feature_fitness_centre)!!
            )
        )
        categories.add(
            Category(
                resources.getString(R.string.feature_special_title),
                getDrawable(R.drawable.feature_star)!!
            )
        )

        val adapter = CategoryAdapter(this, categories)
        rootListView.adapter = adapter

        rootListView.setOnItemClickListener { _, _, position, _ ->
            intent = Intent()
            intent.putExtra("chosenCategory", position + 1)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}