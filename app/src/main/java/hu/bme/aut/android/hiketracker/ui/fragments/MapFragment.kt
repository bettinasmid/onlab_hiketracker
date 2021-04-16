package hu.bme.aut.android.hiketracker.ui.fragments

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
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
import com.google.android.gms.maps.model.*
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.SHARED_PREFERENCES_NAME
import hu.bme.aut.android.hiketracker.TrackerApplication.Companion.TAG_TOTAL_DISTANCE
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.service.PositionCheckerService
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions


@RuntimePermissions
class MapFragment : Fragment(), PositionCheckerService.OnViewUpdateNeededListener{
    private val trackViewModel: TrackViewModel by activityViewModels()
    private lateinit var mMap: GoogleMap
    private lateinit var sp : SharedPreferences
    private var currentZoom : Float = 14.0f
    private var currentPosition: LatLng? = null

    val callback = OnMapReadyCallback { googleMap ->
        onMapReady(googleMap)
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun onMapReady(map: GoogleMap){
        mMap = map
        mMap.mapType = MAP_TYPE_TERRAIN
        mMap.setOnCameraMoveListener {
            currentPosition = mMap.cameraPosition.target
            currentZoom = mMap.cameraPosition.zoom
        }
        mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener)
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
        swSatellite.setOnCheckedChangeListener{ _, isChecked ->
            if(isChecked)
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            else
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
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
        sp = requireContext().getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun drawPolyline(points: List<Point>) {
            val mapPoints = points?.map{ it -> LatLng(it.latitude, it.longitude)}
            val polyline = mMap?.addPolyline(
                PolylineOptions().clickable(true).color(Color.BLUE).addAll(
                    mapPoints
                )
            )
            mMap?.addMarker(MarkerOptions().apply {
                position(mapPoints?.get(0) ?: LatLng(45.508888, -73.561668))
                title("Starting point")
            })
            if(currentPosition == null)
                currentPosition = mapPoints?.get(0)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, currentZoom))

    }

    //source: https://www.zoftino.com/android-show-current-location-on-map-example
    private val onMyLocationButtonClickListener = OnMyLocationButtonClickListener {
        mMap.setMinZoomPreference(20f)
        false
    }

    override fun onViewUpdateNeeded(location: Location) {
        currentPosition = LatLng(location.latitude,
            location.longitude)
        mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(currentPosition, currentZoom, 0.0f, location.bearing)))
        requireActivity().tvDistance.text = "Distance: " + "%.2f".format(sp.getFloat(
            TAG_TOTAL_DISTANCE,
            0.0F) / 1000) + " km"
    }
}