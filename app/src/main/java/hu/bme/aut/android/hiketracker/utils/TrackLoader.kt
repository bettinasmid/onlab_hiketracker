package hu.bme.aut.android.hiketracker.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.Point
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI


class TrackLoader(viewModel: TrackViewModel, context: Context){
    private val viewModel : TrackViewModel = viewModel
    private val parser = GPXParser()
    private val context = context

//TODO separate thread
    fun loadFile(path: Uri?){
        var parsedGpx : Gpx? = null
        try {
            if(path != null) {
                val instr = context.getContentResolver().openInputStream(path)
                parsedGpx = parser.parse(instr)
            } else throw IOException("Cannot open uri: path is null.")
        } catch (e: IOException) {
            // do something with this exception
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        if (parsedGpx == null) {
            Toast.makeText(context, "Chosen file is not a GPX file.",Toast.LENGTH_LONG).show()
            return
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