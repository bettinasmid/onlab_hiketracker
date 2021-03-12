package hu.bme.aut.android.hiketracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.bme.aut.android.hiketracker.TrackerApplication
import hu.bme.aut.android.hiketracker.model.Point
import hu.bme.aut.android.hiketracker.repository.PointRepository
import kotlinx.coroutines.launch

class TrackViewModel : ViewModel() {
    private val repo: PointRepository
    var trackPoints : LiveData<List<Point>>

    init{
        val pointDao = TrackerApplication.pointDatabase.pointDao()
        repo = PointRepository(pointDao)
        trackPoints = repo.getAllPoints()
    }

    fun savePoints(points: List<Point>) = viewModelScope.launch{
        repo.deleteAllPoints()
       // trackPoints.value = points
        repo.insertAll(points)
    }

    fun clearPoints() = viewModelScope.launch {
        repo.deleteAllPoints()
    }

}