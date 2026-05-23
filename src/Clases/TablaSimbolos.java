package Clases;

import java.util.LinkedHashMap;
import java.util.Map;

public class TablaSimbolos {

    public static class Simbolo {
        public final String tipo;
        public final Object valor;
        public Simbolo(String tipo, Object valor) {
            this.tipo  = tipo;
            this.valor = valor;
        }
    }

    private final Map<String, Simbolo> tabla = new LinkedHashMap<>();

    public void agregar(String nombre, Simbolo s) { tabla.put(nombre, s); }

    public Simbolo obtener(String nombre) { return tabla.get(nombre); }

    public boolean existe(String nombre) { return tabla.containsKey(nombre); }

    public void limpiar() { tabla.clear(); }

    /** Para que la GUI itere sobre las entradas en orden de inserción */
    public Map<String, Simbolo> entradas() { return new LinkedHashMap<>(tabla); }
}
