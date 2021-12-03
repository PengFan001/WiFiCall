package com.jiaze.wificall;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jiaze.wificall.call.WifiCallManager;
import com.jiaze.wificall.call.WifiCallStateListener;
import com.jiaze.wificall.common.Constants;
import com.jiaze.wificall.util.LogUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements View.OnClickListener, ServiceStateListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSION = 10000;
    private static final int MSG_UPDATE_WIFI_CALL_STATE = 0;

    private EditText mEtIp;
    private EditText mEtReceiverPort;
    private EditText mEtSenderPort;
    private Button mBtnConfigure;
    private Button mBtnCall;
    private Button mBtnHungUp;

    private MyService mService;
    private MyHandler mHandler;

    private static class MyHandler extends Handler {
        WeakReference<MainActivity> mainActivityWeakReference;
        public MyHandler(MainActivity activity) {
            mainActivityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void dispatchMessage(Message msg) {
            MainActivity activity = mainActivityWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_UPDATE_WIFI_CALL_STATE:
                        int state = (int) msg.obj;
                        if (state == WifiCallManager.CALL_STATE_ACTIVE) {
                            activity.mBtnCall.setEnabled(false);
                            activity.mBtnHungUp.setEnabled(true);
                        } else if (state == WifiCallManager.CALL_STATE_IDLE) {
                            activity.mBtnCall.setEnabled(true);
                            activity.mBtnHungUp.setEnabled(false);
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mEtIp = (EditText) findViewById(R.id.et_ip);
        mEtSenderPort = (EditText) findViewById(R.id.et_sender_port);
        mEtReceiverPort = (EditText) findViewById(R.id.et_receiver_port);

        mBtnConfigure = (Button) findViewById(R.id.btn_config);
        mBtnCall = (Button) findViewById(R.id.btn_call);
        mBtnHungUp = (Button) findViewById(R.id.btn_hungup);

        mBtnConfigure.setEnabled(false);
        mBtnCall.setEnabled(false);
        mBtnHungUp.setEnabled(false);
        mBtnConfigure.setOnClickListener(this);
        mBtnCall.setOnClickListener(this);
        mBtnHungUp.setOnClickListener(this);
    }

    private void initData() {
        mHandler = new MyHandler(this);
        mService = MyApplication.getInstance().getService();
        if (mService == null) {
            MyApplication.getInstance().registerServiceStateListener(this);
        } else {
            String ip = mService.getReceiverIp();
            int senderPort = mService.getSenderPort();
            int receiverPort = mService.getReceiverPort();

            if (mService.getWifiCallState() == WifiCallManager.CALL_STATE_ACTIVE) {
                mBtnConfigure.setEnabled(false);
                mBtnCall.setEnabled(false);
                mBtnHungUp.setEnabled(true);
            } else if (mService.getWifiCallState() == WifiCallManager.CALL_STATE_IDLE) {
                mBtnConfigure.setEnabled(true);
                mBtnCall.setEnabled(!TextUtils.isEmpty(ip) && senderPort != 0 && receiverPort != 0);
                mBtnHungUp.setEnabled(false);
            }

            mEtReceiverPort.setText(String.valueOf(receiverPort));
            mEtSenderPort.setText(String.valueOf(senderPort));
            mEtIp.setText(ip);
            mService.registerWifiCallListener(mCallStateListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        needPermission();
    }

    private void needPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
               permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[0]),
                        REQUEST_CODE_PERMISSION);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_config:
                configureCall();
                break;

            case R.id.btn_call:
                mService.call();
                break;

            case R.id.btn_hungup:
                mService.hungUp();
                break;

            default:
                break;
        }
    }

    private boolean isInputValid(String input, String validFormat){
        boolean isValid;
        Pattern pattern = Pattern.compile(validFormat);
        Matcher matcher = pattern.matcher(input);
        isValid = matcher.matches();
        return isValid;
    }

    private void configureCall() {
        String ip = mEtIp.getText().toString();
        if (TextUtils.isEmpty(ip)) {
            Toast.makeText(this, getString(R.string.limit_ip_can_not_null),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isInputValid(ip, Constants.IP_FORMAT)) {
            Toast.makeText(this, getString(R.string.limit_ip_format_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int senderPort = 0;
        int receiverPort = 0;
        try {
            receiverPort = Integer.parseInt(mEtReceiverPort.getText().toString());
            senderPort = Integer.parseInt(mEtSenderPort.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (receiverPort <= 0 || senderPort <= 0) {
            Toast.makeText(this, getString(R.string.limit_port_error),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mService.configureWifiCall(senderPort, receiverPort, ip);
        mBtnCall.setEnabled(true);
    }

    @Override
    public void onServiceConnected() {
        LogUtil.d(TAG, "onServiceConnected: -----");
        mService = MyApplication.getInstance().getService();
        mBtnConfigure.setEnabled(true);
        mEtReceiverPort.setText(String.valueOf(mService.getReceiverPort()));
        mEtSenderPort.setText(String.valueOf(mService.getSenderPort()));
        mEtIp.setText(mService.getReceiverIp());
        mService.registerWifiCallListener(mCallStateListener);
    }

    @Override
    public void onServiceDisconnected() {
        LogUtil.d(TAG, "onServiceDisconnected: -----");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.getInstance().unregisterServiceStateListener(this);
        mService.unregisterWifiCallListener(mCallStateListener);
    }

    private final WifiCallStateListener mCallStateListener = new WifiCallStateListener() {
        @Override
        public void callStateListener(int state) {
            LogUtil.d(TAG, "callStateListener: state = " + state);
            Message message = mHandler.obtainMessage();
            message.what = MSG_UPDATE_WIFI_CALL_STATE;
            message.obj = state;
            message.sendToTarget();
        }
    };
}