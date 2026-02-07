package com.example.devaudioreccordings.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface AudioTextDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun instertAudioText(audioTextDbData: AudioTextDbData)

    @Delete
    suspend fun deleteAudioText(audioTextDbData: AudioTextDbData)

    @Update
    suspend fun updateAudioText(audioTextDbData: AudioTextDbData)

    @Query("SELECT * FROM AudioTextDbData WHERE header = :header")
    fun getDataByHeader(header: String): Flow<List<AudioTextDbData>>


    @Query("Select * from AudioTextDbData ORDER BY header DESC")
    fun getAudioTextOrderedByHeader():Flow<List<AudioTextDbData>>

    @Query("Select * from AudioTextDbData ORDER BY created_at DESC")
    fun getAudioTextOrderedByCreatedAt():Flow<List<AudioTextDbData>>

    @Query ("Select id from AudioTextDbData ORDER BY created_at DESC Limit  1")
    suspend fun getLatestCreatedId():Int

    @Query ("Select header,created_at from AudioTextDbData ORDER BY created_at DESC")
    fun getHeaderList():Flow<List<HeaderAndCreatedAt>>

    @Query("Select * from AudioTextDbData WHERE id = :id")
    suspend fun getDataById(id: Int): AudioTextDbData
}