package hu.bme.aut.android.hiketracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import hu.bme.aut.android.hiketracker.data.PointRepository
import io.ticofab.androidgpxparser.parser.domain.Point

class RouteViewModel : ViewModel() {
    //private val repo: PointRepository
    var routePoints: LiveData<List<Point>>

    init{
        routePoints = MutableLiveData<List<Point>>()
        //repo = PointRepository(pointDao)
        //routePoints = repo.getAllPoints()
    }

    fun savePoints(points: List<Point>){
        routePoints = MutableLiveData(points)
    }


}