package pictrue.com.reiniot.livepusher.push;

import android.text.TextUtils;

/**
 * 2019/8/19.
 */
public class PushVideo {
    private ConnectListenr connectListenr;

    static {
        System.loadLibrary("native-lib");
    }

    public void setConnectListenr(ConnectListenr wlConnectListenr) {
        this.connectListenr = wlConnectListenr;
    }


    private void onConnecting() {
        if (connectListenr != null) {
            connectListenr.onConnecting();
        }
    }

    private void onConnectSuccess() {
        if (connectListenr != null) {
            connectListenr.onConnectSuccess();
        }
    }

    private void onConnectFail(String msg) {
        if (connectListenr != null) {
            connectListenr.onConnectFail(msg);
        }
    }


    public void init(String url) {
        if (!TextUtils.isEmpty(url)) {
            initPush(url);
        }
    }

    public void pushSpsPPsData(byte[] sps, byte[] pps) {
        if (sps != null && pps != null) {
            pushSpsPps(sps, sps.length, pps, pps.length);
        }
    }

    public void pushVideoData(byte[] data, int len, boolean isKeyFrame) {
        if (data != null) {
            pushVideo(data, len, isKeyFrame);
        }
    }

    private native void initPush(String pushUrl);

    private native void pushSpsPps(byte[] sps, int spslen, byte[] pps, int ppsLen);

    private native void pushVideo(byte[] avc, int len, boolean isKeyFame);
}
