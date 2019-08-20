package pictrue.com.reiniot.livepusher.act;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.imgvideo.ImageVideoActivity;
import pictrue.com.reiniot.livepusher.push.LivePush;
import pictrue.com.reiniot.livepusher.yuv_play.PlayYuvAct;

/**
 * 2019/8/12.
 */
public class EnterAct extends AppCompatActivity {
    private boolean isPermissioned;
    private Button go;
    private Button record;
    private Button pic_muxer_video;
    private Button play_yuv;
    private Button push;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    || PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                }, 1);
            } else {
                isPermissioned = true;
            }
        }
        setContentView(R.layout.act_enter);
        go = findViewById(R.id.id_go_camera);
        record = findViewById(R.id.record);
        pic_muxer_video = findViewById(R.id.pic_muxer_video);
        play_yuv = findViewById(R.id.play_yuv);
        //绘制yuv数据
        play_yuv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(EnterAct.this, PlayYuvAct.class);
                startActivity(it);
            }
        });

        push = findViewById(R.id.push);
        push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(EnterAct.this, LivePush.class);
                startActivity(it);
            }
        });


        //图片合成视频
        pic_muxer_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPermissioned) {
                    Toast.makeText(EnterAct.this, "为获取权限", Toast.LENGTH_SHORT).show();
                }
                Intent it = new Intent(EnterAct.this, ImageVideoActivity.class);
                startActivity(it);
            }
        });


        //视频预览
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPermissioned) {
                    Intent it = new Intent(EnterAct.this, MainActivity.class);
                    startActivity(it);
                }
            }
        });
        //视频录制
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPermissioned) {
                    Toast.makeText(EnterAct.this, "为获取权限", Toast.LENGTH_SHORT).show();
                }
                Intent it = new Intent(EnterAct.this, VideoAct.class);
                startActivity(it);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        isPermissioned = true;
    }
}
