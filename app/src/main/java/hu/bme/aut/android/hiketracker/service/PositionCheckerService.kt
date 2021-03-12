package hu.bme.aut.android.hiketracker.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import hu.bme.aut.android.hiketracker.ui.MainActivity
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.utils.LocationProvider
import hu.bme.aut.android.hiketracker.utils.NotificationHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions

class PositionCheckerService : Service(), LocationProvider.OnNewLocationAvailable {

    private var enabled = false
    private val NOTIF_FOREGROUND_ID = 8
    private var currentLocation: Location? = null
    private var lastCheckPoint : Location? = null
    private val repo: PointRepository
    private var trackPoints = listOf<Location>()
    private var closestSegmentOnTrack = listOf<Location>()
    private val notificationHandler : NotificationHandler
    private lateinit var locationProvider: LocationProvider

    init{
        val pointDao = TrackerApplication.pointDatabase.pointDao()
        repo = PointRepository(pointDao)
        notificationHandler = NotificationHandler(
            NOTIF_FOREGROUND_ID = NOTIF_FOREGROUND_ID,
            NOTIFICATION_CHANNEL_ID = "hike_tracker_notifications",
            NOTIFICATION_CHANNEL_NAME = "Hike Tracker notifications",
            context = this
        )
    }

    override fun onCreate() {
        super.onCreate()
        locationProvider = LocationProvider(applicationContext,this)
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_FOREGROUND_ID, notificationHandler.createNotification("Return to app"))

        if (!enabled) {
            enabled = true
            //TODO on new thread
           startLocationMonitoring()
        }

        return START_STICKY
    }

    private fun populateTrackPoints(){
        val trackPoints = repo.getAllPoints().value?.map{ point ->
            Location("").apply {
                latitude = point.latitude
                longitude = point.longitude
                altitude = point.elevation
            }
        }?: trackPoints
    }

    override fun onNewLocation(location: Location) {
        if(locationIsValid(location)) {
            checkDirection(location)

        }

    }

    private fun checkDirection(location: Location){
        //check visited point
        //find it and the next one in the list
        //calculate bearing between them
        //check if location bearing aligns with the calculated one
    }

    private fun findClosestPointOnTrack(location: Location){
        var minDistance = Float.MAX_VALUE
        for(loc in trackPoints){
            val distance = location.distanceTo(loc)
            if(distance < minDistance)
                minDistance = distance
        }
    }

    fun startLocationMonitoring(){
        locationProvider.startLocationMonitoring()
    }

    override fun onDestroy(){
        locationProvider.stopLocationMonitoring()
        super.onDestroy()
    }

    private fun locationIsValid(location: Location): Boolean{
        if((location.hasSpeed() && location.speed < 4) || currentLocation==null) return true
        if(location.distanceTo(currentLocation)<200) return true
        return false
    }

    fun Location.Equals(location: Location): Boolean{
        return this.distanceTo(location) < 20.0
    }

}