/*
package hu.bme.aut.android.hiketracker.utils

import android.R
import android.content.Context
import android.content.DialogInterface
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class SimpleFileDialog(
    context: Context,
    file_select_type: String,
    SimpleFileDialogListener: SimpleFileDialogListener?
) {
    private var Select_type = FileSave
    private var m_sdcardDirectory = ""
    private val m_context: Context
    private var m_titleView1: TextView? = null
    private var m_titleView: TextView? = null
    var default_file_name = "default.txt"
    private var selected_file_name = default_file_name
    private var input_text: EditText? = null
    private var m_dir = ""
    private var m_subdirs: MutableList<String>? = null
    private var m_SimpleFileDialogListener: SimpleFileDialogListener? = null
    private var m_listAdapter: ArrayAdapter<String>? = null
    private var m_goToUpper = false

    //////////////////////////////////////////////////////
    // Callback interface for selected directory
    //////////////////////////////////////////////////////
    interface SimpleFileDialogListener {
        fun onChosenDir(chosenDir: String?)
    }

    ///////////////////////////////////////////////////////////////////////
    // chooseFile_or_Dir() - load directory chooser dialog for initial
    // default sdcard directory
    ///////////////////////////////////////////////////////////////////////
    fun chooseFile_or_Dir() {
        // Initial directory is sdcard directory
        if (m_dir == "") chooseFile_or_Dir(m_sdcardDirectory) else chooseFile_or_Dir(m_dir)
    }

    ////////////////////////////////////////////////////////////////////////////////
    // chooseFile_or_Dir(String dir) - load directory chooser dialog for initial
    // input 'dir' directory
    ////////////////////////////////////////////////////////////////////////////////
    fun chooseFile_or_Dir(dir: String) {
        var dir = dir
        var dirFile = File(dir)
        while (!dirFile.exists() || !dirFile.isDirectory()) {
            dir = dirFile.getParent()
            dirFile = File(dir)
            Log.d("~~~~~", "dir=$dir")
        }
        Log.d("~~~~~", "dir=$dir")
        //m_sdcardDirectory
        dir = try {
            File(dir).getCanonicalPath()
        } catch (ioe: IOException) {
            return
        }
        m_dir = dir
        m_subdirs = getDirectories(dir)
        class SimpleFileDialogOnClickListener : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, item: Int) {
                val m_dir_old = m_dir
                var sel = "" + (dialog as AlertDialog).getListView().getAdapter().getItem(item)
                if (sel[sel.length - 1] == '/') sel = sel.substring(0, sel.length - 1)

                // Navigate into the sub-directory
                if (sel == "..") {
                    m_dir = m_dir.substring(0, m_dir.lastIndexOf("/"))
                    if ("" == m_dir) {
                        m_dir = "/"
                    }
                } else {
                    m_dir += "/$sel"
                }
                selected_file_name = default_file_name
                if (File(m_dir).isFile()) // If the selection is a regular file
                {
                    m_dir = m_dir_old
                    selected_file_name = sel
                }
                updateDirectory()
            }
        }

        val dialogBuilder: AlertDialog.Builder = createDirectoryChooserDialog(
            dir, m_subdirs,
            SimpleFileDialogOnClickListener()
        )
        dialogBuilder.setPositiveButton("OK", object : OnClickListener() {
            fun onClick(dialog: DialogInterface?, which: Int) {
                // Current directory chosen
                // Call registered listener supplied with the chosen directory
                if (m_SimpleFileDialogListener != null) {
                    run {
                        if (Select_type == FileOpen || Select_type == FileSave) {
                            selected_file_name = input_text!!.text.toString() + ""
                            m_SimpleFileDialogListener.onChosenDir("$m_dir/$selected_file_name")
                        } else {
                            m_SimpleFileDialogListener.onChosenDir(m_dir)
                        }
                    }
                }
            }
        }).setNegativeButton("Cancel", null)
        val dirsDialog: AlertDialog = dialogBuilder.create()

        // Show directory chooser dialog
        dirsDialog.show()
    }

    private fun createSubDir(newDir: String): Boolean {
        val newDirFile = File(newDir)
        return if (!newDirFile.exists()) newDirFile.mkdir() else false
    }

    private fun getDirectories(dir: String): MutableList<String> {
        val dirs: MutableList<String> = ArrayList()
        try {
            val dirFile = File(dir)

            // if directory is not the base sd card directory add ".." for going up one directory
            if ((m_goToUpper || m_dir != m_sdcardDirectory)
                && "/" != m_dir
            ) {
                dirs.add("..")
            }
            Log.d("~~~~", "m_dir=$m_dir")
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs
            }
            for (file in dirFile.listFiles()) {
                if (file.isDirectory()) {
                    // Add "/" to directory names to identify them in the list
                    dirs.add(file.getName().toString() + "/")
                } else if (Select_type == FileSave || Select_type == FileOpen) {
                    // Add file names to the list if we are doing a file save or file open operation
                    dirs.add(file.getName())
                }
            }
        } catch (e: Exception) {
        }
        Collections.sort(dirs, object : Comparator<String?>() {
            fun compare(o1: String, o2: String?): Int {
                return o1.compareTo(o2!!)
            }
        })
        return dirs
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////                                   START DIALOG DEFINITION                                    //////
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    private fun createDirectoryChooserDialog(
        title: String, listItems: List<String>?,
        onClickListener: DialogInterface.OnClickListener
    ): AlertDialog.Builder {
        val dialogBuilder: AlertDialog.Builder = Builder(m_context)
        ////////////////////////////////////////////////
        // Create title text showing file select type //
        ////////////////////////////////////////////////
        m_titleView1 = TextView(m_context)
        m_titleView1!!.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        //m_titleView1.setTextAppearance(m_context, android.R.style.TextAppearance_Large);
        //m_titleView1.setTextColor( m_context.getResources().getColor(android.R.color.black) );
        if (Select_type == FileOpen) m_titleView1!!.text = "Open:"
        if (Select_type == FileSave) m_titleView1!!.text = "Save As:"
        if (Select_type == FolderChoose) m_titleView1!!.text = "Folder Select:"

        //need to make this a variable Save as, Open, Select Directory
        m_titleView1!!.gravity = Gravity.CENTER_VERTICAL
        m_titleView1!!.setBackgroundColor(-12303292) // dark gray 	-12303292
        m_titleView1.setTextColor(m_context.getResources().getColor(R.color.white))

        // Create custom view for AlertDialog title
        val titleLayout1 = LinearLayout(m_context)
        titleLayout1.orientation = LinearLayout.VERTICAL
        titleLayout1.addView(m_titleView1)
        if (Select_type == FolderChoose || Select_type == FileSave) {
            ///////////////////////////////
            // Create New Folder Button  //
            ///////////////////////////////
            val newDirButton = Button(m_context)
            newDirButton.setLayoutParams(
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )
            newDirButton.setText("New Folder")
            newDirButton.setOnClickListener(object : OnClickListener() {
                fun onClick(v: View?) {
                    val input = EditText(m_context)

                    // Show new folder name input dialog
                    Builder(m_context).setTitle("New Folder Name").setView(input)
                        .setPositiveButton("OK",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                val newDir = input.text
                                val newDirName = newDir.toString()
                                // Create new directory
                                if (createSubDir("$m_dir/$newDirName")) {
                                    // Navigate into the new directory
                                    m_dir += "/$newDirName"
                                    updateDirectory()
                                } else {
                                    Toast.makeText(
                                        m_context, "Failed to create '"
                                                + newDirName + "' folder", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }).setNegativeButton("Cancel", null).show()
                }
            }
            )
            titleLayout1.addView(newDirButton)
        }

        /////////////////////////////////////////////////////
        // Create View with folder path and entry text box //
        /////////////////////////////////////////////////////
        val titleLayout = LinearLayout(m_context)
        titleLayout.orientation = LinearLayout.VERTICAL
        m_titleView = TextView(m_context)
        m_titleView!!.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        m_titleView!!.setBackgroundColor(-12303292) // dark gray -12303292
        m_titleView.setTextColor(m_context.getResources().getColor(R.color.white))
        m_titleView!!.gravity = Gravity.CENTER_VERTICAL
        m_titleView!!.text = title
        titleLayout.addView(m_titleView)
        if (Select_type == FileOpen || Select_type == FileSave) {
            input_text = EditText(m_context)
            input_text!!.setText(default_file_name)
            titleLayout.addView(input_text)
        }
        //////////////////////////////////////////
        // Set Views and Finish Dialog builder  //
        //////////////////////////////////////////
        dialogBuilder.setView(titleLayout)
        dialogBuilder.setCustomTitle(titleLayout1)
        m_listAdapter = createListAdapter(listItems)
        dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener)
        dialogBuilder.setCancelable(false)
        return dialogBuilder
    }

    private fun updateDirectory() {
        m_subdirs!!.clear()
        m_subdirs!!.addAll(getDirectories(m_dir))
        m_titleView!!.text = m_dir
        m_listAdapter!!.notifyDataSetChanged()
        //#scorch
        if (Select_type == FileSave || Select_type == FileOpen) {
            input_text!!.setText(selected_file_name)
        }
    }

    private fun createListAdapter(items: List<String>?): ArrayAdapter<String> {
        return object : ArrayAdapter<String?>(
            m_context, R.layout.select_dialog_item, R.id.text1,
            items!!
        ) {
            fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val v: View = super.getView(position, convertView, parent!!)
                if (v is TextView) {
                    // Enable list item (directory) text wrapping
                    val tv = v as TextView
                    tv.layoutParams.height = LayoutParams.WRAP_CONTENT
                    tv.ellipsize = null
                }
                return v
            }
        }
    }

    companion object {
        private const val FileOpen = 0
        private const val FileSave = 1
        private const val FolderChoose = 2
    }

    init {
        if (file_select_type == "FileOpen") {
            Select_type = FileOpen
        } else if (file_select_type == "FileSave") {
            Select_type = FileSave
        } else if (file_select_type == "FolderChoose") {
            Select_type = FolderChoose
        } else if (file_select_type == "FileOpen..") {
            Select_type = FileOpen
            m_goToUpper = true
        } else if (file_select_type == "FileSave..") {
            Select_type = FileSave
            m_goToUpper = true
        } else if (file_select_type == "FolderChoose..") {
            Select_type = FolderChoose
            m_goToUpper = true
        } else Select_type = FileOpen
        m_context = context
        m_sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath()
        m_SimpleFileDialogListener = SimpleFileDialogListener
        try {
            m_sdcardDirectory = File(m_sdcardDirectory).getCanonicalPath()
        } catch (ioe: IOException) {
        }
    }
}*/
