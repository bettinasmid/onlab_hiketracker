package hu.bme.aut.android.hiketracker.ui.elevationview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import hu.bme.aut.android.hiketracker.R
import hu.bme.aut.android.hiketracker.viewmodel.RouteViewModel

class ElevationFragment : Fragment() {

    private lateinit var routeViewModel: RouteViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        routeViewModel =
               ViewModelProviders.of(this).get(RouteViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_elevation, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        routeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}