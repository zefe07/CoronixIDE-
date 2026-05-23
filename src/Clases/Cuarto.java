package Clases;

/** Tipo entero de Coronix (equivale a int) */
public class Cuarto {
    private final int valor;

    public Cuarto(int v)       { this.valor = v; }
    public int    getValor()   { return valor;   }

    @Override
    public String toString()   { return String.valueOf(valor); }
}
