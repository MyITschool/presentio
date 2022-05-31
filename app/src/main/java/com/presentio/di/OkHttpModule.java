package com.presentio.di;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.presentio.http.NetworkInterceptor;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module
public class OkHttpModule {
    public final Context context;

    public OkHttpModule(Context context) {
        this.context = context;
    }

    @Singleton
    @Provides
    public OkHttpClient provideOkHttpClient() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInterceptor interceptor = new NetworkInterceptor(() -> {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        });

        return new OkHttpClient.Builder().cache(new Cache(
                new File(context.getExternalCacheDir(), "http"),
                50L * 1024 * 1024
        ))
                .addNetworkInterceptor(interceptor)
                .addInterceptor(interceptor)
                .build();
    }
}
