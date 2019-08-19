package pictrue.com.reiniot.livepusher.yuv_play;

import android.content.Context;
import android.util.AttributeSet;

import pictrue.com.reiniot.livepusher.egl.EGLSurfaceView;

/**
 * 2019/8/19.
 */
public class YuvView extends EGLSurfaceView {
    YuvRender render;

    public YuvView(Context context) {
        this(context, null);
    }

    public YuvView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YuvView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        render = new YuvRender(context);
        setRender(render);
        setRenderMode(EGLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setFrameData(int w, int h, byte[] y, byte[] u, byte[] v) {
        if (render != null) {
            render.setFrameData(w, h, y, u, v);
            requestRender();
        }
    }

}
