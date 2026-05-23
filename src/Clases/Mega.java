package Clases;

/** Tipo cadena de Coronix (equivale a String) */
public class Mega {
    private final String valor;

    public Mega(String v)      { this.valor = v; }
    public String getValor()   { return valor;   }

    @Override
    public String toString()   { return valor;   }
}
