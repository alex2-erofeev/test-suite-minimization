package ru.erofeev.fl.parser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class PitestParser {
    /**
     * Возвращает строку формата: "killed/total"
     * Использование: java -cp app.jar ru.erofeev.fl.parser.PitestParser <mutations.xml>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: PitestParser <mutations.xml>");
            System.exit(1);
        }
        
        File xmlFile = new File(args[0]);
        if (!xmlFile.exists()) {
            System.out.println("0/0");
            return;
        }

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);
        NodeList mutations = doc.getElementsByTagName("mutation");
        
        int total = mutations.getLength();
        int killed = 0;
        
        for (int i = 0; i < total; i++) {
            Element m = (Element) mutations.item(i);
            if ("KILLED".equals(m.getAttribute("status"))) {
                killed++;
            }
        }
        
        System.out.println(killed + "/" + total);
    }
}