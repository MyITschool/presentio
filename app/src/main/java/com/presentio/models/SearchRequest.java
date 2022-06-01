package com.presentio.models;

import java.io.Serializable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class SearchRequest implements Serializable {
    @Id
    public long id;

    public String query;

    @Override
    public String toString() {
        return query;
    }
}
