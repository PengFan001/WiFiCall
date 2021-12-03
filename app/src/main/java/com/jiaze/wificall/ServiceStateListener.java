package com.jiaze.wificall;

/**
 * =========================================
 * The Project:WifiCall
 * the Package:com.jiaze.wificall
 * on 2021/11/26
 * =========================================
 */
public interface ServiceStateListener {
    void onServiceConnected();

    void onServiceDisconnected();
}
