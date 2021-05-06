package hu.bme.aut.android.hiketracker.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Log.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.logger.Logger
import hu.bme.aut.android.hiketracker.service.PositionCheckerService
import hu.bme.aut.android.hiketracker.ui.fragments.MapFragment
import hu.bme.aut.android.hiketracker.utils.TrackLoader
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.*
import kotlin.system.exitProcess


@RuntimePermissions
class MainActivity : AppCompatActivity() {
    companion object{
        val PICK_GPX_FILE = 2
        val STATE_TRACKING_ON = "trackingOn"
        val STATE_FAB_ENABLED = "fabEnabled"
        val STATE_IS_BOUND = "isBound"
    }

    private lateinit var logger: Logger
    private val viewModel : TrackViewModel by viewModels()
    private lateinit var positionCheckerService: PositionCheckerService
    private var serviceIntent: Intent? = null
    private var trackingOn = false
    private var isBound = false
    private lateinit var mapFragment: MapFragment
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as PositionCheckerService.PositionCheckerServiceBinder
            positionCheckerService = binder.getService()
            logger.log("\tService is enabled: ${positionCheckerService.enabled}")
            positionCheckerService.onViewUpdateNeededListener = mapFragment
            positionCheckerService.startLocationMonitoring()
            isBound = true
            if(positionCheckerService.enabled) {
                trackingOn = true
                fabStart.setImageResource(R.drawable.ic_action_stop)
                fabStart.isEnabled = true
            }
            logger.log("MainActivity: service connected")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
            logger.log("MainActivity: service disconnected")

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger = Logger(this)
        logger.log("MainActivity onCreate called")
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        val fragManager = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragments = fragManager?.childFragmentManager?.fragments
        mapFragment = if(fragments?.get(0) is MapFragment) fragments?.get(0) as MapFragment else fragments?.get(1) as MapFragment
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_map, R.id.navigation_elevation
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        //restore state
        if(savedInstanceState != null) {
            if (savedInstanceState.getBoolean(STATE_TRACKING_ON)) {
                fabStart.setImageResource(R.drawable.ic_action_stop)
                trackingOn = true
            }
            isBound = savedInstanceState.getBoolean(STATE_IS_BOUND)
        }

        btnOpen.setOnClickListener {
            openFilePickerDialog()
        }

        fabStart.setOnClickListener{
            if(fabStart.isEnabled) {
                if (!trackingOn) {
                    startTrackingWithPermissionCheck()
                } else {
                    stopTracking()
                }
            } else {
                Toast.makeText(this, "Load a track first!", Toast.LENGTH_LONG).show()
            }
        }

        btnOff.setOnClickListener{
            stopTracking()
            viewModel.clearPoints()
            finish()
            exitProcess(0)
        }

        fabStart.isEnabled = trackingOn //false if newly created, but if already running, it has to be enabled
    }

    override fun onStart() {
        super.onStart()
        logger.log("MainActivity onStart called")
        d("nyuszi","MainActivity onStart called")
        logger.log("\t$STATE_TRACKING_ON : $trackingOn")
        logger.log("\t$STATE_FAB_ENABLED : ${fabStart.isEnabled}")
        logger.log("\t$STATE_IS_BOUND : $isBound")
        if(trackingOn) {
            logger.log("\tcalling bindService")
            serviceIntent = Intent(this, PositionCheckerService::class.java)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop(){
        super.onStop()
        if(isBound){
            unbindService(serviceConnection)
        }
        logger.log("MainActivity.onStop called")
        d("nyuszi","MainActivity onStop called")
    }


    private fun openFilePickerDialog(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            type = "*/*"
        }
        startActivityForResult(intent, PICK_GPX_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_GPX_FILE && resultCode == RESULT_OK) {
            val loader = TrackLoader(viewModel, applicationContext)
            lifecycleScope.launch {
                loader.loadFile(data?.data)
            }
            fabStart.isEnabled = true
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startTracking(){
        trackingOn = true
        btnOpen.isEnabled = false
        fabStart.setImageResource(R.drawable.ic_action_stop)
        serviceIntent = Intent(this, PositionCheckerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_IMPORTANT)

    }

    fun stopTracking(){
        if(trackingOn) {
            trackingOn = false
            btnOpen.isEnabled = true
            fabStart.setImageResource(android.R.drawable.ic_media_play)
            if(isBound) {
                positionCheckerService.stopLocationMonitoring()
                unbindService(serviceConnection)
            }
            stopService(serviceIntent)
        }
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showDeniedForFineLocation() {
        Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    fun showNeverAskForFineLocation() {
        Toast.makeText(this, "App won't work this way", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putBoolean(STATE_TRACKING_ON, trackingOn)
            putBoolean(STATE_FAB_ENABLED, fabStart.isEnabled)
            putBoolean(STATE_IS_BOUND, isBound)
        }
        super.onSaveInstanceState(outState)
        logger.log("MainActivity onSaveInstanceState called")
        logger.log("\t$STATE_TRACKING_ON : $trackingOn")
        logger.log("\t$STATE_FAB_ENABLED : ${fabStart.isEnabled}")
        logger.log("\t$STATE_IS_BOUND : $isBound")
    }

    override fun onDestroy() {
        logger.log("MainActivity onDestroy called")
        super.onDestroy()
    }
}