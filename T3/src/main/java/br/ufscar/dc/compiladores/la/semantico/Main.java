package br.ufscar.dc.compiladores.la.semantico;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java -jar compilador.jar <entrada> <saida>");
            System.exit(1);
        }

        String caminhoEntrada = args[0];
        String caminhoSaida = args[1];

        CharStream entrada;
        try {
            entrada = CharStreams.fromFileName(caminhoEntrada, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de entrada: " + e.getMessage());
            System.exit(1);
            return;
        }

        LALexer lexer = new LALexer(entrada);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LAParser parser = new LAParser(tokens);
        LAParser.ProgramaContext arvore = parser.programa();

        LASemantico analisador = new LASemantico();
        analisador.visit(arvore);

        try (PrintWriter saida = new PrintWriter(caminhoSaida, "UTF-8")) {
            for (String erro : analisador.getErros()) {
                saida.println(erro);
            }
            saida.println("Fim da compilacao");
        } catch (IOException e) {
            System.err.println("Erro ao escrever arquivo de saida: " + e.getMessage());
            System.exit(1);
        }
    }
}
