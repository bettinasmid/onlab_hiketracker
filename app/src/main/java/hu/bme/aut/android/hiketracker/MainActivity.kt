package hu.bme.aut.android.hiketracker

import abhishekti7.unicorn.filepicker.UnicornFilePicker
import abhishekti7.unicorn.filepicker.utils.Constants.REQ_UNICORN_FILE
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import hu.bme.aut.android.hiketracker.utils.TrackLoader
import hu.bme.aut.android.hiketracker.viewmodel.TrackViewModel
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.*
import java.lang.System.exit

@RuntimePermissions
class MainActivity : AppCompatActivity() {

    val viewModel : TrackViewModel by viewModels()
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

        fab.setOnClickListener {
            //openFilePickerDialogWithPermissionCheck()
            openMockFile() //temporary
        }
    }

    //temporary
    fun openMockFile() {
        val loader = TrackLoader(viewModel, applicationContext)
        loader.loadFile("\\app\\src\\resources\\zebegeny_remete_barlang.gpx")
    }

    @NeedsPermission(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    fun openFilePickerDialog(){
        UnicornFilePicker.from(this)
            .addConfigBuilder()
            .selectMultipleFiles(false)
            .setRootDirectory(
                Environment.getExternalStorageDirectory().absolutePath
            )
            .showHiddenFiles(false)
            .setFilters(arrayOf("gpx"))
            .addItemDivider(true)
            .theme(R.style.UnicornFilePicker_Default)
            .build()
            .forResult(REQ_UNICORN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_UNICORN_FILE && resultCode == RESULT_OK) {
            val files = data?.getStringArrayListExtra("filePaths")
            for (file in files!!) {
                Log.e("tag",file)
            }
            val loader = TrackLoader(viewModel, applicationContext)
            loader.loadFile(files[0])
        }
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