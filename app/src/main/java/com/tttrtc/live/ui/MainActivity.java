package com.tttrtc.live.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtc.live.Helper.WindowManager;
import com.tttrtc.live.LocalConfig;
import com.tttrtc.live.LocalConstans;
import com.tttrtc.live.R;
import com.tttrtc.live.bean.EnterUserInfo;
import com.tttrtc.live.bean.JniObjs;
import com.tttrtc.live.callback.MyTTTRtcEngineEventHandler;
import com.tttrtc.live.callback.PhoneListener;
import com.tttrtc.live.dialog.ExitRoomDialog;
import com.tttrtc.live.utils.MyLog;
import com.wushuangtech.library.Constants;
import com.wushuangtech.utils.PviewLog;
import com.wushuangtech.wstechapi.model.PublisherConfiguration;
import com.wushuangtech.wstechapi.model.VideoCanvas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ttt.ijk.media.exo.widget.media.IRenderView;
import ttt.ijk.media.exo.widget.media.IjkVideoView;
import ttt.ijk.media.player.IMediaPlayer;

import static com.wushuangtech.library.Constants.CLIENT_ROLE_ANCHOR;

public class MainActivity extends BaseActivity {

    private long mUserId;
    private long mRoomID;
    private int mRole = CLIENT_ROLE_ANCHOR;
    private long mAnchorId = -1;

    private TextView mAudioSpeedShow, mVideoSpeedShow, mFpsSpeedShow;
    private ImageView mShangMaiTV;
    private ViewGroup mLocalShowLayoutOne, mLocalShowLayoutTwo;
    private ViewGroup mAVInfosLy;
    private ViewGroup mRemoteVideoLy;
    private SurfaceView mAnchorView;

    private ExitRoomDialog mExitRoomDialog;
    private AlertDialog mErrorExitDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private ProgressDialog mDialog;
    private boolean mIsPhoneComing;
    private boolean mIsSpeaker, mIsBackCamera, mIsJoinRoom, mIsShowSecond;
    private boolean mIsFirst;

    private WindowManager mWindowManager;
    private TelephonyManager mTelephonyManager;
    private PhoneListener mPhoneListener;
    private final Object obj = new Object();
    private Map<Long, Boolean> mUserMutes = new HashMap<>();

    public static int mCurrentAudioRoute;
    private int mLevCount;
    private IjkVideoView mIjkVideoView;
    private boolean mIsRtmpPushMode = true;
    private long mLastClickTime;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEngine();
        initDialog();
        initData();
        mTelephonyManager = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        mPhoneListener = new PhoneListener(this);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        if (mPhoneListener != null && mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            mPhoneListener = null;
            mTelephonyManager = null;
        }

        try{
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e){
            e.printStackTrace();
        }
        // 摄像头切换接口是全局的，所以退房间需要复位
        if (mIsBackCamera) {
            mTTTEngine.switchCamera();
        }
        stopRtmpPublish();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mLocalShowLayoutOne.removeAllViews();
        mLocalShowLayoutTwo.removeAllViews();
        super.onDestroy();
        MyLog.d("MainActivity onDestroy... ");
    }

    private void initView() {
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFpsSpeedShow = findViewById(R.id.main_btn_fpsup);
        mLocalShowLayoutOne = findViewById(R.id.local_view_layout_one);
        mLocalShowLayoutTwo = findViewById(R.id.local_view_layout_two);
        mAVInfosLy = findViewById(R.id.main_area_avinfos);
        mRemoteVideoLy = findViewById(R.id.main_video_ly);
        mShangMaiTV = findViewById(R.id.main_btn_shangmai);

        Intent intent = getIntent();
        mRoomID = intent.getLongExtra("ROOM_ID", 0);
        mUserId = intent.getLongExtra("USER_ID", 0);
        mRole = intent.getIntExtra("ROLE", CLIENT_ROLE_ANCHOR);
        String localChannelName = getString(R.string.ttt_prefix_channel_name) + ":" + mRoomID;
        ((TextView) findViewById(R.id.main_btn_title)).setText(localChannelName);

        findViewById(R.id.main_btn_exit).setOnClickListener((v) -> mExitRoomDialog.show());

        if (mRole != CLIENT_ROLE_ANCHOR)
            findViewById(R.id.main_btn_switch_camera).setVisibility(View.GONE);

        // 摄像头切换控件的点击事件
        findViewById(R.id.main_btn_switch_camera).setOnClickListener(v -> {
            // 摄像头前后置切换接口
            mTTTEngine.switchCamera();
            mIsBackCamera = !mIsBackCamera;
        });

        // 上麦控件的点击事件
        mShangMaiTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 不能非常频繁的上/下麦，会有问题
                long curTime = System.currentTimeMillis();
                if (mLastClickTime != 0 && (curTime - mLastClickTime < 1500)) {
                    Toast.makeText(mContext, "切换太快！", Toast.LENGTH_SHORT).show();
                    return;
                }
                mLastClickTime = curTime;

                // 默认进房间是直推
                if (!mIsJoinRoom) {
                    // 开始上麦流程
                    // 1.设置频道模式，这里用直播模式
                    mTTTEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                    // 2.设置角色
                    if (CLIENT_ROLE_ANCHOR == mRole) {
                        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_ANCHOR);
                    } else {
                        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                    }
                    // 3.设置推流地址
                    PublisherConfiguration mPublisherConfiguration = new PublisherConfiguration();
                    mPublisherConfiguration.setPushUrl(getNewPushUrl());
                    mTTTEngine.configPublisher(mPublisherConfiguration);
                    // 4.设置视频质量
                    mTTTEngine.setVideoProfile(360, 640, 15, 800);
                    // 5.调用进房间接口
                    mTTTEngine.joinChannel("", String.valueOf(mRoomID), mUserId);
                    mIsRtmpPushMode = false;
                    showProgressDialog(getString(R.string.ttt_enter_channel));
                } else {
                    // 开始下麦流程
                    // 如果是主播下麦
                    if (Constants.CLIENT_ROLE_ANCHOR == mRole) {
                        // 先开启RTMP直推
                        startRtmpPublish();
                        showProgressDialog("正在推流中...");
                    } else { // 如果是副播下麦变观众
                        mIjkVideoView = mTTTEngine.CreateIjkRendererView(mContext);
                        mIjkVideoView.start();
                        ViewGroup vp = (ViewGroup) mIjkVideoView.getParent();
                        if (vp != null) {
                            vp.removeView(mIjkVideoView);
                        }
                        mTTTEngine.startIjkPlayer(getPullrl(), true);
                    }
                }
            }
        });
    }

    // 开始RTMP直推
    private void startRtmpPublish() {
        mIsRtmpPushMode = true;
        mTTTEngine.startRtmpPublish(getNewPushUrl());
    }

    // 结束RTMP直推
    private void stopRtmpPublish() {
        mTTTEngine.stopRtmpPublish();
    }

    private void initEngine() {
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
    }

    private void initDialog() {
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(v -> {
            exitRoom();
            mExitRoomDialog.dismiss();
        });
        mExitRoomDialog.mDenyBT.setOnClickListener(v -> mExitRoomDialog.dismiss());


        //添加确定按钮
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.ttt_error_exit_dialog_title))//设置对话框标题
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ttt_confirm), (dialog, which) -> {//确定按钮的响应事件
                    exitRoom();
                });
        mErrorExitDialog = builder.create();

        // 创建dialog
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(false);
        mDialog.setMessage(getString(R.string.ttt_hint_loading_channel));
    }

    private void initData() {
        mWindowManager = new WindowManager(this);
        String localUserName = getString(R.string.ttt_prefix_user_name) + ":" + mUserId;
        ((TextView) findViewById(R.id.main_btn_host)).setText(localUserName);

        mAVInfosLy.setVisibility(View.INVISIBLE);
        mRemoteVideoLy.setVisibility(View.INVISIBLE);
        mShangMaiTV.setImageResource(R.drawable.shangmai);
        if (mRole == CLIENT_ROLE_ANCHOR) { // 如果角色是主播，进行RTMP推流
            SurfaceView mSurfaceView = mTTTEngine.CreateRendererView(this);
            mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, mSurfaceView), getRequestedOrientation());
            mLocalShowLayoutOne.addView(mSurfaceView, 0);
            mTTTEngine.startPreview();
            startRtmpPublish();
            showProgressDialog("正在推流中...");
        } else { // 如果角色是观众，进行RTMP拉流
            mIjkVideoView = mTTTEngine.CreateIjkRendererView(this);
            // 设置IJK拉流画面的显示模式。
            mIjkVideoView.setAspectRatio(IRenderView.AR_MATCH_PARENT);
            // 监听IJK拉流中，出现的错误。
            mIjkVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                    showErrorExitDialog("拉流出现错误, type-" + what);
                    return false;
                }
            });
            // 监听是否成功拉到流
            mIjkVideoView.setOnRenderingStart(new IjkVideoView.OnRenderingStart() {
                @Override
                public void onRendering() {
                    mDialog.dismiss();
                    if (mIsFirst) {
                        mIsFirst = false;
                        return;
                    }
                    // 添加ijkVideoView控件
                    if(mIsShowSecond){
                        addView(mLocalShowLayoutOne, mIjkVideoView);
                        mLocalShowLayoutTwo.removeAllViews();
                        mIsShowSecond = false;
                    } else {
                        addView( mLocalShowLayoutTwo, mIjkVideoView);
                        mLocalShowLayoutOne.removeAllViews();
                        mIsShowSecond = true;
                    }
                    // 切换布局
                    changeAnchorLayout(false);
                    // 调用退房间接口
                    mTTTEngine.leaveChannel();
                    Toast.makeText(mContext, "拉流成功", Toast.LENGTH_SHORT).show();
                }
            });
            mTTTEngine.startIjkPlayer(getPullrl(), true);
            addView(mLocalShowLayoutOne, mIjkVideoView);
            showProgressDialog("正在拉流中...");
            mIsFirst = true;
        }
    }

    private void changeAnchorLayout(boolean isInRoom) {
        if (isInRoom) {
            mAVInfosLy.setVisibility(View.VISIBLE);
            mRemoteVideoLy.setVisibility(View.VISIBLE);
            mShangMaiTV.setImageResource(R.drawable.shangmaizhong);
            mIsJoinRoom = true;
            if (Constants.CLIENT_ROLE_ANCHOR == mRole) {
                stopRtmpPublish();
            }
        } else {
            mAVInfosLy.setVisibility(View.INVISIBLE);
            mRemoteVideoLy.setVisibility(View.INVISIBLE);
            mShangMaiTV.setImageResource(R.drawable.shangmai);
            mIsJoinRoom = false;
            mWindowManager.removeAll();
        }
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    public void exitRoom() {
        MyLog.d("exitRoom was called!... mIsJoinRoom : " + mIsJoinRoom);
        if (mRole == CLIENT_ROLE_ANCHOR) {
            if (mIsJoinRoom) {
                mTTTEngine.leaveChannel();
            } else {
                stopRtmpPublish();
            }
        } else {
            mTTTEngine.leaveChannel();
            mTTTEngine.stopIjkPlayer();
            if (mIjkVideoView != null) {
                mIjkVideoView.release(true);
                mIjkVideoView = null;
            }
        }
        setResult(SplashActivity.ACTIVITY_MAIN);
        finish();
    }

    public String getNewPushUrl() {
        // 如果是主播角色，每次上麦和下麦，推流地址末尾的数字都要加1，数字大的流会自动替换数字小的流，实现无缝切换。
        mLevCount++;
        String pushUrl = LocalConfig.mPushPreifx + mRoomID + "?lev=" + mLevCount;
        PviewLog.d("wzg", "RTMP_COVER getNewPushUrl : " + pushUrl);
        return pushUrl;
    }

    public String getPullrl() {
        return LocalConfig.mPullPreifx + mRoomID + "?lev=" + mLevCount;
    }

    public void showErrorExitDialog(String message) {
        if (mErrorExitDialog != null && mErrorExitDialog.isShowing()) {
            return;
        }

        if (!TextUtils.isEmpty(message)) {
            String msg = getString(R.string.ttt_error_exit_dialog_prefix_msg) + ": " + message;
            mErrorExitDialog.setMessage(msg);//设置显示的内容
            mErrorExitDialog.show();
        }
    }

    /**
     * 显示进度对话框
     *
     * @param hintText 提示文字
     */
    private void showProgressDialog(String hintText) {
        mDialog.setMessage(hintText);
        mDialog.show();
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                MyLog.d("UI onReceive callBack... mJniType : " + mJniObjs.mJniType);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_RTMP_PUSH_STATE:
                        mDialog.dismiss();
                        String toastmsg = "";
                        if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_INITERROR) {
                            toastmsg = "推流初始化失败";
                            showErrorExitDialog(toastmsg);
                            stopRtmpPublish();
                        } else if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_OPENERROR) {
                            toastmsg = "推流启动失败";
                            showErrorExitDialog(toastmsg);
                            stopRtmpPublish();
                        } else if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_LINKFAILED) {
                            toastmsg = "推流发送失败";
                            showErrorExitDialog(toastmsg);
                            stopRtmpPublish();
                        } else if (mJniObjs.mErrorType == Constants.RTMP_PUSH_STATE_LINKSUCCESSED) {
                            if (mIsRtmpPushMode) {
                                Toast.makeText(mContext, "推流成功", Toast.LENGTH_SHORT).show();
                                if (mIsJoinRoom) {
                                    mTTTEngine.leaveChannel();
                                }
                                changeAnchorLayout(false);
                            }
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM:
                        if (Constants.CLIENT_ROLE_AUDIENCE == mRole) {
                            changeAnchorLayout(true);
                            Toast.makeText(mContext, "进入房间成功", Toast.LENGTH_SHORT).show();
                            mDialog.dismiss();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ROOM_PUSH_STATE:
                        mDialog.dismiss();
                        if (mJniObjs.mRoomPushStatus) {
                            Toast.makeText(mContext, "进入房间推流成功", Toast.LENGTH_SHORT).show();
                            changeAnchorLayout(true);
                        } else {
                            mTTTEngine.leaveChannel();
                            Toast.makeText(mContext, "进入房间推流失败", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR:
                        mDialog.dismiss();
                        int mJoinRoomResult = mJniObjs.mErrorType;
                        if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_format), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_TIMEOUT) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_timeout), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_token_invaild), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_BAD_VERSION) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_version), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_CONNECT_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unconnect), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_NOEXIST) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_room_no_exist), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_SERVER_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_verification_failed), Toast.LENGTH_SHORT).show();
                        } else if (mJoinRoomResult == Constants.ERROR_ENTER_ROOM_UNKNOW) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unknow), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_KICK:
                        String message = "";
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST:
                        showErrorExitDialog(getString(R.string.ttt_error_network_disconnected));
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN:
                        long uid = mJniObjs.mUid;
                        int identity = mJniObjs.mIdentity;
                        if (identity == CLIENT_ROLE_ANCHOR) {
                            mAnchorId = uid;
                            String localAnchorName = getString(R.string.ttt_role_anchor) + "ID: " + mAnchorId;
                            ((TextView) findViewById(R.id.main_btn_host)).setText(localAnchorName);
                        }
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            EnterUserInfo userInfo = new EnterUserInfo(uid, identity);
                            mWindowManager.addAndSendSei(mUserId, userInfo);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_OFFLINE:
                        long offLineUserID = mJniObjs.mUid;
                        mWindowManager.removeAndSendSei(mUserId, offLineUserID);
                        break;
                    case LocalConstans.CALL_BACK_ON_SEI:
                        TreeSet<EnterUserInfo> mInfos = new TreeSet<>();
                        try {
                            JSONObject jsonObject = new JSONObject(mJniObjs.mSEI);
                            JSONArray jsonArray = jsonObject.getJSONArray("pos");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonobject2 = (JSONObject) jsonArray.get(i);
                                String devid = jsonobject2.getString("id");
                                float x = Float.valueOf(jsonobject2.getString("x"));
                                float y = Float.valueOf(jsonobject2.getString("y"));
                                float w = Float.valueOf(jsonobject2.getString("w"));
                                float h = Float.valueOf(jsonobject2.getString("h"));

                                long userId;
                                int index = devid.indexOf(":");
                                if (index > 0) {
                                    userId = Long.parseLong(devid.substring(0, index));
                                } else {
                                    userId = Long.parseLong(devid);
                                }
                                MyLog.d("CALL_BACK_ON_SEI", "parse user id : " + userId);
                                if (userId != mAnchorId) {
                                    EnterUserInfo temp = new EnterUserInfo(userId, Constants.CLIENT_ROLE_BROADCASTER);
                                    temp.setXYLocation(x, y);
                                    mInfos.add(temp);
                                } else {
                                    mAnchorView = mTTTEngine.CreateRendererView(MainActivity.this);
                                    mTTTEngine.setupRemoteVideo(new VideoCanvas(userId, Constants.RENDER_MODE_HIDDEN, mAnchorView));
                                }

                            }
                        } catch (JSONException e) {
                            MyLog.d("CALL_BACK_ON_SEI", "parse xml error : " + e.getLocalizedMessage());
                        }

                        int count = 0;
                        for (EnterUserInfo temp : mInfos) {
                            temp.mShowIndex = count;
                            count++;
                        }

                        for (EnterUserInfo next : mInfos) {
                            MyLog.d("CALL_BACK_ON_SEI", "user list : " + next.getId() + " | index : " + next.mShowIndex);
                            mWindowManager.add(mUserId, next.getId(), getRequestedOrientation(), next.mShowIndex);
                        }

                        synchronized (obj) {
                            if (mUserMutes.size() > 0) {
                                Set<Map.Entry<Long, Boolean>> entries = mUserMutes.entrySet();
                                for (Map.Entry<Long, Boolean> next : entries) {
                                    mWindowManager.muteAudio(next.getKey(), next.getValue());
                                }
                            }
                            mUserMutes.clear();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE:
                        if (mJniObjs.mRemoteAudioStats.getUid() != mAnchorId) {
                            String audioString = getResources().getString(R.string.ttt_audio_downspeed);
                            String audioResult = String.format(audioString, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                            mWindowManager.updateAudioBitrate(mJniObjs.mRemoteAudioStats.getUid(), audioResult);
                        } else
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_downspeed, String.valueOf(mJniObjs.mRemoteAudioStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE:
                        if (mJniObjs.mRemoteVideoStats.getUid() != mAnchorId) {
                            String videoString = getResources().getString(R.string.ttt_video_downspeed);
                            String videoResult = String.format(videoString, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                            mWindowManager.updateVideoBitrate(mJniObjs.mRemoteVideoStats.getUid(), videoResult);
                        } else
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_downspeed, String.valueOf(mJniObjs.mRemoteVideoStats.getReceivedBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR)
                            setTextViewContent(mAudioSpeedShow, R.string.ttt_audio_upspeed, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        else {
                            String localAudioString = getResources().getString(R.string.ttt_audio_upspeed);
                            String localAudioResult = String.format(localAudioString, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                            mWindowManager.updateAudioBitrate(mUserId, localAudioResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE:
                        if (mRole == CLIENT_ROLE_ANCHOR) {
                            mFpsSpeedShow.setText("FPS-" + mJniObjs.mLocalVideoStats.getSentFrameRate());
                            setTextViewContent(mVideoSpeedShow, R.string.ttt_video_upspeed, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        } else {
                            String localVideoString = getResources().getString(R.string.ttt_video_upspeed);
                            String localVideoResult = String.format(localVideoString, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                            mWindowManager.updateVideoBitrate(mUserId, localVideoResult);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_COME:
                        mIsPhoneComing = true;
                        mIsSpeaker = mTTTEngine.isSpeakerphoneEnabled();
                        if (mIsSpeaker) {
                            mTTTEngine.setEnableSpeakerphone(false);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_PHONE_LISTENER_IDLE:

                        if (mIsPhoneComing) {
                            if (mIsSpeaker) {
                                mTTTEngine.setEnableSpeakerphone(true);
                            }
                            mIsPhoneComing = false;
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOVE_FIRST_DECODED:
                        long remote_uid = mJniObjs.mUid;
                        if (remote_uid == mAnchorId) {
                            if(mIsShowSecond){
                                addView(mLocalShowLayoutOne, mAnchorView);
                                mLocalShowLayoutTwo.removeAllViews();
                                mIsShowSecond = false;
                            } else {
                                addView(mLocalShowLayoutTwo, mAnchorView);
                                mLocalShowLayoutOne.removeAllViews();
                                mIsShowSecond = true;
                            }

                            mTTTEngine.stopIjkPlayer();
                            if (mIjkVideoView != null) {
                                mIjkVideoView.release(true);
                                mIjkVideoView = null;
                            }
                        }
                        break;
                }
            }
        }
    }

    private void addView(ViewGroup vp, View view){
        if (view == null) {
            return;
        }

        ViewGroup temp = (ViewGroup) view.getParent();
        if (temp != null) {
            temp.removeView(view);
        }
        vp.removeAllViews();
        vp.addView(view, 0);
    }
}
