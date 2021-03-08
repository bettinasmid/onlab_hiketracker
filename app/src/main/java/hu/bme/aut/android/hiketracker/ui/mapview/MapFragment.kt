package hu.bme.aut.android.hiketracker.ui.mapview

import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import io.ticofab.androidgpxparser.parser.domain.Point

class MapFragment : Fragment() {
    private val trackViewModel: TrackViewModel by activityViewModels()
    private var mMap: GoogleMap? = null
    private var mapOptions: GoogleMapOptions? =  null

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        mapOptions = GoogleMapOptions()
        trackViewModel.trackPoints.observe(viewLifecycleOwner, Observer { it ->
            drawPolyline(it)
        })
        //mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun drawPolyline(points: List<Point>?) {
            val mapPoints = points?.map{ it -> LatLng(it.latitude, it.longitude)}
            val polyline = mMap?.addPolyline(PolylineOptions().clickable(true).color(Color.BLUE).addAll(mapPoints))
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(mapPoints?.get(0),13.0f))
            mMap?.addMarker(MarkerOptions().position(mapPoints?.get(0)?: LatLng(45.508888,-73.561668)).title("Starting point"))
    }
}