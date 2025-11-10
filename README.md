# Traductor JSON a XML con Analizador Sintáctico LL(1)

Este proyecto implementa un traductor de archivos JSON a XML, basado en un analizador sintáctico descendente recursivo (LL(1)).
El programa valida la estructura del archivo JSON y genera un archivo XML equivalente si la sintaxis es correcta.

---

## Tecnologías Utilizadas

- **Lenguaje:** Java  
- **IDE:** NetBeans (utilizado para el desarrollo)  
- **Herramientas:** Terminal

---

## Requerimientos

- **Java JDK 17** o superior  
- **Terminal (CMD o Bash)** para la ejecución  
- Archivos de entrada con formato **JSON válido o de prueba**

---

## Archivos Principales

El proyecto está compuesto por los siguientes archivos:

- `Main.java` → Punto de entrada del programa  
- `Lexer.java` → Analizador léxico: genera tokens a partir del archivo JSON  
- `Parser.java` → Analizador sintáctico: valida la estructura del JSON  
- `Token.java` → Clase que representa los tokens  
- `TokenType.java` → Enumeración de tipos de token reconocidos  
- `Traductor.java` → Traduce el árbol sintáctico JSON a XML utilizando **Panic Mode** para recuperación de errores.
- `fuente.txt` → Archivo de ejemplo a analizar

---

## Instrucciones de Uso

### 1. Clonar el Repositorio

```bash
git clone https://github.com/Naidess/TP03_compiladores.git
```

### 2. Mover a la carpeta 

```bash
cd .\AnalizadorSintactico\
```

### 3. Compilar el Proyecto

```bash
javac -d . src/analizadorsintactico/*.java
```

### 4. Ejecutar el Analizador

```bash
java analizadorsintactico.AnalizadorSintactico .\src\fuente.txt
```
- `fuente.txt` → nombre del archivo JSON de ejemplo a analizar.
El programa genera dos archivos:
- `salida.xml` → resultado de la traducción
- `errores.err` → lista de errores encontrados (si los hay)
---
## Funcionamiento Interno

1. `Lexer.java` analiza el archivo y genera una lista de tokens.
2. `Parser.java` aplica la gramática JSON simplificada para validar la estructura.
3. Si se detectan errores, el analizador utiliza métodos de sincronización (Panic Mode) para continuar con el análisis sin detenerse abruptamente.
4. Finalmente, muestra si el archivo es válido o detalla los errores encontrados.

---
## Gramática Simplificada
```bash
json → element EOF
element → object | array
object → { attributes-list } | {}
attributes-list → attribute ( , attribute )*
attribute → string : attribute-value
attribute-value → element | string | number | true | false | null
array → [ element-list ] | []
element-list → element ( , element )*
```