package br.ufscar.dc.compiladores.la.gerador;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import br.ufscar.dc.compiladores.la.gerador.TabelaDeSimbolos.Categoria;
import br.ufscar.dc.compiladores.la.gerador.TabelaDeSimbolos.EntradaTabela;

public class LASemantico extends LABaseVisitor<Void> {

    private final Escopos escopos = new Escopos();
    private final List<String> erros = new ArrayList<>();
    private boolean dentroDeFuncao = false;
    private int contadorRegistroAnonimo = 0;

    public List<String> getErros() {
        return erros;
    }

    private void adicionarErro(int linha, String mensagem) {
        erros.add("Linha " + linha + ": " + mensagem);
    }

    private String nomeDe(LAParser.IdentificadorContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(ctx.IDENT(i).getText());
        }
        return sb.toString();
    }

    private boolean temAcento(ParserRuleContext ctx) {
        return ctx.getStart().getText().equals("^");
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
        String nome = ctx.IDENT().getText();
        EntradaTabela entrada = escopos.buscarEntrada(nome);
        if (entrada != null && entrada.categoria == Categoria.TIPO) {
            return entrada.tipo;
        }
        adicionarErro(ctx.IDENT().getSymbol().getLine(), "tipo " + nome + " nao declarado");
        return Tipo.INVALIDO;
    }

    private Tipo resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx) {
        Tipo base = resolverTipoBasicoIdent(ctx.tipo_basico_ident());
        if (temAcento(ctx)) {
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
                registro.campos.put(nomeDe(id), tipoCampo);
            }
        }
        return registro;
    }

    private void declararVariavel(LAParser.VariavelContext ctx) {
        Tipo tipo = resolverTipo(ctx.tipo());
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            String nome = nomeDe(id);
            int linha = id.getStart().getLine();
            if (escopos.obterEscopoAtual().existe(nome)) {
                adicionarErro(linha, "identificador " + nome + " ja declarado anteriormente");
            } else {
                escopos.obterEscopoAtual().adicionar(nome, Categoria.VARIAVEL, tipo);
            }
        }
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            declararVariavel(ctx.variavel());
            return null;
        }

        String palavraReservada = ctx.getStart().getText();
        String nome = ctx.IDENT().getText();
        int linha = ctx.IDENT().getSymbol().getLine();

        if (escopos.obterEscopoAtual().existe(nome)) {
            adicionarErro(linha, "identificador " + nome + " ja declarado anteriormente");
            return null;
        }

        if (palavraReservada.equals("constante")) {
            escopos.obterEscopoAtual().adicionar(nome, Categoria.CONSTANTE, resolverTipoBasico(ctx.tipo_basico()));
        } else {
            Tipo tipo;
            if (ctx.tipo().registro() != null) {
                tipo = construirRegistro(ctx.tipo().registro(), nome);
            } else {
                tipo = resolverTipo(ctx.tipo());
            }
            escopos.obterEscopoAtual().adicionar(nome, Categoria.TIPO, tipo);
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        boolean ehFuncao = ctx.getStart().getText().equals("funcao");
        String nome = ctx.IDENT().getText();
        int linha = ctx.IDENT().getSymbol().getLine();

        List<Tipo> tiposParametros = new ArrayList<>();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
                Tipo tipo = resolverTipoEstendido(p.tipo_estendido());
                for (int i = 0; i < p.identificador().size(); i++) {
                    tiposParametros.add(tipo);
                }
            }
        }

        Tipo tipoRetorno = ehFuncao ? resolverTipoEstendido(ctx.tipo_estendido()) : Tipo.INVALIDO;

        if (escopos.obterEscopoAtual().existe(nome)) {
            adicionarErro(linha, "identificador " + nome + " ja declarado anteriormente");
        } else {
            Categoria categoria = ehFuncao ? Categoria.FUNCAO : Categoria.PROCEDIMENTO;
            escopos.obterEscopoAtual().adicionar(
                    new EntradaTabela(nome, categoria, tipoRetorno, tiposParametros, tipoRetorno));
        }

        escopos.criarNovoEscopo();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
                Tipo tipo = resolverTipoEstendido(p.tipo_estendido());
                for (LAParser.IdentificadorContext id : p.identificador()) {
                    escopos.obterEscopoAtual().adicionar(nomeDe(id), Categoria.VARIAVEL, tipo);
                }
            }
        }

        boolean estadoAnterior = dentroDeFuncao;
        dentroDeFuncao = ehFuncao;
        for (LAParser.Declaracao_localContext d : ctx.declaracao_local()) {
            visit(d);
        }
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        dentroDeFuncao = estadoAnterior;
        escopos.abandonarEscopo();
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        boolean acento = temAcento(ctx);
        LAParser.IdentificadorContext id = ctx.identificador();
        String alvo = (acento ? "^" : "") + id.getText();
        int linha = id.getStart().getLine();

        Tipo tipoExpressao = verificarTipo(ctx.expressao());
        Tipo tipoVariavel = tipoDoIdentificador(id, false);

        if (tipoVariavel == null) {
            adicionarErro(linha, "identificador " + alvo + " nao declarado");
            return null;
        }

        Tipo destino = acento ? desreferenciar(tipoVariavel) : tipoVariavel;
        if (!compativel(destino, tipoExpressao)) {
            adicionarErro(linha, "atribuicao nao compativel para " + alvo);
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            tipoDoIdentificador(id, true);
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        for (LAParser.ExpressaoContext e : ctx.expressao()) {
            verificarTipo(e);
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        verificarTipo(ctx.expressao());
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        verificarTipo(ctx.expressao());
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        return null;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        String nome = ctx.IDENT().getText();
        if (escopos.buscarEntrada(nome) == null) {
            adicionarErro(ctx.IDENT().getSymbol().getLine(), "identificador " + nome + " nao declarado");
        }
        for (LAParser.Exp_aritmeticaContext e : ctx.exp_aritmetica()) {
            verificarTipo(e);
        }
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        verificarTipo(ctx.exp_aritmetica());
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        if (ctx.selecao() != null) {
            for (LAParser.Item_selecaoContext item : ctx.selecao().item_selecao()) {
                for (LAParser.CmdContext c : item.cmd()) {
                    visit(c);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        verificarTipo(ctx.expressao());
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        verificarChamada(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine(), ctx.expressao());
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        verificarTipo(ctx.expressao());
        if (!dentroDeFuncao) {
            adicionarErro(ctx.getStart().getLine(), "comando retorne nao permitido nesse escopo");
        }
        return null;
    }

    private Tipo desreferenciar(Tipo tipo) {
        if (tipo != null && tipo.categoria == Tipo.Categoria.PONTEIRO) {
            return tipo.apontado;
        }
        return Tipo.INVALIDO;
    }

    private Tipo combina(Tipo a, Tipo b) {
        if (a.ehInvalido() || b.ehInvalido()) {
            return Tipo.INVALIDO;
        }
        if (a.ehNumerico() && b.ehNumerico()) {
            return (a.categoria == Tipo.Categoria.REAL || b.categoria == Tipo.Categoria.REAL)
                    ? Tipo.REAL : Tipo.INTEIRO;
        }
        if (a.categoria == b.categoria) {
            return a;
        }
        return Tipo.INVALIDO;
    }

    private boolean compativel(Tipo destino, Tipo origem) {
        if (destino.ehInvalido() || origem.ehInvalido()) {
            return false;
        }
        if (destino.categoria == Tipo.Categoria.PONTEIRO) {
            return origem.categoria == Tipo.Categoria.ENDERECO
                    || origem.categoria == Tipo.Categoria.PONTEIRO;
        }
        if (destino.ehNumerico() && origem.ehNumerico()) {
            return true;
        }
        if (destino.categoria == Tipo.Categoria.REGISTRO && origem.categoria == Tipo.Categoria.REGISTRO) {
            return destino.nomeRegistro != null && destino.nomeRegistro.equals(origem.nomeRegistro);
        }
        return destino.categoria == origem.categoria;
    }

    private boolean compativelParametro(Tipo formal, Tipo real) {
        if (formal.ehInvalido() || real.ehInvalido()) {
            return true;
        }
        if (formal.categoria == Tipo.Categoria.PONTEIRO) {
            return real.categoria == Tipo.Categoria.ENDERECO
                    || real.categoria == Tipo.Categoria.PONTEIRO;
        }
        if (formal.categoria == Tipo.Categoria.REGISTRO && real.categoria == Tipo.Categoria.REGISTRO) {
            return formal.nomeRegistro != null && formal.nomeRegistro.equals(real.nomeRegistro);
        }
        return formal.categoria == real.categoria;
    }

    private Tipo verificarChamada(String nome, int linha, List<LAParser.ExpressaoContext> argumentos) {
        EntradaTabela entrada = escopos.buscarEntrada(nome);
        List<Tipo> tiposArgumentos = new ArrayList<>();
        for (LAParser.ExpressaoContext e : argumentos) {
            tiposArgumentos.add(verificarTipo(e));
        }
        if (entrada == null) {
            adicionarErro(linha, "identificador " + nome + " nao declarado");
            return Tipo.INVALIDO;
        }
        if (entrada.categoria == Categoria.FUNCAO || entrada.categoria == Categoria.PROCEDIMENTO) {
            boolean ok = entrada.parametros.size() == tiposArgumentos.size();
            for (int i = 0; ok && i < tiposArgumentos.size(); i++) {
                if (!compativelParametro(entrada.parametros.get(i), tiposArgumentos.get(i))) {
                    ok = false;
                }
            }
            if (!ok) {
                adicionarErro(linha, "incompatibilidade de parametros na chamada de " + nome);
            }
            return entrada.categoria == Categoria.FUNCAO ? entrada.tipoRetorno : Tipo.INVALIDO;
        }
        return entrada.tipo;
    }

    private Tipo tipoDoIdentificador(LAParser.IdentificadorContext ctx, boolean reportar) {
        List<TerminalNode> idents = ctx.IDENT();
        String base = idents.get(0).getText();
        EntradaTabela entrada = escopos.buscarEntrada(base);
        if (entrada == null || entrada.tipo == null) {
            if (reportar) {
                adicionarErro(ctx.getStart().getLine(), "identificador " + nomeDe(ctx) + " nao declarado");
            }
            return null;
        }
        Tipo tipo = entrada.tipo;
        for (int i = 1; i < idents.size(); i++) {
            String campo = idents.get(i).getText();
            if (tipo.categoria != Tipo.Categoria.REGISTRO || !tipo.campos.containsKey(campo)) {
                if (reportar) {
                    adicionarErro(ctx.getStart().getLine(), "identificador " + nomeDe(ctx) + " nao declarado");
                }
                return null;
            }
            tipo = tipo.campos.get(campo);
        }
        if (ctx.dimensao() != null) {
            for (LAParser.Exp_aritmeticaContext e : ctx.dimensao().exp_aritmetica()) {
                verificarTipo(e);
            }
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.ExpressaoContext ctx) {
        List<LAParser.Termo_logicoContext> termos = ctx.termo_logico();
        Tipo tipo = verificarTipo(termos.get(0));
        for (int i = 1; i < termos.size(); i++) {
            verificarTipo(termos.get(i));
            tipo = Tipo.LOGICO;
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.Termo_logicoContext ctx) {
        List<LAParser.Fator_logicoContext> fatores = ctx.fator_logico();
        Tipo tipo = verificarTipo(fatores.get(0));
        for (int i = 1; i < fatores.size(); i++) {
            verificarTipo(fatores.get(i));
            tipo = Tipo.LOGICO;
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.Fator_logicoContext ctx) {
        Tipo tipo = verificarTipo(ctx.parcela_logica());
        if (ctx.getChildCount() > 1) {
            return Tipo.LOGICO;
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return verificarTipo(ctx.exp_relacional());
        }
        return Tipo.LOGICO;
    }

    private Tipo verificarTipo(LAParser.Exp_relacionalContext ctx) {
        List<LAParser.Exp_aritmeticaContext> expressoes = ctx.exp_aritmetica();
        Tipo tipo = verificarTipo(expressoes.get(0));
        if (expressoes.size() > 1) {
            verificarTipo(expressoes.get(1));
            return Tipo.LOGICO;
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.Exp_aritmeticaContext ctx) {
        List<LAParser.TermoContext> termos = ctx.termo();
        Tipo tipo = verificarTipo(termos.get(0));
        for (int i = 1; i < termos.size(); i++) {
            tipo = combina(tipo, verificarTipo(termos.get(i)));
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.TermoContext ctx) {
        List<LAParser.FatorContext> fatores = ctx.fator();
        Tipo tipo = verificarTipo(fatores.get(0));
        for (int i = 1; i < fatores.size(); i++) {
            tipo = combina(tipo, verificarTipo(fatores.get(i)));
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.FatorContext ctx) {
        List<LAParser.ParcelaContext> parcelas = ctx.parcela();
        Tipo tipo = verificarTipo(parcelas.get(0));
        for (int i = 1; i < parcelas.size(); i++) {
            tipo = combina(tipo, verificarTipo(parcelas.get(i)));
        }
        return tipo;
    }

    private Tipo verificarTipo(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            return verificarTipo(ctx.parcela_unario());
        }
        return verificarTipo(ctx.parcela_nao_unario());
    }

    private Tipo verificarTipo(LAParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            Tipo tipo = tipoDoIdentificador(ctx.identificador(), true);
            if (tipo == null) {
                return Tipo.INVALIDO;
            }
            if (temAcento(ctx)) {
                return desreferenciar(tipo);
            }
            return tipo;
        }
        if (ctx.IDENT() != null) {
            return verificarChamada(ctx.IDENT().getText(), ctx.IDENT().getSymbol().getLine(), ctx.expressao());
        }
        if (ctx.NUM_INT() != null) {
            return Tipo.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return Tipo.REAL;
        }
        return verificarTipo(ctx.expressao(0));
    }

    private Tipo verificarTipo(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return Tipo.LITERAL;
        }
        Tipo tipo = tipoDoIdentificador(ctx.identificador(), true);
        return Tipo.endereco(tipo == null ? Tipo.INVALIDO : tipo);
    }
}
