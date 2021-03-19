package hu.bme.aut.android.hiketracker.service

import android.app.*
import android.content.Intent
import android.location.Location
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.utils.LocationProvider
import hu.bme.aut.android.hiketracker.utils.NotificationHandler
import kotlinx.coroutines.*

class PositionCheckerService : LifecycleService(), LocationProvider.OnNewLocationAvailable {
    private var enabled = false
    private val NOTIF_FOREGROUND_ID = 8
    //utilities
    private val notificationHandler : NotificationHandler
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val mediaPlayer =  MediaPlayer().apply {
        setOnPreparedListener { start() }
        setOnCompletionListener { reset() }
    }
    private val repo: PointRepository
    private lateinit var locationProvider: LocationProvider
    //data
    private lateinit var trackPoints: LiveData<List<Point>>
    private lateinit var locationPoints : List<Location>
    //location monitoring variables
    private var currentLocation: Location? = null
    private var lastVisitedIndex: Int = -1


    init{
        val pointDao = TrackerApplication.pointDatabase.pointDao()
        repo = PointRepository(pointDao)
        notificationHandler = NotificationHandler(
            NOTIF_FOREGROUND_ID = NOTIF_FOREGROUND_ID,
            NOTIFICATION_CHANNEL_ID = "hike_tracker_notifications",
            NOTIFICATION_CHANNEL_NAME = "Hike Tracker notifications",
            context = this
        )
        trackPoints = repo.getAllPoints()
    }

    override fun onCreate() {
        super.onCreate()
        locationProvider = LocationProvider(applicationContext,this)
    }

    override fun onBind(p0: Intent): IBinder? {
        TODO("Not yet implemented")
        super.onBind(p0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_FOREGROUND_ID, notificationHandler.createNotification("Return to app"))

            //TODO on new thread
        trackPoints.observe(this, Observer {
            if(it != null) {
                if(!enabled) {
                    enabled = true
                    serviceScope.launch{
                        locationPoints = convertTrackPointsToLocation(it)
                    }
                    startLocationMonitoring()
                }
            }
        })

        return START_STICKY
    }

    private suspend fun convertTrackPointsToLocation(points: List<Point>): List<Location>{
       return points.map{
           it.toLocation()
       }
    }

    override fun onNewLocation(location: Location) {
        if(locationIsValid(location)) {
            Toast.makeText(this, "${location.latitude},${location.longitude},${location.bearing}", Toast.LENGTH_SHORT).show()
            //notify user if heading in the wrong direction
            checkDirection(location)
            //check if user visited a new point along the track, mark it visited
            val visitedLocation : Location? = locationPoints.find{ it -> it.Equals(location) }

            if(visitedLocation != null) {
                val index = locationPoints.indexOf(visitedLocation)
                lastVisitedIndex = index
                Toast.makeText(this, "checkpoint: ${index}", Toast.LENGTH_SHORT).show()
                serviceScope.launch{
                    repo.markVisited(trackPoints.value!![index])
                }
            }
        }
    }

    private fun checkDirection(location: Location){
        //receive visited point
        //find the last visited point and the next one in the list
        //compare the bearing of the route between them with the current location's bearing
        //should be aligned if user is heading in the right direction

        fun compareBearings(b1 : Float, b2: Float): Boolean{
            return (b1-b2) < 20 || (b1-b2) > 340
        }
        if(location.hasBearing()) {
            if (lastVisitedIndex == -1 && !compareBearings(location.bearing, location.bearingTo(locationPoints[0])))
                notifyUser()
            else if (!compareBearings(location.bearing, locationPoints[lastVisitedIndex].bearingTo(locationPoints[lastVisitedIndex+1])))
                notifyUser()
        }
    }

    private fun notifyUser(){
        lifecycleScope.launch {
            mediaPlayer.run{
                reset()
                val sound = resources.openRawResourceFd(R.raw.beep)
                setDataSource(sound.fileDescriptor)
                prepare()
            }
        }
        Toast.makeText(this, "Wrong direction!", Toast.LENGTH_LONG).show()
    }

    private fun startLocationMonitoring(){
        locationProvider.startLocationMonitoring()
    }

    private fun locationIsValid(location: Location): Boolean{
        if((location.hasSpeed() && location.speed < 4) || currentLocation==null) return true
        if(location.distanceTo(currentLocation)<200) return true
        return false
    }

    fun Location.Equals(location: Location): Boolean{
        return this.distanceTo(location) < 20.0
    }

    private fun findCurrentLocationOnTrack(location: Location){
        var minDistance = Float.MAX_VALUE
        for(loc in locationPoints){
            val distance = location.distanceTo(loc)
            if(distance < minDistance)
                minDistance = distance
        }
    }

    override fun onDestroy(){
        stopForeground(true)
        enabled = false
        locationProvider.stopLocationMonitoring()
        super.onDestroy()
    }
}