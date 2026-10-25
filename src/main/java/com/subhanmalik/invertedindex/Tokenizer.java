package com.subhanmalik.invertedindex;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lower-casing tokenizer for the index.
 *
 * <p>Text is split on anything that is not a letter or digit, so
 * punctuation and whitespace never become terms. A small stop-word list
 * is dropped: words like "the" and "and" appear in almost every document,
 * so indexing them would bloat every posting list without helping
 * ranking. One consequence worth knowing: a phrase query containing a
 * stop word (for example, "lord of the rings") will not match, because
 * the stop word was never indexed.
 */
public final class Tokenizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "of", "to", "in", "on",
            "for", "with", "is", "are", "was", "were", "be", "by", "at",
            "as", "it", "its", "this", "that", "from", "into");

    private Tokenizer() {
    }

    /** Splits {@code text} into lower-cased, stop-word-free tokens, in order. */
    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).stream()
                .filter(token -> !token.isEmpty() && !STOP_WORDS.contains(token))
                .toList();
    }
}
