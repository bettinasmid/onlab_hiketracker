package hu.bme.aut.android.hiketracker.ui.elevationview

import android.graphics.Color
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
import io.ticofab.androidgpxparser.parser.domain.Point
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.fragment_elevation.*
import java.lang.StringBuilder

class ElevationFragment : Fragment() {

    private val routeViewModel: RouteViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_elevation, container, false)
        routeViewModel.routePoints.observe(viewLifecycleOwner, Observer {
            it -> loadElevationData(it)
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chartElevation.setTouchEnabled(true)
        chartElevation.setPinchZoom(true)
        //loadMockData()
    }

    private fun loadMockData() {
        var entries = mutableListOf<Entry>()
        for(i in 1..10){
            entries.add(Entry(i.toFloat(),(i*i).toFloat()))
        }
        val dataSet = LineDataSet(entries, "Mock elevation chart")
        dataSet.color = Color.GREEN
        val data = LineData(dataSet)
        chartElevation.data = data
        chartElevation.invalidate()
    }

    private fun loadElevationData(points: List<Point>){
        var entries = mutableListOf<Entry>()
        for(i in 0 until (points.size-1)){
            entries.add(Entry(i.toFloat(), points[i].elevation.toFloat()))
            print(points[i].toString() + '\n')
        }
        val dataSet = LineDataSet(entries, "Elevation chart")
        dataSet.color = Color.GREEN
        val data = LineData(dataSet)
        chartElevation.data = data
        chartElevation.invalidate()
    }
}