package hu.bme.aut.android.hiketracker.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface PointDao {
    @Query("SELECT * FROM point")
    fun getAllPoints() : LiveData<List<RoomPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @JvmSuppressWildcards
    fun insertAll(points: List<RoomPoint>)

    @Query("DELETE FROM point")
    fun deleteAllPoints()
}