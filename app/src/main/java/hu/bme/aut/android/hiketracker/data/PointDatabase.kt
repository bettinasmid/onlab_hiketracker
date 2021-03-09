package hu.bme.aut.android.hiketracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 1,
    exportSchema = false,
    entities = [RoomPoint::class]
)
abstract class PointDatabase :RoomDatabase(){
    abstract  fun pointDao() : PointDao
}