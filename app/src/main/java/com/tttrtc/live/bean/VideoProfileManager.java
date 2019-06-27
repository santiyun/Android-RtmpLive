package com.tttrtc.live.bean;

import com.wushuangtech.library.Constants;

import java.util.ArrayList;

public class VideoProfileManager {

    public ArrayList<VideoProfile> mVideoProfiles = new ArrayList<>();

    public VideoProfileManager() {
        mVideoProfiles.add(new VideoProfile("超低质量", Constants.TTTRTC_VIDEOPROFILE_120P, 160, 120, 65, 15));
        mVideoProfiles.add(new VideoProfile("低质量", Constants.TTTRTC_VIDEOPROFILE_240P, 320, 240, 200, 15));
        mVideoProfiles.add(new VideoProfile("高质量", Constants.TTTRTC_VIDEOPROFILE_360P, 640, 360, 400, 15));
        mVideoProfiles.add(new VideoProfile("超高质量", Constants.TTTRTC_VIDEOPROFILE_480P, 640, 480, 500, 15));
        mVideoProfiles.add(new VideoProfile("特高质量", Constants.TTTRTC_VIDEOPROFILE_720P, 1280, 720, 1130, 15));
    }

    public VideoProfile getVideoProfile(String name) {
        for (VideoProfile videoProfile: mVideoProfiles) {
            if (videoProfile.name.equals(name))
                return videoProfile;
        }
        return null;
    }

    public VideoProfile getVideoProfile(int profile) {
        for (VideoProfile videoProfile: mVideoProfiles) {
            if (videoProfile.videoProfile == profile)
                return videoProfile;
        }
        return null;
    }

    public class VideoProfile {

        public String name;
        public int videoProfile;
        public int width, height;
        public int fRate, bRate;

        public VideoProfile(String name, int videoProfile, int width, int height, int bRate, int fRate) {
            this.name = name;
            this.videoProfile = videoProfile;
            this.width = width;
            this.height = height;
            this.fRate = fRate;
            this.bRate = bRate;
        }
    }

}
