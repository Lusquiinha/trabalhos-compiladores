# T2 — Analisador Sintático para a Linguagem LA

Trabalho 2 da disciplina **Construção de Compiladores** (DC/UFSCar).
Implementa um analisador **sintático** para a Linguagem Algorítmica (LA) desenvolvida pelo prof. Jander, construído sobre o gerador de parsers **ANTLR 4**.

O analisador lê um programa-fonte em LA, aponta o **primeiro** erro encontrado — léxico ou sintático — indicando a **linha** e o **lexema** que causou a detecção, e grava o resultado em um arquivo. Os erros léxicos detectados no T1 (comentário não fechado, cadeia literal não fechada e símbolo não identificado) continuam sendo reconhecidos.

## Integrantes do grupo

- Lucas de Oliveira Rodrigues Alves — RA: 811943

---

## Requisitos

| Ferramenta | Versão mínima | Como verificar |
|------------|---------------|----------------|
| Java JDK   | 11            | `java -version` |
| Apache Maven | 3.6         | `mvn -version`  |

> O **ANTLR 4.13.2** (gerador do lexer/parser e biblioteca de runtime) **não precisa ser instalado manualmente**: o Maven o baixa automaticamente durante a compilação, conforme declarado no `pom.xml`. É necessário acesso à internet na primeira compilação.

### Instalação no Ubuntu/Debian

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven
```

### Instalação no Windows

1. Baixe o JDK 21 em <https://adoptium.net>
2. Baixe o Maven em <https://maven.apache.org/download.cgi> e adicione ao PATH
3. Verifique com `java -version` e `mvn -version` no terminal

---

## Como compilar

Na raiz deste projeto (pasta `T2/`), execute:

```bash
mvn clean package
```

Esse comando:

1. gera o **lexer** e o **parser** a partir da gramática `LA.g4` (plugin `antlr4-maven-plugin`);
2. compila as classes Java;
3. empacota tudo, **incluindo o runtime do ANTLR**, em um JAR executável (plugin `maven-shade-plugin`).

O resultado é o arquivo `target/compilador.jar`.

---

## Como executar

São **obrigatórios dois argumentos**: o arquivo de entrada e o arquivo de saída (ambos com caminho completo). A saída é gravada **no arquivo**, nunca no terminal.

```bash
java -jar target/compilador.jar <arquivo-entrada> <arquivo-saida>
```

**Exemplo:**

```bash
java -jar target/compilador.jar programa.la saida.txt
```

- `<arquivo-entrada>` — caminho para o arquivo-fonte em LA
- `<arquivo-saida>`   — caminho onde o resultado da análise será gravado

---

## Saída produzida

Quando o programa está sintaticamente correto, a saída contém apenas:

```
Fim da compilacao
```

Quando há erro, é impressa a mensagem do **primeiro** erro detectado, seguida da linha final:

```
Linha X: erro sintatico proximo a <lexema>      ← erro sintático
Linha X: <símbolo> - simbolo nao identificado    ← erro léxico (símbolo desconhecido)
Linha X: comentario nao fechado                  ← erro léxico
Linha X: cadeia literal nao fechada              ← erro léxico
Fim da compilacao
```

Para indicar o fim do arquivo, o lexema reportado é `EOF`. Para cadeias literais, o lexema é exibido entre aspas (as aspas fazem parte do token).

---

## Estrutura do projeto

```
T2/
├── pom.xml                              # Configuração do Maven (ANTLR + shade)
├── README.md                            # Este arquivo
├── src/main/antlr4/br/ufscar/dc/compiladores/la/sintatico/
│   └── LA.g4                            # Gramática combinada (lexer + parser) da LA
└── src/main/java/br/ufscar/dc/compiladores/la/sintatico/
    ├── Main.java                        # Ponto de entrada: lê/escreve arquivos e aciona o parser
    ├── SintaticoErrorListener.java      # Captura o 1º erro e formata a mensagem
    └── ErroSintaticoException.java      # Exceção usada para interromper a análise no 1º erro
```

---

## Como funciona

1. A gramática oficial da LA foi transcrita para o arquivo `LA.g4`. As regras léxicas
   incluem três tokens especiais (`COMENTARIO_NAO_FECHADO`, `CADEIA_NAO_FECHADA` e `ERRO`)
   que capturam exatamente os erros léxicos previstos no T1.
2. O `Main` cria o lexer e o parser gerados pelo ANTLR e remove os *listeners* de erro
   padrão, instalando o `SintaticoErrorListener`.
3. Ao primeiro erro, o `SintaticoErrorListener` lança uma `ErroSintaticoException` com a
   mensagem já formatada, o que **interrompe a análise** (garantindo que apenas o primeiro
   erro seja reportado, como exigido nos casos de teste).
4. O `Main` grava a mensagem de erro (se houver) e a linha `Fim da compilacao` no arquivo de saída.
