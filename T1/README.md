# T1 — Analisador Léxico para a Linguagem LA

Trabalho 1 da disciplina **Construção de Compiladores** (DC/UFSCar).  
Implementa um analisador léxico para a Linguagem Algorítmica (LA) desenvolvida pelo prof. Jander.

## Integrantes do grupo

- Lucas de Oliveira Rodrigues Alves — RA: 811943

---

## Requisitos

| Ferramenta | Versão mínima | Como verificar |
|------------|---------------|----------------|
| Java JDK   | 11            | `java -version` |
| Apache Maven | 3.6         | `mvn -version`  |

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

Na raiz do projeto (pasta `T1/`), execute:

```bash
mvn package
```

Isso gera o arquivo `target/compilador.jar`.

---

## Como executar

```bash
java -jar target/compilador.jar <arquivo-entrada> <arquivo-saida>
```

**Exemplo:**

```bash
java -jar target/compilador.jar programa.la saida.txt
```

- `<arquivo-entrada>` — caminho para o arquivo-fonte em LA
- `<arquivo-saida>`   — caminho onde a lista de tokens será gravada

---

## Saída produzida

Cada token é escrito em uma linha no formato:

```
<'lexema','tipo'>    ← para palavras reservadas, operadores e pontuação
<'lexema',TIPO>      ← para IDENT, NUM_INT, NUM_REAL e CADEIA
```

Em caso de erro léxico, os tokens reconhecidos até o momento são escritos, seguidos pela mensagem de erro:

```
Linha X: <símbolo> - simbolo nao identificado
Linha X: comentario nao fechado
Linha X: cadeia literal nao fechada
```

---

## Estrutura do projeto

```
T1/
├── pom.xml                          # Configuração do Maven
├── README.md                        # Este arquivo
└── src/main/java/br/ufscar/dc/compiladores/la/lexico/
    ├── Main.java    # Ponto de entrada: lê arquivos e aciona o Lexer
    ├── Lexer.java   # Analisador léxico (autômato finito determinístico)
    └── Token.java   # Representação de um token (lexema + tipo)
```
