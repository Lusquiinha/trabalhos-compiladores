package br.ufscar.dc.compiladores.la.lexico;

import java.util.Set;

public class Token {

    private static final Set<String> CLASS_TYPES = Set.of("IDENT", "NUM_INT", "NUM_REAL", "CADEIA");

    private final String lexeme;
    private final String type;

    public Token(String lexeme, String type) {
        this.lexeme = lexeme;
        this.type = type;
    }

    public String getLexeme() { return lexeme; }
    public String getType()   { return type; }

    @Override
    public String toString() {
        if (CLASS_TYPES.contains(type)) {
            return "<'" + lexeme + "'," + type + ">";
        }
        return "<'" + lexeme + "','" + type + "'>";
    }
}
