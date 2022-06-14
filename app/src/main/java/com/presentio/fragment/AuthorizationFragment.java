package com.presentio.fragment;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInRequest.PasswordRequestOptions;
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.gson.Gson;
import com.presentio.R;
import com.presentio.db.ObjectBox;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.http.Api;
import com.presentio.js2p.JsonAuth;
import com.presentio.models.SearchRequest;
import com.presentio.params.AuthorizeParams;
import com.presentio.util.AccessTokenUtil;
import com.presentio.util.SharedPreferencesUtil;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import io.objectbox.Box;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthorizationFragment extends Fragment {
    private static final int SIGN_IN = 0;
    private static final int REGISTER = 1;

    private SignInClient client;

    private boolean isCurrRequesting = false;

    @Inject
    OkHttpClient httpClient;

    private final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        client = Identity.getSignInClient(getContext());

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(this.getContext())
        ).build();

        component.inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.authorization_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.reg_button).setOnClickListener(v -> {
            if (!isCurrRequesting) {
                prompt(createSignInRequest(false), REGISTER);

                isCurrRequesting = true;
            }
        });

        view.findViewById(R.id.sign_in_button).setOnClickListener(v -> {
            if (!isCurrRequesting) {
                prompt(createSignInRequest(true), SIGN_IN);

                isCurrRequesting = true;
            }
        });
    }

    /**
     * The documentation itself uses deprecated methods
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            SignInCredential credential = client.getSignInCredentialFromIntent(data);
            String idToken = credential.getGoogleIdToken();

            httpClient.cache().evictAll();
            Box<SearchRequest> requestBox = ObjectBox.get().boxFor(SearchRequest.class);

            requestBox.removeAll();

            isCurrRequesting = true;

            if (requestCode == SIGN_IN) {
                performSignIn(idToken);
            } else {
                performSignUp(idToken);
            }
        } catch (ApiException e) {
            switch (e.getStatusCode()) {
                case CommonStatusCodes.CANCELED:
                    Toast.makeText(getContext(), "One Tap UI closed", Toast.LENGTH_SHORT).show();
                    break;
                case CommonStatusCodes.NETWORK_ERROR:
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(getContext(), "Failed to authorize", Toast.LENGTH_SHORT).show();
                    break;
            }

            isCurrRequesting = false;
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to authorize", Toast.LENGTH_SHORT).show();
            isCurrRequesting = false;
        }
    }

    private void performSignIn(String idToken) {
        Single.fromCallable(() -> makeAuthRequest(createHttpSignInRequest(idToken)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new SingleObserver<Optional<JsonAuth>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Optional<JsonAuth> auth) {
                        handleToken(auth, "sign in");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), "Failed to connect to server", Toast.LENGTH_SHORT).show();
                        isCurrRequesting = false;
                    }
                });
    }

    private void performSignUp(String idToken) {
        Single.fromCallable(() -> makeAuthRequest(createHttpSignUpRequest(idToken)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new SingleObserver<Optional<JsonAuth>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Optional<JsonAuth> auth) {
                        handleToken(auth, "sign up");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), "Cannot connect to server", Toast.LENGTH_SHORT).show();
                        isCurrRequesting = false;
                    }
                });
    }

    private Optional<JsonAuth> makeAuthRequest(Request request) throws IOException {
        Response response = httpClient
                .newCall(request)
                .execute();

        int code = response.code();

        if (code != 200) {
            return Optional.empty();
        }

        Gson gson = new Gson();

        JsonAuth auth = gson.fromJson(response.body().charStream(), JsonAuth.class);

        return Optional.of(auth);
    }

    private void handleToken(Optional<JsonAuth> auth, String onFail) {
        if (auth.isPresent()) {
            SharedPreferences preferences = SharedPreferencesUtil.getSharedPreferences(getContext());

            SharedPreferencesUtil.setRefreshToken(preferences, auth.get().refreshToken);

            AccessTokenUtil.setToken(auth.get().accessToken);

            transfer();
        } else {
            Toast.makeText(getContext(), "Failed to " + onFail, Toast.LENGTH_SHORT).show();
        }

        isCurrRequesting = false;
    }

    private BeginSignInRequest createSignInRequest(boolean filterByAuthorizedAccounts) {
        return BeginSignInRequest
                .builder()
                .setPasswordRequestOptions(PasswordRequestOptions
                        .builder()
                        .setSupported(false)
                        .build())
                .setGoogleIdTokenRequestOptions(GoogleIdTokenRequestOptions
                        .builder()
                        .setSupported(true)
                        .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                        .setServerClientId(getString(R.string.google_sign_in_client_id)).build())
                .setAutoSelectEnabled(true)
                .build();
    }

    private Request createHttpSignInRequest(String googleToken) {
        return createAuthRequest(googleToken, "authorize");
    }

    private Request createHttpSignUpRequest(String googleToken) {
        return createAuthRequest(googleToken, "register");
    }

    private Request createAuthRequest(String googleToken, String path) {
        Gson gson = new Gson();

        String tokenJson = gson.toJson(new AuthorizeParams(googleToken));

        return new Request.Builder()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .method("POST", RequestBody.create(MediaType.parse("text/json"), tokenJson))
                .url(Api.HOST_USER_SERVICE + "/v0/auth/" + path)
                .build();
    }

    private void prompt(BeginSignInRequest request, int requestCode) {
        client
                .beginSignIn(request)
                .addOnSuccessListener(result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                requestCode,
                                null,
                                0,
                                0,
                                0,
                                null);
                    } catch (IntentSender.SendIntentException e) {
                        isCurrRequesting = false;
                        Toast.makeText(getContext(), "Couldn't start One Tap UI", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    isCurrRequesting = false;
                    Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void transfer() {
        NavDirections directions =
                AuthorizationFragmentDirections.actionAuthorizationFragmentToAppFragment();

        NavHostFragment.findNavController(this).navigate(directions);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
