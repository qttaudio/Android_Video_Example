package com.qtt_video.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.hjq.toast.ToastUtils;
import com.qtt_video.chat.R;
import com.qtt_video.chat.dialog.ResolutionDialog;
import com.qtt_video.chat.utils.Constants;
import com.qtt_video.chat.utils.TimeUtil;
import com.qttaudio.sdk.channel.AudioMode;
import com.qttaudio.sdk.channel.AudioQuality;
import com.qttaudio.sdk.channel.ChannelEngine;
import com.qttaudio.sdk.channel.ChannelObserver;
import com.qttaudio.sdk.channel.ChannelRole;
import com.qttaudio.sdk.channel.RtcStat;
import com.qttaudio.sdk.channel.VideoEncoderConfiguration;
import com.qttaudio.sdk.channel.VolumeInfo;

public class VideoChatActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = VideoChatActivity.class.getName();

    private AppCompatTextView tvOnlineTime;
    private RelativeLayout rlVideo;
    private FrameLayout flVideo;
    private AppCompatTextView tvUid;
    private AppCompatImageView ivCloseMic;
    private AppCompatTextView tvOtherUid;

    private Runnable timeTask;


    private long time;
    private long uid;
    private long remoteUid;
    private boolean remoteMuteVideo = true;
    private boolean remoteMuteAudio;

    private boolean isCloseMic;
    private boolean isDisableVideo;
    private boolean isCloseSound;

    private SurfaceView localView;
    private SurfaceView remoteView;
    private String roomName;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        tvOnlineTime = findViewById(R.id.tv_onlineTime);
        tvOtherUid = findViewById(R.id.tv_otherUid);
        ivCloseMic = findViewById(R.id.iv_closeMic);

        rlVideo = findViewById(R.id.rl_video);
        rlVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchView(localView);
                switchView(remoteView);
            }
        });

        tvUid = findViewById(R.id.tv_uid);
        flVideo = findViewById(R.id.fl_video);

        ivSwitchCamera.setOnClickListener(this);
        ivExit.setOnClickListener(this);
        ivMic.setOnClickListener(this);
        ivVideo.setOnClickListener(this);
        ivMusic.setOnClickListener(this);
        ivBeauty.setOnClickListener(this);
        ivMore.setOnClickListener(this);
        ivSound.setOnClickListener(this);

        setUp();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_1to1;
    }

    private void setUp() {
        timeTask = new Runnable() {
            @Override
            public void run() {
                time++;
                tvOnlineTime.setText(TimeUtil.secToTime(time));
                handler.postDelayed(timeTask, 1000);
            }
        };

        Intent intent = getIntent();
        roomName = intent.getStringExtra(Constants.ROOM_NAME);
        tvRoomName.setText(roomName);
        initEngineAndJoin(VideoEncoderConfiguration.VD_1280x720);

        initDialogs();
        resolutionDialog.setChangePixelListener(new ResolutionDialog.ChangePixelListener() {
            @Override
            public void pixel(int type) {
                VideoEncoderConfiguration.VideoDimensions videoDimensions = null;
                if (Constants.PIXEL_TYPE_1 == type) {
                    videoDimensions = VideoEncoderConfiguration.VD_640x360;
                } else if (Constants.PIXEL_TYPE_2 == type) {
                    videoDimensions = VideoEncoderConfiguration.VD_640x480;
                } else if (Constants.PIXEL_TYPE_3 == type) {
                    videoDimensions = VideoEncoderConfiguration.VD_1280x720;
                }
                resetVideo(videoDimensions);
            }
        });
    }

    private void initEngineAndJoin(VideoEncoderConfiguration.VideoDimensions videoDimensions) {
        if (uid == 0) {
            uid = getRandomUid();
        }
        channelEngine = ChannelEngine.GetChannelInstance(this, Constants.APP_KEY, channelObserver);
        channelEngine.changeRole(ChannelRole.TALKER);
        channelEngine.enableVideo();
        channelEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                videoDimensions, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                0, VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        ));
        channelEngine.setAudioConfig(AudioQuality.AUDIO_QUALITY_MUSIC_STEREO, AudioMode.AUDIO_MODE_MIX);
        channelEngine.setSpeakerOn(true);
        channelEngine.join("", roomName, uid,"");
    }

    private void resetVideo(VideoEncoderConfiguration.VideoDimensions videoDimensions) {
        channelEngine.disableVideo();
        channelEngine.enableVideo();
        channelEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                videoDimensions, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                0, VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        ));
    }


    private void loadLocalVideo() {
        localView = ChannelEngine.CreateRendererView(this);
        channelEngine.setupLocalVideo(localView, 1, 0);
        flVideo.addView(localView);
    }

    private void setRemoteVideo(long rUId) {
        remoteView = ChannelEngine.CreateRendererView(this);
        remoteView.setZOrderMediaOverlay(false);
        channelEngine.setupRemoteVideo(remoteView, rUId, 1, 0);
        if (localView.getParent() != null && localView.getParent() != rlVideo) {
            removeFromParent(localView);
            localView.setZOrderMediaOverlay(true);
            rlVideo.addView(localView, 0);
            tvUid.setText("ID???" + uid + "(???)");
            rlVideo.setVisibility(View.VISIBLE);
            ivCloseMic.setVisibility(isCloseMic ? View.VISIBLE : View.INVISIBLE);
        }
        tvOtherUid.setText("ID: " + rUId);

        flVideo.addView(remoteView);
        flVideo.setVisibility(View.VISIBLE);
    }


    private ViewGroup removeFromParent(SurfaceView surfaceView) {
        if (surfaceView != null) {
            ViewParent parent = surfaceView.getParent();
            if (parent != null) {
                ViewGroup viewGroup = (ViewGroup) parent;
                viewGroup.removeView(surfaceView);
                return viewGroup;
            }
        }
        return null;
    }

    private void switchView(SurfaceView surfaceView) {
        ViewGroup parent = removeFromParent(surfaceView);
        if (parent == null) {
            return;
        }
        if (parent == rlVideo) {
            surfaceView.setZOrderMediaOverlay(false);
            flVideo.addView(surfaceView);
            if (surfaceView == localView) {
                tvOtherUid.setText("ID???" + uid + "(???)");
            } else if (surfaceView == remoteView) {
                tvOtherUid.setText("ID???" + remoteUid);
            }
        } else if (parent == flVideo) {
            surfaceView.setZOrderMediaOverlay(true);
            rlVideo.addView(surfaceView, 0);
            if (surfaceView == localView) {
                tvUid.setText("ID???" + uid + "(???)");
                ivCloseMic.setVisibility(isCloseMic ? View.VISIBLE : View.INVISIBLE);
            } else if (surfaceView == remoteView) {
                tvUid.setText("ID???" + remoteUid);
                ivCloseMic.setVisibility(remoteMuteAudio ? View.VISIBLE : View.INVISIBLE);
                rlVideo.setVisibility(remoteMuteVideo ? View.INVISIBLE : View.VISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_switchCamera:
                channelEngine.switchCamera();
                break;
            case R.id.iv_exit:
                if (channelEngine != null) {
                    channelEngine.leave();
                }
                finish();
                break;
            case R.id.iv_mic:
                isCloseMic = !isCloseMic;
                channelEngine.muteLocalAudio(isCloseMic);
                ivMic.setImageResource(isCloseMic ? R.mipmap.close_mic_gray : R.mipmap.open_mic);
                ToastUtils.show(isCloseMic ? "??????????????????" : "??????????????????");
                if (localView.getParent() == rlVideo) {
                    ivCloseMic.setVisibility(isCloseMic ? View.VISIBLE : View.INVISIBLE);
                }
                break;
            case R.id.iv_sound:
                isCloseSound = !isCloseSound;
                channelEngine.muteAllRemoteAudio(isCloseSound);
                ivSound.setImageResource(isCloseSound ? R.mipmap.close_sound : R.mipmap.open_sound);
                ToastUtils.show(isCloseSound ? "???????????????" : "???????????????");
                break;
            case R.id.iv_video:
                isDisableVideo = !isDisableVideo;
                channelEngine.muteLocalVideoStream(isDisableVideo);
                resolutionDialog.setAllowChangePixel(!isDisableVideo);
                ivSwitchCamera.setVisibility(isDisableVideo ? View.INVISIBLE : View.VISIBLE);
                ivVideo.setImageResource(isDisableVideo ? R.mipmap.my_close_video : R.mipmap.open_video);
                ToastUtils.show(isDisableVideo ? "??????????????????" : "??????????????????");
                break;
            case R.id.iv_music:
                musicControlDialog.show();
                break;
            case R.id.iv_beauty:
                multiplyEffectDialog.show();
                break;
            case R.id.iv_more:
                resolutionDialog.show();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        musicControlDialog.release();
        if (channelEngine != null) {
            channelEngine.leave();
            ChannelEngine.Destroy();
        }
    }

    private int count;
    private long joinTime = 0L;

    private ChannelObserver channelObserver = new ChannelObserver() {
        @Override
        public void onJoinSuccess(String s, long l, ChannelRole channelRole, boolean b,String suid) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    joinTime = System.currentTimeMillis();
                    uid = l;
                    tvOtherUid.setText("ID:" + l + "(???)");
                    tvMySelfUid.setText("ID:" + l);
                    loadLocalVideo();
                    handler.postDelayed(timeTask, 1000);

                }
            });
        }

        @Override
        public void onReJoinSuccess(String s, long l, ChannelRole channelRole, boolean b,String suid) {

        }

        @Override
        public void onOtherJoin(long l, ChannelRole channelRole, boolean b,String suid) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - joinTime <= 200) {
                        count++;
                        if (count == 2) {
                            finish();
                            ToastUtils.show("???????????????????????????????????????????????????");
                            return;
                        }
                    }
                    if (remoteUid != 0) {
                        return;
                    }
                    remoteUid = l;
                    remoteMuteVideo = false;
                    remoteMuteAudio = b;
                    tvOtherUid.setText("ID???" + remoteUid);
                    setRemoteVideo(l);
                }
            });
        }

        @Override
        public void onJoinFail(int i, String s) {

        }

        @Override
        public void onConnectionBreak() {

        }

        @Override
        public void onConnectionLost() {

        }

        @Override
        public void onError(int i, String s) {

        }

        @Override
        public void onWarning(int i, String s) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (i == 1002 || i == 1001) {
                        ToastUtils.show("?????????????????????????????????????????????");
                    }
                }
            });
        }

        @Override
        public void onLeave() {

        }

        @Override
        public void onOtherLeave(long l, ChannelRole channelRole,String suid) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (remoteUid == l) {
                        remoteUid = 0;
                        remoteMuteVideo = true;
                        if (remoteView != null) {
                            ViewGroup parent = (ViewGroup) remoteView.getParent();
                            if (parent != null && parent == flVideo) {
                                switchView(localView);
                                switchView(remoteView);
                            }
                        }
                        removeFromParent(remoteView);
                        ToastUtils.show("??????" + l + "?????????");
                    }
                }
            });
        }

        @Override
        public void onTalkingVolumeIndication(VolumeInfo[] volumeInfos, int i) {

        }

        @Override
        public void onMuteStatusChanged(long l, boolean b,String suid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (l == remoteUid && remoteUid != 0) {
                        remoteMuteAudio = b;
                        if (remoteView.getParent() == rlVideo) {
                            ivCloseMic.setVisibility(remoteMuteAudio ? View.VISIBLE : View.INVISIBLE);
                        }
                    }
                }
            });
        }

        @Override
        public void onRoleStatusChanged(long l, ChannelRole channelRole,String suid) {

        }

        @Override
        public void onNetworkStats(long l, int txQuality, int rxQuality, RtcStat rtcStat,String suid) {

        }

        @Override
        public void onAudioRouteChanged(int i) {

        }

        @Override
        public void onSoundStateChanged(int i) {
            switch (i) {
                case 0://??????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            musicControlDialog.resetMusicProgress(channelEngine.getSoundMixingDuration());
                            musicControlDialog.updateMusicProgress();
                        }
                    });
                    break;
                case 2://??????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            musicControlDialog.stopUpdateMusicProgress();
                            musicControlDialog.updateMusicProgress(0);
                        }
                    });
                    break;
                case 1://??????
                case 3://????????????
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            musicControlDialog.stopUpdateMusicProgress();
                        }
                    });
                    break;
            }
        }

        @Override
        public void onEffectFinished(int i) {

        }

        @Override
        public void onUserEnableVideo(long l, boolean b,String suid) {

        }

        @Override
        public void onUserEnableLocalVideo(long l, boolean b,String suid) {

        }

        @Override
        public void onUserMuteVideo(long l, boolean b,String suid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (l == remoteUid) {
                        remoteMuteVideo = b;
                        setRemoteVideo(l);
                    }
                }
            });
        }

        @Override
        public void onFirstLocalVideoFrame(int i, int i1, int i2, String s) {

        }

        @Override
        public void onFirstLocalVideoFramePublished(int i, String s) {

        }

        @Override
        public void onFirstRemoteVideoDecoded(long l, int i, int i1, int i2, String s) {

        }

        @Override
        public void onFirstRemoteVideoFrame(long l, int i, int i1, int i2, String s) {

        }

        @Override
        public void onChannelRelayStateChanged(int i, int i1) {

        }
    };
}
