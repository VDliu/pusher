package pictrue.com.reiniot.livepusher.push;

import android.text.TextUtils;

/**
 * 2019/8/19.
 */
public class PushVideo {
    static {
        System.loadLibrary("native-lib");
    }

    public void init(String url) {
        if (!TextUtils.isEmpty(url)) {
            initPush(url);
        }
    }

    private native void initPush(String pushUrl);
}
