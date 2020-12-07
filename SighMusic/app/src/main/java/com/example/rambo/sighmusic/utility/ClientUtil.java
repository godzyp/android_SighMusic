package com.example.rambo.sighmusic.utility;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 尝试自己封装一个OKHTTP客户端什么的
 */
public class ClientUtil {

    private static OkHttpClient mClient;

    public static OkHttpClient getOkHttpClient() {
        if (mClient == null) {
            mClient = new OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return mClient;
    }

}
