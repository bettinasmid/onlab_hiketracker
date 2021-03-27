package hu.bme.aut.android.hiketracker.service

import android.location.Location
import hu.bme.aut.android.hiketracker.model.Point
import java.lang.Math.sqrt
//Location coordinates treated as cartesian coordinates
//returns a direction vector pointing from this to the other point
//calculating as though cartesian coordinates
operator fun Location.minus(other: Location) : Location {
    val lat = this.latitude
    val lng = this.longitude
    return Location("").apply {
        latitude = lat - other.latitude
        longitude = lng -  other.longitude
    }
}

//returns actual point
operator fun Location.plus(other: Location) : Location {
    val lat = this.latitude
    val lng = this.longitude
    return Location("").apply {
        latitude = lat + other.latitude
        longitude = lng +  other.longitude
    }
}

//used for vector normalization
operator fun Location.times(scalar : Float): Location{
    val lat = this.latitude
    val lng = this.longitude
    return Location("").apply {
        latitude = lat*scalar
        longitude = lng*scalar
    }
}

fun Location.normalize() : Location{
    val reciprocalLength = 1/this.length()
    val lat = this.latitude
    val lng = this.longitude
    return Location("").apply{
        latitude = lat*reciprocalLength
        longitude = lng*reciprocalLength
    }
}

fun Location.length(): Double{
    val x = this.latitude.toDouble()
    val y = this.longitude.toDouble()
    return kotlin.math.sqrt(x * x + y * y)
}

fun Location.matches(other: Any?): Boolean{
    return this.distanceTo(other as Location) < 20.0
}

fun Location.toVectorString(): String{
    return "( ${this.latitude.toCoordString()} ; ${this.longitude.toCoordString()} )"
}

fun Double.toCoordString(): String{
    return "%.6f".format(this)
}

//fun Location.toPoint(): Point {
//    return Point(
//        latitude = this.latitude,
//        longitude = this.longitude,
//        elevation = this.altitude)
//}