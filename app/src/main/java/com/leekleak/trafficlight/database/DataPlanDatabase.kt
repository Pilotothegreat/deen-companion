package com.leekleak.trafficlight.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class BookmarkedVerse(
    @PrimaryKey
    val id: String, // Format: "surahNumber:ayahNumber"

    @ColumnInfo val surahNumber: Int,
    @ColumnInfo val ayahNumber: Int,
    @ColumnInfo val surahName: String,
    @ColumnInfo val timestamp: Long
)

@Dao
interface BookmarkedVerseDao {
    @Query("SELECT * FROM bookmarkedverse ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<BookmarkedVerse>>

    @Query("SELECT EXISTS(SELECT * FROM bookmarkedverse WHERE id = :id)")
    suspend fun isBookmarked(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(bookmark: BookmarkedVerse)

    @Query("DELETE FROM bookmarkedverse WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [BookmarkedVerse::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkedVerseDao(): BookmarkedVerseDao
}