package hu.bme.aut.android.hiketracker.service

import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.TAG_TOTAL_DISTANCE
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.logger
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import hu.bme.aut.android.hiketracker.utils.LocationProvider
import hu.bme.aut.android.hiketracker.utils.NotificationHandler
import kotlinx.coroutines.*
import java.lang.Integer.max

class PositionCheckerService : LifecycleService(), LocationProvider.OnNewLocationAvailable {

    public var enabled = false
    private val NOTIF_FOREGROUND_ID = 8
    //utilities
    private lateinit var notificationHandler : NotificationHandler
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val mediaPlayer = MediaPlayer().apply {
        setOnPreparedListener { start() }
        setOnCompletionListener { reset() }
    }

    //communication with other components
    val handler = Handler(Looper.getMainLooper())
    private lateinit var sp : SharedPreferences
    public lateinit var onViewUpdateNeededListener: OnViewUpdateNeededListener

    private lateinit var repo: PointRepository
    private lateinit var locationProvider: LocationProvider

    //data
    private lateinit var trackPoints: LiveData<List<Point>>
    private lateinit var locationPoints : List<Location>

    //location monitoring constans
    private val NUM_OF_SKIPPABLE_POINTS: Int = 3
    private val MAX_ALLOWABLE_METERS_TO_TRACK: Float = 15.0F
    private val AVG_METERS_BETWEEN_CHECKPOINTS : Float  = 10.0F
    private val BEARING_DISCREPANCY_TOLERANCE_DEGREES: Float = 10.0F
    private val MIN_ALLOWABLE_SPEED_MPS: Float = 0.2F
    private val MAX_ALLOWABLE_SPEED_MPS: Float = 4.0F
    private val LOCATION_UPDATE_CORRECTNESS_TOLERANCE: Float = 10.0F
    companion object{
        const val LOCATION_EQUALITY_TOLERANCE_METERS : Float = 8.0F
    }
    //location monitoring variables
    private var started : Boolean = false
    private var lastVisitedIndex: Int = -1
    private lateinit var lastCorrectLocation: Location
    private lateinit var lastLocation: Location
    private var currentSegment = mutableListOf<Location>()
    private var userLost : Boolean = false
    private  var t : Int = 0 //track segment partitioning factor
    private var totalDistance : Float = 0.0F
    private val binder : IBinder = PositionCheckerServiceBinder()

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
        sp = getSharedPreferences(TrackerApplication.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
    }

    override fun onBind(p0: Intent): IBinder? {
        logger.log("PositionCheckerService onBind")
        return binder
    }

    inner class PositionCheckerServiceBinder : Binder() {
        fun getService() : PositionCheckerService{
            return this@PositionCheckerService
        }
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
                }
            }
            logger.log("\tcontent is null: ${it==null}")
        })

        return START_STICKY
    }

    interface OnViewUpdateNeededListener{
        fun onViewUpdateNeeded(location: Location)
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
        t = ((fullDistance / locationPoints.size.toFloat())/AVG_METERS_BETWEEN_CHECKPOINTS).toInt()
       // handler.post{
         //   Runnable(){
                logger.log("calculateSegmentDivisionFactor called\n\ttotal distance:\t${fullDistance} meters\n\tnumber of trackpoints:\t${locationPoints.size}\n\tt:\t$t")
           // }
       // }
    }

    override fun onNewLocation(location: Location) {
        if(!started) {
            lastLocation = location
            started = true
        }
        if(locationIsValid(location)) {
          //  Toast.makeText(this, "${location.latitude},${location.longitude},\nbearing:${location.bearing}\nspeed:${location.speed}", Toast.LENGTH_SHORT).show()
            logger.log("onNewLocation: (${location.latitude},${location.longitude})\t" +
                    "bearing:${location.bearing}\t" +
                    "speed:${location.speed}")
         //   serviceScope.launch {
                //accumulate distance at every update
                totalDistance += location.distanceTo(lastLocation)
                lastLocation = location
                val editor = sp.edit()
                editor.putFloat(TAG_TOTAL_DISTANCE, totalDistance)
                editor.apply()
                onViewUpdateNeededListener.onViewUpdateNeeded(location)
                //check if user visited a new point along the track, mark it visited
                searchCurrentLocationOnTrack(location)
                //notify user if heading in the wrong direction
                checkPosition(location)
          //  }
        }
    }

    //input: the internal points of the current segment
    //decide if user is on track or at least heading back to the track.
    //if not, notify them
    private fun checkPosition(location: Location){
        fun compareBearings(b1 : Float, b2: Float): Boolean{
            return (b1-b2) < BEARING_DISCREPANCY_TOLERANCE_DEGREES || (b1-b2) > 360 - BEARING_DISCREPANCY_TOLERANCE_DEGREES
        }
        logger.log("checkPosition - user lost: ${userLost}")
        if(lastVisitedIndex == -1){
            logger.log("\tuser hasn't started yet")
            return
        }
        //if lost, we want them to head a) back to the point where they left the track b) towards one of the few next points on track
        val userCloseToTrack = currentSegment.any{loc -> location.distanceTo(loc) < MAX_ALLOWABLE_METERS_TO_TRACK}
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
                notifyUser("Wrong direction!")
                return
            }  //else do nothing, they are in the process of getting back

        } else{ //they've been following the track so far
            logger.log("\tuser is close enough to tracksegment: ${userCloseToTrack}")
            if(userCloseToTrack){ //still on track
                lastCorrectLocation = location
                lastLocation = location
                if(userLost) {
                    userLost = false
                    logger.log("\tswitched off lost mode")
                }
            }
            else{ // user left the track, notify them
                userLost = true
                notifyUser("Wrong direction!")
                logger.log("\tuser notified, lost mode is on")
            }
        }
    }

    //reconcile current location with the trackpoints. if a new point is reached on track:
    // 1) step to the next tracksegment and calculate internal segmentpoints
    // 2) update lastCorrectLocation, since this location is certainly correct
    // If no new po
    private fun searchCurrentLocationOnTrack(location: Location){
        val checkPoint : Location? = locationPoints.find{ it -> it.matches(location) }
        val idxOfCheckPoint = locationPoints.indexOf(checkPoint)
        logger.log("searchCurrentLocationOnTrack")
        if(checkPoint != null) {
            if(locationPoints.size - idxOfCheckPoint < NUM_OF_SKIPPABLE_POINTS &&
                idxOfCheckPoint - lastVisitedIndex > NUM_OF_SKIPPABLE_POINTS) { //jumped to the end of track, not good
                notifyUser("Started off in the wrong direction!")
                logger.log("reverse direction: checkpoint ${idxOfCheckPoint} reached <--> lastVisitedIndex = $lastVisitedIndex")
                return // do nothing. checkposition will alert if they continue in the wrong direction
            }
            if(userLost) //switch lost mode off if they reached a valid trackpoint again
                userLost = false
            if(lastVisitedIndex != idxOfCheckPoint) { //reached checkpoint that is not already visited
                lastVisitedIndex = idxOfCheckPoint
                updateCurrentSegment(idxOfCheckPoint)
                serviceScope.launch{
                    repo.markVisited(trackPoints.value!![lastVisitedIndex])
                }
                logger.log("\tcheckpoint $lastVisitedIndex reached, total distance: $totalDistance meters")
                Toast.makeText(this, "checkpoint: $lastVisitedIndex", Toast.LENGTH_SHORT).show()
            }
            lastCorrectLocation = location //either way, location is on track
        } else
            logger.log("\tcurrent location is not a checkpoint")
    }

    private fun updateCurrentSegment(p0idx: Int = 0){
        if(lastVisitedIndex == locationPoints.size-1)
            return
        val p0 = if (lastVisitedIndex == -1) locationPoints[p0idx] else locationPoints[lastVisitedIndex]
        val p1 = if (lastVisitedIndex == -1) locationPoints[p0idx+1] else locationPoints[lastVisitedIndex + 1]

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

    //TODO investigate why prepare is failing
    //sound from zapsplat.com
    private fun notifyUser(message: String){
        //lifecycleScope.launch {
        val sound = resources.openRawResourceFd(R.raw.notif) ?: return
        try {
            mediaPlayer.run{
                reset()
                setDataSource(sound.fileDescriptor)
                prepare()
            }
        } catch(e: Exception){
            Log.e("mediaplayer","Error: " + Log.getStackTraceString(e))
        }
            logger.log("\tuser notified")
        //}
       // handler.post{
         //   Runnable(){
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
           // }
       // }
    }

    public fun startLocationMonitoring(){
        locationProvider.startLocationMonitoring()
    }

    public fun stopLocationMonitoring(){
        locationProvider.stopLocationMonitoring()
        val editor = sp.edit()
        editor.clear()
        editor.apply()
    }

    private fun locationIsValid(location: Location): Boolean{
        if((location.hasSpeed() && location.speed < MAX_ALLOWABLE_SPEED_MPS && location.speed> MIN_ALLOWABLE_SPEED_MPS)) return true
        if(location.distanceTo(lastLocation)< LOCATION_UPDATE_CORRECTNESS_TOLERANCE) return true
        return false
    }

    override fun onDestroy(){
        stopForeground(true)
        mediaPlayer.release()
        enabled = false
        stopLocationMonitoring()
//        lifecycleScope.launch {
//            repo.deleteAllPoints()
//        }
        logger.log("Service onDestroy called")
        super.onDestroy()
    }

    //currently not used, but kept just in case
    private  fun checkDirectionBasedOnBearing(location: Location){
        //receive visited point
        //find the last visited point and the next one in the list
        //compare the bearing of the route between them with the current location's bearing
        //should be aligned if user is heading in the right direction

        fun compareBearings(b1 : Float, b2: Float): Boolean{
            return (b1-b2) < BEARING_DISCREPANCY_TOLERANCE_DEGREES || (b1-b2) > 360 - BEARING_DISCREPANCY_TOLERANCE_DEGREES
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
                    notifyUser("Wrong direction!")
            }
            else{
                if(compareBearings(location.bearing, location.bearingTo(locationPoints[lastVisitedIndex+1]))){
                    lastCorrectLocation = location
                    return
                }
                notifyUser("Wrong direction!")
                userLost = true
            }
        }
    }

}