package com.example.myschedule.data.db

import androidx.room.*
import com.example.myschedule.data.entity.CalendarSource
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarSourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: CalendarSource): Long

    @Delete
    suspend fun delete(source: CalendarSource)

    @Query("SELECT * FROM calendar_sources ORDER BY id ASC")
    fun getAllSources(): Flow<List<CalendarSource>>

    @Query("UPDATE calendar_sources SET isEnabled = :isEnabled WHERE id = :sourceId")
    suspend fun updateEnabled(sourceId: Int, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM calendar_sources")
    suspend fun getCount(): Int
}