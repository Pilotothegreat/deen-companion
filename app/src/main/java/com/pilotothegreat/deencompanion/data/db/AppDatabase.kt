package com.pilotothegreat.deencompanion.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pilotothegreat.deencompanion.core.text.ArabicText
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "BookmarkedVerse")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val timestamp: Long,
) {
    companion object {
        fun idFor(surah: Int, ayah: Int) = "$surah:$ayah"
    }
}

@Entity(tableName = "HadithBookEntity")
data class HadithBookEntity(
    @PrimaryKey val id: String,
    val name: String,
    val compiler: String,
    val hadithCount: Int,
    /** False while only the bundled sample is present. */
    val isComplete: Boolean,
)

@Entity(tableName = "HadithEntity", indices = [Index(value = ["bookId", "number"])])
data class HadithEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val number: Int,
    val arabic: String,
    val english: String,
    val narrator: String,
    val grade: String,
    /** Normalized Arabic, English and narrator text used for diacritic-insensitive search. */
    val searchText: String,
) {
    companion object {
        fun idFor(bookId: String, number: Int) = "${bookId}_$number"
        fun searchTextOf(arabic: String, english: String, narrator: String) =
            ArabicText.normalize("$arabic $english $narrator")
    }
}

/** Kept apart from [HadithEntity] so re-downloading a book never clears favorites. */
@Entity(tableName = "FavoriteHadith")
data class FavoriteHadithEntity(
    @PrimaryKey val hadithId: String,
    val addedAt: Long,
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM BookmarkedVerse ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Upsert
    suspend fun upsert(bookmark: BookmarkEntity)

    @Query("DELETE FROM BookmarkedVerse WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
abstract class HadithDao {
    @Query("SELECT * FROM HadithBookEntity")
    abstract fun observeBooks(): Flow<List<HadithBookEntity>>

    @Query("SELECT * FROM HadithBookEntity WHERE id = :id")
    abstract suspend fun book(id: String): HadithBookEntity?

    @Query("SELECT COUNT(*) FROM HadithBookEntity")
    abstract suspend fun bookCount(): Int

    @Upsert
    protected abstract suspend fun upsertBooks(books: List<HadithBookEntity>)

    @Upsert
    protected abstract suspend fun upsertHadiths(hadiths: List<HadithEntity>)

    @Transaction
    open suspend fun insertBook(book: HadithBookEntity, hadiths: List<HadithEntity>) {
        upsertHadiths(hadiths)
        upsertBooks(listOf(book))
    }

    @Query("SELECT * FROM HadithEntity WHERE bookId = :bookId ORDER BY number LIMIT :limit OFFSET :offset")
    abstract suspend fun page(bookId: String, limit: Int, offset: Int): List<HadithEntity>

    @Query("SELECT * FROM HadithEntity WHERE searchText LIKE '%' || :query || '%' ORDER BY bookId, number LIMIT :limit")
    abstract suspend fun search(query: String, limit: Int): List<HadithEntity>

    @Query("SELECT hadithId FROM FavoriteHadith")
    abstract fun observeFavoriteIds(): Flow<List<String>>

    @Query(
        "SELECT h.* FROM HadithEntity h INNER JOIN FavoriteHadith f ON f.hadithId = h.id " +
            "ORDER BY f.addedAt DESC",
    )
    abstract fun observeFavorites(): Flow<List<HadithEntity>>

    @Query("INSERT OR REPLACE INTO FavoriteHadith (hadithId, addedAt) VALUES (:hadithId, :addedAt)")
    abstract suspend fun addFavorite(hadithId: String, addedAt: Long)

    @Query("DELETE FROM FavoriteHadith WHERE hadithId = :hadithId")
    abstract suspend fun removeFavorite(hadithId: String)
}

@Database(
    entities = [BookmarkEntity::class, HadithBookEntity::class, HadithEntity::class, FavoriteHadithEntity::class],
    version = 7,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun hadithDao(): HadithDao

    companion object {
        private const val NAME = "database"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_6_7)
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
    }
}

/**
 * v7: favorites move to their own table, hadiths gain a normalized search column, books track
 * whether the full collection was downloaded, and the duplicate tasbih table is dropped
 * (tasbih lives in DataStore).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteHadith` (`hadithId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`hadithId`))")
        db.execSQL(
            "INSERT OR IGNORE INTO `FavoriteHadith` (`hadithId`, `addedAt`) " +
                "SELECT `id`, CAST(strftime('%s', 'now') AS INTEGER) * 1000 FROM `HadithEntity` WHERE `isFavorite` = 1",
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `HadithBookEntity_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`compiler` TEXT NOT NULL, `hadithCount` INTEGER NOT NULL, `isComplete` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `HadithBookEntity_new` (`id`, `name`, `compiler`, `hadithCount`, `isComplete`) " +
                "SELECT `id`, `name`, `compiler`, `hadithCount`, `hadithCount` > 20 FROM `HadithBookEntity`",
        )
        db.execSQL("DROP TABLE `HadithBookEntity`")
        db.execSQL("ALTER TABLE `HadithBookEntity_new` RENAME TO `HadithBookEntity`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `HadithEntity_new` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, " +
                "`number` INTEGER NOT NULL, `arabic` TEXT NOT NULL, `english` TEXT NOT NULL, `narrator` TEXT NOT NULL, " +
                "`grade` TEXT NOT NULL, `searchText` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        val insert = db.compileStatement("INSERT INTO `HadithEntity_new` VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
        db.query("SELECT `id`, `bookId`, `number`, `arabic`, `english`, `narrator`, `grade` FROM `HadithEntity`").use { c ->
            while (c.moveToNext()) {
                insert.clearBindings()
                insert.bindString(1, c.getString(0))
                insert.bindString(2, c.getString(1))
                insert.bindLong(3, c.getLong(2))
                insert.bindString(4, c.getString(3))
                insert.bindString(5, c.getString(4))
                insert.bindString(6, c.getString(5))
                insert.bindString(7, c.getString(6))
                insert.bindString(8, HadithEntity.searchTextOf(c.getString(3), c.getString(4), c.getString(5)))
                insert.executeInsert()
            }
        }
        db.execSQL("DROP TABLE `HadithEntity`")
        db.execSQL("ALTER TABLE `HadithEntity_new` RENAME TO `HadithEntity`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_HadithEntity_bookId_number` ON `HadithEntity` (`bookId`, `number`)")

        db.execSQL("DROP TABLE IF EXISTS `TasbihRecord`")
    }
}
