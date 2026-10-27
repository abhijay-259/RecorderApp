package com.example.recorderapp.room

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Submission2:: class],
    version = 5,
    autoMigrations = []
)
abstract class SubmissionDatabase(): RoomDatabase() {
    abstract val dao: SubmissionDao

    companion object {

        // Volatile makes this memory address instantly visible to all threads (UI and Workers) [INDEX]
        @Volatile
        private var INSTANCE: SubmissionDatabase? = null

        // THE ONE TRUE GATEWAY: Returns the single shared instance pool safely [INDEX]
        fun getDatabase(context: Context): SubmissionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubmissionDatabase::class.java,
                    "submissions.db"
                )
                    .addMigrations(migration1To2, migration2To3, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `Submission`")
            }
        }
        val migration2To3 = object : Migration(2,3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS Submission2 (" +
                        "worker_name TEXT NOT NULL, " +
                        "task_id INTEGER NOT NULL, " +
                        "file_path TEXT NOT NULL, " +
                        "worker_id INTEGER NOT NULL, " +
                        "submission_id INTEGER PRIMARY KEY AUTOINCREMENT);")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                CREATE TABLE Submission2_new (
                submission_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                worker_name TEXT NOT NULL,
                task_id INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                worker_id TEXT
                )
                """.trimIndent())

                db.execSQL("""
                INSERT INTO Submission2_new (submission_id, worker_name, task_id, filePath, worker_id)
                SELECT submission_id, worker_name, task_id, filePath, CAST(worker_id AS TEXT)
                FROM Submission2
                """.trimIndent())

                db.execSQL("DROP TABLE Submission2")
                db.execSQL("ALTER TABLE Submission2_new RENAME TO Submission2")
            }
        }
    }
}
