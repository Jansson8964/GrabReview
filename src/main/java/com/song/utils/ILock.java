package com.song.utils;

public interface ILock {
    /**
     * try to get lock
     * @param timeoutSeconds lock timeout, auto release after timeout
     * @return
     */
    boolean tryLock(long timeoutSeconds);

    void unlock();
}
