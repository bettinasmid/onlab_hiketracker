package hu.bme.aut.android.hiketracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "point")
data class RoomPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ordinal : Int,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double
)
