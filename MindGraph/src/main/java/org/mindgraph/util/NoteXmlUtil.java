package org.mindgraph.util;

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.mindgraph.model.Note;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class NoteXmlUtil {

    /** Save a Note along with styled content (no keywords in XML) */
    public static void save(Note note, InlineCssTextArea editor, File file) throws Exception {
        if (note == null || editor == null || file == null) return;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xml = dBuilder.newDocument();

        Element root = xml.createElement("note");
        xml.appendChild(root);

        // Metadata (keywords are only in DB, not here)
        Element meta = xml.createElement("metadata");
        root.appendChild(meta);
        append(meta, "id", note.getId() > 0 ? String.valueOf(note.getId()) : "", xml);
        append(meta, "title", note.getTitle(), xml);
        append(meta, "difficulty", String.valueOf(note.getDifficulty()), xml);
        append(meta, "createdAt", safe(note.getCreatedAt()), xml);
        append(meta, "updatedAt", safe(LocalDateTime.now()), xml);

        // Content
        Element contentEl = xml.createElement("content");
        root.appendChild(contentEl);

        for (int i = 0; i < editor.getParagraphs().size(); i++) {
            String paragraphText = editor.getParagraph(i).getText();
            String paragraphStyle = editor.getParagraph(i).getParagraphStyle();

            Element para = xml.createElement("paragraph");
            para.setAttribute("style", paragraphStyle != null ? paragraphStyle : "");
            contentEl.appendChild(para);

            int absStart = editor.getAbsolutePosition(i, 0);
            StyleSpans<String> spans = editor.getStyleSpans(absStart, absStart + paragraphText.length());

            int pos = 0;
            for (StyleSpan<String> span : spans) {
                String css = span.getStyle() != null ? span.getStyle() : "";
                String chunk = paragraphText.substring(pos, pos + span.getLength());
                pos += span.getLength();

                Element node = xml.createElement("c");
                node.setAttribute("style", css);
                node.setTextContent(chunk);
                para.appendChild(node);
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(xml), new StreamResult(file));
    }

    /** Load a Note and restore styled content, then apply keyword links from DB-loaded Note */
    public static void load(Note note, InlineCssTextArea editor, File file) throws Exception {
        if (note == null || editor == null || file == null) return;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xml = dBuilder.parse(file);
        xml.getDocumentElement().normalize();

        Element root = xml.getDocumentElement();
        Element meta = (Element) root.getElementsByTagName("metadata").item(0);

        // restore only metadata needed from XML (DB remains truth for keywords)
        note.setId(parseIntSafe(textOf(meta, "id"), note.getId()));
        note.setTitle(textOf(meta, "title"));
        note.setDifficulty(parseIntSafe(textOf(meta, "difficulty"), note.getDifficulty()));
        note.setCreatedAt(parseDateSafe(textOf(meta, "createdAt")));
        note.setUpdatedAt(parseDateSafe(textOf(meta, "updatedAt")));

        // Restore content
        editor.clear();
        NodeList paras = root.getElementsByTagName("paragraph");
        for (int i = 0; i < paras.getLength(); i++) {
            Element para = (Element) paras.item(i);
            String paragraphStyle = para.getAttribute("style");

            if (i > 0) editor.appendText("\n");

            NodeList runs = para.getChildNodes();
            for (int j = 0; j < runs.getLength(); j++) {
                if (!(runs.item(j) instanceof Element)) continue;
                Element run = (Element) runs.item(j);

                String style = run.getAttribute("style");
                String text = run.getTextContent();

                int start = editor.getLength();
                editor.appendText(text);

                editor.setStyle(start, editor.getLength(), style != null ? style : "");
            }

            editor.setParagraphStyle(i, paragraphStyle != null ? paragraphStyle : "");
        }

        // Apply keyword links (from DB -> Note object)
        applyKeywordLinks(editor, note.getKeywords());
    }

    /** Apply keyword link styling without removing existing styles */
    private static void applyKeywordLinks(InlineCssTextArea editor, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return;

        String content = editor.getText();
        for (String kw : keywords) {
            int idx = 0;
            while ((idx = content.toLowerCase().indexOf(kw.toLowerCase(), idx)) >= 0) {
                int start = idx;
                int end = idx + kw.length();
                String currentStyle = editor.getStyleOfChar(start);
                if (currentStyle == null) currentStyle = "";
                String linkStyle = currentStyle + "; -fx-fill: blue; -fx-underline: true; keyword-link";
                editor.setStyle(start, end, linkStyle);
                idx = end;
            }
        }
    }

    // --- Helpers ---
    private static void append(Element parent, String name, String value, Document xml) {
        Element e = xml.createElement(name);
        e.setTextContent(value != null ? value : "");
        parent.appendChild(e);
    }

    private static String textOf(Element parent, String tag) {
        Node n = parent.getElementsByTagName(tag).item(0);
        return n == null ? "" : n.getTextContent();
    }

    private static String safe(LocalDateTime dt) {
        return dt == null ? "" : dt.toString();
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static LocalDateTime parseDateSafe(String s) {
        try { return LocalDateTime.parse(s); } catch (Exception e) { return LocalDateTime.now(); }
    }
}