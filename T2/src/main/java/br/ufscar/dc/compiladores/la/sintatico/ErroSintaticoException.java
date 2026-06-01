package br.ufscar.dc.compiladores.la.sintatico;

public class ErroSintaticoException extends RuntimeException {

    public ErroSintaticoException(String mensagem) {
        super(mensagem);
    }
}
