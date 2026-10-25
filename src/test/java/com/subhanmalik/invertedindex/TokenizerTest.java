package com.subhanmalik.invertedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TokenizerTest {

    @Test
    void lowercasesAndSplitsOnPunctuation() {
        assertEquals(List.of("hello", "world", "42"),
                Tokenizer.tokenize("Hello, WORLD! 42."));
    }

    @Test
    void dropsStopWords() {
        assertEquals(List.of("quick", "fox"), Tokenizer.tokenize("the quick fox"));
    }

    @Test
    void emptyInputYieldsNoTokens() {
        assertEquals(List.of(), Tokenizer.tokenize(""));
        assertEquals(List.of(), Tokenizer.tokenize(null));
        assertEquals(List.of(), Tokenizer.tokenize("the and of"));
    }
}
