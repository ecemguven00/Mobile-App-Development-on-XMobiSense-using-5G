package com.example.xmobisense3.services;

import android.content.Context;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.xmobisense3.utils.PermissionUtils;

import java.util.List;

public class CellInfoService {
    private final TelephonyManager telephonyManager;
    private final Context context;

    public CellInfoService(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public String getNetworkOperatorInfo() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            String networkOperator = telephonyManager.getNetworkOperator();
            String networkOperatorName = telephonyManager.getNetworkOperatorName();

            if (networkOperator != null && networkOperator.length() >= 3) {
                String mcc = networkOperator.substring(0, 3);
                String mnc = networkOperator.substring(3);
                return String.format("%s (%s %s)", networkOperatorName, mcc, mnc);
            }
            return networkOperatorName != null ? networkOperatorName : "Unknown";
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getCellIdentity() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList == null || cellInfoList.isEmpty()) {
                return "No cell info available";
            }

            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo instanceof CellInfoLte) {
                    return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getCi());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                    android.telephony.CellIdentity identity = ((CellInfoNr) cellInfo).getCellIdentity();
                    if (identity instanceof android.telephony.CellIdentityNr) {
                        return String.valueOf(((android.telephony.CellIdentityNr) identity).getNci());
                    }
                }
            }
            return "N/A";
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getTrackingAreaCode() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList == null || cellInfoList.isEmpty()) {
                return "No cell info available";
            }

            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo instanceof CellInfoLte) {
                    return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getTac());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                    android.telephony.CellIdentity identity = ((CellInfoNr) cellInfo).getCellIdentity();
                    if (identity instanceof android.telephony.CellIdentityNr) {
                        return String.valueOf(((android.telephony.CellIdentityNr) identity).getTac());
                    }
                }
            }
            return "N/A";
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getPhysicalCellId() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList == null || cellInfoList.isEmpty()) {
                return "No cell info available";
            }

            for (CellInfo cellInfo : cellInfoList) {
                if (cellInfo instanceof CellInfoLte) {
                    return String.valueOf(((CellInfoLte) cellInfo).getCellIdentity().getPci());
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                    android.telephony.CellIdentity identity = ((CellInfoNr) cellInfo).getCellIdentity();
                    if (identity instanceof android.telephony.CellIdentityNr) {
                        return String.valueOf(((android.telephony.CellIdentityNr) identity).getPci());
                    }
                }
            }
            return "N/A";
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        }
    }
}