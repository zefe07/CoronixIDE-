package Analizador;

/**
 * Representa un token del lenguaje Coronix.
 * Constructor: Token(tipo, lexema, patron, reservada)
 */
public class Token {

    public final String token;      // tipo abreviado: PR, ID, NUM, OP, etc.
    public final String lexema;     // texto original del token
    public final String patron;     // patrón o descripción
    public final String reservada;  // "SI" si es palabra reservada, "NO" si no

    public Token(String token, String lexema, String patron, String reservada) {
        this.token     = token;
        this.lexema    = lexema;
        this.patron    = patron;
        this.reservada = reservada;
    }

    @Override
    public String toString() {
        return "[" + token + "] '" + lexema + "'";
    }
}
