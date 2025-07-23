package com.example.xmobisense3.services;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.telephony.ServiceState;


import com.example.xmobisense3.utils.PermissionUtils;

import java.util.List;

public class NetworkTypeService {

    private final TelephonyManager telephonyManager;
    private final Context context;

    public NetworkTypeService(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    // 1. Get Network Type Name (2G, 3G, 4G, 5G)
    public String getNetworkTypeName() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            SignalStrength signalStrength = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signalStrength = telephonyManager.getSignalStrength();
            }

            boolean hasNr = false;
            boolean hasLte = false;

            if (signalStrength != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
                List<CellSignalStrength> strengths = signalStrength.getCellSignalStrengths();
                for (CellSignalStrength signal : strengths) {
                    if (signal instanceof CellSignalStrengthNr) {
                        hasNr = true;
                    } else if (signal instanceof CellSignalStrengthLte) {
                        hasLte = true;
                    }
                }
            }

            if (hasNr && !hasLte) {
                return "NR 5G SA";
            } else if (hasNr && hasLte) {
                return "NR 5G NSA";
            } else if (hasLte) {
                return "LTE 4G";
            }

            // CellInfo fallback
            List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
            if (cellInfoList != null) {
                for (CellInfo cellInfo : cellInfoList) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo.isRegistered()) {
                        if (cellInfo instanceof CellInfoNr) {
                            hasNr = true;
                        } else if (cellInfo instanceof CellInfoLte) {
                            hasLte = true;
                        }
                    }
                }

                if (hasNr && !hasLte) {
                    return "NR 5G SA";
                } else if (hasNr && hasLte) {
                    return "NR 5G NSA";
                } else if (hasLte) {
                    return "LTE 4G";
                }
            }

            // DataNetworkType fallback
            int dataNetworkType = telephonyManager.getDataNetworkType();
            switch (dataNetworkType) {
                case TelephonyManager.NETWORK_TYPE_NR:
                    return "NR 5G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "LTE 4G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                default:
                    break;
            }

            return "No mobile network detected";

        } catch (SecurityException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    public String getNetworkOperatorName() {
        if (!PermissionUtils.hasNetworkPermissions(context)) {
            return "Permission Denied";
        }

        try {
            if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
                return "SIM card not inserted or inactive";
            }

            String operator = telephonyManager.getNetworkOperatorName();
            return operator.isEmpty() ? "Unknown operator" : operator;
        } catch (SecurityException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }


    public String getFrequencyBand() {
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

            String networkType = getNetworkTypeName();

            for (CellInfo cellInfo : cellInfoList) {
                if (!cellInfo.isRegistered()) continue;

                // 5G SA
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
                    android.telephony.CellIdentity identity = ((CellInfoNr) cellInfo).getCellIdentity();
                    if (identity instanceof android.telephony.CellIdentityNr) {
                        int nrarfcn = ((android.telephony.CellIdentityNr) identity).getNrarfcn();

                        return "Band: " + mapNrArfcnToBand(nrarfcn) + "\nNR-ARFCN: " + nrarfcn + "\nType: 5G NR (SA)";
                    }
                }

                // LTE (could be NSA or  4G)
                else if (cellInfo instanceof CellInfoLte) {
                    int earfcn = ((CellInfoLte) cellInfo).getCellIdentity().getEarfcn();
                    String band = mapEarfcnToBand(earfcn);

                    if (networkType.contains("NSA")) {
                        return "Band: " + band + "\nEARFCN: " + earfcn + "\nType: 5G NR (NSA)";
                    } else {
                        return "Band: " + band + "\nEARFCN: " + earfcn + "\nType: LTE (4G)";
                    }
                }

                // 3G WCDMA
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof android.telephony.CellInfoWcdma) {
                    int uarfcn = ((android.telephony.CellInfoWcdma) cellInfo).getCellIdentity().getUarfcn();
                    String band = mapUarfcnToBand(uarfcn);
                    return "Band: " + band + "\nUARFCN: " + uarfcn + " (3G)";
                }
            }

            return "Unable to determine frequency band";

        } catch (SecurityException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
    private String mapEarfcnToBand(int earfcn) {
        if (earfcn >= 0 && earfcn <= 599)
            return "Band 1 (2100 MHz FDD)";
        if (earfcn >= 600 && earfcn <= 1199)
            return "Band 3 (1800 MHz FDD)";
        if (earfcn >= 1200 && earfcn <= 1949)
            return "Band 3 (1800 MHz FDD)";
        if (earfcn >= 1950 && earfcn <= 2399)
            return "Band 2 (1900 MHz FDD)";
        if (earfcn >= 2400 && earfcn <= 2649)
            return "Band 4 (1700 MHz FDD)";
        if (earfcn >= 2750 && earfcn <= 3449)
            return "Band 7 (2600 MHz FDD)";
        if (earfcn >= 3450 && earfcn <= 3799)
            return "Band 8 (900 MHz FDD)";
        if (earfcn >= 6150 && earfcn <= 6449)
            return "Band 20 (800 MHz FDD)";
        if (earfcn >= 6450 && earfcn <= 6599)
            return "Band 14 (700 MHz FDD)";
        if (earfcn >= 8650 && earfcn <= 8949)
            return "Band 5 (850 MHz FDD)";
        return "Unknown";
    }


    private String mapNrArfcnToBand(int nrarfcn) {
        if (nrarfcn >= 0 && nrarfcn <= 599)
            return "n1 (2100 MHz FDD)";
        if (nrarfcn >= 600 && nrarfcn <= 1199)
            return "n2 (1900 MHz FDD)";
        if (nrarfcn >= 1200 && nrarfcn <= 1949)
            return "n3 (1800 MHz FDD)";
        if (nrarfcn >= 222916 && nrarfcn <= 227916)
            return "n5 (850 MHz FDD)";
        if (nrarfcn >= 151600 && nrarfcn <= 160600)
            return "n28 (700 MHz FDD)";
        if (nrarfcn >= 205416 && nrarfcn <= 210416)
            return "n28 (700 MHz FDD)";
        if (nrarfcn >= 158200 && nrarfcn <= 164180)
            return "n77 (3700 MHz TDD)";
        if (nrarfcn >= 620000 && nrarfcn <= 653333)
            return "n78 (3500 MHz TDD)";
        return "Unknown (NR-ARFCN: " + nrarfcn + ")";
    }
    private String mapUarfcnToBand(int uarfcn) {
        if (uarfcn >= 10562 && uarfcn <= 10838)
            return "Band 1 (2100 MHz)";
        if (uarfcn >= 9662 && uarfcn <= 9938)
            return "Band 8 (900 MHz)";
        if (uarfcn >= 1162 && uarfcn <= 1513)
            return "Band 5 (850 MHz)";
        if (uarfcn >= 412 && uarfcn <= 687)
            return "Band 2 (1900 MHz)";
        if (uarfcn >= 1537 && uarfcn <= 1738)
            return "Band 4 (1700 MHz)";
        if (uarfcn >= 2937 && uarfcn <= 3088)
            return "Band 3 (1800 MHz)";
        if (uarfcn >= 3757 && uarfcn <= 3988)
            return "Band 9 (1800 MHz)";
        return "Unknown";
    }
}