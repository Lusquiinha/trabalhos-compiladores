package br.ufscar.dc.compiladores.la.gerador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum Categoria {
        VARIAVEL, CONSTANTE, TIPO, FUNCAO, PROCEDIMENTO
    }

    public static class EntradaTabela {
        public final String nome;
        public final Categoria categoria;
        public final Tipo tipo;
        public final List<Tipo> parametros;
        public final Tipo tipoRetorno;

        public EntradaTabela(String nome, Categoria categoria, Tipo tipo) {
            this(nome, categoria, tipo, new ArrayList<>(), null);
        }

        public EntradaTabela(String nome, Categoria categoria, Tipo tipo,
                List<Tipo> parametros, Tipo tipoRetorno) {
            this.nome = nome;
            this.categoria = categoria;
            this.tipo = tipo;
            this.parametros = parametros;
            this.tipoRetorno = tipoRetorno;
        }
    }

    private final Map<String, EntradaTabela> simbolos = new LinkedHashMap<>();

    public void adicionar(String nome, Categoria categoria, Tipo tipo) {
        simbolos.put(nome, new EntradaTabela(nome, categoria, tipo));
    }

    public void adicionar(EntradaTabela entrada) {
        simbolos.put(entrada.nome, entrada);
    }

    public boolean existe(String nome) {
        return simbolos.containsKey(nome);
    }

    public EntradaTabela obter(String nome) {
        return simbolos.get(nome);
    }
}
