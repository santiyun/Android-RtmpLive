package com.tttrtc.live.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtc.live.LocalConfig;
import com.tttrtc.live.LocalConstans;
import com.tttrtc.live.MainApplication;
import com.tttrtc.live.R;
import com.tttrtc.live.callback.MyTTTRtcEngineEventHandler;
import com.tttrtc.live.utils.SharedPreferencesUtil;
import com.wushuangtech.library.Constants;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.yanzhenjie.permission.AndPermission;

import java.io.File;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;

public class SplashActivity extends BaseActivity {

    private static final int ACTIVITY_MAIN = 100;

    private EditText mRoomIDET;
    private View mAdvanceSetting;
    private RadioButton mHostBT, mAuthorBT;
    private int mRole = CLIENT_ROLE_ANCHOR;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        AndPermission.with(this)
                .permission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
                .start();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == ACTIVITY_MAIN) {
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }
    }

    private boolean createSDK() {
        MainApplication application = (MainApplication) getApplicationContext();
        //1.设置SDK的回调接收类
        application.mMyTTTRtcEngineEventHandler = new MyTTTRtcEngineEventHandler(getApplicationContext());
        //2.创建SDK的实例对象
        mTTTEngine = TTTRtcEngine.create(getApplicationContext(), <这里填APPID引用>,
                false, application.mMyTTTRtcEngineEventHandler);
        if (mTTTEngine == null) {
            finish();
            return false;
        }
        //3.启用SDK视频功能
        mTTTEngine.enableVideo();
        if (!isApkDebugable()) {
            //4.开启日志记录
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File externalStorageDirectory = Environment.getExternalStorageDirectory();
                String abs = externalStorageDirectory.toString() + "/3T_Rtmplive_Log";
                mTTTEngine.setLogFile(abs);
            } else {
                PviewLog.i("Collection log failed! , No permission!");
            }
        }
        return true;
    }

    private void initView() {
        mAuthorBT = findViewById(R.id.vice);
        mHostBT = findViewById(R.id.host);
        mRoomIDET = findViewById(R.id.room_id);
        mAdvanceSetting = findViewById(R.id.set);
        View mSplashCompany = findViewById(R.id.company_info);
        View mSplashAppName = findViewById(R.id.splash_app_name);
        TextView mVersion = findViewById(R.id.version);
        String string = getResources().getString(R.string.version_info);
        String result = String.format(string, TTTRtcEngine.getInstance().getSdkVersion());
        mVersion.setText(result);
        TextView mLogoTextTV = findViewById(R.id.room_id_text);
        mLogoTextTV.setText(getString(R.string.ttt_prefix_live_channel_name) + ": ");
        if (LocalConfig.VERSION_FLAG == LocalConstans.VERSION_WHITE) {
            mVersion.setVisibility(View.INVISIBLE);
            mSplashAppName.setVisibility(View.INVISIBLE);
            mSplashCompany.setVisibility(View.INVISIBLE);
        }
    }

    private void initData() {
        // 读取保存的数据
        String roomID = (String) SharedPreferencesUtil.getParam(this, "RoomID", "");
        mRoomIDET.setText(roomID);
        mRoomIDET.setSelection(mRoomIDET.length());

        mDialog = new ProgressDialog(this);
        mDialog.setTitle("");
        mDialog.setCancelable(false);
        mDialog.setMessage(getString(R.string.ttt_hint_loading_channel));
    }

    private void init() {
        boolean sdkInit = createSDK();
        if (!sdkInit) {
            return;
        }

        initView();
        initData();
        mAdvanceSetting.setVisibility(View.GONE);
        mAuthorBT.setText(getString(R.string.ttt_role_audience));
    }

    public void onClickRoleButton(View v) {
        mHostBT.setChecked(false);
        mAuthorBT.setChecked(false);
        ((RadioButton) v).setChecked(true);
    }

    public void onClickEnterButton(View v) {
        String mRoomName = mRoomIDET.getText().toString().trim();
        if (TextUtils.isEmpty(mRoomName)) {
            Toast.makeText(this, getString(R.string.ttt_error_enterchannel_check_channel_empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mRoomName.length() >= 20) {
            Toast.makeText(this, getString(R.string.hint_channel_name_limit), Toast.LENGTH_SHORT).show();
            return;
        }

        // 角色检查
        if (mHostBT.isChecked()) {
            mRole = Constants.CLIENT_ROLE_ANCHOR;
        } else if (mAuthorBT.isChecked()) {
            mRole = Constants.CLIENT_ROLE_AUDIENCE;
        }
        mDialog.show();
        // 保存配置
        SharedPreferencesUtil.setParam(this, "RoomID", mRoomName);
        // 跳转到主界面进行推流/拉流
        Intent activityIntent = new Intent();
        activityIntent.putExtra("ROOM_ID", Long.parseLong(mRoomName));
        activityIntent.putExtra("USER_ID", LocalConfig.mLocalUserID);
        activityIntent.putExtra("ROLE", mRole);
        activityIntent.setClass(mContext, MainActivity.class);
        startActivityForResult(activityIntent, ACTIVITY_MAIN);
    }

    public boolean isApkDebugable() {
        try {
            ApplicationInfo info = this.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception ignored) {
        }
        return false;
    }
}
