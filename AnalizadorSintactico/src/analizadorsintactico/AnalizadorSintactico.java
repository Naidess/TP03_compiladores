package analizadorsintactico;

import java.io.*;
import java.util.*;

/**
 * Programa principal del Analizador Sintáctico y Traductor JSON→XML.
 *
 * Uso:
 *     java -cp src analizadorsintactico.AnalizadorSintactico ruta/archivo.json
 *
 * Funcionalidad:
 * 1. Toma como entrada un archivo JSON simplificado.
 * 2. Genera tokens a través del Lexer.
 * 3. Realiza análisis sintáctico con Parser.
 * 4. Si el análisis es válido, traduce el contenido a XML mediante Traductor.
 * 5. Escribe los resultados en:
 *      - salida.xml  (traducción)
 *      - errores.txt (errores encontrados)
 */
public class AnalizadorSintactico {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Uso: java -cp src analizadorsintactico.AnalizadorSintactico <ruta-archivo.json>");
            return;
        }

        String filePath = args[0];
        String xmlOut = "salida.xml";
        String errOut = "errores.txt";

        try {
            // Analizador léxico
            Lexer lexer = new Lexer(filePath);
            List<Token> tokens = lexer.tokenizeFile();

            // Analizador sintáctico
            Parser parser = new Parser(tokens);
            parser.parse();

            if (parser.isValid()) {
                System.out.println("El archivo es sintácticamente válido.");

                // Traductor JSON → XML
                Traductor traductor = new Traductor(tokens);
                traductor.translateAndWrite(xmlOut, errOut);

                System.out.println("Traducción completada. Archivo XML generado: " + xmlOut);
                System.out.println("Si hubo errores durante la traducción, se guardaron en: " + errOut);
            } else {
                System.out.println("Se encontraron errores sintácticos:");
                for (String e : parser.getErrors()) {
                    System.out.println("- " + e);
                }

                // Guardar los errores también en archivo
                try (BufferedWriter w = new BufferedWriter(new FileWriter(errOut))) {
                    for (String err : parser.getErrors()) {
                        w.write(err);
                        w.newLine();
                    }
                }
                System.out.println("Los errores se guardaron en: " + errOut);
            }

        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        }
    }
}