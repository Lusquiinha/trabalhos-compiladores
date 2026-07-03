package br.ufscar.dc.compiladores.la.gerador;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tipo {

    public enum Categoria {
        INTEIRO, REAL, LITERAL, LOGICO, REGISTRO, PONTEIRO, ENDERECO, INVALIDO
    }

    public static final Tipo INTEIRO = new Tipo(Categoria.INTEIRO);
    public static final Tipo REAL = new Tipo(Categoria.REAL);
    public static final Tipo LITERAL = new Tipo(Categoria.LITERAL);
    public static final Tipo LOGICO = new Tipo(Categoria.LOGICO);
    public static final Tipo INVALIDO = new Tipo(Categoria.INVALIDO);

    public final Categoria categoria;
    public final String nomeRegistro;
    public final Map<String, Tipo> campos;
    public final Tipo apontado;

    private Tipo(Categoria categoria) {
        this(categoria, null, null, null);
    }

    private Tipo(Categoria categoria, String nomeRegistro, Map<String, Tipo> campos, Tipo apontado) {
        this.categoria = categoria;
        this.nomeRegistro = nomeRegistro;
        this.campos = campos;
        this.apontado = apontado;
    }

    public static Tipo registro(String nomeRegistro) {
        return new Tipo(Categoria.REGISTRO, nomeRegistro, new LinkedHashMap<>(), null);
    }

    public static Tipo ponteiro(Tipo apontado) {
        return new Tipo(Categoria.PONTEIRO, null, null, apontado);
    }

    public static Tipo endereco(Tipo apontado) {
        return new Tipo(Categoria.ENDERECO, null, null, apontado);
    }

    public boolean ehNumerico() {
        return categoria == Categoria.INTEIRO || categoria == Categoria.REAL;
    }

    public boolean ehInvalido() {
        return categoria == Categoria.INVALIDO;
    }
}
