package pictrue.com.reiniot.livepusher.encodec;

import android.content.Context;

/**
 * 2019/8/15.
 */
public class LgMediaEncodec extends BaseMediaEncoder {
    private EncodecRender render;

    public LgMediaEncodec(Context context, int textureid) {
        super(context);
        render = new EncodecRender(context, textureid);
        setRender(render);
        setmRenderMode(RENDERMODE_CONTINUOUSLY);
    }
}
