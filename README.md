# Trabalhos de Construção de Compiladores

Compilador para a **Linguagem Algorítmica (LA)** desenvolvida pelo prof. Jander (DC/UFSCar), construído incrementalmente ao longo dos trabalhos da disciplina **Construção de Compiladores**.

## Integrantes do grupo

- Lucas de Oliveira Rodrigues Alves — RA: 811943

## Trabalhos

| Trabalho | Etapa | Descrição breve | Documentação |
|----------|-------|-----------------|--------------|
| **T1** | Análise léxica | Reconhece os tokens da LA e aponta erros léxicos (símbolo não identificado, comentário e cadeia não fechados). | [T1/README.md](T1/README.md) |
| **T2** | Análise sintática | Verifica a estrutura do programa via gramática ANTLR e aponta o primeiro erro sintático (ou léxico). | [T2/README.md](T2/README.md) |
| **T3** | Análise semântica | Percorre a árvore com um visitor, checando tabela de símbolos e compatibilidade de tipos, reportando todos os erros semânticos. | [T3/README.md](T3/README.md) |

Cada pasta (`T1/`, `T2/`, `T3/`) é um projeto Maven independente, com seu próprio README de documentação externa (requisitos, como compilar e executar).

## Requisitos

- Java JDK 11+
- Apache Maven 3.6+

(O ANTLR 4.13.2 é baixado automaticamente pelo Maven na primeira compilação.)

## Compilação e testes

Há um `Makefile` na raiz que compila cada trabalho e executa o corretor automático oficial:

```bash
make build      # compila T1, T2 e T3
make test       # compila e corrige T1, T2 e T3
make test-t1    # apenas T1 
make test-t2    # apenas T2 
make test-t3    # apenas T3 
make clean      # limpa os artefatos de build
```

> O `Makefile` assume que as pastas `compiladores-corretor-automatico/` e `casos-de-teste/` estão no diretório-pai deste repositório. Esses caminhos (e o RA do grupo) podem ser sobrescritos, ex.: `make test-t3 CASOS=/caminho/para/casos-de-teste`.

Para compilar/executar um trabalho isoladamente, veja o README da respectiva pasta.
