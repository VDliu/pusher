package pictrue.com.reiniot.livepusher.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import pictrue.com.reiniot.livepusher.R;
import pictrue.com.reiniot.livepusher.egl.ShaderUtil;

public class CameraFboRender {

    private Context context;

    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f

            ,
            //图片纹理坐标，全部设置为0只是为了占位
            //后面会计算，并且赋值
            0, 0,
            0, 0,
            0, 0,
            0, 0
    };
    private FloatBuffer vertexBuffer;

    private float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    private FloatBuffer fragmentBuffer;

    private int program;
    private int vPosition;
    private int fPosition;
    private int textureid;
    private int sampler;
    private Bitmap bitmap;

    private int vboId;
    private int custom_texture_id;

    public CameraFboRender(Context context) {
        this.context = context;
        bitmap = ShaderUtil.createTextImage("hello world", 50, "#ff0000", "#00000000", 10);
        float h = 0.1f; //此处是映射到opengl中的高度
        float ratio = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        float w = h * ratio;//此处是映射到opengl中的宽度

        //假设改纹理的右下角在opengl的(0.8,-0.8)处
        vertexData[8] = 0.8f - w;
        vertexData[9] = -0.8f;

        vertexData[10] = 0.8f;
        vertexData[11] = -0.8f;

        vertexData[12] = 0.8f - w;
        vertexData[13] = -0.8f + 0.1f;

        vertexData[14] = 0.8f;
        vertexData[15] = -0.8f +0.1f;



        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);

    }

    public void onCreate() {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,GLES20.GL_ONE_MINUS_SRC_ALPHA);
        String vertexSource = ShaderUtil.getRawResource(context, R.raw.vertex_shader_screen);
        String fragmentSource = ShaderUtil.getRawResource(context, R.raw.fragment_shader_screen);

        program = ShaderUtil.createProgram(vertexSource, fragmentSource);

        vPosition = GLES20.glGetAttribLocation(program, "v_Position");
        fPosition = GLES20.glGetAttribLocation(program, "f_Position");
        sampler = GLES20.glGetUniformLocation(program, "sTexture");

        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        //需要在 opengl线程中创建才会有效
        custom_texture_id = ShaderUtil.loadBitmapTexture(bitmap);
    }

    public void onChange(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    public void onDraw(int textureId) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1f, 0f, 0f, 1f);

        GLES20.glUseProgram(program);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //绘制普通的纹理（绘制fbo）
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8,
                0);

        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8,
                vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        //绘制图片文字纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, custom_texture_id);
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8,
                32);

        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8,
                vertexData.length * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}
