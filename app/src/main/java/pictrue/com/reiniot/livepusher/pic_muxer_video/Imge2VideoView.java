package pictrue.com.reiniot.livepusher.pic_muxer_video;

import android.content.Context;
import android.util.AttributeSet;

import com.ywl5320.libmusic.WlMusic;

import pictrue.com.reiniot.livepusher.egl.EGLSurfaceView;

/**
 * 2019/8/16.
 */
public class Imge2VideoView extends EGLSurfaceView {
    private Image2VideoRender render;
    private int fbotextureid;

    public Imge2VideoView(Context context) {
        this(context, null);
    }

    public Imge2VideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Imge2VideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        render = new Image2VideoRender(context);
        setRender(render);
        setRenderMode(EGLSurfaceView.RENDERMODE_WHEN_DIRTY);
        render.setOnRenderCreateListener(new Image2VideoRender.OnRenderCreateListener() {
            @Override
            public void onCreate(int textid) {
                fbotextureid = textid;
            }
        });

    }

    public void setCurrentImg(int imgsr) {
        if (render != null) {
            render.setCurrentImgSrc(imgsr);
            requestRender();
        }
    }

    public int getFbotextureid() {
        return fbotextureid;
    }
}
