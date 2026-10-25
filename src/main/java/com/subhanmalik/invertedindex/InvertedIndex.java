package com.subhanmalik.invertedindex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An in-memory inverted index.
 *
 * <p>Each term maps to a posting list: document id to the sorted list of
 * positions where the term occurs. Positions are what make phrase search
 * possible without re-scanning document text — a phrase matches only
 * when its terms occupy consecutive positions in the same document.
 *
 * <p>Query language (all matching is case-insensitive):
 * <ul>
 *   <li>{@code cats dogs} — documents containing <em>both</em> terms (AND).</li>
 *   <li>{@code "quick brown fox"} — the exact phrase, in order.</li>
 *   <li>{@code cats -dogs} — documents with "cats" but without "dogs".</li>
 *   <li>Clauses may be combined: {@code "brown fox" cats -dogs}.</li>
 * </ul>
 *
 * <p>Results are ranked by TF-IDF: for each query term,
 * {@code (1 + log(tf)) * (log((N + 1) / (df + 1)) + 1)}, summed over the
 * query's terms. Repeated terms in a document raise its score, and rare
 * terms count more than common ones. The {@code +1} smoothing keeps
 * terms that appear in every document from scoring exactly zero.
 */
public class InvertedIndex {

    private static final Pattern PHRASE = Pattern.compile("\"([^\"]+)\"");

    /** term -> (document id -> positions of the term in that document). */
    private final Map<String, Map<String, List<Integer>>> postings = new HashMap<>();

    private final Map<String, Document> documents = new HashMap<>();

    /** Adds a document, replacing any existing document with the same id. */
    public void addDocument(Document document) {
        removeDocument(document.id());
        documents.put(document.id(), document);
        List<String> tokens = Tokenizer.tokenize(document.text());
        for (int position = 0; position < tokens.size(); position++) {
            postings.computeIfAbsent(tokens.get(position), term -> new HashMap<>())
                    .computeIfAbsent(document.id(), id -> new ArrayList<>())
                    .add(position);
        }
    }

    /** Removes a document and prunes any posting lists left empty. */
    public boolean removeDocument(String documentId) {
        Document removed = documents.remove(documentId);
        if (removed == null) {
            return false;
        }
        for (String term : Tokenizer.tokenize(removed.text())) {
            Map<String, List<Integer>> posting = postings.get(term);
            if (posting != null) {
                posting.remove(documentId);
                if (posting.isEmpty()) {
                    postings.remove(term);
                }
            }
        }
        return true;
    }

    public int documentCount() {
        return documents.size();
    }

    public int indexedTermCount() {
        return postings.size();
    }

    /** Searches the index; an empty or term-less query returns no results. */
    public List<SearchResult> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<List<String>> phrases = new ArrayList<>();
        Matcher matcher = PHRASE.matcher(query);
        while (matcher.find()) {
            List<String> phraseTerms = Tokenizer.tokenize(matcher.group(1));
            if (!phraseTerms.isEmpty()) {
                phrases.add(phraseTerms);
            }
        }
        String withoutPhrases = matcher.reset().replaceAll(" ");

        List<String> required = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        for (String raw : withoutPhrases.split("\\s+")) {
            if (raw.isBlank()) {
                continue;
            }
            if (raw.startsWith("-") && raw.length() > 1) {
                excluded.addAll(Tokenizer.tokenize(raw.substring(1)));
            } else {
                required.addAll(Tokenizer.tokenize(raw));
            }
        }

        if (required.isEmpty() && phrases.isEmpty()) {
            return List.of();
        }

        Set<String> candidates = null;
        for (String term : required) {
            candidates = intersect(candidates, documentsFor(term));
        }
        for (List<String> phrase : phrases) {
            candidates = intersect(candidates, documentsMatchingPhrase(phrase));
        }
        if (candidates == null) {
            return List.of();
        }
        for (String term : excluded) {
            candidates.removeAll(documentsFor(term));
        }

        Set<String> scoringTerms = new LinkedHashSet<>(required);
        phrases.forEach(scoringTerms::addAll);

        List<SearchResult> results = new ArrayList<>();
        for (String documentId : candidates) {
            double score = 0.0;
            for (String term : scoringTerms) {
                score += tfIdf(term, documentId);
            }
            results.add(new SearchResult(documents.get(documentId), score));
        }
        results.sort((a, b) -> {
            int byScore = Double.compare(b.score(), a.score());
            return byScore != 0 ? byScore : a.document().id().compareTo(b.document().id());
        });
        return List.copyOf(results);
    }

    private Set<String> intersect(Set<String> current, Set<String> next) {
        if (current == null) {
            return new HashSet<>(next);
        }
        current.retainAll(next);
        return current;
    }

    private Set<String> documentsFor(String term) {
        Map<String, List<Integer>> posting = postings.get(term);
        return posting == null ? Set.of() : new HashSet<>(posting.keySet());
    }

    private Set<String> documentsMatchingPhrase(List<String> phraseTerms) {
        Set<String> matches = new HashSet<>();
        for (String documentId : documentsFor(phraseTerms.get(0))) {
            if (phraseOccursIn(documentId, phraseTerms)) {
                matches.add(documentId);
            }
        }
        return matches;
    }

    /** True when the phrase terms sit at consecutive positions in the document. */
    private boolean phraseOccursIn(String documentId, List<String> phraseTerms) {
        List<Integer> firstPositions = postings.get(phraseTerms.get(0)).get(documentId);
        for (int start : firstPositions) {
            boolean consecutive = true;
            for (int offset = 1; offset < phraseTerms.size(); offset++) {
                List<Integer> positions = postings.get(phraseTerms.get(offset)).get(documentId);
                if (positions == null || !positions.contains(start + offset)) {
                    consecutive = false;
                    break;
                }
            }
            if (consecutive) {
                return true;
            }
        }
        return false;
    }

    private double tfIdf(String term, String documentId) {
        Map<String, List<Integer>> posting = postings.get(term);
        if (posting == null || !posting.containsKey(documentId)) {
            return 0.0;
        }
        double tf = 1 + Math.log(posting.get(documentId).size());
        double idf = Math.log((documentCount() + 1.0) / (posting.size() + 1.0)) + 1;
        return tf * idf;
    }
}
