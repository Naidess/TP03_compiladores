package analizadorsintactico;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Traductor {
    private final List<Token> tokens;
    private int pos = 0;
    private Token current;
    private final List<String> errors = new ArrayList<>();
    private final StringBuilder xml = new StringBuilder();
    private int indent = 0;

    public Traductor(List<Token> tokens) {
        this.tokens = tokens;
        if (tokens == null || tokens.isEmpty()) {
            this.current = new Token(TokenType.EOF, "EOF");
        } else {
            this.current = tokens.get(0);
        }
    }

    private void advance() {
        if (pos < tokens.size() - 1) {
            pos++;
            current = tokens.get(pos);
        }
    }

    private boolean match(TokenType type) {
        if (current.getType() == type) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        return current.getType() == type;
    }

    private void error(String msg) {
        errors.add(String.format("Error en token '%s' (tipo %s): %s",
                current.getValue(), current.getType(), msg));
    }

    private void synchronize(Set<TokenType> syncSet) {
        while (current.getType() != TokenType.EOF && !syncSet.contains(current.getType())) {
            advance();
        }
    }

    private void pushIndent() {
        indent++;
    }

    private void popIndent() {
        if (indent > 0) indent--;
    }

    private void appendIndent() {
        for (int i = 0; i < indent; i++) xml.append("\t");
    }

    public void translateAndWrite(String outputXmlPath, String outputErrPath) {
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<root>\n");
        pushIndent();

        json();

        popIndent();
        xml.append("</root>\n");

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputXmlPath), StandardCharsets.UTF_8))) {
            w.write(xml.toString());
        } catch (IOException e) {
            System.err.println("Error al escribir XML: " + e.getMessage());
        }

        if (!errors.isEmpty()) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(outputErrPath))) {
                for (String err : errors) {
                    w.write(err);
                    w.write(System.lineSeparator());
                }
            } catch (IOException e) {
                System.err.println("Error al escribir archivo de errores: " + e.getMessage());
            }
        }
    }

    private void json() {
        element();
        if (current.getType() != TokenType.EOF) {
            error("Se esperaba EOF al finalizar JSON");
            synchronize(Set.of(TokenType.EOF));
        }
    }

    // element → object | array
    private void element() {
        if (check(TokenType.L_LLAVE)) {
            objectAsContent(null); 
        } else if (check(TokenType.L_CORCHETE)) {
            arrayAsContent(null);
        } else {
            error("Se esperaba un objeto '{' o un arreglo '[' (element)");
            synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.R_LLAVE, TokenType.EOF));
        }
    }

    private void objectAsContent(String keyName) {
        if (!match(TokenType.L_LLAVE)) return;

        if (keyName != null) {
            appendIndent();
            String tag = sanitizeXmlName(keyName);
            xml.append("<").append(tag).append(">").append("\n");
            pushIndent();
        }

        if (check(TokenType.R_LLAVE)) {
            match(TokenType.R_LLAVE);
        } else {
            attributesList();
            if (!match(TokenType.R_LLAVE)) {
                error("Se esperaba '}' al finalizar el objeto");
                synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.R_CORCHETE, TokenType.EOF));
                if (check(TokenType.R_LLAVE)) advance();
            }
        }

        if (keyName != null) {
            popIndent();
            appendIndent();
            String tag = sanitizeXmlName(keyName);
            xml.append("</").append(tag).append(">").append("\n");
        }
    }

    private void attributesList() {
        attribute();
        while (check(TokenType.COMA)) {
            match(TokenType.COMA);
            if (!check(TokenType.LITERAL_CADENA)) {
                error("Se esperaba STRING (nombre de atributo) después de ','");
                synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
                if (check(TokenType.R_LLAVE)) return;
            } else {
                attribute();
            }
        }
    }

    private void attribute() {
        String attrName = null;
        if (check(TokenType.LITERAL_CADENA)) {
            attrName = current.getValue();
            match(TokenType.LITERAL_CADENA);
        } else {
            error("Se esperaba STRING como nombre del atributo");
            synchronize(Set.of(TokenType.DOS_PUNTOS, TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
            if (!check(TokenType.DOS_PUNTOS)) return;
        }

        if (!match(TokenType.DOS_PUNTOS)) {
            error("Se esperaba ':' después del nombre del atributo");
            synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
            return;
        }

        TokenType t = current.getType();
        switch (t) {
            case L_LLAVE:
                objectAsContent(attrName);
                break;
            case L_CORCHETE:
                arrayAsContent(attrName);
                break;
            case LITERAL_CADENA:
            case LITERAL_NUM:
            case PR_TRUE:
            case PR_FALSE:
            case PR_NULL:
                String value = current.getValue();
                match(t);
                appendIndent();
                String tag = sanitizeXmlName(attrName);
                xml.append("<").append(tag).append(">");
                xml.append(escapeXml(value));
                xml.append("</").append(tag).append(">").append("\n");
                break;
            default:
                error("Valor de atributo inválido. Se esperaba objeto, arreglo, string, number, true, false o null");
                synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.R_CORCHETE, TokenType.EOF));
        }
    }

    private void arrayAsContent(String keyName) {
        if (!match(TokenType.L_CORCHETE)) return;

    String wrapper = keyName == null ? "array" : keyName;
    String wrapperTag = sanitizeXmlName(wrapper);
    appendIndent();
    xml.append("<").append(wrapperTag).append(">").append("\n");
        pushIndent();

        // empty array
        if (check(TokenType.R_CORCHETE)) {
            match(TokenType.R_CORCHETE);
        } else {
            elementListInArray();
            if (!match(TokenType.R_CORCHETE)) {
                error("Se esperaba ']' al finalizar el arreglo");
                synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.R_LLAVE, TokenType.EOF));
                if (check(TokenType.R_CORCHETE)) advance();
            }
        }

        popIndent();
        appendIndent();
        xml.append("</").append(wrapperTag).append(">").append("\n");
    }

    private void elementListInArray() {
        emitArrayElement();
        while (check(TokenType.COMA)) {
            match(TokenType.COMA);
            if (check(TokenType.R_CORCHETE)) {
                error("Elemento esperado después de ',' en array");
                return;
            }
            emitArrayElement();
        }
    }

    private void emitArrayElement() {
        appendIndent();
        xml.append("<item>").append("\n");
        pushIndent();

        if (check(TokenType.L_LLAVE)) {
            objectAsContent(null); 
        } else if (check(TokenType.L_CORCHETE)) {
            arrayAsContent(null);
        } else if (check(TokenType.LITERAL_CADENA) || check(TokenType.LITERAL_NUM)
                || check(TokenType.PR_TRUE) || check(TokenType.PR_FALSE) || check(TokenType.PR_NULL)) {
            String v = current.getValue();
            match(current.getType());
            appendIndent();
            xml.append(escapeXml(v)).append("\n");
        } else {
            error("Se esperaba un elemento válido dentro del array");
            synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.EOF));
        }

        popIndent();
        appendIndent();
        xml.append("</item>").append("\n");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
    }

    /**
     * Sanitiza un nombre de etiqueta para que sea un nombre XML válido.
     * Reemplaza caracteres inválidos por '_' y asegura que no comience con dígito.
     */
    private String sanitizeXmlName(String s) {
        if (s == null || s.isEmpty()) return "key";
        // Escapar y luego normalizar: eliminamos espacios y caracteres no permitidos
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((i == 0 && (Character.isLetter(c) || c == '_')) || (i > 0 && (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.'))) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        // Si el primer carácter no es letra o '_' hacer un prefijo
        if (!Character.isLetter(sb.charAt(0)) && sb.charAt(0) != '_') {
            return "k_" + sb.toString();
        }
        return sb.toString();
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getXml() {
        return xml.toString();
    }
}