package com.subhanmalik.invertedindex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * CLI: indexes every {@code .txt} file in a directory and runs one query.
 *
 * <pre>
 *   java -jar inverted-index.jar &lt;docs-dir&gt; &lt;query...&gt;
 * </pre>
 *
 * The document id is the file name without its extension, and the whole
 * file is the document text. See {@link InvertedIndex} for the query
 * language: AND terms, {@code "exact phrases"}, and {@code -exclusions}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java -jar inverted-index.jar <docs-dir> <query...>");
            System.err.println("Example: java -jar inverted-index.jar examples \"quick brown\" -fox");
            System.exit(1);
        }

        Path dir = Path.of(args[0]);
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir);
            System.exit(1);
        }

        InvertedIndex index = new InvertedIndex();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.toString().endsWith(".txt"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String stem = path.getFileName().toString().replaceFirst("\\.txt$", "");
                            index.addDocument(new Document(stem, stem,
                                    Files.readString(path, StandardCharsets.UTF_8)));
                        } catch (IOException e) {
                            throw new IllegalStateException("Cannot read " + path, e);
                        }
                    });
        }

        String query = String.join(" ", List.of(args).subList(1, args.length));
        List<SearchResult> results = index.search(query);
        System.out.printf("Indexed %d document(s), %d term(s).%n",
                index.documentCount(), index.indexedTermCount());
        System.out.printf("%d result(s) for \"%s\":%n", results.size(), query);
        for (SearchResult result : results) {
            String snippet = result.document().text().replaceAll("\\s+", " ").trim();
            if (snippet.length() > 160) {
                snippet = snippet.substring(0, 157) + "...";
            }
            System.out.printf("[%.4f] %s - %s%n", result.score(), result.document().id(), snippet);
        }
    }
}
