package br.ufscar.dc.compiladores.la.semantico;

import java.util.LinkedList;

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
}
