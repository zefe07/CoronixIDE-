/*
 * ================================================================
 *  CORONIX — Analizador Léxico
 *  Archivo : Lexico.flex
 *  Paquete : Analizador
 *
 *  Palabras reservadas del lenguaje:
 *    cuarto  → int    (número entero)
 *    media   → double (número decimal)
 *    mega    → String (cadena de texto)
 *    if      → condicional
 *    try     → bloque de manejo de errores
 *    break   → fin de bloque o sentencia
 *
 *  Operadores propios de Coronix:
 *    =>   asignación
 *    @    suma / concatenación de cadenas
 *    ~    resta
 *    #    multiplicación
 *    %    división
 *    !!   fin de sentencia
 *    ( )  agrupación de expresiones
 *    { }  delimitador de bloque (if / try)
 *
 *  Operadores relacionales (para el if):
 *    >=   mayor o igual
 *    <=   menor o igual
 *    ==   igual
 *    !=   diferente
 *    >    mayor que
 *    <    menor que
 *
 *  Restricción: identificadores máximo 10 caracteres.
 *  Comentarios de línea: // texto
 * ================================================================
 */

package Analizador;

import java_cup.runtime.Symbol;
import java.util.ArrayList;
import java.util.List;

%%

%class   Lexer
%public
%unicode
%cup
%line
%column
%type    Symbol

%{
    /** Lista de tokens reconocidos — la GUI la usa para la tabla */
    public List<Token> listaTokens = new ArrayList<>();

    /** Registra y devuelve un Symbol sin valor semántico */
    private Symbol mk(int tipo, String nombreTipo) {
        listaTokens.add(new Token(yytext(), nombreTipo, yyline + 1, ""));
        return new Symbol(tipo, yyline + 1, yycolumn + 1);
    }

    /** Registra y devuelve un Symbol con valor semántico */
    private Symbol mk(int tipo, String nombreTipo, Object valor) {
        listaTokens.add(new Token(yytext(), nombreTipo,
                                  yyline + 1, String.valueOf(valor)));
        return new Symbol(tipo, yyline + 1, yycolumn + 1, valor);
    }
%}

/* ── Macros ──────────────────────────────────────────────────── */
LETRA      = [a-zA-Z_]
DIGITO     = [0-9]
ID         = {LETRA}({LETRA}|{DIGITO})*
ENTERO     = {DIGITO}+
DECIMAL    = {DIGITO}+"."{DIGITO}+
CADENA     = \"[^\"\n]*\"
ESPACIO    = [ \t\r\n]+
COMENTARIO = "//"[^\n]*

%%

/* ── Ignorar ──────────────────────────────────────────────────── */
{COMENTARIO}    { /* comentario de línea — ignorar */  }
{ESPACIO}       { /* espacios y saltos — ignorar */    }

/* ── Palabras reservadas — tipo ──────────────────────────────── */
"cuarto"        { return mk(sym.CUARTO, "RESERVADA"); }
"media"         { return mk(sym.MEDIA,  "RESERVADA"); }
"mega"          { return mk(sym.MEGA,   "RESERVADA"); }

/* ── Palabras reservadas — control ──────────────────────────── */
"if"            { return mk(sym.IF,    "RESERVADA"); }
"try"           { return mk(sym.TRY,   "RESERVADA"); }
"break"         { return mk(sym.BREAK, "RESERVADA"); }

/* ── Operadores relacionales (2 caracteres antes que 1) ─────── */
">="            { return mk(sym.MAYOR_IGUAL, "OP_REL"); }
"<="            { return mk(sym.MENOR_IGUAL, "OP_REL"); }
"=="            { return mk(sym.IGUAL_IGUAL, "OP_REL"); }
"!="            { return mk(sym.DIFERENTE,   "OP_REL"); }

/* ── Asignación (=> antes de > y <) ─────────────────────────── */
"=>"            { return mk(sym.ASIGNAR, "ASIGNACION"); }

/* ── Operadores relacionales de 1 carácter ───────────────────── */
">"             { return mk(sym.MAYOR, "OP_REL"); }
"<"             { return mk(sym.MENOR, "OP_REL"); }

/* ── Operadores aritméticos propios de Coronix ──────────────── */
"@"             { return mk(sym.SUMA,        "OP_SUMA");  }
"~"             { return mk(sym.RESTA,       "OP_RESTA"); }
"#"             { return mk(sym.MULTIPLICAR, "OP_MULTI"); }
"%"             { return mk(sym.DIVIDIR,     "OP_DIV");   }

/* ── Fin de sentencia (!! antes que ! para evitar conflicto) ── */
"!!"            { return mk(sym.FIN, "FIN_SENT"); }

/* ── Delimitadores ───────────────────────────────────────────── */
"("             { return mk(sym.LPAREN, "LPAREN"); }
")"             { return mk(sym.RPAREN, "RPAREN"); }
"{"             { return mk(sym.LBRACE, "LBRACE"); }
"}"             { return mk(sym.RBRACE, "RBRACE"); }

/* ── Literales (DECIMAL antes que ENTERO) ────────────────────── */
{DECIMAL}       {
                    double v = Double.parseDouble(yytext());
                    return mk(sym.LIT_DECIMAL, "DECIMAL", v);
                }
{ENTERO}        {
                    int v = Integer.parseInt(yytext());
                    return mk(sym.LIT_ENTERO, "ENTERO", v);
                }
{CADENA}        {
                    // Quitar las comillas del valor semántico
                    String v = yytext().substring(1, yytext().length() - 1);
                    return mk(sym.LIT_CADENA, "CADENA", v);
                }

/* ── Identificadores ─────────────────────────────────────────── */
{ID}            {
                    String nombre = yytext();
                    if (nombre.length() > 10) {
                        throw new LexicalException(
                            "Error léxico en línea " + (yyline + 1) +
                            ": el identificador '" + nombre +
                            "' supera el límite de 10 caracteres.");
                    }
                    return mk(sym.ID, "IDENTIFICADOR", nombre);
                }

/* ── Carácter no reconocido → error léxico ───────────────────── */
[^]             {
                    throw new LexicalException(
                        "Error léxico en línea " + (yyline + 1) +
                        ", col " + (yycolumn + 1) +
                        ": carácter no reconocido → '" + yytext() + "'");
                }
