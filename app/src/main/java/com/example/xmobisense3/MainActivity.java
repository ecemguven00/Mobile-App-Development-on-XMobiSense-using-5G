package com.example.xmobisense3;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.xmobisense3.database.AppDatabase;
import com.example.xmobisense3.database.NetworkMeasurementRepository;
import com.example.xmobisense3.services.CallStateService;
import com.example.xmobisense3.services.CellInfoService;
import com.example.xmobisense3.services.DataUsageService;
import com.example.xmobisense3.services.LocationCoordinatesService;
import com.example.xmobisense3.services.NetworkTypeService;
import com.example.xmobisense3.services.SignalStrengthService;
import com.example.xmobisense3.utils.PermissionUtils;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    // UI elements
    private Button btnStartService, btnStopService;
    private TextView txtVersion, txtDate, txtLocation, txtNetworkType,
            txtOperator, txtRxPower, txtTxPower, txtReceivedData, txtTransmittedData;
    private TextView txtSimNet, txtSimNci, txtSimTac, txtSimPci;
    private TextView txtBand;
    private TextView txtCallState;


    // Services
    private LocationCoordinatesService locationService;
    private NetworkTypeService networkTypeService;
    private TelephonyManager telephonyManager;
    private SignalStrengthService signalStrengthService;
    private DataUsageService dataUsageService;
    private MapView mapView;
    private CellInfoService cellInfoService;
    private CallStateService callStateService;

    private File currentCsvFile;
    private AppDatabase database;
    private NetworkMeasurementRepository repository;

    // Handler for periodic updates
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int UPDATE_INTERVAL_MS = 10000; // 10 seconds
    private final Runnable periodicUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            refreshAllData();
            mainHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // i Initialized UI elements
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        txtVersion = findViewById(R.id.txtVersion);
        txtDate = findViewById(R.id.txtDate);
        txtLocation = findViewById(R.id.txtLocation);
        txtNetworkType = findViewById(R.id.txtNetworkType);
        txtOperator = findViewById(R.id.txtOperator);
        txtRxPower = findViewById(R.id.txtRxPower);
        txtTxPower = findViewById(R.id.txtTxPower);
        txtReceivedData = findViewById(R.id.txtReceivedData);
        txtTransmittedData = findViewById(R.id.txtTransmittedData);
        txtSimNet = findViewById(R.id.txtSimNet);
        txtSimNci = findViewById(R.id.txtSimNci);
        txtSimTac = findViewById(R.id.txtSimTac);
        txtSimPci = findViewById(R.id.txtSimPci);
        txtBand = findViewById(R.id.txtBand);
        txtCallState = findViewById(R.id.txtCallState);


        mapView = findViewById(R.id.map);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15); // default zoom
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // i Initialized services
        locationService = new LocationCoordinatesService(this, txtLocation, mapView);
        networkTypeService = new NetworkTypeService(this);
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        signalStrengthService = new SignalStrengthService(this, txtRxPower);
        dataUsageService = new DataUsageService(this, txtReceivedData, txtTransmittedData);
        cellInfoService = new CellInfoService(this);
        callStateService = new CallStateService(this);

        database = AppDatabase.getDatabase(this);
        repository = new NetworkMeasurementRepository(this);

        //  i Set button listeners
        btnStartService.setOnClickListener(v -> startService());
        btnStopService.setOnClickListener(v -> stopService());

        //  i Set the initial color for the Start Service button (Purple)
        btnStartService.setBackgroundColor(Color.parseColor("#6200EE"));
        // Set the initial color for the Stop Service button (Grey) and disable it
        btnStopService.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnStopService.setEnabled(false); // Initially disable the Stop

        //  this the default look of my output
        loadInitialData();

    }

    private void startService() {
        // i Disableed Start button and enable Stop button
        btnStartService.setEnabled(false);
        btnStopService.setEnabled(true);
        btnStartService.setBackgroundColor(Color.parseColor("#E0E0E0"));
        btnStopService.setBackgroundColor(Color.parseColor("#6200EE"));


        // i Requested permissions only when user clicks Start
        requestAllPermissions();

        // 🚨 Clear all existing measurements when service starts
        repository.deleteAllMeasurements();

        // Reset the CSV file for a new session
        currentCsvFile = null;

        telephonyManager.listen(signalStrengthService, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        callStateService.startListening();


        startPeriodicUpdates();

    }



    private void stopService() {
        // i Enableed Start button and disable Stop button
        btnStartService.setEnabled(true);
        btnStopService.setEnabled(false);
        btnStartService.setBackgroundColor(Color.parseColor("#6200EE"));
        btnStopService.setBackgroundColor(Color.parseColor("#E0E0E0"));

        // i Displayed current time when service stops
        txtDate.setText("Service Stopped at: " + getCurrentTimeIso());

        stopPeriodicUpdates();

        telephonyManager.listen(signalStrengthService, PhoneStateListener.LISTEN_NONE);
        callStateService.stopListening();
        txtCallState.setText("Checking CallState...");


        //i  Reseted location text when service stops
        txtLocation.setText("Checking for GPS signal...");
        txtNetworkType.setText("Checking Network Type...");
        txtOperator.setText("Checking network provider...");
        txtBand.setText("Checking frequency band...");
        txtRxPower.setText("Checking Rx Power...");
        txtRxPower.setTextColor(Color.parseColor("#9E9E9E"));
        txtReceivedData.setText("Checking Received Data...");
        txtTransmittedData.setText("Checking Transmitted Data...");
        txtBand.setText("Checking frequency band...");
        txtSimNet.setText("Checking SIM Network...");
        txtSimNci.setText("Checking SIM NCI...");
        txtSimTac.setText("Checking SIM TAC...");
        txtSimPci.setText("Checking SIM PCI...");

    }



    private void loadInitialData() {
        // Loading initial data into UI fields
        txtVersion.setText("XMobiSense 5G");
        txtDate.setText(getCurrentTimeIso());
        txtLocation.setText("Checking for GPS signal...");
        txtNetworkType.setText("Checking Network Type...");
        txtOperator.setText("Checking network provider...");
        txtRxPower.setText("Checking Rx Power...");
        txtTxPower.setText("Tx Power: Not Available (restricted by OS)");
        txtReceivedData.setText("Checking Received Data...");
        txtTransmittedData.setText("Checking Transmitted Data...");
        txtBand.setText("Checking frequency band...");
        txtSimNet.setText("Checking SIM Network...");
        txtSimNci.setText("Checking SIM NCI...");
        txtSimTac.setText("Checking SIM TAC...");
        txtSimPci.setText("Checking SIM PCI...");
        txtCallState.setText("Checking Call State...");
    }
    private void startPeriodicUpdates() {
        mainHandler.post(periodicUpdateRunnable);
    }

    private void stopPeriodicUpdates() {
        mainHandler.removeCallbacks(periodicUpdateRunnable);
    }

    private void refreshAllData() {

        locationService.getLocation();


        txtNetworkType.setText(networkTypeService.getNetworkTypeName());
        txtOperator.setText(networkTypeService.getNetworkOperatorName());
        txtBand.setText(networkTypeService.getFrequencyBand());


        signalStrengthService.updateSignalStrengthUI();



        // Update cell-specific info
        txtSimNet.setText(cellInfoService.getNetworkOperatorInfo());
        txtSimNci.setText(cellInfoService.getCellIdentity());
        txtSimTac.setText(cellInfoService.getTrackingAreaCode());
        txtSimPci.setText(cellInfoService.getPhysicalCellId());



        dataUsageService.updateUsage();

        txtCallState.setText("Call State: " + callStateService.getCurrentCallState());

        // Save to database
        saveCurrentMeasurement();
        // Update time
        txtDate.setText("Last Updated: " + getCurrentTimeIso());
    }

    private void saveCurrentMeasurement() {
        try {
            // Get location
            String location = txtLocation.getText().toString();
            double latitude = 0.0;
            double longitude = 0.0;
            try {
                String[] parts = location.split("\n");
                for (String part : parts) {
                    part = part.trim();
                    if (part.startsWith("Longitude:")) {
                        longitude = Double.parseDouble(part.substring("Longitude:".length()).trim());
                    } else if (part.startsWith("Latitude:")) {
                        latitude = Double.parseDouble(part.substring("Latitude:".length()).trim());
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to parse latitude/longitude: " + e.getMessage());
            }


            // Get network information using existing service
            String networkType = networkTypeService.getNetworkTypeName();
            String frequencyBand = networkTypeService.getFrequencyBand();
            String operator = networkTypeService.getNetworkOperatorName();

            // Parse the signal strength text to get dBm value and quality
            String rxPowerText = txtRxPower.getText().toString();

            int rxPower = -999; // Default value for invalid/unavailable signal
            int rx5G = 0; // Default value
            String signalQuality = "Unknown";


            if (rxPowerText.contains("dBm")) {
                // For 5G NSA mode (contains both 4G and 5G values)
                if (rxPowerText.contains("5G NSA:")) {
                    // Extract 4G Rx power
                    String[] parts = rxPowerText.split("4G: ");
                    if (parts.length > 1) {
                        String[] powerParts = parts[1].split(" dBm");
                        try {
                            rxPower = Integer.parseInt(powerParts[0].trim());
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", "Failed to parse 4G Rx power: " + e.getMessage());
                        }
                    }

                    // Extract 5G Rx power
                    parts = rxPowerText.split("5G: ");
                    if (parts.length > 1) {
                        String[] powerParts = parts[1].split(" dBm");
                        try {
                            rx5G = Integer.parseInt(powerParts[0].trim());
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", "Failed to parse 5G Rx power: " + e.getMessage());
                        }
                    }

                    // Extract quality
                    if (rxPowerText.contains("(") && rxPowerText.contains(")")) {
                        int startIndex = rxPowerText.lastIndexOf("(") + 1;
                        int endIndex = rxPowerText.lastIndexOf(")");
                        if (startIndex < endIndex) {
                            signalQuality = rxPowerText.substring(startIndex, endIndex);
                        }
                    }
                }
                // For 4G/3G mode
                else {
                    // Extract dBm value
                    String[] parts = rxPowerText.split("dBm");
                    if (parts.length > 0) {
                        String[] valueParts = parts[0].split(":");
                        if (valueParts.length > 1) {
                            try {
                                rxPower = Integer.parseInt(valueParts[1].trim());
                            } catch (NumberFormatException e) {
                                Log.e("MainActivity", "Failed to parse Rx power: " + e.getMessage());
                            }
                        }
                    }

                    // Extract quality
                    if (rxPowerText.contains("(") && rxPowerText.contains(")")) {
                        int startIndex = rxPowerText.indexOf("(") + 1;
                        int endIndex = rxPowerText.indexOf(")");
                        if (startIndex < endIndex) {
                            signalQuality = rxPowerText.substring(startIndex, endIndex);
                        }
                    }
                }
            }

            double rxGbps = dataUsageService.getLastRxGbps();
            double txGbps = dataUsageService.getLastTxGbps();

            // Get call state
            String callState = callStateService.getCurrentCallState();
            Date timestamp = new Date();

            repository.insertMeasurement(timestamp, longitude,latitude, networkType,
                    frequencyBand, operator, rxPower, signalQuality,
                    rxGbps, txGbps, callState, rx5G);

            // Save to CSV
            writeMeasurementToCsv(timestamp, longitude,latitude, networkType, frequencyBand,
                    operator, rxPower, signalQuality, rxGbps, txGbps, callState, rx5G);


            Log.d("Measurement", "=====================");
            Log.d("Measurement", "Saved at: " + getCurrentTimeIso());
            Log.d("Measurement", "Location : " + location);
            Log.d("Measurement", "Operator : " + operator);
            Log.d("Measurement", "Network  : " + networkType);
            Log.d("Measurement", "Band     : " + frequencyBand);
            Log.d("Measurement", "Rx Power : " + rxPower + " dBm (" + signalQuality + ")");
            Log.d("Measurement", "Rx Gbps  : " + rxGbps);
            Log.d("Measurement", "Tx Gbps  : " + txGbps);
            Log.d("Measurement", "CallState: " + callState);
            Log.d("Measurement", "=====================");

        } catch (Exception e) {
            Log.e("MainActivity", "Error saving measurement: " + e.getMessage());
        }
    }
    private void writeMeasurementToCsv(Date timestamp, double longitude, double latitude, String networkType,
                                       String frequencyBand, String operator, int rxPower,
                                       String signalQuality, double rxGbps, double txGbps,
                                       String callState,int rx5G) {
        try {
            File csvDir = new File(getExternalFilesDir(null), "measurement");
            if (csvDir == null) {
                Log.e("CSV_ERROR", "CSV Directory is null");
                return;
            }

            if (!csvDir.exists()) {
                csvDir.mkdirs();
            }

            // Only create the file the first time it's needed
            if (currentCsvFile == null) {
                String timestampStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(timestamp);
                String fileName = "network_measurements_" + timestampStr + ".csv";
                currentCsvFile = new File(csvDir, fileName);
                Log.d("CSV_CREATION", "New CSV file created: " + fileName);
            }

            boolean fileExists = currentCsvFile.exists();
            FileWriter writer = new FileWriter(currentCsvFile, true);

            // Write header if file is new
            if (!fileExists) {
                writer.append("Timestamp,Longitude,Latitude,NetworkType,FrequencyBand,Operator,RxPower,SignalQuality,RxGbps,TxGbps,CallState,Rx5G\n");
            }


            String row = String.format("\"%s\",%.6f,%.6f,\"%s\",\"%s\",\"%s\",%d,\"%s\",%.6f,%.6f,\"%s\",%d\n",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp),
                    longitude,
                    latitude,
                    networkType,
                    frequencyBand,
                    operator,
                    rxPower,
                    signalQuality,
                    rxGbps,
                    txGbps,
                    callState,
                    rx5G);

            writer.append(row);
            writer.flush();
            writer.close();

            Log.d("CSV_PATH", "CSV saved at: " + currentCsvFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e("MainActivity", "Error writing CSV: " + e.getMessage());
        }
    }
    private void requestAllPermissions() {
        if (PermissionUtils.hasAllPermissions(this)) {

            telephonyManager.listen(signalStrengthService, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            callStateService.startListening();

            startPeriodicUpdates();
        } else {
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE
            };
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (PermissionUtils.hasAllPermissions(this)) {
                telephonyManager.listen(signalStrengthService, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                callStateService.startListening();
                startPeriodicUpdates();
            } else {
                txtLocation.setText("Location Permission Denied");
                txtNetworkType.setText("Network Permission Denied");
                txtOperator.setText("Operator Permission Denied");
                txtBand.setText("Frequency Band Permission Denied");
                txtRxPower.setText("Signal Strength Permission Denied");
                txtReceivedData.setText("Received Data Permission Denied");
                txtTransmittedData.setText("Transmitted Data Permission Denied");
                txtSimNet.setText("SIM Network Permission Denied");
                txtSimNci.setText("SIM NCI Permission Denied");
                txtSimTac.setText("SIM TAC Permission Denied");
                txtSimPci.setText("SIM PCI Permission Denied");
                txtCallState.setText("Call State Permission Denied");
            }
        }
    }

    private String getCurrentTimeIso() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPeriodicUpdates();
    }
}

