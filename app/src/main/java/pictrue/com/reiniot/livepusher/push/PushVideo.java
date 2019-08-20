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

    public void setWlConnectListenr(ConnectListenr wlConnectListenr) {
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

    private native void initPush(String pushUrl);
}
