package Clases;

import Analizador.ErrorC;
import Analizador.Token;
import Interfaz.InterfazCoronix;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class ClasePrincipal {

    public static TablaSimbolos tabla = new TablaSimbolos();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(InterfazCoronix::new);
    }

    // ─────────────────────────────────────────────────────────────
    // Resultado de procesar una línea: texto de consola + tipo
    // ─────────────────────────────────────────────────────────────
    public enum TipoResultado { OK, BLOQUE_INICIO, BLOQUE_FIN, VACIO, ERROR }

    public static class ResultadoLinea {
        public final TipoResultado tipo;
        public final String texto;
        public ResultadoLinea(TipoResultado tipo, String texto) {
            this.tipo = tipo;
            this.texto = texto;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Procesa una línea completa del lenguaje Coronix
    // ─────────────────────────────────────────────────────────────
    public static String procesarLinea(String linea, int ln) throws ErrorC {
        return procesarLineaCompleta(linea, ln).texto;
    }

    public static ResultadoLinea procesarLineaCompleta(String linea, int ln) throws ErrorC {

        // Ignorar comentarios
        if (linea.startsWith("//")) return new ResultadoLinea(TipoResultado.VACIO, "");

        // Línea vacía
        if (linea.isEmpty()) return new ResultadoLinea(TipoResultado.VACIO, "");

        // Cierre de bloque
        if (linea.equals("}")) return new ResultadoLinea(TipoResultado.BLOQUE_FIN, "}");

        // Bloques try { } e if ( cond ) { — detectar inicio de bloque
        if (linea.startsWith("try") && linea.endsWith("{")) {
            return new ResultadoLinea(TipoResultado.BLOQUE_INICIO, "(bloque) " + linea);
        }
        if (linea.startsWith("try")) {
            // try sin { — error sintáctico
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln, "Se esperaba '{' después de 'try'");
        }

        // Bloque if
        if (linea.startsWith("if")) {
            return procesarIf(linea, ln);
        }

        // Sentencia normal: debe terminar con !! o !! break
        if (!linea.endsWith("!!") && !linea.endsWith("!! break")) {
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Falta '!!' al final de la sentencia");
        }

        // Quitar break y !!
        boolean tieneBreak = linea.endsWith("!! break");
        linea = linea.replace("!! break", "").replace("!!", "").trim();

        String[] partes = linea.split("=>", 2);
        if (partes.length != 2)
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln, "Falta '=>'");

        String[] izq = partes[0].trim().split("\\s+");
        if (izq.length != 2)
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln, "Declaración inválida");

        String tipo   = izq[0].trim();
        String nombre = izq[1].trim();

        if (nombre.length() > 10)
            throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "El identificador '" + nombre + "' supera 10 caracteres");

        String expr = partes[1].trim();

        if (expr.isEmpty())
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Falta expresión después de '=>'");

        String resultado = switch (tipo) {
            case "cuarto" -> procesarCuarto(nombre, expr, ln, tieneBreak);
            case "media"  -> procesarMedia (nombre, expr, ln, tieneBreak);
            case "mega"   -> procesarMega  (nombre, expr, ln, tieneBreak);
            default -> throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "Tipo desconocido: '" + tipo + "'");
        };
        return new ResultadoLinea(TipoResultado.OK, resultado);
    }

    // ─────────────────────────────────────────────────────────────
    // Procesa un if — evalúa la condición y devuelve estado de bloque
    // ─────────────────────────────────────────────────────────────
    public static ResultadoLinea procesarIf(String linea, int ln) throws ErrorC {
        // Formato: if ( <expr> <op> <expr> ) {
        if (!linea.endsWith("{"))
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Se esperaba '{' al final del 'if'");

        // Extraer la parte entre paréntesis
        int pIni = linea.indexOf('(');
        int pFin = linea.lastIndexOf(')');
        if (pIni < 0 || pFin < 0 || pFin <= pIni)
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Condición del 'if' mal formada — se esperan paréntesis");

        String condStr = linea.substring(pIni + 1, pFin).trim();
        if (condStr.isEmpty())
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Condición del 'if' vacía");

        // Detectar operador relacional
        String[] ops = {">=", "<=", "==", "!=", ">", "<"};
        String opEncontrado = null;
        String exprA = null, exprB = null;
        for (String op : ops) {
            int idx = condStr.indexOf(op);
            if (idx >= 0) {
                exprA = condStr.substring(0, idx).trim();
                exprB = condStr.substring(idx + op.length()).trim();
                opEncontrado = op;
                break;
            }
        }
        if (opEncontrado == null)
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Operador relacional no encontrado en la condición");

        if (exprA.isEmpty())
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Falta operando izquierdo en la condición");
        if (exprB.isEmpty())
            throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                "Falta operando derecho en la condición");

        double valA = evaluarNumerica(exprA, ln);
        double valB = evaluarNumerica(exprB, ln);

        boolean resultado = switch (opEncontrado) {
            case ">=" -> valA >= valB;
            case "<=" -> valA <= valB;
            case "==" -> valA == valB;
            case "!=" -> valA != valB;
            case ">"  -> valA >  valB;
            case "<"  -> valA <  valB;
            default   -> false;
        };

        String msg = "if: condición [" + exprA + " " + opEncontrado + " " + exprB + "] → " + resultado;
        // Usamos BLOQUE_INICIO y codificamos el resultado en el texto con prefijo
        return new ResultadoLinea(TipoResultado.BLOQUE_INICIO,
            (resultado ? "TRUE:" : "FALSE:") + msg);
    }

    // ── cuarto (entero) ───────────────────────────────────────────
    private static String procesarCuarto(String nom, String expr,
                                         int ln, boolean brk) throws ErrorC {
        double v = evaluarNumerica(expr, ln);
        if (v % 1 != 0)
            throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "'cuarto' no acepta decimales");
        int val = (int) v;
        tabla.agregar(nom, new TablaSimbolos.Simbolo("cuarto", new Cuarto(val)));
        return "cuarto " + nom + " = " + val + (brk ? "  [break]" : "");
    }

    // ── media (decimal) ───────────────────────────────────────────
    private static String procesarMedia(String nom, String expr,
                                        int ln, boolean brk) throws ErrorC {
        double v = evaluarNumerica(expr, ln);
        tabla.agregar(nom, new TablaSimbolos.Simbolo("media", new Media(v)));
        return "media " + nom + " = " + v + (brk ? "  [break]" : "");
    }

    // ── mega (cadena) ─────────────────────────────────────────────
    private static String procesarMega(String nom, String expr,
                                       int ln, boolean brk) throws ErrorC {
        String v = evaluarCadena(expr, ln);
        if (v.length() > 64)
            throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "La cadena supera 64 caracteres");
        tabla.agregar(nom, new TablaSimbolos.Simbolo("mega", new Mega(v)));
        return "mega " + nom + " = \"" + v + "\"" + (brk ? "  [break]" : "");
    }

    // ─────────────────────────────────────────────────────────────
    // Evaluación de expresiones numéricas
    // ─────────────────────────────────────────────────────────────
    public static double evaluarNumerica(String expr, int ln) throws ErrorC {
        // Normalizar operadores con espacios
        expr = expr.replace("@"," @ ").replace("~"," ~ ")
                   .replace("#"," # ").replace("%"," % ");
        String[] p = expr.trim().split("\\s+");
        double res = obtenerNumero(p[0], ln);
        for (int i = 1; i < p.length; i += 2) {
            if (i + 1 >= p.length)
                throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln, "Falta operando");
            double val = obtenerNumero(p[i + 1], ln);
            res = switch (p[i]) {
                case "@" -> res + val;
                case "~" -> res - val;
                case "#" -> res * val;
                case "%" -> {
                    if (val == 0)
                        throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                            "División entre cero");
                    yield res / val;
                }
                default -> throw new ErrorC(ErrorC.Tipo.SINTACTICO, ln,
                    "Operador desconocido: " + p[i]);
            };
        }
        return res;
    }

    private static double obtenerNumero(String t, int ln) throws ErrorC {
        if (t.matches("\\d+"))           return Integer.parseInt(t);
        if (t.matches("\\d+\\.\\d*") ||
            t.matches("\\.\\d+"))        return Double.parseDouble(t);
        TablaSimbolos.Simbolo s = tabla.obtener(t);
        if (s == null)
            throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "Variable no declarada: '" + t + "'");
        if (s.valor instanceof Cuarto c) return c.getValor();
        if (s.valor instanceof Media  m) return m.getValor();
        throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
            "'" + t + "' no es un tipo numérico");
    }

    // ─────────────────────────────────────────────────────────────
    // Evaluación de expresiones de cadena
    // ─────────────────────────────────────────────────────────────
    public static String evaluarCadena(String expr, int ln) throws ErrorC {
        String[] partes = expr.split("@");
        StringBuilder sb = new StringBuilder(obtenerCadena(partes[0].trim(), ln));
        for (int i = 1; i < partes.length; i++)
            sb.append(obtenerCadena(partes[i].trim(), ln));
        return sb.toString();
    }

    private static String obtenerCadena(String t, int ln) throws ErrorC {
        if (t.startsWith("\"") && t.endsWith("\""))
            return t.substring(1, t.length() - 1);
        TablaSimbolos.Simbolo s = tabla.obtener(t);
        if (s == null)
            throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
                "Variable no declarada: '" + t + "'");
        if (s.valor instanceof Mega g) return g.getValor();
        throw new ErrorC(ErrorC.Tipo.SEMANTICO, ln,
            "'" + t + "' no es de tipo mega");
    }

    // ─────────────────────────────────────────────────────────────
    // Tokenización — para la tabla de tokens de la GUI
    // ─────────────────────────────────────────────────────────────
    public static List<Token> obtenerTokens(String linea) {
        List<Token> lista = new ArrayList<>();

        linea = linea.replace("!!", " !! ")
                     .replace("=>", " => ")
                     .replace("@",  " @ ")
                     .replace("~",  " ~ ")
                     .replace("#",  " # ")
                     .replace("%",  " % ");

        // Separar respetando cadenas entre comillas
        List<String> partes = new ArrayList<>();
        boolean enComillas = false;
        StringBuilder actual = new StringBuilder();
        for (char c : linea.toCharArray()) {
            if (c == '"') { enComillas = !enComillas; actual.append(c); }
            else if (c == ' ' && !enComillas) {
                if (actual.length() > 0) { partes.add(actual.toString()); actual.setLength(0); }
            } else actual.append(c);
        }
        if (actual.length() > 0) partes.add(actual.toString());

        for (String s : partes) {
            if (s.isEmpty()) continue;
            if (s.equals("cuarto") || s.equals("media") || s.equals("mega") ||
                s.equals("if")     || s.equals("try")   || s.equals("break")) {
                lista.add(new Token("PR",    s, "(" + s + ")", "SI"));
            } else if (s.equals("=>")) {
                lista.add(new Token("ASIG",  s, "(=>)",        "SI"));
            } else if (s.equals("!!")) {
                lista.add(new Token("FIN",   s, "(!!)",        "SI"));
            } else if (s.matches("[@~#%]")) {
                lista.add(new Token("OP",    s, "[@~#%]",      "NO"));
            } else if (s.matches(">=|<=|==|!=|>|<")) {
                lista.add(new Token("OP_REL",s, s,             "NO"));
            } else if (s.matches("\\d+\\.\\d*") || s.matches("\\.\\d+")) {
                lista.add(new Token("NUM",   s, "(decimal)",   "NO"));
            } else if (s.matches("\\d+")) {
                lista.add(new Token("NUM",   s, "(entero)",    "NO"));
            } else if (s.startsWith("\"") && s.endsWith("\"")) {
                lista.add(new Token("CAD",   s, "\".*\"",      "NO"));
            } else if (s.equals("{") || s.equals("}")) {
                lista.add(new Token("BLOQUE",s, "[{}]",        "SI"));
            } else if (s.equals("(") || s.equals(")")) {
                lista.add(new Token("PAREN", s, "[()]",        "NO"));
            } else if (s.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                lista.add(new Token("ID",    s, "[a-zA-Z_]+",  "NO"));
            } else if (!s.startsWith("//")) {
                lista.add(new Token("ERROR", s, "?",           "NO"));
            }
        }
        return lista;
    }
}
