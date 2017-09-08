package com.comit.mac;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * 作者：叶应是叶
 * 时间：2017/9/8 20:02
 * 描述：
 */
public class ConnectA2dpActivity extends AppCompatActivity {

    private DeviceAdapter deviceAdapter;

    private BluetoothAdapter bluetoothAdapter;

    private Handler handler = new Handler();

    private BluetoothA2dp bluetoothA2dp;

    private LoadingDialog loadingDialog;

    private final String TAG = "ConnectA2dpActivity";

    private BroadcastReceiver a2dpReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                    int connectState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                    if (connectState == BluetoothA2dp.STATE_DISCONNECTED) {
                        Toast.makeText(ConnectA2dpActivity.this, "已断开连接", Toast.LENGTH_SHORT).show();
                    } else if (connectState == BluetoothA2dp.STATE_CONNECTED) {
                        Toast.makeText(ConnectA2dpActivity.this, "已连接", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED:
                    int playState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                    if (playState == BluetoothA2dp.STATE_PLAYING) {
                        Toast.makeText(ConnectA2dpActivity.this, "处于播放状态", Toast.LENGTH_SHORT).show();
                    } else if (playState == BluetoothA2dp.STATE_NOT_PLAYING) {
                        Toast.makeText(ConnectA2dpActivity.this, "未在播放", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    showLoadingDialog("正在搜索蓝牙设备，搜索时间大约一分钟");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(ConnectA2dpActivity.this, "搜索蓝牙设备结束", Toast.LENGTH_SHORT).show();
                    hideLoadingDialog();
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    deviceAdapter.addDevice(bluetoothDevice);
                    deviceAdapter.notifyDataSetChanged();
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int status = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                    if (status == BluetoothDevice.BOND_BONDED) {
                        Toast.makeText(ConnectA2dpActivity.this, "已连接", Toast.LENGTH_SHORT).show();
                    } else if (status == BluetoothDevice.BOND_NONE) {
                        Toast.makeText(ConnectA2dpActivity.this, "未连接", Toast.LENGTH_SHORT).show();
                    }
                    hideLoadingDialog();
                    break;
            }
        }
    };

    private BluetoothProfile.ServiceListener profileServiceListener = new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                Toast.makeText(ConnectA2dpActivity.this, "onServiceDisconnected", Toast.LENGTH_SHORT).show();
                bluetoothA2dp = null;
            }
        }

        @Override
        public void onServiceConnected(int profile, final BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                Toast.makeText(ConnectA2dpActivity.this, "onServiceConnected", Toast.LENGTH_SHORT).show();
                bluetoothA2dp = (BluetoothA2dp) proxy;
            }
        }
    };

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = deviceAdapter.getDevice(position);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(ConnectA2dpActivity.this, "已连接该设备", Toast.LENGTH_SHORT).show();
                return;
            }
            showLoadingDialog("正在连接");
            connectA2dp(device);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_a2dp);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(ConnectA2dpActivity.this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }
        bluetoothAdapter.getProfileProxy(this, profileServiceListener, BluetoothProfile.A2DP);
        initView();
        registerDiscoveryReceiver();
        registerA2dpReceiver();
        startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(a2dpReceiver);
        unregisterReceiver(discoveryReceiver);
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private void initView() {
        ListView lv_deviceList = (ListView) findViewById(R.id.lv_deviceList);
        deviceAdapter = new DeviceAdapter(this);
        lv_deviceList.setAdapter(deviceAdapter);
        lv_deviceList.setOnItemClickListener(itemClickListener);
    }

    private void registerDiscoveryReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(discoveryReceiver, intentFilter);
    }

    private void registerA2dpReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        registerReceiver(a2dpReceiver, intentFilter);
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
                Toast.makeText(ConnectA2dpActivity.this, "请求蓝牙权限被拒绝", Toast.LENGTH_SHORT).show();
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

    public void setPriority(BluetoothDevice device, int priority) {
        try {
            Method connectMethod = BluetoothA2dp.class.getMethod("setPriority", BluetoothDevice.class, int.class);
            connectMethod.invoke(bluetoothA2dp, device, priority);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectA2dp(BluetoothDevice bluetoothDevice) {
        if (bluetoothA2dp == null || bluetoothDevice == null) {
            return;
        }
        setPriority(bluetoothDevice, 100);
        try {
            Method connectMethod = BluetoothA2dp.class.getMethod("connect", BluetoothDevice.class);
            connectMethod.invoke(bluetoothA2dp, bluetoothDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
