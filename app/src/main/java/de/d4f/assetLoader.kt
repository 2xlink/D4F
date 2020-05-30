package de.d4f

import android.content.Context
import android.util.Log
import com.beust.klaxon.Parser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.InvalidParameterException
import java.util.*
import kotlin.collections.ArrayList

private val TAG = "assetLoader"
private val CACHE_TIMEOUT_S = 3600 * 24 // One hour * 24

fun getPartnersFeatureCollection(context: Context) : FeatureCollection {
    val url = "https://www.dresdenforfriends.de/?q=partner_filter&date=&time=-1&location=&lat=&lng=&zoom=12&freetext=&sort=4"
    val prefs = context.getSharedPreferences(context.getString(R.string.shared_prefs_key), 0)
    val file = File(context.filesDir, "partners.json")

    val featureCollection: FeatureCollection

    val lastUpdate = prefs.getLong(context.getString(R.string.sp_key_last_update), 0)
    val currentTime = Calendar.getInstance().timeInMillis / 1000

    // Check if file must be retrieved anew
    if (currentTime - lastUpdate > CACHE_TIMEOUT_S || !file.exists()) {
        Log.i(TAG, "Retrieving partners update from server")

        val unparsedJsonString = URL(url).readText()
        val unparsedJsonObject = JsonParser.parseString(unparsedJsonString).asJsonObject
        val unparsedPartners = unparsedJsonObject.getAsJsonArray("list")
        val featureCollectionList: ArrayList<Feature> = ArrayList()

        for (i in 0 until unparsedPartners!!.size() - 1) {
            val partner = unparsedPartners[i].asJsonObject
            partner.addProperty("selected", 0)
            partner.addProperty("favorite", 0)

            val partnerCat = partner.get("c").asInt
            partner.addProperty("c", categoryIndexToString(partnerCat))

            try {
                featureCollectionList.add(
                    Feature.fromGeometry(
                        Point.fromLngLat(partner.get("lng").asDouble, partner.get("lat").asDouble),
                        partner)
                )
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }

        featureCollection = FeatureCollection.fromFeatures(featureCollectionList)

        file.writeText(featureCollection.toJson())
        prefs.edit().putLong(context.getString(R.string.sp_key_last_update), currentTime).apply()

    } else {
        Log.d(TAG, "Loading features from cache")
        featureCollection = FeatureCollection.fromJson(file.readText())
    }

    return featureCollection
}

fun categoryIndexToString(i: Int): String {
    when (i) {
        1 -> return "restaurant"
        2 -> return "theatre"
        3 -> return "bar"
        4 -> return "recycling"
        5 -> return "fitness-centre"
        6 -> return "star"
    }
    throw InvalidParameterException()
}