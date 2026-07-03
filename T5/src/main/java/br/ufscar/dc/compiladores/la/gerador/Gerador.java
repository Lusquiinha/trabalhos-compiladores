package br.ufscar.dc.compiladores.la.gerador;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import br.ufscar.dc.compiladores.la.gerador.TabelaDeSimbolos.Categoria;
import br.ufscar.dc.compiladores.la.gerador.TabelaDeSimbolos.EntradaTabela;

public class Gerador {

    private final Escopos escopos = new Escopos();
    private final StringBuilder globais = new StringBuilder();
    private StringBuilder saida;
    private int nivel;
    private int contadorRegistroAnonimo = 0;

    public String gerar(LAParser.ProgramaContext ctx) {
        StringBuilder principal = new StringBuilder();

        saida = globais;
        nivel = 0;
        for (LAParser.Decl_local_globalContext decl : ctx.declaracoes().decl_local_global()) {
            gerarDeclaracaoGlobalOuLocal(decl);
        }

        saida = principal;
        nivel = 1;
        gerarCorpo(ctx.corpo());

        StringBuilder resultado = new StringBuilder();
        resultado.append("#include <stdio.h>\n");
        resultado.append("#include <stdlib.h>\n");
        resultado.append("#include <string.h>\n");
        resultado.append("\n");
        if (globais.length() > 0) {
            resultado.append(globais);
            resultado.append("\n");
        }
        resultado.append("int main() {\n");
        resultado.append(principal);
        resultado.append("\treturn 0;\n");
        resultado.append("}\n");
        return resultado.toString();
    }

    private void linha(String texto) {
        for (int i = 0; i < nivel; i++) {
            saida.append('\t');
        }
        saida.append(texto).append('\n');
    }

    private void gerarDeclaracaoGlobalOuLocal(LAParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null) {
            gerarDeclaracaoLocal(ctx.declaracao_local());
        } else {
            gerarSubrotina(ctx.declaracao_global());
        }
    }

    private void gerarCorpo(LAParser.CorpoContext ctx) {
        for (LAParser.Declaracao_localContext d : ctx.declaracao_local()) {
            gerarDeclaracaoLocal(d);
        }
        for (LAParser.CmdContext c : ctx.cmd()) {
            gerarCmd(c);
        }
    }

    private void gerarDeclaracaoLocal(LAParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            emitirDeclaracaoVariavel(ctx.variavel(), true);
            return;
        }

        String palavraReservada = ctx.getStart().getText();
        String nome = ctx.IDENT().getText();

        if (palavraReservada.equals("constante")) {
            Tipo tipo = resolverTipoBasico(ctx.tipo_basico());
            escopos.obterEscopoAtual().adicionar(nome, Categoria.CONSTANTE, tipo);
            globais.append("#define ").append(nome).append(' ')
                    .append(valorConstanteC(ctx.valor_constante())).append('\n');
        } else {
            Tipo tipo;
            if (ctx.tipo().registro() != null) {
                tipo = construirRegistro(ctx.tipo().registro(), nome);
                escopos.obterEscopoAtual().adicionar(nome, Categoria.TIPO, tipo);
                linha("typedef struct {");
                nivel++;
                for (LAParser.VariavelContext campo : ctx.tipo().registro().variavel()) {
                    emitirDeclaracaoVariavel(campo, false);
                }
                nivel--;
                linha("} " + nome + ";");
            } else {
                tipo = resolverTipo(ctx.tipo());
                escopos.obterEscopoAtual().adicionar(nome, Categoria.TIPO, tipo);
                linha("typedef " + tipoBaseC(tipo) + " " + nome + ";");
            }
        }
    }

    private void emitirDeclaracaoVariavel(LAParser.VariavelContext ctx, boolean registrar) {
        Tipo tipo = resolverTipo(ctx.tipo());

        if (ctx.tipo().registro() != null) {
            linha("struct {");
            nivel++;
            for (LAParser.VariavelContext campo : ctx.tipo().registro().variavel()) {
                emitirDeclaracaoVariavel(campo, false);
            }
            nivel--;
            StringBuilder nomes = new StringBuilder();
            for (int i = 0; i < ctx.identificador().size(); i++) {
                if (i > 0) {
                    nomes.append(", ");
                }
                nomes.append(ctx.identificador(i).IDENT(0).getText());
            }
            linha("} " + nomes + ";");
            if (registrar) {
                for (LAParser.IdentificadorContext id : ctx.identificador()) {
                    escopos.obterEscopoAtual().adicionar(id.IDENT(0).getText(), Categoria.VARIAVEL, tipo);
                }
            }
            return;
        }

        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            String nome = id.IDENT(0).getText();
            if (registrar) {
                escopos.obterEscopoAtual().adicionar(nome, Categoria.VARIAVEL, tipo);
            }
            if (id.dimensao() != null && !id.dimensao().exp_aritmetica().isEmpty()) {
                StringBuilder dims = new StringBuilder();
                for (LAParser.Exp_aritmeticaContext e : id.dimensao().exp_aritmetica()) {
                    dims.append('[').append(gerarExpAritmetica(e)).append(']');
                }
                linha(tipoBaseC(tipo) + " " + nome + dims + ";");
            } else if (tipo.categoria == Tipo.Categoria.LITERAL) {
                linha("char " + nome + "[80];");
            } else {
                linha(tipoBaseC(tipo) + " " + nome + ";");
            }
        }
    }

    private void gerarSubrotina(LAParser.Declaracao_globalContext ctx) {
        boolean ehFuncao = ctx.getStart().getText().equals("funcao");
        String nome = ctx.IDENT().getText();

        List<Tipo> tiposParametros = new ArrayList<>();
        StringBuilder assinatura = new StringBuilder();
        if (ctx.parametros() != null) {
            boolean primeiro = true;
            for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
                Tipo tipoParam = resolverTipoEstendido(p.tipo_estendido());
                String tipoParamC = tipoParametroC(p.tipo_estendido());
                for (LAParser.IdentificadorContext id : p.identificador()) {
                    tiposParametros.add(tipoParam);
                    if (!primeiro) {
                        assinatura.append(", ");
                    }
                    assinatura.append(tipoParamC).append(' ').append(id.IDENT(0).getText());
                    primeiro = false;
                }
            }
        }

        Tipo tipoRetorno = ehFuncao ? resolverTipoEstendido(ctx.tipo_estendido()) : Tipo.INVALIDO;
        Categoria categoria = ehFuncao ? Categoria.FUNCAO : Categoria.PROCEDIMENTO;
        escopos.obterEscopoAtual().adicionar(
                new EntradaTabela(nome, categoria, tipoRetorno, tiposParametros, tipoRetorno));

        String tipoRetornoC = ehFuncao ? tipoBaseC(tipoRetorno) : "void";
        globais.append(tipoRetornoC).append(' ').append(nome)
                .append('(').append(assinatura).append(") {\n");

        escopos.criarNovoEscopo();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
                Tipo tipoParam = resolverTipoEstendido(p.tipo_estendido());
                for (LAParser.IdentificadorContext id : p.identificador()) {
                    escopos.obterEscopoAtual().adicionar(id.IDENT(0).getText(), Categoria.VARIAVEL, tipoParam);
                }
            }
        }

        saida = globais;
        nivel = 1;
        for (LAParser.Declaracao_localContext d : ctx.declaracao_local()) {
            gerarDeclaracaoLocal(d);
        }
        for (LAParser.CmdContext c : ctx.cmd()) {
            gerarCmd(c);
        }
        nivel = 0;
        globais.append("}\n\n");
        escopos.abandonarEscopo();
    }

    private void gerarCmd(LAParser.CmdContext ctx) {
        if (ctx.cmdLeia() != null) {
            gerarCmdLeia(ctx.cmdLeia());
        } else if (ctx.cmdEscreva() != null) {
            gerarCmdEscreva(ctx.cmdEscreva());
        } else if (ctx.cmdSe() != null) {
            gerarCmdSe(ctx.cmdSe());
        } else if (ctx.cmdCaso() != null) {
            gerarCmdCaso(ctx.cmdCaso());
        } else if (ctx.cmdPara() != null) {
            gerarCmdPara(ctx.cmdPara());
        } else if (ctx.cmdEnquanto() != null) {
            gerarCmdEnquanto(ctx.cmdEnquanto());
        } else if (ctx.cmdFaca() != null) {
            gerarCmdFaca(ctx.cmdFaca());
        } else if (ctx.cmdAtribuicao() != null) {
            gerarCmdAtribuicao(ctx.cmdAtribuicao());
        } else if (ctx.cmdChamada() != null) {
            gerarCmdChamada(ctx.cmdChamada());
        } else if (ctx.cmdRetorne() != null) {
            gerarCmdRetorne(ctx.cmdRetorne());
        }
    }

    private void gerarCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            Tipo tipo = tipoDoIdentificador(id);
            String nome = nomeC(id);
            boolean deref = ehIdentificadorDesreferenciado(ctx, id);
            String alvo = deref ? "*" + nome : nome;
            Tipo tipoLido = deref ? desreferenciar(tipo) : tipo;
            if (tipoLido != null && tipoLido.categoria == Tipo.Categoria.LITERAL) {
                linha("scanf(\"%s\", " + alvo + ");");
            } else if (tipoLido != null && tipoLido.categoria == Tipo.Categoria.REAL) {
                linha("scanf(\"%f\", &" + alvo + ");");
            } else {
                linha("scanf(\"%d\", &" + alvo + ");");
            }
        }
    }

    private boolean ehIdentificadorDesreferenciado(LAParser.CmdLeiaContext ctx, LAParser.IdentificadorContext id) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) == id && i > 0 && ctx.getChild(i - 1).getText().equals("^")) {
                return true;
            }
        }
        return false;
    }

    private void gerarCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (LAParser.ExpressaoContext e : ctx.expressao()) {
            String cadeia = stringConstante(e);
            if (cadeia != null) {
                linha("printf(" + cadeia + ");");
                continue;
            }
            Tipo tipo = tipoDe(e);
            String codigo = gerarExpressao(e);
            if (tipo != null && tipo.categoria == Tipo.Categoria.LITERAL) {
                linha("printf(\"%s\", " + codigo + ");");
            } else if (tipo != null && tipo.categoria == Tipo.Categoria.REAL) {
                linha("printf(\"%f\", " + codigo + ");");
            } else {
                linha("printf(\"%d\", " + codigo + ");");
            }
        }
    }

    private void gerarCmdSe(LAParser.CmdSeContext ctx) {
        List<LAParser.CmdContext> entao = new ArrayList<>();
        List<LAParser.CmdContext> senao = new ArrayList<>();
        boolean temSenao = false;
        boolean depoisDoSenao = false;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof TerminalNode
                    && ctx.getChild(i).getText().equals("senao")) {
                depoisDoSenao = true;
                temSenao = true;
            } else if (ctx.getChild(i) instanceof LAParser.CmdContext) {
                (depoisDoSenao ? senao : entao).add((LAParser.CmdContext) ctx.getChild(i));
            }
        }

        linha("if (" + gerarExpressao(ctx.expressao()) + ") {");
        nivel++;
        for (LAParser.CmdContext c : entao) {
            gerarCmd(c);
        }
        nivel--;
        linha("}");
        if (temSenao) {
            linha("else {");
            nivel++;
            for (LAParser.CmdContext c : senao) {
                gerarCmd(c);
            }
            nivel--;
            linha("}");
        }
    }

    private void gerarCmdCaso(LAParser.CmdCasoContext ctx) {
        linha("switch (" + gerarExpAritmetica(ctx.exp_aritmetica()) + ") {");
        nivel++;
        for (LAParser.Item_selecaoContext item : ctx.selecao().item_selecao()) {
            for (LAParser.Numero_intervaloContext intervalo : item.constantes().numero_intervalo()) {
                int inicio = valorNumeroIntervalo(intervalo, 0);
                int fim = intervalo.NUM_INT().size() > 1 ? valorNumeroIntervalo(intervalo, 1) : inicio;
                for (int v = inicio; v <= fim; v++) {
                    linha("case " + v + ":");
                }
            }
            nivel++;
            for (LAParser.CmdContext c : item.cmd()) {
                gerarCmd(c);
            }
            linha("break;");
            nivel--;
        }
        if (temSenaoNoCaso(ctx)) {
            linha("default:");
            nivel++;
            for (LAParser.CmdContext c : ctx.cmd()) {
                gerarCmd(c);
            }
            nivel--;
        }
        nivel--;
        linha("}");
    }

    private boolean temSenaoNoCaso(LAParser.CmdCasoContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) instanceof TerminalNode
                    && ctx.getChild(i).getText().equals("senao")) {
                return true;
            }
        }
        return false;
    }

    private int valorNumeroIntervalo(LAParser.Numero_intervaloContext ctx, int indice) {
        TerminalNode numero = ctx.NUM_INT(indice);
        int valor = Integer.parseInt(numero.getText());
        for (int i = 1; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i) == numero && ctx.getChild(i - 1).getText().equals("-")) {
                valor = -valor;
            }
        }
        return valor;
    }

    private void gerarCmdPara(LAParser.CmdParaContext ctx) {
        String var = ctx.IDENT().getText();
        String inicio = gerarExpAritmetica(ctx.exp_aritmetica(0));
        String fim = gerarExpAritmetica(ctx.exp_aritmetica(1));
        linha("for (" + var + " = " + inicio + "; " + var + " <= " + fim + "; " + var + "++) {");
        nivel++;
        for (LAParser.CmdContext c : ctx.cmd()) {
            gerarCmd(c);
        }
        nivel--;
        linha("}");
    }

    private void gerarCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        linha("while (" + gerarExpressao(ctx.expressao()) + ") {");
        nivel++;
        for (LAParser.CmdContext c : ctx.cmd()) {
            gerarCmd(c);
        }
        nivel--;
        linha("}");
    }

    private void gerarCmdFaca(LAParser.CmdFacaContext ctx) {
        linha("do {");
        nivel++;
        for (LAParser.CmdContext c : ctx.cmd()) {
            gerarCmd(c);
        }
        nivel--;
        linha("} while (" + gerarExpressao(ctx.expressao()) + ");");
    }

    private void gerarCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        boolean acento = ctx.getStart().getText().equals("^");
        LAParser.IdentificadorContext id = ctx.identificador();
        String nome = (acento ? "*" : "") + nomeC(id);
        Tipo tipo = tipoDoIdentificador(id);
        if (acento) {
            tipo = desreferenciar(tipo);
        }
        String valor = gerarExpressao(ctx.expressao());
        if (tipo != null && tipo.categoria == Tipo.Categoria.LITERAL) {
            linha("strcpy(" + nome + ", " + valor + ");");
        } else {
            linha(nome + " = " + valor + ";");
        }
    }

    private void gerarCmdChamada(LAParser.CmdChamadaContext ctx) {
        linha(ctx.IDENT().getText() + "(" + gerarListaExpressoes(ctx.expressao()) + ");");
    }

    private void gerarCmdRetorne(LAParser.CmdRetorneContext ctx) {
        linha("return " + gerarExpressao(ctx.expressao()) + ";");
    }

    private String gerarListaExpressoes(List<LAParser.ExpressaoContext> expressoes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressoes.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(gerarExpressao(expressoes.get(i)));
        }
        return sb.toString();
    }

    private String gerarExpressao(LAParser.ExpressaoContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<LAParser.Termo_logicoContext> termos = ctx.termo_logico();
        sb.append(gerarTermoLogico(termos.get(0)));
        for (int i = 1; i < termos.size(); i++) {
            sb.append(" || ").append(gerarTermoLogico(termos.get(i)));
        }
        return sb.toString();
    }

    private String gerarTermoLogico(LAParser.Termo_logicoContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<LAParser.Fator_logicoContext> fatores = ctx.fator_logico();
        sb.append(gerarFatorLogico(fatores.get(0)));
        for (int i = 1; i < fatores.size(); i++) {
            sb.append(" && ").append(gerarFatorLogico(fatores.get(i)));
        }
        return sb.toString();
    }

    private String gerarFatorLogico(LAParser.Fator_logicoContext ctx) {
        String parcela = gerarParcelaLogica(ctx.parcela_logica());
        if (ctx.getChildCount() > 1) {
            return "!" + parcela;
        }
        return parcela;
    }

    private String gerarParcelaLogica(LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return gerarExpRelacional(ctx.exp_relacional());
        }
        return ctx.getText().equals("verdadeiro") ? "1" : "0";
    }

    private String gerarExpRelacional(LAParser.Exp_relacionalContext ctx) {
        List<LAParser.Exp_aritmeticaContext> expressoes = ctx.exp_aritmetica();
        String esquerda = gerarExpAritmetica(expressoes.get(0));
        if (expressoes.size() > 1) {
            String operador = operadorRelacionalC(ctx.op_relacional().getText());
            return esquerda + " " + operador + " " + gerarExpAritmetica(expressoes.get(1));
        }
        return esquerda;
    }

    private String operadorRelacionalC(String operador) {
        switch (operador) {
            case "=":
                return "==";
            case "<>":
                return "!=";
            default:
                return operador;
        }
    }

    private String gerarExpAritmetica(LAParser.Exp_aritmeticaContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<LAParser.TermoContext> termos = ctx.termo();
        sb.append(gerarTermo(termos.get(0)));
        for (int i = 1; i < termos.size(); i++) {
            sb.append(' ').append(ctx.op1(i - 1).getText()).append(' ').append(gerarTermo(termos.get(i)));
        }
        return sb.toString();
    }

    private String gerarTermo(LAParser.TermoContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<LAParser.FatorContext> fatores = ctx.fator();
        sb.append(gerarFator(fatores.get(0)));
        for (int i = 1; i < fatores.size(); i++) {
            sb.append(' ').append(ctx.op2(i - 1).getText()).append(' ').append(gerarFator(fatores.get(i)));
        }
        return sb.toString();
    }

    private String gerarFator(LAParser.FatorContext ctx) {
        StringBuilder sb = new StringBuilder();
        List<LAParser.ParcelaContext> parcelas = ctx.parcela();
        sb.append(gerarParcela(parcelas.get(0)));
        for (int i = 1; i < parcelas.size(); i++) {
            sb.append(' ').append(ctx.op3(i - 1).getText()).append(' ').append(gerarParcela(parcelas.get(i)));
        }
        return sb.toString();
    }

    private String gerarParcela(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            String prefixo = ctx.op_unario() != null ? "-" : "";
            return prefixo + gerarParcelaUnario(ctx.parcela_unario());
        }
        return gerarParcelaNaoUnario(ctx.parcela_nao_unario());
    }

    private String gerarParcelaUnario(LAParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String prefixo = ctx.getStart().getText().equals("^") ? "*" : "";
            return prefixo + nomeC(ctx.identificador());
        }
        if (ctx.IDENT() != null) {
            return ctx.IDENT().getText() + "(" + gerarListaExpressoes(ctx.expressao()) + ")";
        }
        if (ctx.NUM_INT() != null) {
            return ctx.NUM_INT().getText();
        }
        if (ctx.NUM_REAL() != null) {
            return ctx.NUM_REAL().getText();
        }
        return "(" + gerarExpressao(ctx.expressao(0)) + ")";
    }

    private String gerarParcelaNaoUnario(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return ctx.CADEIA().getText();
        }
        return "&" + nomeC(ctx.identificador());
    }

    private String nomeC(LAParser.IdentificadorContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao() != null) {
            for (LAParser.Exp_aritmeticaContext e : ctx.dimensao().exp_aritmetica()) {
                sb.append('[').append(gerarExpAritmetica(e)).append(']');
            }
        }
        return sb.toString();
    }

    private String stringConstante(LAParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() != 1) {
            return null;
        }
        LAParser.Termo_logicoContext tl = ctx.termo_logico(0);
        if (tl.fator_logico().size() != 1) {
            return null;
        }
        LAParser.Fator_logicoContext fl = tl.fator_logico(0);
        if (fl.getChildCount() > 1) {
            return null;
        }
        LAParser.Parcela_logicaContext pl = fl.parcela_logica();
        if (pl.exp_relacional() == null) {
            return null;
        }
        LAParser.Exp_relacionalContext er = pl.exp_relacional();
        if (er.exp_aritmetica().size() != 1) {
            return null;
        }
        LAParser.Exp_aritmeticaContext ea = er.exp_aritmetica(0);
        if (ea.termo().size() != 1) {
            return null;
        }
        LAParser.TermoContext termo = ea.termo(0);
        if (termo.fator().size() != 1) {
            return null;
        }
        LAParser.FatorContext fator = termo.fator(0);
        if (fator.parcela().size() != 1) {
            return null;
        }
        LAParser.ParcelaContext parcela = fator.parcela(0);
        if (parcela.parcela_nao_unario() == null || parcela.parcela_nao_unario().CADEIA() == null) {
            return null;
        }
        return parcela.parcela_nao_unario().CADEIA().getText();
    }

    private String valorConstanteC(LAParser.Valor_constanteContext ctx) {
        if (ctx.getText().equals("verdadeiro")) {
            return "1";
        }
        if (ctx.getText().equals("falso")) {
            return "0";
        }
        return ctx.getText();
    }

    private String tipoBaseC(Tipo tipo) {
        switch (tipo.categoria) {
            case INTEIRO:
            case LOGICO:
                return "int";
            case REAL:
                return "float";
            case LITERAL:
                return "char";
            case PONTEIRO:
                return tipoBaseC(tipo.apontado) + "*";
            case REGISTRO:
                return tipo.nomeRegistro;
            default:
                return "int";
        }
    }

    private String tipoParametroC(LAParser.Tipo_estendidoContext ctx) {
        Tipo tipo = resolverTipoEstendido(ctx);
        if (tipo.categoria == Tipo.Categoria.LITERAL) {
            return "char*";
        }
        return tipoBaseC(tipo);
    }

    private Tipo resolverTipoBasico(LAParser.Tipo_basicoContext ctx) {
        switch (ctx.getText()) {
            case "inteiro":
                return Tipo.INTEIRO;
            case "real":
                return Tipo.REAL;
            case "literal":
                return Tipo.LITERAL;
            case "logico":
                return Tipo.LOGICO;
            default:
                return Tipo.INVALIDO;
        }
    }

    private Tipo resolverTipoBasicoIdent(LAParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return resolverTipoBasico(ctx.tipo_basico());
        }
        EntradaTabela entrada = escopos.buscarEntrada(ctx.IDENT().getText());
        if (entrada != null && entrada.categoria == Categoria.TIPO) {
            return entrada.tipo;
        }
        return Tipo.INVALIDO;
    }

    private Tipo resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx) {
        Tipo base = resolverTipoBasicoIdent(ctx.tipo_basico_ident());
        if (ctx.getStart().getText().equals("^")) {
            return Tipo.ponteiro(base);
        }
        return base;
    }

    private Tipo resolverTipo(LAParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return construirRegistro(ctx.registro(), "registro$" + (contadorRegistroAnonimo++));
        }
        return resolverTipoEstendido(ctx.tipo_estendido());
    }

    private Tipo construirRegistro(LAParser.RegistroContext ctx, String nomeRegistro) {
        Tipo registro = Tipo.registro(nomeRegistro);
        for (LAParser.VariavelContext variavel : ctx.variavel()) {
            Tipo tipoCampo = resolverTipo(variavel.tipo());
            for (LAParser.IdentificadorContext id : variavel.identificador()) {
                registro.campos.put(id.IDENT(0).getText(), tipoCampo);
            }
        }
        return registro;
    }

    private Tipo desreferenciar(Tipo tipo) {
        if (tipo != null && tipo.categoria == Tipo.Categoria.PONTEIRO) {
            return tipo.apontado;
        }
        return Tipo.INVALIDO;
    }

    private Tipo combina(Tipo a, Tipo b) {
        if (a == null || b == null || a.ehInvalido() || b.ehInvalido()) {
            return Tipo.INVALIDO;
        }
        if (a.ehNumerico() && b.ehNumerico()) {
            return (a.categoria == Tipo.Categoria.REAL || b.categoria == Tipo.Categoria.REAL)
                    ? Tipo.REAL : Tipo.INTEIRO;
        }
        return a;
    }

    private Tipo tipoDoIdentificador(LAParser.IdentificadorContext ctx) {
        List<TerminalNode> idents = ctx.IDENT();
        EntradaTabela entrada = escopos.buscarEntrada(idents.get(0).getText());
        if (entrada == null || entrada.tipo == null) {
            return Tipo.INVALIDO;
        }
        Tipo tipo = entrada.tipo;
        for (int i = 1; i < idents.size(); i++) {
            if (tipo.categoria != Tipo.Categoria.REGISTRO) {
                return Tipo.INVALIDO;
            }
            tipo = tipo.campos.get(idents.get(i).getText());
            if (tipo == null) {
                return Tipo.INVALIDO;
            }
        }
        return tipo;
    }

    private Tipo tipoDe(LAParser.ExpressaoContext ctx) {
        List<LAParser.Termo_logicoContext> termos = ctx.termo_logico();
        if (termos.size() > 1) {
            return Tipo.LOGICO;
        }
        return tipoDe(termos.get(0));
    }

    private Tipo tipoDe(LAParser.Termo_logicoContext ctx) {
        List<LAParser.Fator_logicoContext> fatores = ctx.fator_logico();
        if (fatores.size() > 1) {
            return Tipo.LOGICO;
        }
        return tipoDe(fatores.get(0));
    }

    private Tipo tipoDe(LAParser.Fator_logicoContext ctx) {
        if (ctx.getChildCount() > 1) {
            return Tipo.LOGICO;
        }
        return tipoDe(ctx.parcela_logica());
    }

    private Tipo tipoDe(LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return tipoDe(ctx.exp_relacional());
        }
        return Tipo.LOGICO;
    }

    private Tipo tipoDe(LAParser.Exp_relacionalContext ctx) {
        if (ctx.exp_aritmetica().size() > 1) {
            return Tipo.LOGICO;
        }
        return tipoDe(ctx.exp_aritmetica(0));
    }

    private Tipo tipoDe(LAParser.Exp_aritmeticaContext ctx) {
        Tipo tipo = tipoDe(ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++) {
            tipo = combina(tipo, tipoDe(ctx.termo(i)));
        }
        return tipo;
    }

    private Tipo tipoDe(LAParser.TermoContext ctx) {
        Tipo tipo = tipoDe(ctx.fator(0));
        for (int i = 1; i < ctx.fator().size(); i++) {
            tipo = combina(tipo, tipoDe(ctx.fator(i)));
        }
        return tipo;
    }

    private Tipo tipoDe(LAParser.FatorContext ctx) {
        Tipo tipo = tipoDe(ctx.parcela(0));
        for (int i = 1; i < ctx.parcela().size(); i++) {
            tipo = combina(tipo, tipoDe(ctx.parcela(i)));
        }
        return tipo;
    }

    private Tipo tipoDe(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            return tipoDe(ctx.parcela_unario());
        }
        return tipoDe(ctx.parcela_nao_unario());
    }

    private Tipo tipoDe(LAParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            Tipo tipo = tipoDoIdentificador(ctx.identificador());
            if (ctx.getStart().getText().equals("^")) {
                return desreferenciar(tipo);
            }
            return tipo;
        }
        if (ctx.IDENT() != null) {
            EntradaTabela entrada = escopos.buscarEntrada(ctx.IDENT().getText());
            if (entrada != null && entrada.categoria == Categoria.FUNCAO) {
                return entrada.tipoRetorno;
            }
            return Tipo.INVALIDO;
        }
        if (ctx.NUM_INT() != null) {
            return Tipo.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return Tipo.REAL;
        }
        return tipoDe(ctx.expressao(0));
    }

    private Tipo tipoDe(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return Tipo.LITERAL;
        }
        return Tipo.endereco(tipoDoIdentificador(ctx.identificador()));
    }
}
