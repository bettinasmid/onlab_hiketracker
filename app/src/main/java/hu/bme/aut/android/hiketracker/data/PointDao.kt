package hu.bme.aut.android.hiketracker.data

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface PointDao {
    @Query("SELECT * FROM point")
    fun getAllPoints() : LiveData<List<RoomPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(points: List<RoomPoint>)

    @Query("DELETE FROM point")
    fun deleteAllPoints()

    @Query("UPDATE point SET visited=1 WHERE id=:id")
    fun markVisited(id: Long)
}