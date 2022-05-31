package com.presentio.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.presentio.R;
import com.presentio.js2p.structs.JsonFpost;
import com.presentio.js2p.structs.JsonUser;
import com.squareup.picasso.Picasso;

public class RepostSheetFragment extends BottomSheetDialogFragment {
    public static final String TAG = "RepostBottomSheetFragment";

    private JsonUser user;
    private JsonFpost post;
    private OnClickHandler handler;

    public interface OnClickHandler {
        void onClick(JsonFpost post, String text);
    }

    public RepostSheetFragment(JsonUser user, JsonFpost post, OnClickHandler handler) {
        this.user = user;
        this.post = post;
        this.handler = handler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.TransparentBottomDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.repost_bottom_sheet, container, false);

        fillView(view);

        return view;
    }

    private void fillView(View view) {
        ImageView userImage = view.findViewById(R.id.repost_user_image);

        userImage.setClipToOutline(true);
        Picasso.get().load(user.pfpUrl).into(userImage);

        TextView userName = view.findViewById(R.id.repost_user_name);

        userName.setText(user.name);

        Button button = view.findViewById(R.id.repost_confirm);

        TextView text = view.findViewById(R.id.create_text);

        button.setOnClickListener(v -> {
            String repostText = text.getText().toString().trim();

            if (repostText.isEmpty()) {
                Toast.makeText(getContext(), "Repost text cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            handler.onClick(post, text.getText().toString());
            dismiss();
        });
    }
}
