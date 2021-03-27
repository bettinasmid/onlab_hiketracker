package hu.bme.aut.android.hiketracker.service

import android.content.Intent
import android.location.Location
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.*
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.utils.LocationProvider
import hu.bme.aut.android.hiketracker.utils.NotificationHandler
import kotlinx.coroutines.*
import java.lang.Integer.max

class PositionCheckerService : LifecycleService(), LocationProvider.OnNewLocationAvailable {
    //logging
    private var logger = TrackerApplication.logger

    private var enabled = false
    private val NOTIF_FOREGROUND_ID = 8
    //utilities
    private lateinit var notificationHandler : NotificationHandler
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val mediaPlayer =  MediaPlayer().apply {
        setOnPreparedListener { start() }
        setOnCompletionListener { reset() }
    }
    val handler = Handler(Looper.getMainLooper())

    private lateinit var repo: PointRepository
    private lateinit var locationProvider: LocationProvider
    //data
    private lateinit var trackPoints: LiveData<List<Point>>
    private lateinit var locationPoints : List<Location>
    //location monitoring variables
    private var currentLocation: Location? = null
    private var lastVisitedIndex: Int = -1
    private lateinit var lastCorrectLocation: Location
    private var currentSegment = mutableListOf<Location>()
    private var userLost : Boolean = false
    private  var t : Int = 0 //track segment partitioning factor

    override fun onCreate() {
        super.onCreate()
        locationProvider = LocationProvider(applicationContext,this)
        val pointDao = TrackerApplication.pointDatabase.pointDao()
        notificationHandler = NotificationHandler(
            NOTIF_FOREGROUND_ID = NOTIF_FOREGROUND_ID,
            NOTIFICATION_CHANNEL_ID = "hike_tracker_notifications",
            NOTIFICATION_CHANNEL_NAME = "Hike Tracker notifications",
            context = this
        )
        repo = PointRepository(pointDao)
        trackPoints = repo.getAllPoints()
    }

    override fun onBind(p0: Intent): IBinder? {
        TODO("Not yet implemented")
        logger.log("onBind called")
        super.onBind(p0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_FOREGROUND_ID, notificationHandler.createNotification("Return to app"))

            //TODO on new thread
        trackPoints.observe(this, Observer {
            logger.log("Observer triggered")
            if(it != null) {
                if(!enabled) {
                    enabled = true
                 //   serviceScope.launch{
                        convertTrackPointsToLocation(it)
                        calculateSegmentDivisionFactor()
                        lastCorrectLocation = locationPoints[0]
                        updateCurrentSegment()
                  //  }
                    startLocationMonitoring()
                }
            }
            logger.log("\tcontent is null: ${it==null}")
        })

        return START_STICKY
    }

    private fun convertTrackPointsToLocation(points: List<Point>){
        logger.log("convertTrackPointsToLocation called")
        locationPoints =  points.map{
           it.toLocation()
        }
    }

    private fun calculateSegmentDivisionFactor(){
        var fullDistance : Float = 0.0F
        for(i in 0..locationPoints.size-2)
            fullDistance+=(locationPoints[i].distanceTo(locationPoints[i+1]))
        t = ((fullDistance / locationPoints.size.toFloat())/10.0F).toInt()
       // handler.post{
         //   Runnable(){
                logger.log("calculateSegmentDivisionFactor called\n\ttotal distance:\t${fullDistance} meters\n\tnumber of trackpoints:\t${locationPoints.size}\n\tt:\t$t")
           // }
       // }
    }

    override fun onNewLocation(location: Location) {
        if(locationIsValid(location)) {
          //  Toast.makeText(this, "${location.latitude},${location.longitude},\nbearing:${location.bearing}\nspeed:${location.speed}", Toast.LENGTH_SHORT).show()
            logger.log("onNewLocation: (${location.latitude},${location.longitude})\t" +
                    "bearing:${location.bearing}\t" +
                    "speed:${location.speed}")
         //   serviceScope.launch {
                //check if user visited a new point along the track, mark it visited
                searchCurrentLocationOnTrack(location)
                //notify user if heading in the wrong direction
                checkPosition(location)
          //  }
        }
    }

    //TODO check backwards
    private fun checkPosition(location: Location){
        fun compareBearings(b1 : Float, b2: Float): Boolean{
            return (b1-b2) < 20 || (b1-b2) > 340
        }
        logger.log("checkPosition - user lost: ${userLost}")
        if(lastVisitedIndex == -1){
            logger.log("\tuser hasn't started yet")
            return
        }
        //if lost, we want them to head a) back to the point where they left the track b) towards one of the few next points on track
        val userCloseToTrack = currentSegment.any{loc -> location.distanceTo(loc) < 11.0F}
        if(userLost){ // user has been off track, check their direction
            if(userCloseToTrack) { //found the way back
                logger.log("\tuser was lost but found the way back to track")
                userLost = false
                return
            }
            val userHeadingBack = compareBearings(location.bearing, location.bearingTo(lastCorrectLocation))
            val userSkippedPoint = currentSegment.any{loc -> compareBearings(location.bearing, location.bearingTo(loc))}
            logger.log("\tuser is heading to lastCorrectLocation: ${userHeadingBack}")
            logger.log("\tuser skipped point(s) but back on track again: ${userSkippedPoint}")
            if(!userHeadingBack && !userSkippedPoint) { //they are failing to head back to the track, notify them
                notifyUser()
                return
            }  //else do nothing, they are in the process of getting back

        } else{ //they've been following the track so far
            logger.log("\tuser is close enough to tracksegment: ${userCloseToTrack}")
            if(userCloseToTrack){ //still on track
                lastCorrectLocation = location
                userLost = false
                logger.log("\tswitched off lost mode")
            }
            else{ // user left the track, notify them
                userLost = true
                notifyUser()
                logger.log("\tuser notified, lost mode is on")
            }
        }
    }

    private fun searchCurrentLocationOnTrack(location: Location){
        val checkPoint : Location? = locationPoints.find{ it -> it.matches(location) }
        logger.log("searchCurrentLocationOnTrack")
        if(checkPoint != null) {
            if(userLost) //switch lost mode off if they reached a valid trackpoint again
                userLost = false
            lastVisitedIndex = locationPoints.indexOf(checkPoint)
            lastCorrectLocation = location

            Toast.makeText(this, "checkpoint: $lastVisitedIndex", Toast.LENGTH_SHORT).show()
            logger.log("\tcheckpoint $lastVisitedIndex reached")

            updateCurrentSegment()
            serviceScope.launch{
                repo.markVisited(trackPoints.value!![lastVisitedIndex])
            }
        } else
            logger.log("\tcurrent location is not a checkpoint")
    }

    private fun updateCurrentSegment(){
        val p0 = if (lastVisitedIndex == -1) locationPoints[0] else locationPoints[lastVisitedIndex]
        val p1 = if (lastVisitedIndex == -1) locationPoints[1] else locationPoints[lastVisitedIndex+1]
        val vecToNext = p1-p0
        val dirVector = vecToNext.normalize()
        val step = vecToNext.length()/t
        logger.log("updateCurrentSegment - between " +
                "p0(${p0.toVectorString()}) and p1(${p1.toVectorString()})\n\t" +
                "directionVector:\t${vecToNext.toVectorString()}\tnormalized: ${dirVector.toVectorString()}\n\t" +
                "distance: ${p0.distanceTo(p1)} meters")
        currentSegment.clear()
        currentSegment.add(p0)
        logger.log("\tcurrentSegment points:")
        for(i in 1 until t){
            val newPoint = p0+dirVector*step.toFloat()*i.toFloat()
            currentSegment.add(newPoint)
            logger.log("\t\tcurrentSegment point $i: ${newPoint.toVectorString()}")
        }
    }

    private fun notifyUser(){
        //lifecycleScope.launch {
            mediaPlayer.run{
                reset()
                val sound = resources.openRawResourceFd(R.raw.beep)
                mediaPlayer.setDataSource(sound.fileDescriptor)
                prepare()
            }
            logger.log("\tuser notified")
        //}
       // handler.post{
         //   Runnable(){
                Toast.makeText(this, "Wrong direction!", Toast.LENGTH_LONG).show()
           // }
       // }
    }

    private fun startLocationMonitoring(){
        locationProvider.startLocationMonitoring()
    }

    private fun locationIsValid(location: Location): Boolean{
        if((location.hasSpeed() && location.speed < 4 && location.speed>0.2) || currentLocation==null) return true
        if(location.distanceTo(currentLocation)<200) return true
        return false
    }

    override fun onDestroy(){
        stopForeground(true)
        enabled = false
        locationProvider.stopLocationMonitoring()
        logger.log("onDestroy called")
        logger.writeToSDFile()
        super.onDestroy()
    }

    private  fun checkDirectionBasedOnBearing(location: Location){
        //receive visited point
        //find the last visited point and the next one in the list
        //compare the bearing of the route between them with the current location's bearing
        //should be aligned if user is heading in the right direction

        fun compareBearings(b1 : Float, b2: Float): Boolean{
            return (b1-b2) < 20 || (b1-b2) > 340
        }

        if(location.hasBearing() && location.speed>0.1F) {
            if(userLost){
                //we want them to head a) back to the point they left the track b) towards one of the few next points on track
                var directionOk = false
                if(compareBearings(location.bearing, location.bearingTo(lastCorrectLocation))) // a)
                    directionOk = true

                for(i in max(lastVisitedIndex,0)..lastVisitedIndex+2) // b)
                    if(compareBearings(location.bearing, location.bearingTo(locationPoints[i])))
                        directionOk = true
                if(!directionOk)
                    notifyUser()
            }
            else{
                if(compareBearings(location.bearing, location.bearingTo(locationPoints[lastVisitedIndex+1]))){
                    lastCorrectLocation = location
                    return
                }
                notifyUser()
                userLost = true
            }
        }
    }

}