package hu.bme.aut.android.hiketracker.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import com.google.android.gms.maps.model.LatLng
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.viewmodel.RouteViewModel
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Point
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import io.ticofab.androidgpxparser.parser.domain.WayPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream


class RouteLoader(viewModel: RouteViewModel, context: Context){
    private val viewModel : RouteViewModel = viewModel
    private val parser = GPXParser()
    private val context = context

//TODO separate thread
    fun loadFile(path: String){
        var parsedGpx : Gpx? = null
        try {
            //val instr: InputStream = File(path).inputStream()
                val instr: InputStream = context.getResources().openRawResource(R.raw.zebegeny_remete_barlang)
            parsedGpx = parser.parse(instr)
        } catch (e: IOException) {
            // do something with this exception
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        if (parsedGpx == null) {
            print("error parsing gpx file")
        } else {
            //success, save points
            val points = mutableListOf<Point>()
            for(trk in parsedGpx.tracks)
                for(trkseg in trk.trackSegments)
                    points.addAll(trkseg.trackPoints.map{it -> it as Point})

            viewModel.savePoints(points)
        }

    }

}