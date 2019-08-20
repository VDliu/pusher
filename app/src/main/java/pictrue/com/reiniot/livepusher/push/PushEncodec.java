package pictrue.com.reiniot.livepusher.push;

import android.content.Context;

import pictrue.com.reiniot.livepusher.encodec.BaseMediaEncoder;
import pictrue.com.reiniot.livepusher.encodec.EncodecRender;

/**
 * 2019/8/15.
 */
public class PushEncodec extends BasePushEncoder {
    private EncodecRender render;

    public PushEncodec(Context context, int textureid) {
        super(context);
        render = new EncodecRender(context, textureid);
        setRender(render);
        setmRenderMode(RENDERMODE_CONTINUOUSLY);
    }
}
