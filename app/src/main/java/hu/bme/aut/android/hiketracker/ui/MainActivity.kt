package hu.bme.aut.android.hiketracker.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
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
import hu.bme.aut.android.hiketracker.service.PositionCheckerService
import hu.bme.aut.android.hiketracker.utils.TrackLoader
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import permissions.dispatcher.*
import kotlin.system.exitProcess


@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val PICK_GPX_FILE = 2
    private val viewModel : TrackViewModel by viewModels()
    private var positionCheckerService: PositionCheckerService? = null
    private var trackingOn = false
    private var serviceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_map, R.id.navigation_elevation
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        btnOpen.setOnClickListener {
            openFilePickerDialog()
        }

        fabStart.setOnClickListener{
           if(!trackingOn){
               startTrackingWithPermissionCheck()
           } else{
               stopTracking()
           }
        }

        btnOff.setOnClickListener{
            if(trackingOn)
                stopTracking()
            viewModel.clearPoints()
            finish()
            exitProcess(0)
        }

        fabStart.isEnabled = false

    }

    fun openFilePickerDialog(){
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
    }

    fun stopTracking(){
        trackingOn = false
        btnOpen.isEnabled = true
        fabStart.setImageResource(android.R.drawable.ic_media_play)
        stopService(serviceIntent)
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

}