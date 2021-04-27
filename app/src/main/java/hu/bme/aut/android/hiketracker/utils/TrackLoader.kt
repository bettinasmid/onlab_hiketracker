package hu.bme.aut.android.hiketracker.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import hu.bme.aut.android.hiketracker.model.Point
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.*
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI


class TrackLoader(private val viewModel: TrackViewModel, private val context: Context){
    private val parser = GPXParser()

   /*suspend */fun loadFile(path: Uri?){
        var parsedGpx : Gpx? = null
      //  withContext(Dispatchers.IO){
            try {
                if(path != null) {
                    val instr = context.contentResolver.openInputStream(path)
                    parsedGpx = parser.parse(instr)
                } else throw IOException("Cannot open uri: path is null.")
            } catch (e: IOException) {
                // do something with this exception
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            }
      //  }
        if (parsedGpx == null) {
            Toast.makeText(context, "Chosen file is not a GPX file.",Toast.LENGTH_LONG).show()
            return
        } else {
            //success, save points
            val points = mutableListOf<Point>()
       //     withContext(Dispatchers.IO) {
                for (trk in parsedGpx.tracks) {
                    var i = 0
                    for (trkseg in trk.trackSegments) {
                        for (trkpoint in trkseg.trackPoints) {
                            points.add(trkpoint.toModelPoint(i))
                            i++
                        }
                    }
                }
           // }
            //already on background thread via Room
            viewModel.savePoints(points)
        }

    }

    private fun TrackPoint.toModelPoint(ordinal: Int): Point{
        return Point(
            ordinal = ordinal,
            latitude = this.latitude,
            longitude = this.longitude,
            elevation = this.elevation
        )
    }

}