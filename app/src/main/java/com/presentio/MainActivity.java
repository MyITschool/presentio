package com.presentio;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.presentio.db.ObjectBox;
import com.presentio.fragment.AuthorizationFragmentDirections;
import com.presentio.util.SharedPreferencesUtil;

import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ObjectBox.init(this);

        NavHostFragment hostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);

        NavController controller = hostFragment.getNavController();

        SharedPreferences preferences = SharedPreferencesUtil.getSharedPreferences(this);

        String refreshToken = SharedPreferencesUtil.getRefreshToken(preferences);

        if (refreshToken != null) {
            controller.navigate(AuthorizationFragmentDirections.actionAuthorizationFragmentToAppFragment());
        }

        RxJavaPlugins.setErrorHandler(throwable -> {
            if (!(throwable instanceof UndeliverableException)) {
                throwable.printStackTrace();
                throw new RuntimeException("Unexpected throwable in RxJava error handler " + throwable.getMessage());
            }
        });
    }
}