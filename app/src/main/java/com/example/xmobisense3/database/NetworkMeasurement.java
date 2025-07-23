package com.example.xmobisense3.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "network_measurements")
public class NetworkMeasurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Date timestamp;
    public double longitude;
    public double latitude;

    public String networkType;
    public String frequencyBand;
    public String operator;
    public int rxPower;
    public String signalQuality;
    public double receivedDataRate;
    public double transmittedDataRate;
    public String callState;
    public int rx5G;


}