package de.d4f.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import de.d4f.R
import de.d4f.categoryIndexToString
import de.d4f.getPartnersFeatureCollection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {
    private lateinit var slideLayout: SlidingUpPanelLayout
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var bottomNavigationView: BottomNavigationView

    private val MAPBOX_LAYER_ID = "mapbox.poi.maki"
    private val MAPBOX_SOURCE_ID = "mapbox.poi"
    private val SEARCH_INTENT_REQUEST_CODE = 1
    private val LOGIN_INTENT_REQUEST_CODE = 2
    private val TAG = "MainActivity"

    private var featureCollection: FeatureCollection? = null
    private lateinit var geoJsonSource: GeoJsonSource
    private val permissionsManager = PermissionsManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(getString(R.string.shared_prefs_key), 0)
        val mapboxAccessToken = prefs.getString(getString(R.string.mapbox_access_token_key), null)

        if (mapboxAccessToken == null || mapboxAccessToken == "") {
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivityForResult(intent, LOGIN_INTENT_REQUEST_CODE)
        } else {
            Mapbox.getInstance(applicationContext, mapboxAccessToken)

            setContentView(R.layout.activity_main)
            slideLayout = findViewById(R.id.layout_main)
            bottomNavigationView = findViewById(R.id.bottom_nav_view)
            mapView = findViewById(R.id.mapView)

            slideLayout.panelHeight = 400
            slideLayout.isOverlayed = true
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN

            mapView!!.onCreate(savedInstanceState)
            mapView!!.getMapAsync(this)

            geoJsonSource = GeoJsonSource(MAPBOX_SOURCE_ID)

            // Bottom navigation view
            val onNavigationItemSelectedListener = bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_search -> {
                        val intent = Intent(applicationContext, SearchActivity::class.java)
                        startActivityForResult(intent, SEARCH_INTENT_REQUEST_CODE)
                    }
                    R.id.navigation_favorites -> {

                    }
                }
                false
            }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        this.mapboxMap.setStyle(Style.Builder().fromUri("mapbox://styles/justanothermapper/ckaqog8yq0aeg1ikcvuto245f")) { style ->

            geoJsonSource.setGeoJson(featureCollection)
            style.addSource(geoJsonSource)

            GlobalScope.launch {
                featureCollection = getPartnersFeatureCollection(applicationContext)

                runOnUiThread {
                    Log.i(TAG, "Refreshing features")
                    geoJsonSource.setGeoJson(featureCollection)
                }
            }

            val iconLayer = SymbolLayer(MAPBOX_LAYER_ID, MAPBOX_SOURCE_ID)
            style.addLayer(
                iconLayer.withProperties(
//                        PropertyFactory.iconOffset(Array<Float>(0f, -8f)),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconImage("{c}-15"),
//                        PropertyFactory.iconColor(resources.getColor(R.color.mapbox_blue))

                    /* when feature is in selected state, grow icon */
                    PropertyFactory.iconSize(
                        Expression.match(
                            Expression.get("selected"),
                            Expression.literal(1),     // default value
                            Expression.stop(0, 1), // If not selected, size * 1
                            Expression.stop(1, 2)  // If selected, size * 2
                        )
                    )
                )
            )

            mapboxMap.addOnMapClickListener { point ->
                handleOnMapClick(point, geoJsonSource)
                return@addOnMapClickListener true
            }

            enableLocationComponent(style, this.mapboxMap)
        }
    }

    private fun handleOnMapClick(point: LatLng, source: GeoJsonSource) {
        val screenPoint = mapboxMap.projection.toScreenLocation(point)
        val selectedFeatures = mapboxMap.queryRenderedFeatures(screenPoint, MAPBOX_LAYER_ID)
        val slideViewShort = findViewById<LinearLayout>(R.id.slideViewShort)
        val slideViewWebContent = findViewById<ScrollView>(R.id.slideViewWebContent)

        // If slide view is open, collapse it and return
        if (slideLayout.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            return
        }

        // Reset previously selected feature
        for (feature in featureCollection!!.features()!!) {
            feature.properties()!!.addProperty("selected", 0)
        }

        // Extract values of the selected feature
        if (!selectedFeatures.isEmpty()) {
            val selectedFeature     = selectedFeatures[0]
            val partnerName         = selectedFeature.getStringProperty("t")
            val partnerSavingType   = selectedFeature.getStringProperty("o")

            val partnerSavings: String
            partnerSavings =
                if (selectedFeature.hasNonNullValueForProperty("s"))
                    selectedFeature.getStringProperty("s")
                else ""

            val partnerRating: Int
            partnerRating =
                if (selectedFeature.hasNonNullValueForProperty("v"))
                    selectedFeature.getStringProperty("v").toInt()
                else 0

            val partnerUrl = "https://dresdenforfriends.de/" +
                    selectedFeature.getStringProperty("l")

            // Find the related feature in the featureCollection
            for (feature in featureCollection!!.features()!!) {
                if (feature.getStringProperty("nid").equals(
                        selectedFeature.getStringProperty("nid")
                    )
                ) {
                    feature.properties()!!.addProperty("selected", 1)
                }
            }

            // Reset slide view
            slideViewWebContent.removeAllViews()
            
            // Set the slide view
            val sliderTitle = findViewById<TextView>(R.id.slider_title)
            val sliderDescription = findViewById<TextView>(R.id.slider_description)
            val sliderSavings = findViewById<TextView>(R.id.slider_saving)
            val slideStars: List<ImageView> = listOf(
                findViewById(R.id.slider_star1),
                findViewById(R.id.slider_star2),
                findViewById(R.id.slider_star3),
                findViewById(R.id.slider_star4),
                findViewById(R.id.slider_star5)
            )

            sliderTitle.text = partnerName
            sliderDescription.text = partnerSavingType
            sliderSavings.text =
                if (partnerSavings != "")
                    getString(R.string.label_savings, partnerSavings)
                else
                    ""

            slideStars.forEachIndexed { index, view ->
                view.setImageResource(
                    if (partnerRating >= (index + 1) * 20)
                        R.drawable.star_black
                    else if (partnerRating >= (index + 1) * 20 - 10)
                        R.drawable.star_half_black
                    else
                        R.drawable.star_border
                )
            }

            val webView = WebView(applicationContext)
            webView.loadUrl(partnerUrl)
            slideViewWebContent.addView(webView)
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        } else {
            // Remove the slider view
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        }
        // Update map
        source.setGeoJson(featureCollection)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style, mapboxMap: MapboxMap) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Create and customize the LocationComponent's options
            val customLocationComponentOptions = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this,
                    R.color.mapbox_blue
                ))
                .build()

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .locationComponentOptions(customLocationComponentOptions)
                .build()

            // Get an instance of the LocationComponent and then adjust its settings
            mapboxMap.locationComponent.apply {

                // Activate the LocationComponent with options
                activateLocationComponent(locationComponentActivationOptions)

                // Enable to make the LocationComponent visible
                isLocationComponentEnabled = true

                // Set the LocationComponent's camera mode
                cameraMode = CameraMode.TRACKING

                // Set the LocationComponent's render mode
                renderMode = RenderMode.COMPASS
            }

        } else {
            permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SEARCH_INTENT_REQUEST_CODE) {
            if (data != null) {
                val chosenCategory = data.getIntExtra("chosenCategory", -1)

                if (chosenCategory != -1) {

                    // TODO: This could throw NetworkOnMainThreadException if cache too old
                    featureCollection = getPartnersFeatureCollection(this)
                    val newFeatureList = ArrayList<Feature>()
                    for (feature in featureCollection!!.features()!!) {
                        if (feature.getStringProperty("c") == categoryIndexToString(chosenCategory)) {
                            newFeatureList.add(feature)
                        }
                    }
                    featureCollection = FeatureCollection.fromFeatures(newFeatureList)
                    geoJsonSource.setGeoJson(featureCollection)
                }
            }
        } else if (requestCode == LOGIN_INTENT_REQUEST_CODE) {
            if (data != null && resultCode == Activity.RESULT_OK) {
                val token = data.getStringExtra("token")
                val prefs = getSharedPreferences(getString(R.string.shared_prefs_key), 0)
                prefs.edit().putString(getString(R.string.mapbox_access_token_key), token).apply()
                recreate()
            } else {
                finish()
            }
        }
    }

    private fun openWebPage(url: String) {
        val webpage: Uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webpage)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    // Add the mapView's own lifecycle methods to the activity's lifecycle methods
    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }


    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            if (mapboxMap.style != null) {
                enableLocationComponent(mapboxMap.style!!, this.mapboxMap);
            } else {
                Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    override fun onBackPressed() {
        if (slideLayout.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            return
        } else if (slideLayout.panelState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            slideLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN

            // Reset previously selected feature
            for (feature in featureCollection!!.features()!!) {
                feature.properties()!!.addProperty("selected", 0)
            }
            // Update map
            geoJsonSource.setGeoJson(featureCollection)

            return
        } else {
            super.onBackPressed()
        }
    }
}
