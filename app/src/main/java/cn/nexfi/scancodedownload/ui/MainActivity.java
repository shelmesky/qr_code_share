package cn.nexfi.scancodedownload.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import cn.nexfi.scancodedownload.R;
import cn.nexfi.scancodedownload.utils.Network;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn;
    private View view_share, v_parent;
    private PopupWindow mPopupWindow_share = null;
    private LayoutInflater inflate;
    private String ssid = "NexFi";
    private WifiManager wifiManager;
    private Handler mHandler, ex_handler;
    private AlertDialog mAlertDialog;
    private Thread thread;
    private boolean isExit, isWifiOpen, isApOpen;
    private String SSID, preSharedKey, userSSID, userPreShareedKey;
    private Network network;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        network = new Network(this);
        isWifiOpen = network.getWifiState();
        isApOpen = network.getApState();

        SSID = network.initApSSID();
        preSharedKey = network.initApPreSharedKey();
        if (SSID != null && !SSID.equals(ssid)) {
            saveNetConfig(this, SSID, preSharedKey);
        }
        inflate = getLayoutInflater();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                network.createFile(R.raw.nexfi_ble, "nexfi_ble.apk");
                network.createWifiAccessPoint(ssid);
                network.startWebService();
            }
        });
        thread.start();
        mHandler = new Handler();
        ex_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                isExit = false;
            }
        };
        initDialog();
        initView();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mAlertDialog.dismiss();
                Toast.makeText(MainActivity.this, "服务启动成功", Toast.LENGTH_SHORT).show();
                btn.setVisibility(View.VISIBLE);
            }
        }, 2000);
        initUserNetConfig();
        Log.e("SSID:" + SSID + "\n" + "preSharedKey:" + preSharedKey, "+++++++++++++++++++++++++++++++++++++++");
    }

    private void saveNetConfig(Context context, String SSID, String preSharedKey) {
        SharedPreferences sp = context.getSharedPreferences("netConfig", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("SSID", SSID);
        editor.putString("preSharedKey", preSharedKey);
        editor.commit();
    }

    private void initUserNetConfig() {
        userSSID = initSSID(this);
        userPreShareedKey = initPreSharedKey(this);
        Log.e("initUserNetConfig=\n", userSSID + "\n" + userPreShareedKey + "-----------------------");
    }

    private String initPreSharedKey(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("netConfig", Context.MODE_PRIVATE);
        return preferences.getString("preSharedKey", null);
    }

    private String initSSID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("netConfig", Context.MODE_PRIVATE);
        return preferences.getString("SSID", null);
    }


    private void initDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View v = inflater.inflate(R.layout.dialog_loading, null);
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        mAlertDialog.show();
        mAlertDialog.getWindow().setContentView(v);
        mAlertDialog.setCancelable(false);
        Toast.makeText(MainActivity.this, "正在启动服务，请稍后", Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                mHandler.sendEmptyMessageDelayed(0, 1500);
                Toast.makeText(this, "再按一次退出NexFi，关闭服务", Toast.LENGTH_SHORT).show();
                return false;
            } else {
                finish();
            }
        }
        return true;
    }


    private void initPopShare() {
        if (mPopupWindow_share == null) {
            mPopupWindow_share = new PopupWindow(view_share, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            mPopupWindow_share.setBackgroundDrawable(new ColorDrawable(0x00000000));
        }
        mPopupWindow_share.showAtLocation(v_parent, Gravity.CENTER, 0, 0);
    }


    private void initView() {
        btn = (Button) findViewById(R.id.btn);
        v_parent = inflate.inflate(R.layout.activity_main, null);
        view_share = inflate.inflate(R.layout.layout_share, null);
        btn.setOnClickListener(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        initUserNetConfig();
        network.stopWebService();
        network.restore(userSSID, userPreShareedKey);
        if (!isApOpen) {
            network.setApEnabled();
        }
        if (isWifiOpen) {
            network.setWifiEnabled(true);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn:
                initPopShare();
                break;
        }
    }


}
