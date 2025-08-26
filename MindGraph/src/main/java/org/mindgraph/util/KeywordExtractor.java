package org.mindgraph.util;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KeywordExtractor {
    private static POSTaggerME posTagger;

    static {
        try {
            InputStream modelIn = KeywordExtractor.class.getResourceAsStream("/en-pos-maxent.bin");
            POSModel model = new POSModel(modelIn);
            posTagger = new POSTaggerME(model);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.isBlank()) return keywords;

        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(text);
        String[] tags = posTagger.tag(tokens);

        for (int i = 0; i < tokens.length; i++) {
            // Keep nouns (NN, NNS, NNP, NNPS)
            if (tags[i].startsWith("NN") && tokens[i].length() > 2) {
                keywords.add(tokens[i].toLowerCase());
            }
        }

        return keywords;
    }
}
