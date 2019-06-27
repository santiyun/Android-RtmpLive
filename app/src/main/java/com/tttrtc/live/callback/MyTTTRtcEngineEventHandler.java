package com.tttrtc.live.callback;

import android.content.Context;
import android.content.Intent;

import com.tttrtc.live.LocalConstans;
import com.tttrtc.live.bean.JniObjs;
import com.tttrtc.live.ui.MainActivity;
import com.tttrtc.live.utils.MyLog;
import com.wushuangtech.expansion.bean.LocalAudioStats;
import com.wushuangtech.expansion.bean.LocalVideoStats;
import com.wushuangtech.expansion.bean.RemoteAudioStats;
import com.wushuangtech.expansion.bean.RemoteVideoStats;
import com.wushuangtech.expansion.bean.RtcStats;
import com.wushuangtech.wstechapi.TTTRtcEngineEventHandler;

import java.util.ArrayList;
import java.util.List;

import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_AUDIO_ROUTE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_CONNECTLOST;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_ENTER_ROOM;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_ERROR;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_MUTE_AUDIO;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_REMOVE_FIRST_FRAME_COME;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_ROOM_PUSH_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_RTMP_PUSH_STATE;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_SEI;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_SPEAK_MUTE_AUDIO;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_USER_JOIN;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_USER_MUTE_VIDEO;
import static com.tttrtc.live.LocalConstans.CALL_BACK_ON_USER_OFFLINE;


/**
 * Created by wangzhiguo on 17/10/24.
 */

public class MyTTTRtcEngineEventHandler extends TTTRtcEngineEventHandler {

    public static final String TAG = "MyTTTRtcEngineEventHandlerMMLIVE";
    public static final String MSG_TAG = "MyTTTRtcEngineEventHandlerMSGMMLIVE";
    private boolean mIsSaveCallBack;
    private List<JniObjs> mSaveCallBack;
    private Context mContext;

    public MyTTTRtcEngineEventHandler(Context mContext) {
        this.mContext = mContext;
        mSaveCallBack = new ArrayList<>();
    }

    @Override
    public void onJoinChannelSuccess(String channel, long uid) {
        MyLog.i("wzg", "RTMP_COVER onJoinChannelSuccess.... channel ： " + channel + " | uid : " + uid);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ENTER_ROOM;
        mJniObjs.mChannelName = channel;
        mJniObjs.mUid = uid;
        sendMessage(mJniObjs);
    }

    @Override
    public void onStatusOfRtmpPublish(int status) {
        MyLog.d("wzg", "RTMP_COVER onStatusOfRtmpPublish status : " + status);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_RTMP_PUSH_STATE;
        mJniObjs.mErrorType = status;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRtcPushStatus(String url, boolean status) {
        MyLog.d("wzg", "RTMP_COVER onRtcPushStatus status : " + status);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ROOM_PUSH_STATE;
        mJniObjs.mRoomPushStatus = status;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onError(final int errorType) {
        MyLog.i("wzg", "RTMP_COVER onError.... errorType ： " + errorType + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_ERROR;
        mJniObjs.mErrorType = errorType;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
        MyLog.i("wzg", "RTMP_COVER onLeaveChannel...");
    }

    @Override
    public void onUserKicked(long uid, int reason) {
        MyLog.i("wzg", "onUserKicked.... uid ： " + uid + "reason : " + reason + "mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_KICK;
        mJniObjs.mErrorType = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }


    @Override
    public void onUserJoined(long nUserId, int identity) {
        MyLog.i("wzg", "onUserJoined.... nUserId ： " + nUserId + " | identity : " + identity
                + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_JOIN;
        mJniObjs.mUid = nUserId;
        mJniObjs.mIdentity = identity;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserOffline(long nUserId, int reason) {
        MyLog.i("wzg", "onUserOffline.... nUserId ： " + nUserId + " | reason : " + reason);

        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_OFFLINE;
        mJniObjs.mUid = nUserId;
        mJniObjs.mReason = reason;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onReconnectServerFailed() {
        MyLog.i("wzg", "onReconnectServerFailed.... ");
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_CONNECTLOST;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserEnableVideo(long uid, boolean muted) {
        MyLog.i("wzg", "onUserEnableVideo.... uid : " + uid + " | mute : " + muted);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_USER_MUTE_VIDEO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsEnableVideo = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onFirstRemoteVideoFrame(long uid, int width, int height) {
        MyLog.i("wzg", "onFirstRemoteVideoFrame.... uid ： " + uid + " | width : " + width + " | height : " + height);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOVE_FIRST_FRAME_COME;
        mJniObjs.mUid = uid;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteVideoStats(RemoteVideoStats stats) {
//        MyLog.i("wzg", "onRemoteVideoStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate()
//                + " | framerate : " + stats.getReceivedFrameRate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_VIDEO_STATE;
        mJniObjs.mRemoteVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onRemoteAudioStats(RemoteAudioStats stats) {
//        MyLog.i("wzg", "RemoteAudioStats.... uid : " + stats.getUid() + " | bitrate : " + stats.getReceivedBitrate());
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_REMOTE_AUDIO_STATE;
        mJniObjs.mRemoteAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalVideoStats(LocalVideoStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_VIDEO_STATE;
        mJniObjs.mLocalVideoStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onLocalAudioStats(LocalAudioStats stats) {
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_LOCAL_AUDIO_STATE;
        mJniObjs.mLocalAudioStats = stats;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSetSEI(String sei) {
        MyLog.i("wzg", "onSei.... sei : " + sei);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SEI;
        mJniObjs.mSEI = sei;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onUserMuteAudio(long uid, boolean muted) {
        MyLog.i("wzg", "OnRemoteAudioMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onSpeakingMuted(long uid, boolean muted) {
        MyLog.i("wzg", "onSpeakingMuted.... uid : " + uid + " | muted : " + muted + " | mIsSaveCallBack : " + mIsSaveCallBack);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_SPEAK_MUTE_AUDIO;
        mJniObjs.mUid = uid;
        mJniObjs.mIsDisableAudio = muted;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onAudioRouteChanged(int routing) {
        MyLog.i("wzg", "onAudioRouteChanged.... routing : " + routing);
        MainActivity.mCurrentAudioRoute = routing;
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = CALL_BACK_ON_AUDIO_ROUTE;
        mJniObjs.mAudioRoute = routing;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    @Override
    public void onClientRoleChanged(long uid, int userRole) {
        super.onClientRoleChanged(uid, userRole);
        MyLog.i("wzg", "onUserRoleChanged... userID : " + uid + " userRole : " + userRole);
        JniObjs mJniObjs = new JniObjs();
        mJniObjs.mJniType = LocalConstans.CALL_BACK_ON_USER_ROLE_CHANGED;
        mJniObjs.mUid = uid;
        mJniObjs.mIdentity = userRole;
        if (mIsSaveCallBack) {
            saveCallBack(mJniObjs);
        } else {
            sendMessage(mJniObjs);
        }
    }

    private void sendMessage(JniObjs mJniObjs) {
        Intent i = new Intent();
        i.setAction(TAG);
        i.putExtra(MSG_TAG, mJniObjs);
        i.setExtrasClassLoader(JniObjs.class.getClassLoader());
        mContext.sendBroadcast(i);
    }

    public void setIsSaveCallBack(boolean mIsSaveCallBack) {
        this.mIsSaveCallBack = mIsSaveCallBack;
        if (!mIsSaveCallBack) {
            for (int i = 0; i < mSaveCallBack.size(); i++) {
                sendMessage(mSaveCallBack.get(i));
            }
            mSaveCallBack.clear();
        }
    }

    private void saveCallBack(JniObjs mJniObjs) {
        if (mIsSaveCallBack) {
            mSaveCallBack.add(mJniObjs);
        }
    }
}
