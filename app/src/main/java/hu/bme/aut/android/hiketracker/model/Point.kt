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

//    //returns a direction vector pointing from this to the other point
//    //calculating as though cartesian coordinates
//    fun minus(other: Point) : Point{
//        return Point(
//            id = -1,
//            ordinal = -1,
//            visited = false,
//            latitude = other.latitude - this.latitude,
//            longitude = other.longitude - this.longitude,
//            elevation = -1.0
//        )
//    }
//
//    //returns actual point
//    fun plus(other: Point) : Point{
//        return Point(
//            id = -1,
//            ordinal = -1,
//            visited = false,
//            latitude = other.latitude + this.latitude,
//            longitude = other.longitude + this.longitude,
//            elevation = -1.0
//        )
//    }
//
//    //used for vector normalization
//    fun times(scalar : Float): Point{
//        return Point(
//            id = -1,
//            ordinal = -1,
//            visited = false,
//            latitude = latitude * scalar,
//            longitude = longitude * scalar,
//            elevation = -1.0
//        )
//    }
}