package analizadorsintactico;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;
    private Token current;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = tokens.get(0);
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

    private void error(String msg) {
        errors.add("Error en token '" + current.getValue() + "' (tipo " + current.getType() + "): " + msg);
    }

    private void synchronize(Set<TokenType> syncSet) {
        while (current.getType() != TokenType.EOF && !syncSet.contains(current.getType())) {
            advance();
        }
    }

    public void parse() {
        json();
        if (current.getType() != TokenType.EOF) {
            error("Se esperaba EOF al final del archivo");
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    // json → element EOF
    private void json() {
        element();
    }

    // element → object | array
    private void element() {
        if (current.getType() == TokenType.L_LLAVE) {
            object();
        } else if (current.getType() == TokenType.L_CORCHETE) {
            array();
        } else {
            error("Se esperaba un objeto '{' o un arreglo '[' (element)");
            synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.R_LLAVE, TokenType.EOF));
        }
    }

    // array → [ element-list ] | []
    private void array() {
        if (!match(TokenType.L_CORCHETE)) return;
        if (current.getType() == TokenType.R_CORCHETE) {
            match(TokenType.R_CORCHETE);
            return;
        }
        elementList();
        if (!match(TokenType.R_CORCHETE)) {
            error("Se esperaba ']' al finalizar el arreglo");
            synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.R_LLAVE, TokenType.EOF));
            if (current.getType() == TokenType.R_CORCHETE) advance();
        }
    }

    // element-list → element ( , element )*
    private void elementList() {
        element();
        while (current.getType() == TokenType.COMA) {
            match(TokenType.COMA);
            if (current.getType() != TokenType.L_LLAVE && current.getType() != TokenType.L_CORCHETE) {
                error("Se esperaba inicio de elemento después de ',' en element-list");
                synchronize(Set.of(TokenType.COMA, TokenType.R_CORCHETE, TokenType.R_LLAVE, TokenType.EOF));
                if (current.getType() == TokenType.R_CORCHETE) return;
            } else {
                element();
            }
        }
    }

    // object → { attributes-list } | {}
    private void object() {
        if (!match(TokenType.L_LLAVE)) return;
        if (current.getType() == TokenType.R_LLAVE) {
            match(TokenType.R_LLAVE);
            return;
        }
        attributesList();
        if (!match(TokenType.R_LLAVE)) {
            error("Se esperaba '}' al finalizar el objeto");
            synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.R_CORCHETE, TokenType.EOF));
            if (current.getType() == TokenType.R_LLAVE) advance();
        }
    }

    // attributes-list → attribute ( , attribute )*
    private void attributesList() {
        attribute();
        while (current.getType() == TokenType.COMA) {
            match(TokenType.COMA);
            if (current.getType() != TokenType.LITERAL_CADENA) {
                error("Se esperaba STRING (nombre de atributo) después de ','");
                synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
                if (current.getType() == TokenType.R_LLAVE) return;
            } else {
                attribute();
            }
        }
    }

    // attribute → attribute-name : attribute-value
    private void attribute() {
        attributeName();
        if (!match(TokenType.DOS_PUNTOS)) {
            error("Se esperaba ':' después del nombre del atributo");
            synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
            return;
        }
        attributeValue();
    }

    private void attributeName() {
        if (current.getType() == TokenType.LITERAL_CADENA) {
            match(TokenType.LITERAL_CADENA);
        } else {
            error("Se esperaba STRING como nombre del atributo");
            synchronize(Set.of(TokenType.DOS_PUNTOS, TokenType.COMA, TokenType.R_LLAVE, TokenType.EOF));
        }
    }

    // attribute-value → element | string | number | true | false | null
    private void attributeValue() {
        TokenType t = current.getType();
        switch (t) {
            case L_LLAVE -> object();
            case L_CORCHETE -> array();
            case LITERAL_CADENA -> match(TokenType.LITERAL_CADENA);
            case LITERAL_NUM -> match(TokenType.LITERAL_NUM);
            case PR_TRUE -> match(TokenType.PR_TRUE);
            case PR_FALSE -> match(TokenType.PR_FALSE);
            case PR_NULL -> match(TokenType.PR_NULL);
            default -> {
                error("Valor de atributo inválido. Se esperaba objeto, arreglo, string, number, true, false o null");
                synchronize(Set.of(TokenType.COMA, TokenType.R_LLAVE, TokenType.R_CORCHETE, TokenType.EOF));
            }
        }
    }
}
