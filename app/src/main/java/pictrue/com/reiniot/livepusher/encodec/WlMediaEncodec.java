package pictrue.com.reiniot.livepusher.encodec;

import android.content.Context;

import pictrue.com.reiniot.livepusher.imgvideo.WlBaseMediaEncoder;
import pictrue.com.reiniot.livepusher.imgvideo.WlEncodecRender;

public class WlMediaEncodec extends WlBaseMediaEncoder{

    private WlEncodecRender wlEncodecRender;

    public WlMediaEncodec(Context context, int textureId) {
        super(context);
        wlEncodecRender = new WlEncodecRender(context, textureId);
        setRender(wlEncodecRender);
        setmRenderMode(WlBaseMediaEncoder.RENDERMODE_CONTINUOUSLY);
    }
}
