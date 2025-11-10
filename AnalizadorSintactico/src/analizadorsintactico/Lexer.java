package analizadorsintactico;

import java.io.*;
import java.util.*;

public class Lexer {
    private final String contenido;
    private int lineNumber;

    public Lexer(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n"); // conservamos saltos de línea
        }
        reader.close();
        this.contenido = sb.toString();
        this.lineNumber = 1;
    }

    public void processFile() {
        List<Token> tokens = processLine(contenido);
        printTokens(tokens, 0);
    }

    private List<Token> processLine(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            char currentChar = text.charAt(i);

            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') lineNumber++;
                i++;
                continue;
            }

            if (currentChar == '"') {
                String str = extractString(text, i);
                tokens.add(new Token(TokenType.LITERAL_CADENA, str));
                i += str.length() + 2;
            } else if (Character.isDigit(currentChar) || (currentChar == '-' && i + 1 < text.length() && Character.isDigit(text.charAt(i+1)))) {
                String num = extractNumber(text, i);
                tokens.add(new Token(TokenType.LITERAL_NUM, num));
                i += num.length();
            } else if (Character.isLetter(currentChar)) {
                String word = extractWord(text, i);
                TokenType type = getKeywordType(word);
                if (type != TokenType.ERROR) {
                    tokens.add(new Token(type, word));
                    i += word.length();
                } else {
                    System.err.println("Error léxico en línea " + lineNumber + ": palabra no reconocida '" + word + "'");
                    break;
                }
            } else {
                TokenType type = getTokenType(currentChar);
                if (type != TokenType.ERROR) {
                    tokens.add(new Token(type, String.valueOf(currentChar)));
                    i++;
                } else {
                    System.err.println("Error léxico en línea " + lineNumber + ": carácter inválido '" + currentChar + "'");
                    break;
                }
            }
        }

        return tokens;
    }

    private String extractString(String text, int startIndex) {
        int endIndex = text.indexOf('"', startIndex + 1);
        if (endIndex == -1) return "";
        return text.substring(startIndex + 1, endIndex);
    }

    private String extractNumber(String text, int startIndex) {
        int i = startIndex;
        if (text.charAt(i) == '-') i++; // soporte negativo
        while (i < text.length() && (Character.isDigit(text.charAt(i)) || text.charAt(i) == '.')) i++;
        return text.substring(startIndex, i);
    }

    private String extractWord(String text, int startIndex) {
        int i = startIndex;
        while (i < text.length() && Character.isLetter(text.charAt(i))) i++;
        return text.substring(startIndex, i);
    }

    private TokenType getKeywordType(String word) {
        return switch (word.toLowerCase()) {
            case "true" -> TokenType.PR_TRUE;
            case "false" -> TokenType.PR_FALSE;
            case "null" -> TokenType.PR_NULL;
            default -> TokenType.ERROR;
        };
    }

    private TokenType getTokenType(char currentChar) {
        return switch (currentChar) {
            case '{' -> TokenType.L_LLAVE;
            case '}' -> TokenType.R_LLAVE;
            case '[' -> TokenType.L_CORCHETE;
            case ']' -> TokenType.R_CORCHETE;
            case ':' -> TokenType.DOS_PUNTOS;
            case ',' -> TokenType.COMA;
            default -> TokenType.ERROR;
        };
    }

    private int printTokens(List<Token> tokens, int level) {
        StringBuilder output = new StringBuilder();
        boolean nuevaLinea = true;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            switch (token.getType()) {
                case LITERAL_CADENA -> {
                    if (nuevaLinea) output.append("\t".repeat(level));
                    output.append("STRING");
                    nuevaLinea = false;

                    if (i + 1 < tokens.size() && tokens.get(i + 1).getType() == TokenType.DOS_PUNTOS) {
                        i++;
                        output.append(" DOS_PUNTOS ");

                        if (i + 1 < tokens.size()) {
                            Token valor = tokens.get(i + 1);
                            switch (valor.getType()) {
                                case LITERAL_CADENA -> output.append("STRING");
                                case LITERAL_NUM -> output.append("NUMBER");
                                case PR_TRUE -> output.append("PR_TRUE");
                                case PR_FALSE -> output.append("PR_FALSE");
                                case PR_NULL -> output.append("PR_NULL");
                                case L_LLAVE, L_CORCHETE -> {
                                    output.append(valor.getType().name());
                                    level++;
                                    nuevaLinea = true;

                                    // Array vacío [ ]
                                    if (valor.getType() == TokenType.L_CORCHETE &&
                                        i + 2 < tokens.size() &&
                                        tokens.get(i + 2).getType() == TokenType.R_CORCHETE) {
                                        output.append("\t".repeat(level)).append("\n");
                                    }
                                }
                                default -> output.append(valor.getType().name());
                            }
                            i++;
                        }

                        if (i + 1 < tokens.size() && tokens.get(i + 1).getType() == TokenType.COMA) {
                            output.append(" COMA");
                            i++;
                        }
                        output.append("\n");
                        nuevaLinea = true;
                    }
                }

                case L_LLAVE, L_CORCHETE -> {
                    if (!nuevaLinea) output.append("\n");
                    output.append("\t".repeat(level)).append(token.getType().name()).append("\n");
                    level++;
                    nuevaLinea = true;

                    if (token.getType() == TokenType.L_CORCHETE &&
                        i + 1 < tokens.size() &&
                        tokens.get(i + 1).getType() == TokenType.R_CORCHETE) {
                        output.append("\t".repeat(level)).append("\n");
                    }
                }

                case R_LLAVE, R_CORCHETE -> {
                    level--;
                    if (!nuevaLinea) output.append("\n");
                    output.append("\t".repeat(level)).append(token.getType().name());
                    if (i + 1 < tokens.size() && tokens.get(i + 1).getType() == TokenType.COMA) {
                        output.append(" COMA");
                        i++;
                    }
                    output.append("\n");
                    nuevaLinea = true;
                }

                default -> {
                    if (nuevaLinea) output.append("\t".repeat(level));
                    output.append(token.getType().name());
                    nuevaLinea = false;
                }
            }
        }

        System.out.print(output.toString());
        return level;
    }

    // tokenizeFile() usa el contenido ya cargado en el constructor
    public List<Token> tokenizeFile() {
        List<Token> allTokens = processLine(contenido);
        allTokens.add(new Token(TokenType.EOF, "EOF"));
        return allTokens;
    }
}