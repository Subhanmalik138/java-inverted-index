package com.subhanmalik.invertedindex;

/** A single document in the index. The {@code id} must be unique. */
public record Document(String id, String title, String text) {
    public Document {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("document id must not be blank");
        }
        if (text == null) {
            throw new IllegalArgumentException("document text must not be null");
        }
        if (title == null) {
            title = id;
        }
    }
}
