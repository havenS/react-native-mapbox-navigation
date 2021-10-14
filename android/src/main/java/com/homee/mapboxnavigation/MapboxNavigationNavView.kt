// package com.homee.mapboxnavigation

// import android.location.Location
// import com.facebook.react.bridge.Arguments
// import com.facebook.react.uimanager.ThemedReactContext
// import com.facebook.react.uimanager.events.RCTEventEmitter
// import com.mapbox.api.directions.v5.models.DirectionsRoute
// import com.mapbox.mapboxsdk.camera.CameraPosition
// import com.mapbox.navigation.base.trip.model.RouteProgress
// import com.mapbox.navigation.core.MapboxNavigation
// import com.mapbox.navigation.core.trip.session.LocationObserver
// import com.mapbox.navigation.core.trip.session.RouteProgressObserver
// import com.mapbox.navigation.ui.NavigationView
// import com.mapbox.navigation.ui.NavigationViewOptions
// import com.mapbox.navigation.ui.OnNavigationReadyCallback
// import com.mapbox.navigation.ui.listeners.NavigationListener

// class MapboxNavigationNavView(private val context: ThemedReactContext) : NavigationView(context.baseContext), NavigationListener,
//     OnNavigationReadyCallback {

//     init {
//         onCreate(null)
//         onResume()
//         initialize(this, getInitialCameraPosition())
//     }

//     private fun getInitialCameraPosition(): CameraPosition {
//         return CameraPosition.Builder()
//             .zoom(15.0)
//             .build()
//     }

//     override fun onCancelNavigation() {
//         val event = Arguments.createMap()
//         event.putString("onCancelNavigation", "Navigation Closed")
//         context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onCancelNavigation", event)
//     }

//     override fun onNavigationFinished() {

//     }

//     override fun onNavigationRunning() {

//     }

//     override fun onNavigationReady(isRunning: Boolean) {
//         //try {
// //            print("-------$navigationToken")
// //            val accessToken = navigationToken
// //            if (accessToken == null) {
// //                sendErrorToReact("Mapbox access token is not set")
// //                return
// //            }
// //
// //            if (origin == null || destination == null) {
// //                sendErrorToReact("origin and destination are required")
// //                return
// //            }
// //
// //            if (::navigationMapboxMap.isInitialized) {
// //                return
// //            }
// //
// //            //if (this.retrieveNavigationMapboxMap() == null) {
// //            //    sendErrorToReact("retrieveNavigationMapboxMap() is null")
// //            //    return
// //            //}
// //
// //            //this.navigationMapboxMap = this.retrieveNavigationMapboxMap()!!
// //
// //            //this.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it } // this does not work
// //
// //            // fetch the route
// //            val navigationOptions = MapboxNavigation
// //                .defaultNavigationOptionsBuilder(context, accessToken)
// //                .isFromNavigationUi(true)
// //                .build()
// //            //this.mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
// //            //this.mapboxNavigation.requestRoutes(RouteOptions.builder()
// //            //        .applyDefaultParams()
// //            //        .accessToken(accessToken)
// //            //        .coordinates(mutableListOf(origin, destination))
// //            //        .profile(RouteUrl.PROFILE_DRIVING)
// //            //        .steps(true)
// //            //        .voiceInstructions(true)
// //            //        .build(), routesReqCallback)
//         //} catch (ex: Exception) {
// //            sendErrorToReact(ex.toString())
//         //}
//     }

//     private fun startNav(route: DirectionsRoute) {
//         val optionsBuilder = NavigationViewOptions.builder(this.getContext())
//         optionsBuilder.navigationListener(this)
//         optionsBuilder.locationObserver(locationObserver)
//         optionsBuilder.routeProgressObserver(routeProgressObserver)
//         optionsBuilder.directionsRoute(route)
//         //optionsBuilder.shouldSimulateRoute(this.shouldSimulateRoute)
//         optionsBuilder.waynameChipEnabled(true)
//         this.startNavigation(optionsBuilder.build())
//     }

//     private val locationObserver = object : LocationObserver {
//         override fun onRawLocationChanged(rawLocation: Location) {

//         }

//         override fun onEnhancedLocationChanged(
//             enhancedLocation: Location,
//             keyPoints: List<Location>
//         ) {
//             val event = Arguments.createMap()
//             event.putDouble("longitude", enhancedLocation.longitude)
//             event.putDouble("latitude", enhancedLocation.latitude)
//             context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onLocationChange", event)
//         }
//     }

//     private val routeProgressObserver = object : RouteProgressObserver {
//         override fun onRouteProgressChanged(routeProgress: RouteProgress) {
//             val event = Arguments.createMap()
//             event.putDouble("distanceTraveled", routeProgress.distanceTraveled.toDouble())
//             event.putDouble("durationRemaining", routeProgress.durationRemaining.toDouble())
//             event.putDouble("fractionTraveled", routeProgress.fractionTraveled.toDouble())
//             event.putDouble("distanceRemaining", routeProgress.distanceRemaining.toDouble())
//             context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onRouteProgressChange", event)
//         }
//     }

//     override fun onFinalDestinationArrival(enableDetailedFeedbackFlowAfterTbt: Boolean, enableArrivalExperienceFeedback: Boolean) {
//         //super.onFinalDestinationArrival(this.showsEndOfRouteFeedback, this.showsEndOfRouteFeedback)
//         val event = Arguments.createMap()
//         event.putString("onArrive", "")
//         context.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, "onArrive", event)
//     }

//     override fun onStop() {
//         super.onStop()
//         //this.mapboxNavigation?.unregisterLocationObserver(locationObserver)
//     }
// }