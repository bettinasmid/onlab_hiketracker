package hu.bme.aut.android.hiketracker

import android.app.Application
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import hu.bme.aut.android.hiketracker.data.PointDatabase

class TrackerApplication : Application() {
    companion object {
        lateinit var pointDatabase: PointDatabase
            private set
    }


    override fun onCreate() {
        super.onCreate()
        pointDatabase = databaseBuilder(
            applicationContext,
            PointDatabase::class.java,
            "point_database"
        ).fallbackToDestructiveMigration().build()
    }
}