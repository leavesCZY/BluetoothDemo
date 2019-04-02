package leavesc.hello.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import leavesc.hello.bluetooth.dialog.LoadingDialog;

/**
 * 作者：leavesC
 * 时间：2019/3/23 12:07
 * 描述：
 * GitHub：https://github.com/leavesC
 * Blog：https://www.jianshu.com/u/9df45b87cfdf
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    protected LoadingDialog loadingDialog;

    protected void showLoadingDialog(String message) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(this);
        }
        loadingDialog.show(message, true, false);
    }

    protected void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    protected <T extends Activity> void startActivity(Class<T> clazz) {
        startActivity(new Intent(this, clazz));
    }

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}