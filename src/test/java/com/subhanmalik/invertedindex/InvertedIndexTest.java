package com.subhanmalik.invertedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvertedIndexTest {

    private InvertedIndex index;

    @BeforeEach
    void setUp() {
        index = new InvertedIndex();
        index.addDocument(new Document("fox", "Fox", "The quick brown fox jumps over the lazy dog"));
        index.addDocument(new Document("cats", "Cats", "Quick cats and quick dogs chase the brown fox"));
        index.addDocument(new Document("ocean", "Ocean", "Deep ocean waves roll under a quiet sky"));
    }

    private List<String> ids(List<SearchResult> results) {
        return results.stream().map(result -> result.document().id()).toList();
    }

    @Test
    void singleTermFindsMatchingDocuments() {
        assertEquals(List.of("cats", "fox"), ids(index.search("brown")).stream().sorted().toList());
    }

    @Test
    void unknownTermReturnsNothing() {
        assertTrue(index.search("zebra").isEmpty());
    }

    @Test
    void emptyAndBlankQueriesReturnNothing() {
        assertTrue(index.search("").isEmpty());
        assertTrue(index.search("   ").isEmpty());
        assertTrue(index.search(null).isEmpty());
    }

    @Test
    void multipleTermsAreAnded() {
        assertEquals(List.of("fox"), ids(index.search("quick brown jumps")));
        assertEquals(List.of("cats"), ids(index.search("cats dogs")));
    }

    @Test
    void searchIsCaseInsensitive() {
        assertEquals(ids(index.search("QUICK")), ids(index.search("quick")));
    }

    @Test
    void repeatedTermRanksHigher() {
        // "cats" contains "quick" twice; "fox" contains it once.
        List<SearchResult> results = index.search("quick");
        assertEquals("cats", results.get(0).document().id());
        assertTrue(results.get(0).score() > results.get(1).score());
    }

    @Test
    void exactPhraseMatchesOnlyConsecutiveTerms() {
        assertEquals(List.of("fox"), ids(index.search("\"quick brown fox\"")));
        // "brown" and "fox" are consecutive in "fox" but not in "cats"
        // ("brown fox" appears at the end of "cats" too — both match).
        assertEquals(List.of("cats", "fox"),
                ids(index.search("\"brown fox\"")).stream().sorted().toList());
        assertTrue(index.search("\"fox quick\"").isEmpty());
    }

    @Test
    void exclusionRemovesMatchingDocuments() {
        assertEquals(List.of("fox"), ids(index.search("quick -cats")));
        assertTrue(index.search("quick -cats -fox").isEmpty());
    }

    @Test
    void stopWordsAreNotIndexed() {
        assertTrue(index.search("the").isEmpty());
    }

    @Test
    void removingDocumentUpdatesResultsAndTermCounts() {
        assertTrue(index.removeDocument("fox"));
        assertEquals(List.of("cats"), ids(index.search("brown")));
        assertTrue(index.search("\"quick brown fox\"").isEmpty());
    }

    @Test
    void removingUnknownDocumentReturnsFalse() {
        assertTrue(!index.removeDocument("missing"));
    }

    @Test
    void reAddingSameIdReplacesTheDocument() {
        index.addDocument(new Document("ocean", "Ocean", "Cats sleep on warm roofs"));
        assertEquals(List.of("cats", "ocean"),
                ids(index.search("cats")).stream().sorted().toList());
        assertTrue(index.search("ocean").isEmpty());
    }

    @Test
    void phraseCombinedWithTermAndExclusion() {
        assertEquals(List.of("fox"), ids(index.search("\"brown fox\" quick -cats")));
    }

    @Test
    void documentAndTermCounts() {
        assertEquals(3, index.documentCount());
        assertTrue(index.indexedTermCount() > 0);
    }
}
