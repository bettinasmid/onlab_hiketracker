package hu.bme.aut.android.hiketracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.ticofab.androidgpxparser.parser.domain.Point

class TrackViewModel : ViewModel() {
    //private val repo: PointRepository
    var trackPoints: LiveData<List<Point>>

    init{
        trackPoints = MutableLiveData<List<Point>>()
        //repo = PointRepository(pointDao)
        //routePoints = repo.getAllPoints()
    }

    fun savePoints(points: List<Point>){
        trackPoints = MutableLiveData(points)
    }


}