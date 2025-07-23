package com.example.xmobisense3.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
// database will contain a table for the NetworkMeasurement entity.
@Database(entities = {NetworkMeasurement.class}, version = 5)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract NetworkMeasurementDao networkMeasurementDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) { //only one instance of the database exists
            //no database instance has been created yet.
            synchronized (AppDatabase.class) { //only one thread (part of your app) can run this block of code
                if (INSTANCE == null) {//without this second check, the database could be created twice.
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "xmobisense_database1")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}