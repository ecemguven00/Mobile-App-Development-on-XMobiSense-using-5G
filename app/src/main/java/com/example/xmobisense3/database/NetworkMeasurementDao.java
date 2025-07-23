package com.example.xmobisense3.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NetworkMeasurementDao {
    @Insert
    void insert(NetworkMeasurement measurement);

    @Query("SELECT * FROM network_measurements ORDER BY timestamp DESC")
    LiveData<List<NetworkMeasurement>> getAllMeasurements();

    @Query("DELETE FROM network_measurements")
    void deleteAll();
}