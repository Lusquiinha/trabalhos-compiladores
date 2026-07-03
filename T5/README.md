# T5 — Gerador de Código C para a Linguagem LA

Trabalho 5 da disciplina **Construção de Compiladores** (DC/UFSCar).
Estende o compilador do T4 com um **gerador de código**: a partir de um programa escrito na Linguagem Algorítmica (LA), produz um programa **equivalente em C**, compilável com o **GCC**.

O executável final combina todas as fases anteriores (léxico + sintático + semântico) com o gerador. O comportamento depende da entrada:

- se a entrada tiver **algum erro** (léxico, sintático ou semântico), a saída contém a **descrição dos erros** (uma por linha), terminada por `Fim da compilacao`;
- se a entrada **não tiver erros**, a saída contém o **código C gerado**.

## Integrantes do grupo

- Lucas de Oliveira Rodrigues Alves — RA: 811943

---

## Requisitos

| Ferramenta | Versão mínima | Como verificar |
|------------|---------------|----------------|
| Java JDK   | 11            | `java -version` |
| Apache Maven | 3.6         | `mvn -version`  |
| GCC        | qualquer versão recente | `gcc --version` |

> O **ANTLR 4.13.2** é baixado automaticamente pelo Maven na primeira compilação (necessário acesso à internet).
> O **GCC** não é usado pelo compilador em si: ele serve apenas para **compilar o código C gerado** por este trabalho.

### Instalação no Ubuntu/Debian

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven gcc
```

### Instalação no Windows

1. Baixe o JDK 21 em <https://adoptium.net>
2. Baixe o Maven em <https://maven.apache.org/download.cgi> e adicione ao PATH
3. Instale o GCC (por exemplo via [MinGW-w64](https://www.mingw-w64.org/) ou [MSYS2](https://www.msys2.org/))
4. Verifique com `java -version`, `mvn -version` e `gcc --version` no terminal

---

## Como compilar

Na raiz deste projeto (pasta `T5/`), execute:

```bash
mvn clean package
```

Esse comando gera o lexer/parser (com o visitor) a partir de `LA.g4`, compila as classes Java e empacota tudo (incluindo o runtime do ANTLR) em `target/compilador.jar`.

---

## Como executar

São **obrigatórios dois argumentos** (entrada e saída, com caminho completo). A saída é gravada **no arquivo**, nunca no terminal.

```bash
java -jar target/compilador.jar <arquivo-entrada> <arquivo-saida>
```

**Exemplo:**

```bash
java -jar target/compilador.jar programa.la saida.c
```

Se `programa.la` for válido, `saida.c` conterá o código C. Para compilar e executar esse código:

```bash
gcc saida.c -o programa
./programa
```

---

## Exemplo de geração

Entrada (`programa.la`):

```
algoritmo
  declare
    x: literal
  leia(x)
  escreva(x)
fim_algoritmo
```

Saída produzida (`saida.c`):

```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
	char x[80];
	scanf("%s", x);
	printf("%s", x);
	return 0;
}
```

> O código gerado não precisa ser idêntico caractere a caractere ao dos casos de teste: ele apenas precisa **compilar com o GCC** e produzir a **mesma execução** (mesmo mapeamento entrada → saída).

---

## Mapeamento LA → C

### Tipos e declarações

| Construção LA | Código C gerado |
|---------------|-----------------|
| `x: inteiro` | `int x;` |
| `x: real` | `float x;` |
| `x: literal` | `char x[80];` |
| `x: logico` | `int x;` (verdadeiro → `1`, falso → `0`) |
| `x: ^inteiro` | `int* x;` |
| `vetor[5]: inteiro` | `int vetor[5];` |
| `constante c: inteiro = 8` | `#define c 8` (antes do `main`) |
| `declare r: registro ... fim_registro` | `struct { ... } r;` |
| `tipo t: registro ... fim_registro` | `typedef struct { ... } t;` |

Parâmetros do tipo `literal` são passados como `char*`.

### Comandos

| Comando LA | Código C gerado |
|------------|-----------------|
| `leia(x)` | `scanf(...)` (`%d`, `%f` ou `%s` conforme o tipo) |
| `escreva(a, b, ...)` | um `printf` por argumento (`%d`, `%f`, `%s` ou cadeia literal) |
| `x <- e` | `x = e;` (ou `strcpy(x, e)` quando `x` é literal) |
| `^p <- e` | `*p = e;` |
| `se ... entao ... senao ... fim_se` | `if (...) { ... } else { ... }` |
| `caso e seja ... fim_caso` | `switch (e) { case ...: ... break; default: ... }` |
| `para i <- a ate b faca ... fim_para` | `for (i = a; i <= b; i++) { ... }` |
| `enquanto c faca ... fim_enquanto` | `while (c) { ... }` |
| `faca ... ate c` | `do { ... } while (c);` |
| `retorne e` | `return e;` |
| chamada de procedimento | `nome(args);` |

### Operadores

| LA | C |
|----|---|
| `=` | `==` |
| `<>` | `!=` |
| `e` | `&&` |
| `ou` | `\|\|` |
| `nao` | `!` |
| `&x` | `&x` (endereço) |
| `^p` | `*p` (desreferência) |

Os intervalos do comando `caso` (por exemplo `0..7`) são expandidos em uma sequência de rótulos `case`.

---

## Estrutura do projeto

```
T5/
├── pom.xml                              # Configuração do Maven (ANTLR com visitor + shade)
├── README.md                            # Este arquivo
├── src/main/antlr4/br/ufscar/dc/compiladores/la/gerador/
│   └── LA.g4                            # Gramática combinada (lexer + parser) da LA
└── src/main/java/br/ufscar/dc/compiladores/la/gerador/
    ├── Main.java                        # Ponto de entrada: roda a analise e, sem erros, gera o C
    ├── LASemantico.java                 # Analisador semantico (reaproveitado do T4)
    ├── Gerador.java                     # Gerador de codigo C (percorre a arvore e emite o programa)
    ├── Escopos.java                     # Pilha de tabelas de simbolos (um nivel por escopo)
    ├── TabelaDeSimbolos.java            # Tabela de simbolos (nome, categoria, tipo, parametros)
    └── Tipo.java                        # Representacao de tipos: basicos, ponteiro, endereco e registro
```

---

## Como funciona

1. O `Main` usa o lexer/parser do ANTLR para construir a árvore sintática da entrada.
2. O `LASemantico` (reaproveitado do T4) percorre a árvore verificando a tabela de símbolos e a compatibilidade de tipos, acumulando os erros encontrados.
3. Se **houver erros**, o `Main` grava a lista de erros seguida de `Fim da compilacao`.
4. Se **não houver erros**, o `Gerador` percorre a mesma árvore:
   - reconstrói a tabela de símbolos para saber o tipo de cada identificador (necessário para escolher `%d`/`%f`/`%s` e para decidir entre `=` e `strcpy`);
   - traduz declarações (variáveis, constantes, tipos, registros, vetores, ponteiros) para as construções C correspondentes;
   - traduz as sub-rotinas (`funcao`/`procedimento`) para funções C definidas antes do `main`;
   - traduz cada comando e cada expressão para C, ajustando operadores e formatos;
   - monta o programa final com os cabeçalhos (`stdio.h`, `stdlib.h`, `string.h`), a seção global e o `main`.
5. O código C resultante é gravado no arquivo de saída. Ele pode então ser compilado com o GCC e executado.
