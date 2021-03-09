package hu.bme.aut.android.hiketracker.model

import io.ticofab.androidgpxparser.parser.domain.TrackPoint

class Point(
    val id: Long? = null,
    val ordinal: Int,
    val latitude : Double,
    val longitude: Double,
    val elevation: Double
)