package com.presentio.params;

import java.util.ArrayList;

public class CreatePostParams {
    public final String text;
    public ArrayList<String> tags = new ArrayList<>();
    public String[] attachments = new String[]{};
    public Long sourceId, sourceUserId;
    public Float photoRatio;

    public CreatePostParams(String text, Long sourceId, Long sourceUserId) {
        this.text = text;
        this.sourceId = sourceId;
        this.sourceUserId = sourceUserId;
    }

    public CreatePostParams(
            String text,
            ArrayList<String> tags,
            String[] attachments,
            Float photoRatio
    ) {
        this.text = text;
        this.tags = tags;
        this.attachments = attachments;
        this.photoRatio = photoRatio;
    }
}
