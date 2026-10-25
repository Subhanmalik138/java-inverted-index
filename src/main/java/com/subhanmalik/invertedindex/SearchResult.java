package com.subhanmalik.invertedindex;

/** One ranked hit: the matching document and its TF-IDF score. */
public record SearchResult(Document document, double score) {
}
