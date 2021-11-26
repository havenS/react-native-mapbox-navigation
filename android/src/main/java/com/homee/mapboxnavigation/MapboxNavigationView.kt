package com.homee.mapboxnavigation

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.LinearLayout
import com.facebook.react.uimanager.ThemedReactContext
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import android.graphics.drawable.Drawable
import com.mapbox.maps.plugin.compass.compass
import java.net.URL
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.facebook.react.bridge.*
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap

@SuppressLint("ViewConstructor")
class MapboxNavigationView(private val context: ThemedReactContext, private val mCallerContext: ReactApplicationContext): LinearLayout(context.baseContext) {
    private var origin: Point? = null
    private var destination: Point? = null
    private var shouldSimulateRoute = false
    private var showsEndOfRouteFeedback = false
    private var mapToken: String? = null
    private var navigationToken: String? = null
    private var camera: ReadableMap? = null
    private var destinationMarker: Drawable? = null
    private var userLocatorMap: Drawable? = null
    private var userLocatorNavigation: Drawable? = null
    private var styleURL: String? = null
    private var transportMode: String = "bike"
    private var showUserLocation = false
    private var markers: ReadableArray? = null
    private var polylines: ReadableArray? = null

    var mapboxMap: MapboxMap? = null
    private var mapView: MapView? = null
    private var mapboxNavigation: MapboxNavigationNavigation? = null

    private var isNavigation = false
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var polylineAnnotation: PolylineAnnotation? = null
    private var pointAnnotation: PointAnnotation? = null
    private var pointAnnotationManager: PointAnnotationManager? = null

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .build()

        mapboxMap?.setCamera(cameraOptions)
    }
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener { bearing ->
        val cameraOptions = CameraOptions.Builder()
            .bearing(bearing)
            .build()

        mapboxMap?.setCamera(cameraOptions)
    }
    private val onMoveListener = object : OnMoveListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }
        override fun onMoveBegin(detector: MoveGestureDetector) {
            sendEvent("onMapMove", Arguments.createMap())
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    companion object {
        var instance: MapboxNavigationView? = null
    }

    init {
        instance = this
    }

    @SuppressLint("ClickableViewAccessibility")
    fun createMap() {
        if (mapView != null) return

        ResourceOptionsManager.getDefault(context.baseContext, mapToken!!)
        val layout = inflate(context, R.layout.mapview_layout, this)

        mapView = layout.findViewById(R.id.mapView)

        mapView?.setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_UP) {
                val event = Arguments.createMap()
                event.putString("onTap", "")
                context.getJSModule(RCTEventEmitter::class.java).receiveEvent(this.id, "onTap", event)
            }
            false
        }

        mapView?.let { mapView ->
            mapboxMap = mapView.getMapboxMap()
            mapView.logo.marginLeft = 3000.0F
            mapView.compass.enabled = false
            mapView.attribution.iconColor = Color.TRANSPARENT
            mapView.scalebar.enabled = false

            mapboxMap?.addOnMoveListener(onMoveListener)

            val annotationApi = mapView.annotations

            polylineAnnotationManager = annotationApi.createPolylineAnnotationManager(mapView)
            pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView)
        }

        updateMap()
    }

    private fun updateMap() {
        if (styleURL != null) {
            mapboxMap?.loadStyleUri(styleURL!!) {
                customizeMap()
            }
        } else {
            customizeMap()
        }
    }

    private fun customizeMap() {
        if (showUserLocation) {
            mapView?.location?.updateSettings {
                enabled = true
                pulsingEnabled = false
            }
        }

        if (userLocatorMap != null) {
            mapView?.location?.locationPuck = LocationPuck2D(
                topImage = userLocatorMap,
            )
        }

        addMarkers()
        addPolylines()

        if (!isNavigation) {
            fitCameraForAnnotations()
        }
    }

    private fun addPolylines() {
        Handler(Looper.getMainLooper()).post {
            if (mapView != null) {
                deletePolylines()
                if (polylines != null && polylineAnnotationManager != null && polylines!!.size() > 0) {
                    for (i in 0 until polylines!!.size()) {
                        val coordinates = mutableListOf<Point>()
                        val polylineInfo = polylines!!.getMap(i)!!
                        val polyline = polylineInfo.getArray("coordinates")
                        val color = polylineInfo.getString("color")
                        val opacity =
                            if (polylineInfo.hasKey("opacity")) polylineInfo.getDouble("opacity") else 1.0

                        for (j in 0 until polyline!!.size()) {
                            val polylineArr = polyline.getArray(j)!!
                            val lat = polylineArr.getDouble(0)
                            val lng = polylineArr.getDouble(1)
                            val point = Point.fromLngLat(lng, lat)

                            coordinates.add(point)
                        }

                        val polylineAnnotationOptions = PolylineAnnotationOptions()
                            .withPoints(coordinates)
                            .withLineColor(color ?: "#00AA8D")
                            .withLineWidth(10.0)
                            .withLineOpacity(opacity)
                        polylineAnnotation =
                            polylineAnnotationManager!!.create(polylineAnnotationOptions)

                    }
                }
            }
        }
    }

    private fun addMarkers() {
        Handler(Looper.getMainLooper()).post {
            if (mapView != null) {
                if (markers != null && markers!!.size() > 0) {
                    DoAsync {
                        for (i in 0 until markers!!.size()) {
                            val marker = markers!!.getMap(i)!!

                            val markerLatitude = marker.getDouble("latitude")
                            val markerLongitude = marker.getDouble("longitude")

                            val markerIcon = marker.getMap("image")!!
                            val markerUrl = markerIcon.getString("uri") ?: return@DoAsync
                            val icon = getDrawableFromUri(markerUrl)
                            val point = Point.fromLngLat(markerLongitude, markerLatitude)
                            val pointAnnotationOptions: PointAnnotationOptions =
                                PointAnnotationOptions()
                                    .withPoint(point)

                            if (icon !== null) {
                                pointAnnotationOptions.withIconImage(icon.getBitmap())
                            }

                            pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
                        }
                    }
                } else {
                    if (pointAnnotation != null) {
                        pointAnnotationManager?.deleteAll()
                        pointAnnotation = null
                    }
                }
            }
        }
    }

    private fun fitCameraForAnnotations() {
        val points = mutableListOf<Point>()

        // add polylines points
        if (polylines != null) {
            for (i in 0 until polylines!!.size()) {
                val polylineInfo = polylines!!.getMap(i)!!
                val polyline = polylineInfo.getArray("coordinates")

                for (j in 0 until polyline!!.size()) {
                    val polylineArr = polyline.getArray(j)!!
                    val lat = polylineArr.getDouble(0)
                    val lng = polylineArr.getDouble(1)
                    val point = Point.fromLngLat(lng, lat)

                    points.add(point)
                }
            }
        }

        // add markers points
        if (markers != null) {
            for (i in 0 until markers!!.size()) {
                val marker = markers!!.getMap(i)!!
                val markerLatitude = marker.getDouble("latitude")
                val markerLongitude = marker.getDouble("longitude")
                val point = Point.fromLngLat(markerLongitude, markerLatitude)

                points.add(point)
            }
        }

        if (points.size > 0) {
            val newCameraOptions = mapboxMap!!.cameraForCoordinates(
                points,
                EdgeInsets(
                    if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 62.0 else 42.0,
                    72.0,
                    if (camera!!.hasKey("offset") && camera!!.getBoolean("offset")) 328.0 else 32.0,
                    72.0
                )
            )
            mapboxMap?.setCamera(newCameraOptions)
        } else {
            updateCamera()
        }
    }

    private fun updateCamera() {
        if (camera != null) {
            val center = try {
                    Point.fromLngLat(
                        camera!!.getArray("center")!!.getDouble(1),
                        camera!!.getArray("center")!!.getDouble(0)
                    )
                } catch (e: Exception) {
                    mapboxMap?.cameraState?.center
                }

            val zoom = try {
                camera!!.getDouble("zoom")
            } catch (e: Exception) {
                15.0
            }

            val pitch = try {
                camera!!.getDouble("pitch")
            } catch (e: Exception) {
                0.0
            }

            val cameraOptions = CameraOptions.Builder()
                .center(center)
                .zoom(zoom)
                .pitch(pitch)
                .build()

            mapboxMap?.setCamera(cameraOptions)
        }
    }

    private fun sendEvent(name: String, data: WritableMap) {
        context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, data)
    }

    private fun deletePolylines() {
        if (polylineAnnotation != null) {
            polylineAnnotationManager?.deleteAll()
            polylineAnnotation = null
        }
    }

    fun startNavigation() {
        if (navigationToken != null
            && destination != null
            && mapView != null
        ) {
            deletePolylines()
            setFollowUser(false)

            Handler(Looper.getMainLooper()).post {
                mapboxNavigation =
                    MapboxNavigationNavigation(context, navigationToken!!, id, mapView!!)
                mapboxNavigation?.startNavigation(
                    origin!!,
                    destination!!,
                    transportMode,
                    shouldSimulateRoute
                )
            }
            isNavigation = true
        }
    }

    fun stopNavigation() {
        if (isNavigation && mapboxNavigation != null) {
            isNavigation = false
            setFollowUser(true)

            mapboxNavigation!!.stopNavigation()
        }
    }

    fun startTracking() {
        isNavigation = true
        setFollowUser(true)

        mapView?.let {
            mapView!!.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            mapView!!.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        }
    }

    fun stopTracking() {
        isNavigation = false

        mapView?.let {
            mapView!!.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            mapView!!.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        }
        updateCamera()
    }

    fun setOrigin(origin: Point?) {
        this.origin = origin
    }

    fun setDestination(destination: Point?) {
        this.destination = destination
    }

    fun setFollowUser(followUser: Boolean) {
        mapView?.let {
            if (followUser) {
                mapView!!.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            } else {
                mapView!!.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            }
        }
    }

    fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
        this.shouldSimulateRoute = shouldSimulateRoute
    }

    fun setShowsEndOfRouteFeedback(showsEndOfRouteFeedback: Boolean) {
        this.showsEndOfRouteFeedback = showsEndOfRouteFeedback
    }

    fun setMapToken(mapToken: String) {
        val needCreation = this.mapToken == null
        this.mapToken = mapToken

        if (needCreation) {
            createMap()
        }
    }

    fun setTransportMode(transportMode: String?) {
        if(transportMode != null) {
            this.transportMode = transportMode
        }
    }

    fun setNavigationToken(navigationToken: String) {
        this.navigationToken = navigationToken
    }

    fun setCamera(camera: ReadableMap) {
        val offset = if(camera.hasKey("offset"))
            camera.getBoolean("offset")
        else
            if(this.camera != null && this.camera!!.hasKey("offset"))
                this.camera!!.getBoolean("offset")
            else
                false
        val center = if(camera.hasKey("center"))
            camera.getArray("center")
        else
            if(this.camera != null && this.camera!!.hasKey("center"))
                this.camera!!.getArray("center")
            else
                null
        val zoom = if(camera.hasKey("zoom"))
            camera.getDouble("zoom")
        else
            if(this.camera != null && this.camera!!.hasKey("zoom"))
                this.camera!!.getDouble("zoom")
            else
                null
        val pitch = if(camera.hasKey("pitch"))
            camera.getDouble("pitch")
        else
            if(this.camera != null && this.camera!!.hasKey("pitch"))
                this.camera!!.getDouble("pitch")
            else
                null

        val newCamera = Arguments.createMap()
        if (center != null) {
            val centerWritableArray = Arguments.createArray()
            centerWritableArray.pushDouble(center.getDouble(0))
            centerWritableArray.pushDouble(center.getDouble(1))
            newCamera.putArray("center", centerWritableArray)
        }
        if (zoom != null) newCamera.putDouble("zoom", zoom)
        if (pitch != null) newCamera.putDouble("pitch", pitch)
        newCamera.putBoolean("offset", offset)

        this.camera = newCamera

        updateCamera()
    }

    fun setDestinationMarker(destinationMarker: ReadableMap) {
        DoAsync {
            val imageUrl = destinationMarker.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
            this.destinationMarker = drawable
            updateMap()
        }
    }

    fun setUserLocatorMap(userLocatorMap: ReadableMap) {
        DoAsync {
            val imageUrl = userLocatorMap.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
            this.userLocatorMap = drawable
            updateMap()
        }
    }

    fun setUserLocatorNavigation(userLocatorNavigation: ReadableMap) {
        DoAsync {
            val imageUrl = userLocatorNavigation.getString("uri")
            val drawable: Drawable? = getDrawableFromUri(imageUrl)
            this.userLocatorNavigation = drawable
            updateMap()
        }
    }

    fun setStyleURL(styleURL: String) {
        this.styleURL = styleURL
        updateMap()
    }

    fun setShowUserLocation(showUserLocation: Boolean) {
        this.showUserLocation = showUserLocation
        updateMap()
    }

    fun setMarkers(markers: ReadableArray?) {
        this.markers = markers
    }

    fun setPolylines(polylines: ReadableArray?) {
        this.polylines = polylines
        updateMap()
    }

    fun onDropViewInstance() {
        mapView?.onDestroy()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun getDrawableFromUri(imageUrl: String?): Drawable? {
        val drawable = if (imageUrl?.contains("http") == true) {
            val inputStream = URL(imageUrl).openStream()
            Drawable.createFromStream(inputStream, "src")
        } else {
            val resourceId = mCallerContext.resources.getIdentifier(
                imageUrl,
                "drawable",
                mCallerContext.packageName
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources.getDrawable(
                    resourceId,
                    mCallerContext.theme
                )
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }
        }

        return drawable
    }

    @SuppressLint("NewApi")
    class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        init {
            execute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }
}
