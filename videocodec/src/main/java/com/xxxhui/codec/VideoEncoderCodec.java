package com.xxxhui.codec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.xxxhui.common.Processor;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoderCodec implements Processor<VideoEncodeParam, Boolean> {

    private static final String TAG = "VideoEncoderCodec";
    private static final String MIME_TYPE = "video/avc";
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private EncoderDrainListener mOutListener;

    private int mState = STATE_UN_INIT;

    private DrainEncoderThread mDrainEncoderThread;
    private Surface mSurface;


    @Override
    public synchronized boolean init(VideoEncodeParam param) {

        Log.d(TAG, "init");

        if (param == null) {
            return false;
        }

        if (mState != STATE_UN_INIT) {
            return false;
        }

        mBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, param.getWidth(), param.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, param.getBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, param.getFrameRate());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, param.getiFrameInterval());

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "init error ", e);
        }

        if (mSurface != null) {
            mState = STATE_INIT;
            mOutListener = param.getOutListener();
            mDrainEncoderThread = new DrainEncoderThread();
            start();
            param.setSurface(mSurface);
            Log.d(TAG, "init success ");
            return true;
        }

        return false;
    }

    @Override
    public synchronized boolean start() {
        if (mState == STATE_INIT) {
            mEncoder.start();
            mDrainEncoderThread.start();
            mState = STATE_START;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean process(Boolean endOfStream) {
        return false;
    }

    @Override
    public synchronized boolean pause() {
        return false;
    }

    @Override
    public synchronized boolean stop() {
        if (mState == STATE_START) {
            mEncoder.signalEndOfInputStream();
            mState = STATE_STOP;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void release() {

        Log.d(TAG, "release ");

        if (mState > STATE_UN_INIT && mState < STATE_STOP) {
            stop();
        }

        if (mState == STATE_STOP) {
            mState = STATE_UN_INIT;
            try {
                mDrainEncoderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mDrainEncoderThread = null;
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    @Override
    public synchronized int getState() {
        return mState;
    }

    class DrainEncoderThread extends Thread {

        @Override
        public void run() {

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();

            while (true) {
                Log.d(TAG, "dequeueOutputBuffer ");

                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, 10000);

                Log.d(TAG, "dequeueOutputBuffer encoderStatus "+encoderStatus);

                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // should happen before receiving buffers, and should only happen once

                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    ByteBuffer sps = newFormat.getByteBuffer("csd-0");
                    ByteBuffer pps = newFormat.getByteBuffer("csd-1");
                    byte[] config = new byte[sps.limit() + pps.limit()];
                    sps.get(config, 0, sps.limit());
                    pps.get(config, sps.limit(), pps.limit());

                    if (mOutListener != null) {
                        mOutListener.onConfig(config);
                    }

                    Log.d(TAG, "encoder output format changed: " + newFormat);

                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it

                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                        Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {

                        Log.d("zxhhaha", "mBufferInfo.size "+mBufferInfo.size + " mBufferInfo.presentationTimeUs +mBufferInfo.presentationTimeUs");

                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                        Log.d(TAG, "mBufferInfo.size "+mBufferInfo.size);

                        if (mOutListener != null) {
                            mOutListener.onDrain(encodedData, mBufferInfo.presentationTimeUs);
                        }

                        encodedData.position(mBufferInfo.offset);

                    }
                    mEncoder.releaseOutputBuffer(encoderStatus, false);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;      // out of while
                    }

                }
            }
        }
    }

}
