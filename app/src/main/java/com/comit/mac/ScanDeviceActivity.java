package com.comit.mac;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.Toast;

/**
 * 作者：叶应是叶
 * 时间：2017/9/8 20:13
 * 描述：
 */
public class ScanDeviceActivity extends AppCompatActivity {

    private LoadingDialog loadingDialog;

    private DeviceAdapter deviceAdapter;

    private BluetoothAdapter bluetoothAdapter;

    private Handler handler = new Handler();

    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    showLoadingDialog("正在搜索附近的蓝牙设备");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(ScanDeviceActivity.this, "搜索结束", Toast.LENGTH_SHORT).show();
                    hideLoadingDialog();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    deviceAdapter.addDevice(bluetoothDevice);
                    deviceAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        initView();
        registerDiscoveryReceiver();
        startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(discoveryReceiver);
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void initView() {
        ListView lv_deviceList = (ListView) findViewById(R.id.lv_deviceList);
        deviceAdapter = new DeviceAdapter(this);
        lv_deviceList.setAdapter(deviceAdapter);
    }

    private void registerDiscoveryReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(discoveryReceiver, intentFilter);
    }

    private void startScan() {
        if (!bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.enable()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanDevice();
                    }
                }, 1500);
            } else {
                Toast.makeText(this, "请求蓝牙权限被拒绝，请授权", Toast.LENGTH_SHORT).show();
            }
        } else {
            scanDevice();
        }
    }

    private void scanDevice() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.show(message, true, false);
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

}
