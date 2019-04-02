package leavesc.hello.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ListView;

import leavesc.hello.bluetooth.adapter.DeviceAdapter;

/**
 * 作者：leavesC
 * 时间：2019/3/23 11:43
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
public class ScanDeviceActivity extends BaseActivity {

    private DeviceAdapter deviceAdapter;

    private BluetoothAdapter bluetoothAdapter;

    private Handler handler = new Handler();

    private BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        showLoadingDialog("正在搜索附近的蓝牙设备");
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        showToast("搜索结束");
                        hideLoadingDialog();
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        deviceAdapter.addDevice(bluetoothDevice);
                        deviceAdapter.notifyDataSetChanged();
                        break;
                }
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
            showToast("当前设备不支持蓝牙");
            finish();
            return;
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
        ListView lv_deviceList = findViewById(R.id.lv_deviceList);
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
        if (bluetoothAdapter.isEnabled()) {
            scanDevice();
        } else {
            if (bluetoothAdapter.enable()) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanDevice();
                    }
                }, 1500);
            } else {
                showToast("请求蓝牙权限被拒绝，请授权");
            }
        }
    }

    private void scanDevice() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

}