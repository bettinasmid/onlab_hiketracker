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
        lateinit var logger: Logger
    }

    override fun onCreate() {
        super.onCreate()
        pointDatabase = getDatabase(this)
        logger = Logger(this)
        //change here if no logging required
        logger.enabled = true
    }
}