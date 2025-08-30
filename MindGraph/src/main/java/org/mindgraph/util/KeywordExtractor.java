package org.mindgraph.util;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for extracting keywords (nouns) from text using OpenNLP POS tagger.
 */
public class KeywordExtractor {

    private static POSTaggerME posTagger;

    static {
        try (InputStream modelIn = KeywordExtractor.class.getResourceAsStream("/models/en-pos-maxent.bin")) {
            if (modelIn == null) {
                System.err.println("POS model not found in resources! Keyword extraction will be disabled.");
            } else {
                POSModel model = new POSModel(modelIn);
                posTagger = new POSTaggerME(model);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize POS tagger", e);
        }
    }

    /**
     * Extracts keywords (nouns) from the given text.
     * Returns empty list if POS model is not loaded.
     *
     * @param text Text to extract keywords from.
     * @return A list of unique keywords in lowercase.
     */
    public static List<String> extractKeywords(String text) {
        if (text == null || text.isBlank() || posTagger == null) {
            return Collections.emptyList();
        }

        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text);
        String[] tags = posTagger.tag(tokens);

        Set<String> keywords = new LinkedHashSet<>(); // preserves order
        for (int i = 0; i < tokens.length; i++) {
            // Keep nouns (NN, NNS, NNP, NNPS) and ignore very short words
            if (tags[i].startsWith("NN") && tokens[i].length() > 2) {
                keywords.add(tokens[i].toLowerCase());
            }
        }

        return new ArrayList<>(keywords);
    }

    /**
     * Extracts keywords and returns as a comma-separated string.
     *
     * @param text Text to extract keywords from.
     * @return Keywords as CSV string.
     */
    public static String extractAsCsv(String text) {
        return extractKeywords(text).stream().collect(Collectors.joining(","));
    }
}