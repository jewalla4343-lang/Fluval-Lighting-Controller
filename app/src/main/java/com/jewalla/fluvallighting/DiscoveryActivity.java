package com.jewalla.fluvallighting;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class DiscoveryActivity extends Activity {
    private static final String SCAN_PERMISSION = Manifest.permission.BLUETOOTH_SCAN;
    private static final int REQUEST_SCAN = 1;
    private static final long SCAN_DURATION_MS = 15_000;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> devices = new LinkedHashMap<>();
    private final Runnable timeout = () -> stopScan("Scan complete.");
    private BluetoothLeScanner scanner;
    private ScanCallback activeScan;
    private Button start;
    private Button stop;
    private TextView status;
    private TextView results;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setTextSize(24);
        title.setText("Fluval Discovery");
        layout.addView(title);
        status = new TextView(this);
        status.setText("Scan nearby Bluetooth LE advertisements.");
        layout.addView(status);
        start = new Button(this);
        start.setText("Start scan");
        start.setOnClickListener(view -> requestScan());
        layout.addView(start);
        stop = new Button(this);
        stop.setText("Stop scan");
        stop.setEnabled(false);
        stop.setOnClickListener(view -> stopScan("Scan stopped."));
        layout.addView(stop);
        ScrollView scroll = new ScrollView(this);
        results = new TextView(this);
        results.setTextSize(16);
        scroll.addView(results);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
    }

    private void requestScan() {
        if (checkSelfPermission(SCAN_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            requestPermissions(new String[] {SCAN_PERMISSION}, REQUEST_SCAN);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                     int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != REQUEST_SCAN) {
            return;
        }
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            status.setText("Allow Nearby devices in Android Settings to scan.");
        }
    }

    private void startScan() {
        if (activeScan != null || checkSelfPermission(SCAN_PERMISSION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            BluetoothManager manager = getSystemService(BluetoothManager.class);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            scanner = adapter == null ? null : adapter.getBluetoothLeScanner();
            if (scanner == null) {
                status.setText("Turn on Bluetooth and try again.");
                return;
            }
            devices.clear();
            results.setText("");
            activeScan = new ScanCallback() {
                @Override public void onScanResult(int type, ScanResult result) {
                    main.post(() -> {
                        if (activeScan == this) {
                            showResult(result);
                        }
                    });
                }

                @Override public void onScanFailed(int errorCode) {
                    main.post(() -> {
                        if (activeScan == this) {
                            stopScan("Bluetooth scan failed (code " + errorCode + "). Try again shortly.");
                        }
                    });
                }
            };
            scanner.startScan(activeScan);
            start.setEnabled(false);
            stop.setEnabled(true);
            status.setText("Scanning for 15 seconds…");
            main.postDelayed(timeout, SCAN_DURATION_MS);
        } catch (SecurityException | IllegalStateException exception) {
            stopScan("Check that Bluetooth and Nearby devices access are enabled.");
        }
    }

    private void showResult(ScanResult result) {
        String address = result.getDevice().getAddress();
        ScanRecord record = result.getScanRecord();
        String name = record == null ? null : record.getDeviceName();
        String advertisedName = name == null || name.trim().isEmpty() ? "Unnamed device" : name;
        String services = record == null || record.getServiceUuids() == null
                ? "" : "\nAdvertised services " + record.getServiceUuids();
        devices.put(address, String.format(Locale.ROOT, "%s\n%s   %d dBm%s",
                advertisedName, address, result.getRssi(), services));
        results.setText(String.join("\n\n", devices.values()));
    }

    private void stopScan(String message) {
        main.removeCallbacks(timeout);
        ScanCallback callback = activeScan;
        activeScan = null;
        BluetoothLeScanner currentScanner = scanner;
        scanner = null;
        if (callback != null && currentScanner != null
                && checkSelfPermission(SCAN_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            try {
                currentScanner.stopScan(callback);
            } catch (SecurityException | IllegalStateException ignored) {
                // Bluetooth or scan permission can change while a scan is running.
            }
        }
        start.setEnabled(true);
        stop.setEnabled(false);
        status.setText(message + " Devices found: " + devices.size());
    }

    @Override protected void onStop() {
        if (activeScan != null) {
            stopScan("Scan stopped.");
        }
        super.onStop();
    }
}
