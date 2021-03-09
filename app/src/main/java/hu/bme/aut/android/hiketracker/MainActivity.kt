package hu.bme.aut.android.hiketracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.bme.aut.android.hiketracker.service.PositionCheckerService
import hu.bme.aut.android.hiketracker.utils.TrackLoader
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*


@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private val PICK_GPX_FILE = 2
    private val viewModel : TrackViewModel by viewModels()
    private var positionCheckerService: PositionCheckerService? = null

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

        fabOpen.setOnClickListener {
            openFilePickerDialog()
        }

        fabStart.setOnClickListener{
            startTracking()
        }

    }

    @NeedsPermission(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun openFilePickerDialog(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent,PICK_GPX_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_GPX_FILE && resultCode == RESULT_OK) {
            val loader = TrackLoader(viewModel, applicationContext)
            loader.loadFile(data?.data)
        }
    }

    fun startTracking(){
        val serviceIntent = Intent(this, PositionCheckerService::class.java)
        startService(serviceIntent)
    }

    @OnPermissionDenied
    fun onAccessDenied(){
        Toast.makeText(this, getString(R.string.storage_access_permission_denied_warning), Toast.LENGTH_LONG).show()
    }

    @OnShowRationale(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE)
    fun showRationaleForCall(request: PermissionRequest) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.call_permission_explanation)
            .setCancelable(false)
            .setPositiveButton(R.string.proceed) { dialog, id -> request.proceed() }
            .setNegativeButton(R.string.exit) { dialog, id -> request.cancel() }
            .create()
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

}