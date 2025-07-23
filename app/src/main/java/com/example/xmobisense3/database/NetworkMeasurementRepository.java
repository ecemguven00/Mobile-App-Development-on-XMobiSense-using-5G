package com.example.xmobisense3.database;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NetworkMeasurementRepository {
    private final NetworkMeasurementDao measurementDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public NetworkMeasurementRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        measurementDao = db.networkMeasurementDao();
    }

    public void insertMeasurement(Date timestamp, double longitude, double latitude, String networkType,
                                  String frequencyBand, String operator, int rxPower,
                                  String signalQuality, double receivedDataRate,
                                  double transmittedDataRate, String callState,int rx5G) {
        executor.execute(() -> {
            NetworkMeasurement measurement = new NetworkMeasurement();
            measurement.timestamp = timestamp;
            measurement.longitude = longitude;
            measurement.latitude = latitude;
            measurement.networkType = networkType;
            measurement.frequencyBand = frequencyBand;
            measurement.operator = operator;
            measurement.rxPower = rxPower;
            measurement.signalQuality = signalQuality;
            measurement.receivedDataRate = receivedDataRate;
            measurement.transmittedDataRate = transmittedDataRate;
            measurement.callState = callState;
            measurement.rx5G = rx5G;
            measurementDao.insert(measurement);
        });
    }

    public LiveData<List<NetworkMeasurement>> getAllMeasurements() {
        return measurementDao.getAllMeasurements();
    }
    public void deleteAllMeasurements() {
        executor.execute(() -> measurementDao.deleteAll());
    }
}