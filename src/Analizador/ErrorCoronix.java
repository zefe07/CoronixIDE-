package Analizador;

public class ErrorCoronix {

    public enum Tipo { LEXICO, SINTACTICO, SEMANTICO }

    private final Tipo   tipo;
    private final int    linea;
    private final String mensaje;

    public ErrorCoronix(Tipo tipo, int linea, String mensaje) {
        this.tipo    = tipo;
        this.linea   = linea;
        this.mensaje = mensaje;
    }

    public Tipo   getTipo()   { return tipo;    }
    public int    getLinea()  { return linea;   }
    public String getMensaje(){ return mensaje; }

    @Override
    public String toString() {
        return "[" + tipo + "] Línea " + linea + ": " + mensaje;
    }
}
