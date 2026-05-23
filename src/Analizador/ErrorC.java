package Analizador;

public class ErrorC extends Exception {

    public enum Tipo {
        LEXICO, SINTACTICO, SEMANTICO
    }

    private Tipo tipo;
    private int linea;

    public ErrorC(Tipo tipo, int linea, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
        this.linea = linea;
    }

    @Override
    public String toString() {
        return "Error " + tipo + " en línea " + linea + ": " + getMessage();
    }
}