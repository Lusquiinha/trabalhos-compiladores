package br.ufscar.dc.compiladores.la.semantico;

import java.util.HashMap;
import java.util.Map;

public class TabelaDeSimbolos {

    public enum TipoLA {
        INTEIRO, REAL, LITERAL, LOGICO, REGISTRO, INVALIDO
    }

    public static class EntradaTabela {
        public final String nome;
        public final TipoLA tipo;

        public EntradaTabela(String nome, TipoLA tipo) {
            this.nome = nome;
            this.tipo = tipo;
        }
    }

    private final Map<String, EntradaTabela> simbolos = new HashMap<>();

    public void adicionar(String nome, TipoLA tipo) {
        simbolos.put(nome, new EntradaTabela(nome, tipo));
    }

    public boolean existe(String nome) {
        return simbolos.containsKey(nome);
    }

    public TipoLA verificar(String nome) {
        EntradaTabela entrada = simbolos.get(nome);
        return entrada == null ? null : entrada.tipo;
    }
}
