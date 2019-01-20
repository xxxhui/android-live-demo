package com.xxxhui.common;

public interface Processor<INIT, PROCESS> {

    int STATE_UN_INIT = 0;
    int STATE_INIT = 1;
    int STATE_START = 2;
    int STATE_PAUSE = 3;
    int STATE_STOP = 4;

    boolean init(INIT init);

    boolean start();

    boolean process(PROCESS process);

    boolean pause();

    boolean stop();

    void release();

    int getState();

}
