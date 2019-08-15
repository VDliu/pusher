package pictrue.com.reiniot.livepusher.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.OrientationEventListener;

public class DisplayUtil {

    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = context.getResources().getDisplayMetrics();
        return metric.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics metric = context.getResources().getDisplayMetrics();
        return metric.heightPixels;
    }

    public static int getScreenOritation(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)

        {
            return orientation;  //手机平放时，检测不到有效的角度
        }
        //只检测是否有四个角度的改变
        if (orientation > 350 || orientation < 10)
        { //0度
            orientation = 0;
        } else if (orientation > 80 && orientation < 100)

        { //90度
            orientation = 90;
        } else if (orientation > 170 && orientation < 190)

        { //180度
            orientation = 180;
        } else if (orientation > 260 && orientation < 280)

        { //270度
            orientation = 270;
        }

        return orientation;
    }
}
