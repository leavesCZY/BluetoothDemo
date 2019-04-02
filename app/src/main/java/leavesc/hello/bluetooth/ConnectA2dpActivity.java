package leavesc.hello.bluetooth;

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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.reflect.Method;

import leavesc.hello.bluetooth.adapter.DeviceAdapter;

/**
 * 作者：leavesC
 * 时间：2019/3/23 11:43
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class ConnectA2dpActivity extends BaseActivity {

    private DeviceAdapter deviceAdapter;

    private BluetoothAdapter bluetoothAdapter;

    private Handler handler = new Handler();

    private BluetoothA2dp bluetoothA2dp;

    private static final String TAG = "ConnectA2dpActivity";

    private BroadcastReceiver a2dpReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                        int connectState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                        if (connectState == BluetoothA2dp.STATE_DISCONNECTED) {
                            showToast("已断开连接");
                        } else if (connectState == BluetoothA2dp.STATE_CONNECTED) {
                            showToast("已连接");
                        }
                        break;
                    case BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED:
                        int playState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                        if (playState == BluetoothA2dp.STATE_PLAYING) {
                            showToast("处于播放状态");
                        } else if (playState == BluetoothA2dp.STATE_NOT_PLAYING) {
                            showToast("未在播放");
                        }
                        break;
                }
            }
        }
    };

    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        showLoadingDialog("正在搜索蓝牙设备，搜索时间大约一分钟");
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        showToast("搜索蓝牙设备结束");
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
                            showToast("已连接");
                        } else if (status == BluetoothDevice.BOND_NONE) {
                            showToast("未连接");
                        }
                        hideLoadingDialog();
                        break;
                }
            }
        }
    };

    private BluetoothProfile.ServiceListener profileServiceListener = new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                showToast("onServiceDisconnected");
                bluetoothA2dp = null;
            }
        }

        @Override
        public void onServiceConnected(int profile, final BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                showToast("onServiceConnected");
                bluetoothA2dp = (BluetoothA2dp) proxy;
            }
        }
    };

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = deviceAdapter.getDevice(position);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                showToast("已连接该设备");
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
            showToast("当前设备不支持蓝牙");
            finish();
            return;
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
        ListView lv_deviceList = findViewById(R.id.lv_deviceList);
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
                showToast("请求蓝牙权限被拒绝");
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

}