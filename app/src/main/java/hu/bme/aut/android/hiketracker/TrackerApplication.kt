package hu.bme.aut.android.hiketracker

import android.app.Application
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import hu.bme.aut.android.hiketracker.data.PointDatabase
import hu.bme.aut.android.hiketracker.data.getDatabase
import hu.bme.aut.android.hiketracker.logger.Logger

class TrackerApplication : Application() {
    companion object {
        lateinit var pointDatabase: PointDatabase
            private set
        lateinit var logger: Logger //TODO
        const val SHARED_PREFERENCES_NAME = "appPrefs"
        const val TAG_TOTAL_DISTANCE = "total_distance"
    }

    override fun onCreate() {
        super.onCreate()
        pointDatabase = getDatabase(this)
        logger = Logger(this)
        //change here if no logging required
        logger.enabled = true
    }
}