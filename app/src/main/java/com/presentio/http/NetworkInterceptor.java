package com.presentio.http;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkInterceptor implements Interceptor {
    public interface NetworkMonitor {
        boolean isOnline();
    }

    final NetworkMonitor monitor;

    public NetworkInterceptor(NetworkMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request newRequest = chain.request();

        if (!monitor.isOnline()) {
            newRequest = newRequest
                    .newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
        }

        Response response = chain
                .proceed(newRequest);

        return response
                .newBuilder()
                .addHeader("Vary", "")
                .build();
    }
}
