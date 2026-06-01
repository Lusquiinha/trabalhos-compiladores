package br.ufscar.dc.compiladores.la.lexico;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Lexer {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "algoritmo", "fim_algoritmo",
        "declare",
        "inteiro", "real", "logico", "literal",
        "leia", "escreva",
        "se", "entao", "senao", "fim_se",
        "enquanto", "faca", "fim_enquanto",
        "para", "ate", "passo", "fim_para",
        "e", "ou", "nao",
        "verdadeiro", "falso",
        "tipo", "fim_tipo",
        "procedimento", "fim_procedimento",
        "funcao", "fim_funcao",
        "retorne",
        "caso", "seja", "fim_caso",
        "registro", "fim_registro",
        "constante",
        "var"
    ));

    private final String source;
    private int pos;
    private int line;
    private String error;

    public Lexer(String source) {
        this.source = source;
        this.pos = 0;
        this.line = 1;
        this.error = null;
    }

    public String getError() { return error; }

    public Token nextToken() {
        while (pos < source.length()) {
            char c = current();

            if (Character.isWhitespace(c)) {
                if (c == '\n') line++;
                pos++;
                continue;
            }

            if (c == '{') {
                if (!readComment()) return null;
                continue;
            }

            if (c == '"') return readString();

            if (Character.isLetter(c)) return readIdentOrKeyword();

            if (Character.isDigit(c)) return readNumber();

            return readOperator();
        }
        return null;
    }

    private char current() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private char peek() {
        return (pos + 1) < source.length() ? source.charAt(pos + 1) : '\0';
    }

    private boolean readComment() {
        int startLine = line;
        pos++;
        while (pos < source.length()) {
            char c = current();
            if (c == '}') {
                pos++;
                return true;
            }
            if (c == '\n') {
                error = "Linha " + startLine + ": comentario nao fechado";
                return false;
            }
            pos++;
        }
        error = "Linha " + startLine + ": comentario nao fechado";
        return false;
    }

    private Token readString() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        pos++;
        while (pos < source.length()) {
            char c = current();
            if (c == '"') {
                sb.append('"');
                pos++;
                return new Token(sb.toString(), "CADEIA");
            }
            if (c == '\n') {
                error = "Linha " + startLine + ": cadeia literal nao fechada";
                return null;
            }
            sb.append(c);
            pos++;
        }
        error = "Linha " + startLine + ": cadeia literal nao fechada";
        return null;
    }

    private Token readIdentOrKeyword() {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && (Character.isLetterOrDigit(current()) || current() == '_')) {
            sb.append(current());
            pos++;
        }
        String word = sb.toString();
        if (KEYWORDS.contains(word)) {
            return new Token(word, word);
        }
        return new Token(word, "IDENT");
    }

    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        while (pos < source.length() && Character.isDigit(current())) {
            sb.append(current());
            pos++;
        }
        if (current() == '.' && Character.isDigit(peek())) {
            sb.append('.');
            pos++;
            while (pos < source.length() && Character.isDigit(current())) {
                sb.append(current());
                pos++;
            }
            return new Token(sb.toString(), "NUM_REAL");
        }
        return new Token(sb.toString(), "NUM_INT");
    }

    private Token readOperator() {
        char c = current();
        switch (c) {
            case '(': pos++; return new Token("(", "(");
            case ')': pos++; return new Token(")", ")");
            case '[': pos++; return new Token("[", "[");
            case ']': pos++; return new Token("]", "]");
            case ',': pos++; return new Token(",", ",");
            case '+': pos++; return new Token("+", "+");
            case '-': pos++; return new Token("-", "-");
            case '*': pos++; return new Token("*", "*");
            case '/': pos++; return new Token("/", "/");
            case '=': pos++; return new Token("=", "=");
            case '%': pos++; return new Token("%", "%");
            case '^': pos++; return new Token("^", "^");
            case '&': pos++; return new Token("&", "&");
            case ':': pos++; return new Token(":", ":");
            case '.':
                if (peek() == '.') { pos += 2; return new Token("..", ".."); }
                pos++;
                return new Token(".", ".");
            case '<':
                if (peek() == '-') { pos += 2; return new Token("<-", "<-"); }
                if (peek() == '=') { pos += 2; return new Token("<=", "<="); }
                if (peek() == '>') { pos += 2; return new Token("<>", "<>"); }
                pos++;
                return new Token("<", "<");
            case '>':
                if (peek() == '=') { pos += 2; return new Token(">=", ">="); }
                pos++;
                return new Token(">", ">");
            default:
                error = "Linha " + line + ": " + c + " - simbolo nao identificado";
                pos++;
                return null;
        }
    }
}
