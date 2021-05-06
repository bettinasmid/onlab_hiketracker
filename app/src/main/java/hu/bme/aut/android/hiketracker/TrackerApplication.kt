package hu.bme.aut.android.hiketracker

import android.app.Application
import hu.bme.aut.android.hiketracker.data.PointDatabase
import hu.bme.aut.android.hiketracker.data.getDatabase

class TrackerApplication : Application() {

    companion object {
        lateinit var pointDatabase: PointDatabase
            private set
        const val SHARED_PREFERENCES_NAME = "appPrefs"
        const val TAG_TOTAL_DISTANCE = "total_distance"
    }

    override fun onCreate() {
        super.onCreate()
        pointDatabase = getDatabase(this)
        //change here if no logging required
    }
}