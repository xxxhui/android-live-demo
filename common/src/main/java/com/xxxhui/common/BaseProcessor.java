package com.xxxhui.common;

public abstract class BaseProcessor<INIT, PROCESS> implements Processor<INIT, PROCESS> {

    protected int mState = STATE_UN_INIT;

    @Override
    public synchronized boolean init(INIT param) {
        if (mState == STATE_UN_INIT) {
            if(onInit(param)) {
                mState = STATE_INIT;
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean start() {
        if (mState == STATE_INIT) {
            if(onStart()) {
                mState = STATE_START;
                return true;
            }
        } else if (mState == STATE_PAUSE) {
            if(onPauseReStart()) {
                mState = STATE_START;
                return true;
            }
        } else if (mState == STATE_STOP) {
            if(onStopReStart()) {
                mState = STATE_START;
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean process(PROCESS process) {
        if (mState == STATE_START) {
            return onProcess(process);
        }
        return false;
    }

    @Override
    public synchronized boolean pause() {
        if (mState == STATE_START) {
            mState = STATE_PAUSE;
            return onStart();
        }
        return false;
    }

    @Override
    public synchronized boolean stop() {
        if (mState == STATE_START || mState == STATE_PAUSE) {
            mState = STATE_STOP;
            return onStop();
        }
        return false;
    }

    @Override
    public synchronized void release() {
        if (mState > STATE_UN_INIT) {
            if (mState != STATE_STOP) {
                stop();
            }
            mState = STATE_UN_INIT;
            onRelease();
        }
    }

    @Override
    public synchronized int getState() {
        return mState;
    }

    protected synchronized void setState(int state) {
        this.mState = state;
    }

    protected abstract boolean onInit(INIT param);

    protected abstract boolean onStart();

    protected abstract boolean onPauseReStart();

    protected abstract boolean onStopReStart();

    protected abstract boolean onProcess(PROCESS process);

    protected abstract boolean onPause();

    protected abstract boolean onStop();

    protected abstract void onRelease();

}
