package com.presentio.handler;

import com.presentio.js2p.structs.JsonFpost;

public interface PostEventHandler {
    void onOpen(JsonFpost post);
    void onRepost(JsonFpost post);
}
