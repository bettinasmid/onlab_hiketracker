package hu.bme.aut.android.hiketracker.model

import android.location.Location
import io.ticofab.androidgpxparser.parser.domain.TrackPoint

class Point(
    val id: Long = -1,
    val ordinal: Int,
    var visited : Boolean = false,
    val latitude : Double,
    val longitude: Double,
    val elevation: Double
){
    override fun toString(): String {
        return "point ${ordinal} id:${id} : ${latitude} ; ${longitude} ; ${elevation}"
    }

    fun toLocation(): Location {
        return Location("").apply {
            latitude = this@Point.latitude
            longitude = this@Point.longitude
            altitude = this@Point.elevation
        }
    }

/*    fun Location.toPoint(): Point{
        return Point(
            latitude = this.latitude,
            longitude = this.longitude,
            elevation = this.altitude)

    }*/
}