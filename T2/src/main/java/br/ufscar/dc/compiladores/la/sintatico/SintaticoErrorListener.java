package br.ufscar.dc.compiladores.la.sintatico;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class SintaticoErrorListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int linha,
                            int posicaoNaLinha,
                            String mensagem,
                            RecognitionException excecao) {

        Token token = (Token) offendingSymbol;

        if (token == null) {
            throw new ErroSintaticoException("Linha " + linha + ": erro sintatico proximo a EOF");
        }

        switch (token.getType()) {
            case LALexer.COMENTARIO_NAO_FECHADO:
                throw new ErroSintaticoException("Linha " + linha + ": comentario nao fechado");
            case LALexer.CADEIA_NAO_FECHADA:
                throw new ErroSintaticoException("Linha " + linha + ": cadeia literal nao fechada");
            case LALexer.ERRO:
                throw new ErroSintaticoException("Linha " + linha + ": " + token.getText() + " - simbolo nao identificado");
            case Token.EOF:
                throw new ErroSintaticoException("Linha " + linha + ": erro sintatico proximo a EOF");
            default:
                throw new ErroSintaticoException("Linha " + linha + ": erro sintatico proximo a " + token.getText());
        }
    }
}
