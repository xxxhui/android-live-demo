package com.xxxhui.livedemo;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.xxxhui.codec.EncoderDrainListener;
import com.xxxhui.codec.VideoEncodeParam;
import com.xxxhui.codec.VideoEncoderCodec;
import com.xxxhui.render.gles.EglCore;
import com.xxxhui.render.gles.FullFrameRect;
import com.xxxhui.render.gles.Texture2dProgram;
import com.xxxhui.render.gles.WindowSurface;
import com.xxxhui.render.shape.Triangle;
import com.xxxhui.videocapture.CameraCapture;
import com.xxxhui.videocapture.CameraParam;

import net.butterflytv.rtmp_client.RTMPMuxer;

import java.nio.ByteBuffer;

/**
 * 发布直播的步骤：
 * 1，音视频数据采集
 * 2，音视频数据处理
 * 3，音视频数据编码
 * 4，音视频数据上传
 *
 * opengl环境创建步骤：
 * 1，初始化EGLDisplay
 * 2，创建EGLConfig
 * 3，创建EGLContext
 * 4，创建EGLSurface
 *
 * opengl2.0/3.0绘制核心步骤：
 * 1，根据顶点着色器和片元着色器创建opengl程序 glCreateProgram
 * 2，向opengl程序传递数据 glVertexAttribPointer glUniform4fv 等
 * 3，选择绘制方式并绘制 glDrawArrays 等
 *
 *
 */
public class MainActivity extends AppCompatActivity implements EncoderDrainListener {

    private static final String TAG = "MainActivity";
    private CameraCapture mCameraCapture;
    private SurfaceView mSurfaceView;
    private EglCore mEglCore;
    private WindowSurface mCameraWindowSurface;
    private FullFrameRect mEncodeFullScreen;
    private SurfaceTexture mSurfaceTexture;
    private FullFrameRect mCameraFullScreen;
    private EglCore mEncoderEglCore;
    private VideoEncoderCodec mEncoderCodec;
    private WindowSurface mEncoderWindowSurface;
    private int mTextureId;
    private RTMPMuxer rtmpMuxer;
    private VideoEncodeParam videoEncodeParam;
    private boolean isStartRtmp;
    private final Object mLock = new Object();

    private final float[] mSTMatrix = new float[16];

    private Triangle mTriangle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        }, 1);

        mSurfaceView = findViewById(R.id.sfv);


        mCameraCapture = new CameraCapture();
        mEncoderCodec = new VideoEncoderCodec();
        rtmpMuxer = new RTMPMuxer();

        Matrix.setIdentityM(mSTMatrix, 0);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                /**
                 * 创建opengl环境
                 */

                mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
                mCameraWindowSurface = new WindowSurface(mEglCore, holder.getSurface(), false);
                mCameraWindowSurface.makeCurrent();

                /**
                 * 创建相机数据纹理
                 */
                mCameraFullScreen = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                mTextureId = mCameraFullScreen.createTextureObject();
//                mCameraFullScreen.getProgram().setTexSize(720, 1080);
                mTriangle = new Triangle();

                /**
                 * 创建SurfaceTexture, 打开相机
                 */
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                CameraParam cameraParam = new CameraParam();
                cameraParam.setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
                cameraParam.setSurfaceTexture(mSurfaceTexture);
                cameraParam.setSize(new android.util.Pair<>(720, 1280));
                mCameraCapture.init(cameraParam);
                mCameraCapture.start();


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * 开启rtmp推流
                         */
                        int open = rtmpMuxer.open("rtmp://gx-push.loomo.com/gx/test-ygf", 0, 0);
                        Log.d(TAG, "rtmpMuxer open " + open);
                        if (open < 0) {
                            throw new RuntimeException("open error");
                        }

                        /**
                         * 开启编码器
                         */
                        videoEncodeParam = new VideoEncodeParam();
                        videoEncodeParam.setFrameRate(25);
                        videoEncodeParam.setBitRate(850000);
                        videoEncodeParam.setiFrameInterval(1);
                        videoEncodeParam.setWidth(720);
                        videoEncodeParam.setHeight(1080);
                        videoEncodeParam.setOutListener(MainActivity.this);
                        mEncoderCodec.init(videoEncodeParam);

                        isStartRtmp = true;
                    }
                }).start();

                /**
                 * 监听有效数据回调
                 */
                mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

                        synchronized (mLock) {

                            if (mEglCore == null) {
                                return;
                            }

                            /**
                             * 绘制到预览SurfaceView
                             */

                            mCameraWindowSurface.makeCurrent();
                            surfaceTexture.updateTexImage();

//                            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
//                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

//                            mSurfaceTexture.getTransformMatrix(mSTMatrix);
                            /**
                             * 画相机采集的图像数据
                             */
                            mCameraFullScreen.drawFrame(mTextureId, mSTMatrix);

                            /**
                             * 画三角形
                             */
                            mTriangle.draw();

                            //drawBox(20);
                            mCameraWindowSurface.swapBuffers();

                            if (!isStartRtmp) {
                                return;
                            }

                            /**
                             * 绘制到编码器(填充图像数据到编码器)
                             */

                            if (mEncoderWindowSurface == null) {
                                Surface surface = videoEncodeParam.getSurface();
                                mEncoderEglCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
                                mEncoderWindowSurface = new WindowSurface(mEncoderEglCore, surface, false);
                            }

                            mEncoderWindowSurface.makeCurrent();
                            mEncodeFullScreen = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
//                            surfaceTexture.getTransformMatrix(mSTMatrix);
                            mEncodeFullScreen.drawFrame(mTextureId, mSTMatrix);
                            drawBox(20);
                            mEncoderWindowSurface.setPresentationTime(System.nanoTime());
                            mEncoderWindowSurface.swapBuffers();
                        }

                    }
                });


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                rtmpMuxer.close();
                isStartRtmp = false;

                synchronized (mLock) {
                    mEncoderCodec.release();
                    mCameraCapture.release();
                    mEncoderWindowSurface.release();
                    mCameraWindowSurface.release();
                    mEncoderEglCore.release();
                    mEglCore.release();
                    mEglCore = null;
                }

            }
        });

    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        int xpos = (posn * 4) % (720 - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 100, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }


    @Override
    public void onConfig(byte[] config) {
        /**
         * 发送视频的pps/sps信息
         */
        rtmpMuxer.writeVideo(config, 0, config.length, 0);
    }

    @Override
    public void onDrain(ByteBuffer data, long presentationTimeUs) {
        /**
         * 发送编码后的图像信息
         */
        byte[] bytes = new byte[data.limit()];
        data.get(bytes);
        rtmpMuxer.writeVideo(bytes, 0, bytes.length, (int) (presentationTimeUs / 1000));
    }
}
