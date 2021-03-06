package hu.bme.aut.android.hiketracker.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import hu.bme.aut.android.hiketracker.data.PointDao
import hu.bme.aut.android.hiketracker.data.RoomPoint
import hu.bme.aut.android.hiketracker.model.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PointRepository(private val pointDao: PointDao){

    fun getAllPoints(): LiveData<List<Point>> {
        return pointDao.getAllPoints().map{
            roomPoints -> roomPoints.map{
                roomPoint -> roomPoint.toDomainModel()}
        }
    }

    suspend fun insertAll(points: List<Point>){
        pointDao.insertAll(points.map{point -> point.toRoomModel() })
    }

    suspend fun deleteAllPoints() {
        pointDao.deleteAllPoints()
    }

    suspend fun markVisited(point: Point){
        pointDao.markVisited(point.id)
    }

    private fun RoomPoint.toDomainModel(): Point{
        return Point(
            id = id,
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            ordinal = ordinal,
            visited = visited
        )
    }

    private fun Point.toRoomModel(): RoomPoint{
        return RoomPoint(
            latitude = latitude,
            longitude = longitude,
            elevation = elevation,
            ordinal = ordinal,
            visited = visited
        )
    }
}