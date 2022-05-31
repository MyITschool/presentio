package com.presentio.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.drjacky.imagepicker.ImagePicker;
import com.github.drjacky.imagepicker.constant.ImageProvider;
import com.github.drjacky.imagepicker.util.FileUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.presentio.R;
import com.presentio.adapter.ImagePagerAdapter;
import com.presentio.adapter.TagAdapter;
import com.presentio.di.DaggerOkHttpComponent;
import com.presentio.di.OkHttpComponent;
import com.presentio.di.OkHttpModule;
import com.presentio.http.Api;
import com.presentio.js2p.structs.JsonTag;
import com.presentio.requests.Finisher;
import com.presentio.requests.NewPostRequest;
import com.presentio.util.ObservableUtil;
import com.presentio.util.ViewUtil;
import com.presentio.view.RatioViewPager;

import java.util.ArrayList;
import java.util.UUID;

import javax.inject.Inject;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlin.Pair;
import okhttp3.OkHttpClient;

public class CreatePostFragment extends Fragment {
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private Api postsApi;
    private ActivityResultLauncher<Intent> launcher;
    private ImagePagerAdapter adapter;
    private CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    OkHttpClient client;

    private ArrayList<JsonTag> jsonTags = new ArrayList<>();
    private ArrayList<String> localUris = new ArrayList<>(), tags = new ArrayList<>();
    private float ratio;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OkHttpComponent component = DaggerOkHttpComponent.builder().okHttpModule(
                new OkHttpModule(getContext())
        ).build();

        component.inject(this);
        
        postsApi = new Api(getContext(), client, Api.HOST_POSTS_SERVICE);

        adapter = new ImagePagerAdapter(localUris);

        launcher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri uri = result.getData().getData();

                        addImageToPager(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_post_fragment, container, false);

        fillView(view);

        return view;
    }

    private void fillView(View view) {
        fillPager(view);
        fillTags(view);

        setupButtons(view);
        setupButtonListeners(view);
    }

    private void fillPager(View view) {
        RatioViewPager pager = view.findViewById(R.id.create_image_pager);
        pager.WHRatio = 2;
        pager.setClipToOutline(true);

        pager.setAdapter(adapter);
    }

    private void fillTags(View view) {
        ImageView tagAdd = view.findViewById(R.id.create_tag_add);
        EditText tagText = view.findViewById(R.id.create_tag);

        RecyclerView tagsView = view.findViewById(R.id.create_tags);

        TagAdapter tagAdapter = new TagAdapter(jsonTags, getContext());

        tagAdapter.setHandler(position -> {
            jsonTags.remove(position);
            tags.remove(position);

            tagAdapter.notifyItemRemoved(position);
        });

        tagsView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        tagsView.setAdapter(tagAdapter);

        tagAdd.setOnClickListener(v -> {
            if (jsonTags.size() > 4) {
                Toast.makeText(getContext(), "Unable to add more than 5 tags!", Toast.LENGTH_SHORT).show();
                return;
            }

            JsonTag tag = new JsonTag();

            tag.name = tagText.getText().toString().trim();
            tagText.setText("");

            if (jsonTags.contains(tag)) {
                Toast.makeText(getContext(), "This tag is already added", Toast.LENGTH_SHORT).show();
                return;
            }


            if (!tag.name.matches("\\p{L}[\\p{L}\\p{N}_\\-]*")) {
                Toast.makeText(getContext(), "Tag name contains disallowed characters!", Toast.LENGTH_SHORT).show();
                return;
            }

            jsonTags.add(tag);
            tags.add(tag.name);
            tagAdapter.notifyItemInserted(jsonTags.size());
        });
    }

    private void setupButtons(View view) {
        RatioViewPager pager = view.findViewById(R.id.create_image_pager);

        int items = pager.getAdapter().getCount();

        ViewUtil.setVisible(view.findViewById(R.id.create_add_image_first), items == 0);
        ViewUtil.setVisible(view.findViewById(R.id.create_add_image), items != 0);
    }

    private void setupButtonListeners(View view) {
        view.findViewById(R.id.create_add_image_first).setOnClickListener(this::imageAdd);
        view.findViewById(R.id.create_add_image).setOnClickListener(this::imageAdd);

        view.findViewById(R.id.create_confirm).setOnClickListener(this::createPost);
    }

    private void imageAdd(View view) {
        if (adapter.getCount() > 9) {
            Toast.makeText(getContext(), "Unable to add more than 10 attachments!", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View customView = inflater.inflate(R.layout.image_dialog,null);

        new MaterialAlertDialogBuilder(getContext(), R.style.ImageAlertDialog)
                .setTitle("Choose")
                .setView(customView)
                .setNegativeButton("Cancel", null)
                .show();

        customView.findViewById(R.id.pick_camera).setOnClickListener(v -> openPicker(ImageProvider.CAMERA));

        customView.findViewById(R.id.pick_gallery).setOnClickListener(v -> openPicker(ImageProvider.GALLERY));
    }

    private void openPicker(ImageProvider provider) {
        launcher.launch(
                ImagePicker.Companion
                        .with(getActivity())
                        .galleryMimeTypes(new String[]{"image/*"})
                        .crop()
                        .provider(provider)
                        .maxResultSize(1920, 1920, true)
                        .createIntent()
        );
    }

    private void addImageToPager(Uri uri) {
        if (adapter.getCount() == 0) {
            View view = getView();
            RatioViewPager pager = view.findViewById(R.id.create_image_pager);

            Pair<Integer, Integer> imageResolution = FileUtil.INSTANCE.getImageResolution(getContext(), uri);

            pager.WHRatio = imageResolution.getFirst() / (float) imageResolution.getSecond();
            ratio = pager.WHRatio;
        }

        adapter.addItem(uri.toString());

        setupButtons(getView());
    }

    private void createPost(View buttonView) {
        View view = getView();

        EditText postText = view.findViewById(R.id.create_text);
        String text = postText.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(getContext(), "Post text cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (jsonTags.isEmpty()) {
            Toast.makeText(getContext(), "Post must have at least one tag!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (localUris.isEmpty()) {
            Toast.makeText(getContext(), "Post must have at least one image!", Toast.LENGTH_SHORT).show();
            return;
        }

        disableCreation();
        sendCreateRequest(text);
    }

    private void sendCreateRequest(String text) {
        String[] uploadedUris = new String[localUris.size()];

        Finisher finisher = new Finisher() {
            private final int total = localUris.size();
            private int finished = 0;

            @Override
            public void finish(boolean success) {
                if (success) {
                    ++finished;

                    if (finished == total) {
                        uploadPost(uploadedUris, text);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to upload file", Toast.LENGTH_SHORT).show();
                }
            }
        };

        for (int i = 0; i < localUris.size(); ++i) {
            String imageUri = localUris.get(i);

            String name = UUID.randomUUID() + imageUri.substring(imageUri.lastIndexOf('.'));

            StorageReference ref = storage.getReference(name);

            UploadTask uploadTask = ref.putFile(Uri.parse(imageUri));

            int finalI = i;

            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    finisher.finish(false);
                }

                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    finisher.finish(false);
                }

                uploadedUris[finalI] = task.getResult().toString();
                finisher.finish(true);
            });
        }
    }

    private void disableCreation() {
        View view = getView();

        view.findViewById(R.id.create_confirm).setEnabled(false);
    }

    private void enableCreation() {
        View view = getView();

        view.findViewById(R.id.create_confirm).setEnabled(true);
    }

    private void uploadPost(String[] uploadedUris, String text) {
        ObservableUtil.singleIo(
                new NewPostRequest(postsApi, text, tags, uploadedUris, ratio),
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        if (integer == 201) {
                            onCreated();
                        } else {
                            Toast.makeText(getContext(), "Wrong post data!", Toast.LENGTH_SHORT).show();

                            enableCreation();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof Api.InvalidCredentialsException) {
                            Api.logout();
                            return;
                        }

                        enableCreation();
                        Toast.makeText(getContext(), "Failed to create post", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void onCreated() {
        Toast.makeText(getContext(), "Post created!", Toast.LENGTH_SHORT).show();

        getFragmentManager().popBackStack();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        disposable.dispose();
    }
}
