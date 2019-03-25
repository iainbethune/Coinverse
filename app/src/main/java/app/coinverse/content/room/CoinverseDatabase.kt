package app.coinverse.content.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.coinverse.content.models.Content
import app.coinverse.utils.DATABASE_NAME

@Database(entities = arrayOf(Content::class), version = 3)
@TypeConverters(Converters::class)
abstract class CoinverseDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {

        private var INSTANCE: CoinverseDatabase? = null

        fun getAppDatabase(context: Context): CoinverseDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context.applicationContext,
                        CoinverseDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
            }
            return INSTANCE as CoinverseDatabase
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE content ADD COLUMN loadingStatus INTEGER DEFAULT 0 NOT NULL")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                        "CREATE TABLE content_new (id TEXT DEFAULT '123' NOT NULL, qualityScore REAL DEFAULT 0.0 NOT NULL, contentType INTEGER DEFAULT 0 NOT NULL, timestamp INTEGER DEFAULT 0 NOT NULL, creator TEXT DEFAULT '' NOT NULL, title TEXT DEFAULT '' NOT NULL, previewImage TEXT DEFAULT '' NOT NULL, description TEXT DEFAULT '' NOT NULL, url TEXT DEFAULT '' NOT NULL, textUrl TEXT DEFAULT '' NOT NULL, audioUrl TEXT DEFAULT '' NOT NULL, feedType INTEGER DEFAULT 0 NOT NULL, savedPosition INTEGER DEFAULT 0 NOT NULL, viewCount REAL DEFAULT 0.0 NOT NULL, startCount REAL DEFAULT 0.0 NOT NULL, consumeCount REAL DEFAULT 0.0 NOT NULL, finishCount REAL DEFAULT 0.0 NOT NULL, organizeCount REAL DEFAULT 0.0 NOT NULL, shareCount REAL DEFAULT 0.0 NOT NULL, clearFeedCount REAL DEFAULT 0.0 NOT NULL, dismissCount REAL DEFAULT 0.0 NOT NULL, PRIMARY KEY(id))")
                // Copy the data
                database.execSQL(
                        "INSERT INTO content_new (id, qualityScore, contentType, timestamp, creator, title, previewImage, description, url, textUrl, audioUrl, feedType, savedPosition, viewCount, startCount, consumeCount, finishCount, organizeCount, shareCount, clearFeedCount, dismissCount) SELECT id, qualityScore, contentType, timestamp, creator, title, previewImage, description, url, textUrl, audioUrl, feedType, savedPosition, viewCount, startCount, consumeCount, finishCount, organizeCount, shareCount, clearFeedCount, dismissCount FROM content")
                // Remove the old table
                database.execSQL("DROP TABLE content")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE content_new RENAME TO content")
            }
        }
    }

}
