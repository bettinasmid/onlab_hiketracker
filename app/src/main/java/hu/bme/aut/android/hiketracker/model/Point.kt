package hu.bme.aut.android.hiketracker.model

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
}