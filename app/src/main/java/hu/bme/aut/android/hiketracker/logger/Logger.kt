package hu.bme.aut.android.hiketracker.logger

import android.Manifest
import android.content.Context
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.util.Log
import permissions.dispatcher.NeedsPermission
import java.io.*
import java.time.LocalDateTime

class Logger ( val context : Context){
    private val TAG = "MEDIA"
    private var tv : String = ""
    private var messages = mutableListOf<String>()
    var enabled = true

    init{
        checkExternalMedia()
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun checkExternalMedia() {
        var mExternalStorageAvailable = false
        var mExternalStorageWriteable = false
        val state: String = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageWriteable = true
            mExternalStorageAvailable = mExternalStorageWriteable
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true
            mExternalStorageWriteable = false
        } else {
            // Can't read or write
            mExternalStorageWriteable = false
            mExternalStorageAvailable = mExternalStorageWriteable
        }
        Log.d("devlog log",
            "External Media: readable=$mExternalStorageAvailable writable=$mExternalStorageWriteable")
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun writeToSDFile() {

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal
        val root: File? = context.getExternalFilesDir(null)
        tv = tv.plus("\nExternal file system root: $root")

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder
        val dir = File(root!!.absolutePath.toString() + "/hikrdebug")
        dir.mkdirs()
        val file = File(dir, "hikrrunlog_${LocalDateTime.now()}.txt")
        try {
            val f = FileOutputStream(file)
            val pw = PrintWriter(f)
            for(m in messages)
                pw.println(m)
            pw.flush()
            pw.close()
            f.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        tv = tv.plus("\n\nFile written to $file")
        Log.d("devlog", tv)
    }

    fun log(message: String){
        messages.add(message)
    }
}