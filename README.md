# Inverted Index Search Engine (Java)

A small, dependency-free search engine core: index a set of documents,
then answer ranked queries over them. The piece of a real search engine
that matters most — the inverted index itself — with the ranking and
query features built directly on top of it.

## What it does

- Indexes documents into term → posting lists (document + positions).
- Answers queries with three clause types, combinable in one query:
  - `cats dogs` — documents containing **both** terms (AND).
  - `"quick brown fox"` — the exact phrase, in order.
  - `cats -dogs` — documents with "cats" but without "dogs".
- Ranks results by TF-IDF, so a document that mentions a query term
  repeatedly, or mentions a rare term, scores above one that mentions a
  common term once.
- Ships a CLI that indexes a directory of `.txt` files and runs a query.

## Design decisions

1. **Positions in the posting lists, not just document ids.** Storing
   where each term occurs costs a little memory but is what makes phrase
   search possible without re-scanning document text: a phrase matches
   only when its terms sit at consecutive positions.
2. **AND as the default, not OR.** For a small engine, a query's terms
   are usually all meant to be present; OR would flood results with
   documents matching a single common term. Exclusion (`-term`) covers
   the common "but not this" case.
3. **Smoothed TF-IDF instead of raw counts.** Scoring is
   `(1 + log(tf)) * (log((N + 1) / (df + 1)) + 1)`. The log on term
   frequency stops a document that repeats a word 50 times from
   dominating, and the `+1` smoothing keeps terms present in every
   document from scoring exactly zero.
4. **A small stop-word list, dropped at index time.** Words like "the"
   and "and" would otherwise bloat every posting list. The trade-off is
   documented in `Tokenizer`: phrase queries containing a stop word
   cannot match, because the stop word was never indexed.

## How to run

Requires JDK 17 or later, no other dependencies.

```bash
./build.sh
java -jar out/inverted-index.jar examples "quick brown"
java -jar out/inverted-index.jar examples '"brown fox" -cats'
```

To search your own documents, put `.txt` files in a directory and pass
it instead of `examples` — the file name (without extension) becomes
the document id.

## How to run tests

```bash
./build.sh test
```

17 JUnit 5 tests cover tokenization, AND/phrase/exclusion queries,
TF-IDF ranking order, stop-word handling, and document removal and
replacement. The build script downloads the JUnit console-standalone jar
from Maven Central on first use if it is not already present.

## Tech stack

- Java 17+ (developed against JDK 21), standard library only
- JUnit 5 for tests
- No build tool required — `build.sh` uses `javac`/`jar` directly

## Project structure

```
src/main/java/com/subhanmalik/invertedindex/
  InvertedIndex.java   # index, query parsing, TF-IDF ranking
  Tokenizer.java       # lower-casing, punctuation split, stop words
  Document.java        # document record
  SearchResult.java    # ranked-hit record
  Main.java            # CLI: index a directory, run one query
src/test/java/...      # JUnit 5 tests
examples/              # three sample .txt documents
```

## License

MIT — see `LICENSE`.
