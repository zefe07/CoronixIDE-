package Clases;

/** Tipo decimal de Coronix (equivale a double) */
public class Media {
    private final double valor;

    public Media(double v)     { this.valor = v; }
    public double getValor()   { return valor;   }

    @Override
    public String toString() {
        // Muestra sin .0 innecesario
        if (valor == Math.floor(valor) && !Double.isInfinite(valor))
            return String.valueOf((long) valor);
        return String.valueOf(valor);
    }
}
