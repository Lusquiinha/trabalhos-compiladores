package br.ufscar.dc.compiladores.la.semantico;

import java.util.LinkedList;

import br.ufscar.dc.compiladores.la.semantico.TabelaDeSimbolos.EntradaTabela;

public class Escopos {

    private final LinkedList<TabelaDeSimbolos> pilha = new LinkedList<>();

    public Escopos() {
        pilha.push(new TabelaDeSimbolos());
    }

    public TabelaDeSimbolos obterEscopoAtual() {
        return pilha.peek();
    }

    public void criarNovoEscopo() {
        pilha.push(new TabelaDeSimbolos());
    }

    public void abandonarEscopo() {
        pilha.pop();
    }

    public LinkedList<TabelaDeSimbolos> percorrerEscopos() {
        return pilha;
    }

    public EntradaTabela buscarEntrada(String nome) {
        for (TabelaDeSimbolos tabela : pilha) {
            if (tabela.existe(nome)) {
                return tabela.obter(nome);
            }
        }
        return null;
    }
}
