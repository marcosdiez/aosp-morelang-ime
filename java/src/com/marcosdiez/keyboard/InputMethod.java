//package com.marcosdiez.keyboard;
package com.marcosdiez.keyboard;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class InputMethod  {
    static class InputPattern {
        private Pattern inputPattern;
        private Pattern contextPattern;
        private String replacement;
        private Boolean altGr;

        public InputPattern(String input, String context, String replacement, Boolean altGr) {
            this.inputPattern = Pattern.compile(input + "$");
            if(context != null) {
                this.contextPattern = Pattern.compile(context + "$");
            } else {
                this.contextPattern = null;
            }
            this.replacement = replacement;
            this.altGr = altGr;
        }

        public InputPattern(String input, String replacement, Boolean altGr) {
            this(input, null, replacement, altGr);
        }

        public InputPattern(String input, String context, String replacement) {
            this(input, context, replacement, false);
        }

        public InputPattern(String input, String replacement) {
            this(input, null, replacement, false);
        }
    }

    private String id;
    private String name;
    private String description;
    private String author;
    private String version;
    private int maxKeyLength;
    private int contextLength;

    private ArrayList<InputPattern> patterns;

    public InputMethod(String id, String name, String description, String author, String version, int contextLenght, int maxKeyLength, ArrayList<InputPattern> patterns) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.version = version;
        this.patterns = patterns;
        this.maxKeyLength = maxKeyLength;
        this.contextLength = contextLenght;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public int getContextLength() {
        return contextLength;
    }

    public int getMaxKeyLength() {
        return maxKeyLength;
    }

    // Implementation of Matcher.replaceAll differs in Android and JDK
    // In JDK, if a group has not been captured, it is replaced with ""
    // In android, it is replaced with the string 'null'
    // This is annoying. Can't really be fixed without fully encapsulating the class
    // So, rewriting replaceAll algorithm
    // So we'll just do backref replacement and pass it back to the original replaceAll
    private String replaceAll(Matcher matcher, String input, String replacement) {
        // Process substitution string to replace group references with groups
        int cursor = 0;
        StringBuilder result = new StringBuilder();

        while (cursor < replacement.length()) {
            char nextChar = replacement.charAt(cursor);
            if (nextChar == '\\') {
                cursor++;
                nextChar = replacement.charAt(cursor);
                result.append(nextChar);
                cursor++;
            } else if (nextChar == '$') {
                // Skip past $
                cursor++;
                // The first number is always a group
                int refNum = (int)replacement.charAt(cursor) - '0';
                if ((refNum < 0)||(refNum > 9))
                    throw new IllegalArgumentException(
                            "Illegal group reference");
                cursor++;

                // Capture the largest legal group string
                boolean done = false;
                while (!done) {
                    if (cursor >= replacement.length()) {
                        break;
                    }
                    int nextDigit = replacement.charAt(cursor) - '0';
                    if ((nextDigit < 0)||(nextDigit > 9)) { // not a number
                        break;
                    }
                    int newRefNum = (refNum * 10) + nextDigit;
                    if (matcher.groupCount() < newRefNum) {
                        done = true;
                    } else {
                        refNum = newRefNum;
                        cursor++;
                    }
                }
                // Append group
                if (matcher.group(refNum) != null)
                    result.append(matcher.group(refNum));
            } else {
                result.append(nextChar);
                cursor++;
            }
        }

        return matcher.replaceAll(result.toString());
    }

    public String transliterate(String input, String context, Boolean altGr) {
       for(InputPattern pattern: patterns) {
           Matcher inputMatcher = pattern.inputPattern.matcher(input);
          if(inputMatcher.find()) {
              if(pattern.contextPattern == null || pattern.contextPattern.matcher(context).find()) {
                  if(pattern.altGr == altGr) {
                      return replaceAll(inputMatcher,  input, pattern.replacement);
                  }
              }
          }
       }
       return input;
    }

    public static int firstDivergence(String str1, String str2) {
        int length = str1.length() > str2.length() ? str2.length() : str1.length();
        for(int i = 0; i < length; i++) {
            if(str1.charAt(i) != str2.charAt(i)) {
                return i;
            }
        }
        return length - 1; // Default
    }

    public String transliterateAll(String input, ArrayList<Boolean> altGr) {
        String curOutput = "";
        String replacement;
        String context = "";
        boolean curAltGr = false;
        for(int i=0; i < input.length(); i++) {
            String c = String.valueOf(input.charAt(i));
            int startPos = curOutput.length() > this.getMaxKeyLength() ? curOutput.length() - this.getMaxKeyLength() : 0;
            String toReplace = curOutput.substring(startPos) + c;
            curAltGr = altGr != null && altGr.size() > i && altGr.get(i);
            replacement = this.transliterate(toReplace, context, curAltGr);
            int divIndex = InputMethod.firstDivergence(toReplace, replacement);
            replacement = replacement.substring(divIndex);
            curOutput = curOutput.substring(0, startPos + divIndex)  + replacement;

            context += c;
            if(context.length() > this.getContextLength()) {
                context = context.substring(context.length() - this.getContextLength());
            }
        }
        return curOutput;
    }



    public static InputMethod fromName(String name) throws IllegalArgumentException {
        InputStream stream = InputMethod.class.getClassLoader().getResourceAsStream("com/marcosdiez/keyboard/res/" + name + ".xml");
            return fromFile(stream);
    }
    public static InputMethod fromFile(InputStream input) throws IllegalArgumentException {
        if(input == null) {
            throw new IllegalArgumentException("No such input method exists!");
        }
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        } catch (SAXException e) {
            // Since this shouldn't happen, let me semi-swallow this into a runtime exception
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            // Since this shouldn't happen, let me semi-swallow this into a runtime exception
            throw new RuntimeException(e);
        } catch (IOException e) {
            // Since this shouldn't happen, let me semi-swallow this into a runtime exception
            throw new RuntimeException(e);
        }
        Node root = doc.getDocumentElement();
        String id = root.getAttributes().getNamedItem("id").getTextContent();
        String name = root.getAttributes().getNamedItem("name").getTextContent();
        String description = root.getAttributes().getNamedItem("description").getTextContent();
        String author = root.getAttributes().getNamedItem("author").getTextContent();
        String version = root.getAttributes().getNamedItem("version").getTextContent();
        int maxKeyLength = Integer.parseInt(root.getAttributes().getNamedItem("maxKeyLength").getTextContent());
        int contextLength = Integer.parseInt(root.getAttributes().getNamedItem("contextLength").getTextContent());

        ArrayList<InputPattern> patterns = new ArrayList<InputMethod.InputPattern>();
        NodeList patternsXML = doc.getElementsByTagName("pattern");
        for(int i=0; i< patternsXML.getLength(); i++) {
           NamedNodeMap attribs = patternsXML.item(i).getAttributes();
           InputPattern p;
           String inputPattern = attribs.getNamedItem("input").getTextContent();
           String replacement = attribs.getNamedItem("replacement").getTextContent();
           String context = null;
           Boolean altGr = false;
           if(attribs.getNamedItem("context") != null) {
              context = attribs.getNamedItem("context").getTextContent();
           }
           if(attribs.getNamedItem("altGr") != null) {
               altGr = attribs.getNamedItem("altGr").getTextContent().equals("true");
           }
           p = new InputPattern(inputPattern, context, replacement, altGr);
           patterns.add(p);
        }
        return new InputMethod(id, name, description, author, version, contextLength, maxKeyLength, patterns);
    }
}
