package com.presentio.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.presentio.R;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.http.Api;
import com.presentio.util.AccessTokenUtil;
import com.presentio.util.ObservableUtil;
import com.presentio.util.SharedPreferencesUtil;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.OkHttpClient;

public class AppFragment extends Fragment {
    @Inject
    OkHttpClient client;

    private CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);

        loadToken();

        Api.setHandler(() -> {
            Toast.makeText(getContext(), "Cannot authorize!", Toast.LENGTH_SHORT).show();

            doLogout();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.app_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavHostFragment hostFragment =
                (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.app_nav_host);

        BottomNavigationView bottomNavigation = getView().findViewById(R.id.bottom_menu);

        NavController controller = hostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNavigation, controller);
    }

    private void loadToken() {
        ObservableUtil.singleIo(() -> {
            Api api = new Api(getContext(), client, Api.HOST_USER_SERVICE);

            return api.getNewToken();
        }, new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(String token) {
                        AccessTokenUtil.setToken(token);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), "Cannot authorize!", Toast.LENGTH_SHORT).show();

                        doLogout();
                    }
                });
    }

    private void doLogout() {
        SharedPreferences preferences =
                SharedPreferencesUtil.getSharedPreferences(getContext());

        SharedPreferencesUtil.removeRefreshToken(preferences);

        NavHostFragment appHostFragment = (NavHostFragment) getParentFragment();

        appHostFragment.getNavController().navigate(
                AppFragmentDirections.actionAppFragmentToAuthorizationFragment()
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
