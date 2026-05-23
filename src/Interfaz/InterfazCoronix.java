package Interfaz;

import Analizador.ErrorC;
import Analizador.Token;
import Clases.ClasePrincipal;
import Clases.ClasePrincipal.ResultadoLinea;
import Clases.ClasePrincipal.TipoResultado;
import Clases.TablaSimbolos;
import Clases.Cuarto;
import Clases.Media;
import Clases.Mega;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Interfaz gráfica del compilador Coronix.
 *
 * Maneja bloques if { } y try { } anidados con una pila de contextos,
 * ejecutando o saltando las líneas internas según la condición.
 */
public class InterfazCoronix extends JFrame {

    // ── Editor y consola ──────────────────────────────────────────
    private final JTextArea editor  = new JTextArea();
    private final JTextArea consola = new JTextArea();

    // ── Modelos de las tres tablas ────────────────────────────────
    private final DefaultTableModel mdlTokens = new DefaultTableModel(
        new String[]{"Token", "Lexema", "Patrón", "Reservada"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel mdlGramatica = new DefaultTableModel(
        new String[]{"Elemento", "Categoría gramatical"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final DefaultTableModel mdlSimbolos = new DefaultTableModel(
        new String[]{"Nombre", "Tipo", "Valor"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    // ─────────────────────────────────────────────────────────────
    public InterfazCoronix() {
        super("Coronix IDE — Compilador");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        construirUI();
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    private void construirUI() {

        // Editor
        editor.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editor.setTabSize(4);
        editor.setText(
            "// Programa de prueba Coronix\n" +
            "cuarto x => 9 !!\n" +
            "cuarto y => x @ 5 !!\n" +
            "media pi => 3.14 !!\n" +
            "mega msg => \"Hola\" @ \" mundo\" !!\n"
        );

        // Consola
        consola.setEditable(false);
        consola.setFont(new Font("Monospaced", Font.PLAIN, 13));
        consola.setBackground(new Color(30, 30, 30));
        consola.setForeground(new Color(180, 255, 180));
        consola.setCaretColor(Color.WHITE);

        // Botones
        JButton btnEjecutar = new JButton("▶  Ejecutar");
        btnEjecutar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnEjecutar.setBackground(new Color(50, 130, 50));
        btnEjecutar.setForeground(Color.WHITE);
        btnEjecutar.setFocusPainted(false);
        btnEjecutar.addActionListener(e -> ejecutar());

        JButton btnLimpiar = new JButton("✖  Limpiar");
        btnLimpiar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.addActionListener(e -> {
            editor.setText("");
            limpiarTablas();
        });

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelBotones.add(btnEjecutar);
        panelBotones.add(btnLimpiar);

        // Panel derecho: 4 secciones apiladas
        JSplitPane sp3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            seccion("Tabla de Símbolos",       tabla(mdlSimbolos),   110),
            seccion("Consola de resultados",   new JScrollPane(consola), 130));
        sp3.setResizeWeight(0.45);

        JSplitPane sp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            seccion("Gramática (clasificación)", tabla(mdlGramatica), 140),
            sp3);
        sp2.setResizeWeight(0.35);

        JSplitPane sp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            seccion("Tabla de Tokens",          tabla(mdlTokens),    180),
            sp2);
        sp1.setResizeWeight(0.40);

        // Split principal
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            seccion("Editor de Código", new JScrollPane(editor), 0),
            sp1);
        main.setResizeWeight(0.35);
        main.setDividerLocation(380);

        add(panelBotones, BorderLayout.NORTH);
        add(main,         BorderLayout.CENTER);

        JLabel status = new JLabel("  Coronix IDE — listo");
        status.setFont(new Font("SansSerif", Font.PLAIN, 11));
        status.setForeground(Color.GRAY);
        add(status, BorderLayout.SOUTH);
    }

    // ─────────────────────────────────────────────────────────────
    private void limpiarTablas() {
        mdlTokens.setRowCount(0);
        mdlGramatica.setRowCount(0);
        mdlSimbolos.setRowCount(0);
        consola.setText("");
    }

    // ─────────────────────────────────────────────────────────────
    // Estado de un bloque anidado: si se debe ejecutar o saltar
    // ─────────────────────────────────────────────────────────────
    private enum TipoBloque { IF_VERDADERO, IF_FALSO, TRY }

    // ─────────────────────────────────────────────────────────────
    private void ejecutar() {
        limpiarTablas();

        String texto = editor.getText().trim();
        if (texto.isEmpty()) {
            consola.setText("⚠  El editor está vacío.");
            return;
        }

        // Reiniciar tabla de símbolos en cada ejecución
        ClasePrincipal.tabla = new TablaSimbolos();

        StringBuilder sb = new StringBuilder();
        String[] lineas = texto.split("\n");

        // Pila de bloques: cada entrada dice si estamos ejecutando o saltando
        Deque<TipoBloque> pilaBloque = new ArrayDeque<>();

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty() || linea.startsWith("//")) continue;

            int numLinea = i + 1;

            // ¿Estamos dentro de un bloque que NO se ejecuta?
            boolean saltando = pilaBloque.stream().anyMatch(b -> b == TipoBloque.IF_FALSO);

            // Cierre de bloque — siempre se procesa para desapilar
            if (linea.equals("}")) {
                if (!pilaBloque.isEmpty()) {
                    TipoBloque b = pilaBloque.pop();
                    if (!saltando) {
                        sb.append("✔  [L").append(numLinea).append("] fin de bloque\n");
                    } else if (b == TipoBloque.IF_FALSO) {
                        // Al salir del bloque falso, ya no saltamos más (si no hay otro encima)
                        sb.append("✔  [L").append(numLinea).append("] fin de bloque (condición era false — bloque omitido)\n");
                    }
                } else {
                    sb.append("❌  [L").append(numLinea).append("] Error SINTÁCTICO: '}' sin '{' correspondiente\n");
                }
                continue;
            }

            // Si estamos saltando (if false), solo registrar tokens pero no ejecutar
            if (saltando) {
                // Igual registrar tokens para la tabla
                List<Token> tokens = ClasePrincipal.obtenerTokens(linea);
                for (Token tk : tokens) {
                    mdlTokens.addRow(new Object[]{tk.token, tk.lexema, tk.patron, tk.reservada});
                    mdlGramatica.addRow(new Object[]{tk.lexema, categoriaGramatical(tk)});
                }
                // Si hay un { anidado dentro del bloque falso, empilamos otro IF_FALSO
                if (linea.endsWith("{")) {
                    pilaBloque.push(TipoBloque.IF_FALSO);
                }
                continue;
            }

            // ── Tabla de tokens ──────────────────────────────────
            List<Token> tokens = ClasePrincipal.obtenerTokens(linea);
            for (Token tk : tokens) {
                mdlTokens.addRow(new Object[]{tk.token, tk.lexema, tk.patron, tk.reservada});
                mdlGramatica.addRow(new Object[]{tk.lexema, categoriaGramatical(tk)});
            }

            // ── Procesar la línea ────────────────────────────────
            try {
                ResultadoLinea res = ClasePrincipal.procesarLineaCompleta(linea, numLinea);

                switch (res.tipo) {
                    case OK -> sb.append("✔  [L").append(numLinea).append("] ").append(res.texto).append("\n");

                    case BLOQUE_INICIO -> {
                        String txt = res.texto;
                        if (txt.startsWith("TRUE:")) {
                            pilaBloque.push(TipoBloque.IF_VERDADERO);
                            sb.append("✔  [L").append(numLinea).append("] ").append(txt.substring(5)).append("\n");
                        } else if (txt.startsWith("FALSE:")) {
                            pilaBloque.push(TipoBloque.IF_FALSO);
                            sb.append("✔  [L").append(numLinea).append("] ").append(txt.substring(6)).append("\n");
                        } else {
                            // try block
                            pilaBloque.push(TipoBloque.TRY);
                            sb.append("✔  [L").append(numLinea).append("] ").append(txt).append("\n");
                        }
                    }

                    case BLOQUE_FIN -> {
                        // ya manejado arriba, no debería llegar aquí
                    }

                    case VACIO -> { /* nada */ }
                }

            } catch (ErrorC e) {
                sb.append("❌  ").append(e).append("\n");
            }
        }

        // Bloques sin cerrar
        if (!pilaBloque.isEmpty()) {
            sb.append("❌  Error SINTÁCTICO: ").append(pilaBloque.size())
              .append(" bloque(s) sin cerrar — falta(n) '}'\n");
        }

        // ── Tabla de símbolos ────────────────────────────────────
        for (var entrada : ClasePrincipal.tabla.entradas().entrySet()) {
            TablaSimbolos.Simbolo s = entrada.getValue();
            String val = "";
            if (s.valor instanceof Cuarto c) val = String.valueOf(c.getValor());
            else if (s.valor instanceof Media  m) val = String.valueOf(m.getValor());
            else if (s.valor instanceof Mega   g) val = "\"" + g.getValor() + "\"";
            mdlSimbolos.addRow(new Object[]{ entrada.getKey(), s.tipo, val });
        }

        if (sb.isEmpty()) sb.append("⚠  No se procesó ninguna línea válida.");
        consola.setText(sb.toString());
        consola.setCaretPosition(0);
    }

    // ─────────────────────────────────────────────────────────────
    private String categoriaGramatical(Token tk) {
        return switch (tk.token) {
            case "PR"     -> "<palabra_reservada>  →  " + tk.lexema;
            case "ID"     -> "<identificador>  →  "     + tk.lexema;
            case "ASIG"   -> "<asignación>  →  =>";
            case "NUM"    -> "<constante_numérica>  →  "  + tk.lexema;
            case "CAD"    -> "<constante_cadena>  →  "    + tk.lexema;
            case "OP"     -> "<operador_aritmético>  →  " + tk.lexema;
            case "FIN"    -> "<fin_sentencia>  →  !!"    ;
            case "OP_REL" -> "<operador_relacional>  →  " + tk.lexema;
            default       -> "<token>  →  "              + tk.lexema;
        };
    }

    // ── Helpers de layout ─────────────────────────────────────────
    private JScrollPane tabla(DefaultTableModel mdl) {
        JTable t = new JTable(mdl);
        t.setFont(new Font("Monospaced", Font.PLAIN, 12));
        t.setRowHeight(20);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.setSelectionBackground(new Color(180, 210, 255));
        return new JScrollPane(t);
    }

    private JPanel seccion(String titulo, JComponent contenido, int altMin) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        if (altMin > 0) contenido.setPreferredSize(new Dimension(0, altMin));
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }
}
