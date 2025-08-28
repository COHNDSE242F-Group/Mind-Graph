package org.mindgraph.util;

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.mindgraph.model.Note;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDateTime;

/**
 * Utility for saving/loading Notes with styled content (InlineCssTextArea)
 */
public class NoteXmlUtil {

    /** Save a Note along with styled content */
    public static void save(Note note, InlineCssTextArea editor, File file) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xml = dBuilder.newDocument();

        Element root = xml.createElement("note");
        xml.appendChild(root);

        // Metadata
        Element meta = xml.createElement("metadata");
        root.appendChild(meta);

        Element id = xml.createElement("id"); id.setTextContent(note.getId()); meta.appendChild(id);
        Element title = xml.createElement("title"); title.setTextContent(note.getTitle()); meta.appendChild(title);
        Element diff = xml.createElement("difficulty"); diff.setTextContent(String.valueOf(note.getDifficulty())); meta.appendChild(diff);
        Element created = xml.createElement("createdAt"); created.setTextContent(note.getCreatedAt().toString()); meta.appendChild(created);
        Element updated = xml.createElement("updatedAt"); updated.setTextContent(note.getUpdatedAt().toString()); meta.appendChild(updated);

        // Content
        Element contentEl = xml.createElement("content");
        root.appendChild(contentEl);

        for (int i = 0; i < editor.getParagraphs().size(); i++) {
            String paragraphText = editor.getParagraph(i).getText();
            String paragraphStyle = editor.getParagraph(i).getParagraphStyle();

            Element para = xml.createElement("paragraph");
            para.setAttribute("style", paragraphStyle != null ? paragraphStyle : "");

            // Per-character CSS
            StringBuilder sb = new StringBuilder();
            StyleSpans<String> spans = editor.getStyleSpans(editor.getAbsolutePosition(i, 0), editor.getParagraphLength(i));
            int pos = 0;
            for (var span : spans) {
                String css = span.getStyle(); // now just String
                for (int j = 0; j < span.getLength(); j++) {
                    char ch = paragraphText.charAt(pos++);
                    sb.append("<c style=\"").append(escapeXml(css)).append("\">")
                            .append(escapeXml(ch))
                            .append("</c>");
                }
            }

            para.setTextContent(sb.toString());
            contentEl.appendChild(para);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(new DOMSource(xml), new StreamResult(file));
    }

    /** Load a Note and restore styled content */
    public static void load(Note note, InlineCssTextArea editor, File file) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document xml = dBuilder.parse(file);
        xml.getDocumentElement().normalize();

        Element root = xml.getDocumentElement();
        Element meta = (Element) root.getElementsByTagName("metadata").item(0);

        note.setId(meta.getElementsByTagName("id").item(0).getTextContent());
        note.setTitle(meta.getElementsByTagName("title").item(0).getTextContent());
        note.setDifficulty(Integer.parseInt(meta.getElementsByTagName("difficulty").item(0).getTextContent()));
        note.setCreatedAt(LocalDateTime.parse(meta.getElementsByTagName("createdAt").item(0).getTextContent()));
        note.setUpdatedAt(LocalDateTime.parse(meta.getElementsByTagName("updatedAt").item(0).getTextContent()));

        editor.clear();

        NodeList paras = root.getElementsByTagName("paragraph");
        for (int i = 0; i < paras.getLength(); i++) {
            Element para = (Element) paras.item(i);
            String paragraphStyle = para.getAttribute("style");
            String raw = para.getTextContent();

            if (i > 0) editor.appendText("\n");

            // Parse <c style="...">x</c>
            while (raw.contains("<c style=\"")) {
                int idx = raw.indexOf("<c style=\"");
                int startStyle = idx + 10;
                int endStyle = raw.indexOf("\">", startStyle);
                int endTag = raw.indexOf("</c>", endStyle);
                if (endStyle < 0 || endTag < 0) break;

                String style = unescapeXml(raw.substring(startStyle, endStyle));
                char ch = raw.charAt(endStyle + 2);

                editor.appendText(String.valueOf(ch));
                editor.setStyle(editor.getLength() - 1, editor.getLength(), style);

                raw = raw.substring(endTag + 4);
            }

            editor.setParagraphStyle(i, paragraphStyle);
        }
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String escapeXml(char c) {
        switch (c) {
            case '&': return "&amp;";
            case '<': return "&lt;";
            case '>': return "&gt;";
            case '"': return "&quot;";
            case '\'': return "&apos;";
            default: return String.valueOf(c);
        }
    }

    private static String unescapeXml(String s) {
        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }
}