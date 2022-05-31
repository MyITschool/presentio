package com.presentio.di;

import com.presentio.fragment.AppFragment;
import com.presentio.fragment.AuthorizationFragment;
import com.presentio.fragment.CreatePostFragment;
import com.presentio.fragment.FavoritesFragment;
import com.presentio.fragment.HomeFragment;
import com.presentio.fragment.PostFragment;
import com.presentio.fragment.ProfileFragment;
import com.presentio.fragment.SearchFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {OkHttpModule.class})
public interface OkHttpComponent {
    void inject(HomeFragment homeFragment);
    void inject(AuthorizationFragment authorizationFragment);
    void inject(ProfileFragment profileFragment);
    void inject(AppFragment appFragment);
    void inject(SearchFragment searchFragment);
    void inject(PostFragment postFragment);
    void inject(CreatePostFragment createPostFragment);
    void inject(FavoritesFragment favoritesFragment);
}
