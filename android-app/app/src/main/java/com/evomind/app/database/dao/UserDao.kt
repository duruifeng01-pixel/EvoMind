package com.evomind.app.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.evomind.app.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getByIdLive(id: Long): LiveData<UserEntity?>

    @Query("SELECT * FROM users WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    fun getAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    fun getAllLive(): LiveData<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getCount(): Int

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("SELECT * FROM users WHERE uuid LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<UserEntity>>
}
