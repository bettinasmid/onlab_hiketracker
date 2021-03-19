package hu.bme.aut.android.hiketracker.ui.fragments

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions


@RuntimePermissions
class MapFragment : Fragment() {
    private val trackViewModel: TrackViewModel by activityViewModels()
    private lateinit var mMap: GoogleMap

    val callback = OnMapReadyCallback { googleMap ->
        onMapReady(googleMap)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onMapReady(map: GoogleMap){
        mMap = map
        mMap.mapType = MAP_TYPE_TERRAIN
        mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener)
        mMap.setOnMyLocationClickListener(onMyLocationClickListener)
        try {
            mMap.isMyLocationEnabled = true
            
        }catch (ex: SecurityException){
            Toast.makeText(context, "Location access denied", Toast.LENGTH_SHORT).show()
        }
        trackViewModel.trackPoints.observe(viewLifecycleOwner, Observer { points ->
            if (points.isNotEmpty()) {
                drawPolyline(points)
            }
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
            val polyline = mMap?.addPolyline(
                PolylineOptions().clickable(true).color(Color.BLUE).addAll(
                    mapPoints
                )
            )
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(mapPoints?.get(0), 13.0f))
            mMap?.addMarker(MarkerOptions().apply {
                position(mapPoints?.get(0) ?: LatLng(45.508888, -73.561668))
                title("Starting point")
            })
    }

    //source: https://www.zoftino.com/android-show-current-location-on-map-example
    private val onMyLocationClickListener =
        OnMyLocationClickListener { location ->
            mMap.setMinZoomPreference(12f)
            val circleOptions = CircleOptions()
            circleOptions.center(
                LatLng(
                    location.latitude,
                    location.longitude
                )
            )
            circleOptions.radius(200.0)
            circleOptions.fillColor(Color.RED)
            circleOptions.strokeWidth(6f)
            mMap.addCircle(circleOptions)
        }

    private val onMyLocationButtonClickListener = OnMyLocationButtonClickListener {
        mMap.setMinZoomPreference(15f)
        false
    }
}