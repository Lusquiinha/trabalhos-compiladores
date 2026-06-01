# T3 — Analisador Semântico para a Linguagem LA

Trabalho 3 da disciplina **Construção de Compiladores** (DC/UFSCar).
Implementa parte de um analisador **semântico** para a Linguagem Algorítmica (LA), construído sobre o lexer/parser gerados pelo **ANTLR 4** (mesma gramática do T2) e um **visitor** que percorre a árvore sintática verificando a tabela de símbolos e a compatibilidade de tipos.

Diferente do T2, o analisador **não para no primeiro erro**: ele reporta todos os erros semânticos encontrados, em ordem de linha, e grava o resultado em arquivo.

## Integrantes do grupo

- Lucas de Oliveira Rodrigues Alves — RA: 811943

---

## Erros semânticos detectados

| Mensagem | Quando ocorre |
|----------|---------------|
| `Linha X: tipo <nome> nao declarado` | tipo usado em uma declaração que não é básico (`inteiro`, `real`, `literal`, `logico`) nem foi declarado |
| `Linha X: identificador <nome> ja declarado anteriormente` | identificador redeclarado no mesmo escopo (mesmo que para categorias diferentes) |
| `Linha X: identificador <nome> nao declarado` | uso de variável/constante/identificador não declarado |
| `Linha X: atribuicao nao compativel para <nome>` | tipo da expressão é incompatível com o tipo declarado do destino |

Regras de compatibilidade de tipos:

- `(real \| inteiro) <- (real \| inteiro)`
- `literal <- literal`
- `logico <- logico`
- `registro <- registro` (mesmo nome de tipo)

Ao combinar tipos incompatíveis em uma expressão (por exemplo `literal + inteiro`), o resultado é `tipo_indefinido`, o que inviabiliza a atribuição.

---

## Requisitos

| Ferramenta | Versão mínima | Como verificar |
|------------|---------------|----------------|
| Java JDK   | 11            | `java -version` |
| Apache Maven | 3.6         | `mvn -version`  |

> O **ANTLR 4.13.2** é baixado automaticamente pelo Maven na primeira compilação (necessário acesso à internet).

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

Na raiz deste projeto (pasta `T3/`), execute:

```bash
mvn clean package
```

Esse comando gera o lexer/parser **com o visitor** a partir de `LA.g4`, compila as classes Java e empacota tudo (incluindo o runtime do ANTLR) em `target/compilador.jar`.

---

## Como executar

São **obrigatórios dois argumentos** (entrada e saída, com caminho completo). A saída é gravada **no arquivo**, nunca no terminal.

```bash
java -jar target/compilador.jar <arquivo-entrada> <arquivo-saida>
```

**Exemplo:**

```bash
java -jar target/compilador.jar programa.la saida.txt
```

---

## Saída produzida

Uma linha por erro semântico encontrado (em ordem de linha), seguida da linha final:

```
Linha 7: tipo inteir nao declarado
Linha 11: identificador idades nao declarado
Fim da compilacao
```

Quando não há erros, a saída contém apenas `Fim da compilacao`.

---

## Estrutura do projeto

```
T3/
├── pom.xml                              # Configuração do Maven (ANTLR com visitor + shade)
├── README.md                            # Este arquivo
├── src/main/antlr4/br/ufscar/dc/compiladores/la/semantico/
│   └── LA.g4                            # Gramática combinada (lexer + parser) da LA
└── src/main/java/br/ufscar/dc/compiladores/la/semantico/
    ├── Main.java                        # Ponto de entrada: lê/escreve arquivos, aciona o visitor
    ├── LASemantico.java                 # Visitor: tabela de símbolos e verificação de tipos
    ├── Escopos.java                     # Pilha de tabelas de símbolos (um nível por escopo)
    └── TabelaDeSimbolos.java            # Tabela de símbolos e enum dos tipos da LA
```

---

## Como funciona

1. O `Main` usa o lexer/parser do ANTLR para construir a árvore sintática (as entradas do T3 são sintaticamente válidas).
2. O `LASemantico` (subclasse de `LABaseVisitor`) percorre a árvore:
   - nas **declarações**, registra cada identificador na tabela de símbolos do escopo atual, detectando redeclarações e tipos inexistentes;
   - nos **comandos**, verifica se os identificadores usados foram declarados e calcula o tipo das expressões para validar atribuições.
3. Os erros são acumulados em uma lista (na ordem em que aparecem) e, ao final, são gravados no arquivo de saída seguidos de `Fim da compilacao`.
