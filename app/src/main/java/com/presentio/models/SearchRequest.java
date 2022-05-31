package com.presentio.models;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class SearchRequest {
    @Id
    public long id;

    public String query;

    @Override
    public String toString() {
        return query;
    }
}
