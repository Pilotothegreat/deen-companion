package com.pilotothegreat.deencompanion.database

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

@Entity
data class HadithBookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val compiler: String,
    val hadithCount: Int
)

@Entity
data class HadithEntity(
    @PrimaryKey val id: String, // Format: "bookId_number"
    val bookId: String,
    val number: Int,
    val arabic: String,
    val english: String,
    val narrator: String,
    val grade: String,
    val isFavorite: Boolean = false
)

@Dao
interface HadithDao {
    @Query("SELECT * FROM HadithBookEntity")
    fun getHadithBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT * FROM HadithBookEntity WHERE id = :id LIMIT 1")
    suspend fun getHadithBook(id: String): HadithBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadithBooks(books: List<HadithBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(hadiths: List<HadithEntity>)

    @Query("SELECT * FROM HadithEntity WHERE bookId = :bookId AND (arabic != '' OR english != '') ORDER BY number ASC LIMIT :limit OFFSET :offset")
    suspend fun getHadithsPaged(bookId: String, limit: Int, offset: Int): List<HadithEntity>

    @Query("SELECT * FROM HadithEntity WHERE bookId = :bookId AND (arabic != '' OR english != '') ORDER BY number ASC")
    fun getHadithsFlow(bookId: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM HadithEntity WHERE isFavorite = 1 AND (arabic != '' OR english != '')")
    fun getFavoritesFlow(): Flow<List<HadithEntity>>

    @Query("UPDATE HadithEntity SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("SELECT * FROM HadithEntity WHERE bookId = :bookId AND (arabic != '' OR english != '') AND (english LIKE :q OR arabic LIKE :q OR narrator LIKE :q)")
    suspend fun searchHadithsInBook(bookId: String, q: String): List<HadithEntity>

    @Query("SELECT * FROM HadithEntity WHERE (arabic != '' OR english != '') AND (english LIKE :q OR arabic LIKE :q OR narrator LIKE :q) LIMIT 100")
    suspend fun searchHadithsGlobal(q: String): List<HadithEntity>

    @Query("SELECT COUNT(*) FROM HadithEntity WHERE bookId = :bookId AND (arabic != '' OR english != '')")
    suspend fun getHadithCount(bookId: String): Int
}

@Entity
data class TasbihRecord(
    @PrimaryKey val id: String = "default",
    val count: Int,
    val historyJson: String,
    val dhikr: String,
    val target: Int
)

@Dao
interface TasbihDao {
    @Query("SELECT * FROM TasbihRecord WHERE id = 'default' LIMIT 1")
    suspend fun getRecord(): TasbihRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TasbihRecord)
}

@Database(entities = [BookmarkedVerse::class, HadithBookEntity::class, HadithEntity::class, TasbihRecord::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkedVerseDao(): BookmarkedVerseDao
    abstract fun hadithDao(): HadithDao
    abstract fun tasbihDao(): TasbihDao
}
