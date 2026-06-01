package br.ufscar.dc.compiladores.la.lexico;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java -jar compilador.jar <entrada> <saida>");
            System.exit(1);
        }

        String inputPath  = args[0];
        String outputPath = args[1];

        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(inputPath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de entrada: " + e.getMessage());
            System.exit(1);
            return;
        }

        Lexer lexer = new Lexer(source);
        try (PrintWriter out = new PrintWriter(outputPath, "UTF-8")) {
            Token t;
            while ((t = lexer.nextToken()) != null) {
                out.println(t);
            }
            if (lexer.getError() != null) {
                out.println(lexer.getError());
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever arquivo de saida: " + e.getMessage());
            System.exit(1);
        }
    }
}
