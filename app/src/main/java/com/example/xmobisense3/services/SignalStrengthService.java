package com.example.xmobisense3.services;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.TextView;

import java.util.List;

public class SignalStrengthService extends PhoneStateListener {
    private final TextView txtRxPower;
    private final TelephonyManager telephonyManager;
    private SignalStrength latestSignalStrength;

    public SignalStrengthService(Context context, TextView txtRxPower) {
        this.txtRxPower = txtRxPower;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        this.latestSignalStrength = signalStrength;
    }

    public void updateSignalStrengthUI() {
        int simState = telephonyManager.getSimState();
        if (simState != TelephonyManager.SIM_STATE_READY) {
            txtRxPower.setText("SIM card not inserted or not active");
            return;
        }

        if (latestSignalStrength == null) {
            txtRxPower.setText("No signal");
            return;
        }

        int rxPower = 0;
        int rx5G = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            List<CellSignalStrength> strengths = latestSignalStrength.getCellSignalStrengths();

            CellSignalStrengthNr nrSignal = null;
            CellSignalStrengthLte lteSignal = null;
            CellSignalStrengthWcdma wcdmaSignal = null;

            for (CellSignalStrength signal : strengths) {
                if (signal instanceof CellSignalStrengthNr) {
                    nrSignal = (CellSignalStrengthNr) signal;
                } else if (signal instanceof CellSignalStrengthLte) {
                    lteSignal = (CellSignalStrengthLte) signal;
                } else if (signal instanceof CellSignalStrengthWcdma) {
                    wcdmaSignal = (CellSignalStrengthWcdma) signal;
                }
            }


            if (nrSignal != null && lteSignal == null) {
                // 5G SA mode (only NR present)
                rx5G = nrSignal.getDbm();
                String quality = get5GQuality(rx5G);
                txtRxPower.setText("NR 5G SA: " + rx5G + " dBm (" + quality + ")");
                setColor(quality);
                return;

            } else if (nrSignal != null && lteSignal != null) {
                // 5G NSA mode (both NR and LTE present)
                rxPower = lteSignal.getDbm();
                rx5G = nrSignal.getDbm();

                String quality = get5GQuality(rx5G);
                txtRxPower.setText("NR 5G NSA:\n4G: " + rxPower + " dBm\n5G: " + rx5G + " dBm (" + quality + ")");
                setColor(quality);
                return;

            } else if (lteSignal != null) {
                // only 4G
                rxPower = lteSignal.getDbm();

                String quality = get4GQuality(rxPower);
                txtRxPower.setText("4G LTE: " + rxPower + " dBm (" + quality + ")");
                setColor(quality);
                return;

            } else if (wcdmaSignal != null) {
                // Only 3G
                rxPower = wcdmaSignal.getDbm();

                String quality = get3GQuality(rxPower);
                txtRxPower.setText("3G WCDMA: " + rxPower + " dBm (" + quality + ")");
                setColor(quality);
                return;
            }

        } else {
            int level = latestSignalStrength.getLevel();
            txtRxPower.setText("Signal level (approx): " + level);
        }
    }
    private String get3GQuality(int rscp) {
        if (rscp >= -75) return "Excellent";
        if (rscp >= -85) return "Good";
        if (rscp >= -95) return "Fair";
        return "Poor";
    }

    private String get5GQuality(int rsrp) {
        if (rsrp >= -80) return "Excellent";
        if (rsrp >= -90) return "Good";
        if (rsrp >= -100) return "Fair";
        return "Poor";
    }

    private String get4GQuality(int rsrp) {
        if (rsrp >= -85) return "Excellent";
        if (rsrp >= -95) return "Good";
        if (rsrp >= -105) return "Fair";
        return "Poor";
    }

    private void setColor(String quality) {
        switch (quality) {
            case "Excellent":
                txtRxPower.setTextColor(Color.parseColor("#2E7D32")); // Green
                break;
            case "Good":
                txtRxPower.setTextColor(Color.parseColor("#1565C0")); // Blue
                break;
            case "Fair":
                txtRxPower.setTextColor(Color.parseColor("#EF6C00")); // Orange
                break;
            case "Poor":
                txtRxPower.setTextColor(Color.parseColor("#D32F2F")); // Red
                break;
        }
    }
}