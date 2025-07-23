package com.example.xmobisense3.services;

import android.content.Context;
import android.net.TrafficStats;
import android.telephony.TelephonyManager;
import android.widget.TextView;

public class DataUsageService {

    private final Context context;
    private final TextView txtReceivedData;
    private final TextView txtTransmittedData;

    private long previousRxBytes = 0;
    private long previousTxBytes = 0;

    private static final int INTERVAL_SECONDS = 10;

    private double lastRxGbps = 0.0;
    private double lastTxGbps = 0.0;

    public DataUsageService(Context context, TextView txtReceivedData, TextView txtTransmittedData) {
        this.context = context;
        this.txtReceivedData = txtReceivedData;
        this.txtTransmittedData = txtTransmittedData;
    }

    public boolean isSimReady() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager != null && telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    public void updateUsage() {
        if (!isSimReady()) {
            txtReceivedData.setText("SIM card not inserted or inactive");
            txtTransmittedData.setText("SIM card not inserted or inactive");
            return;
        }

        long currentRxBytes = TrafficStats.getMobileRxBytes();
        long currentTxBytes = TrafficStats.getMobileTxBytes();

        if (previousRxBytes == 0 && previousTxBytes == 0) {
            previousRxBytes = currentRxBytes;
            previousTxBytes = currentTxBytes;
            txtReceivedData.setText("Waiting for data...");
            txtTransmittedData.setText("Waiting for data...");
            return;
        }

        long deltaRx = currentRxBytes - previousRxBytes;
        long deltaTx = currentTxBytes - previousTxBytes;

        previousRxBytes = currentRxBytes;
        previousTxBytes = currentTxBytes;

        lastRxGbps = (deltaRx * 8.0) / INTERVAL_SECONDS / 1_000_000_000;
        lastTxGbps = (deltaTx * 8.0) / INTERVAL_SECONDS / 1_000_000_000;

        txtReceivedData.setText(String.format("Received Data Rate: %.6f Gbps", lastRxGbps));
        txtTransmittedData.setText(String.format("Transmitted Data Rate: %.6f Gbps", lastTxGbps));
    }

    public double getLastRxGbps() {
        return lastRxGbps;
    }

    public double getLastTxGbps() {
        return lastTxGbps;
    }
}