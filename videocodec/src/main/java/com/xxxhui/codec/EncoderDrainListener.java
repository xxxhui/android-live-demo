package com.xxxhui.codec;

import java.nio.ByteBuffer;

public interface EncoderDrainListener {
    void onConfig(byte[] config);
    void onDrain(ByteBuffer data, long presentationTimeUs);
}
