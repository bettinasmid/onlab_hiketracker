package hu.bme.aut.android.hiketracker.data

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface PointDao {
    @Query("SELECT * FROM point")
    fun getAllPoints() : LiveData<List<RoomPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    suspend fun insertAll(points: List<RoomPoint>)

    @Query("DELETE FROM point")
    suspend fun deleteAllPoints()

    @Query("UPDATE point SET visited=1 WHERE id=:id")
    suspend fun markVisited(id: Long)
}