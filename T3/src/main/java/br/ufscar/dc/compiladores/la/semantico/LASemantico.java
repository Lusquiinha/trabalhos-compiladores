package br.ufscar.dc.compiladores.la.semantico;

import java.util.ArrayList;
import java.util.List;

import br.ufscar.dc.compiladores.la.semantico.TabelaDeSimbolos.TipoLA;

public class LASemantico extends LABaseVisitor<Void> {

    private final Escopos escopos = new Escopos();
    private final List<String> erros = new ArrayList<>();

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

    private TipoLA buscarTipo(String nome) {
        for (TabelaDeSimbolos tabela : escopos.percorrerEscopos()) {
            if (tabela.existe(nome)) {
                return tabela.verificar(nome);
            }
        }
        return null;
    }

    private boolean declaradoEmAlgumEscopo(String nome) {
        return buscarTipo(nome) != null;
    }

    private TipoLA combina(TipoLA a, TipoLA b) {
        if (a == TipoLA.INVALIDO || b == TipoLA.INVALIDO) {
            return TipoLA.INVALIDO;
        }
        if (a == b) {
            return a;
        }
        boolean aNumerico = a == TipoLA.INTEIRO || a == TipoLA.REAL;
        boolean bNumerico = b == TipoLA.INTEIRO || b == TipoLA.REAL;
        if (aNumerico && bNumerico) {
            return TipoLA.REAL;
        }
        return TipoLA.INVALIDO;
    }

    private boolean compativel(TipoLA destino, TipoLA origem) {
        if (destino == TipoLA.INVALIDO || origem == TipoLA.INVALIDO) {
            return false;
        }
        if (destino == origem) {
            return true;
        }
        boolean destinoNumerico = destino == TipoLA.INTEIRO || destino == TipoLA.REAL;
        boolean origemNumerico = origem == TipoLA.INTEIRO || origem == TipoLA.REAL;
        return destinoNumerico && origemNumerico;
    }

    private TipoLA resolverTipoBasico(LAParser.Tipo_basicoContext ctx) {
        switch (ctx.getText()) {
            case "inteiro":
                return TipoLA.INTEIRO;
            case "real":
                return TipoLA.REAL;
            case "literal":
                return TipoLA.LITERAL;
            case "logico":
                return TipoLA.LOGICO;
            default:
                return TipoLA.INVALIDO;
        }
    }

    private TipoLA resolverTipoBasicoIdent(LAParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return resolverTipoBasico(ctx.tipo_basico());
        }
        String nome = ctx.IDENT().getText();
        if (declaradoEmAlgumEscopo(nome)) {
            return buscarTipo(nome);
        }
        adicionarErro(ctx.IDENT().getSymbol().getLine(), "tipo " + nome + " nao declarado");
        return TipoLA.INVALIDO;
    }

    private TipoLA resolverTipoEstendido(LAParser.Tipo_estendidoContext ctx) {
        return resolverTipoBasicoIdent(ctx.tipo_basico_ident());
    }

    private TipoLA resolverTipo(LAParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return TipoLA.REGISTRO;
        }
        return resolverTipoEstendido(ctx.tipo_estendido());
    }

    private void declararVariavel(LAParser.VariavelContext ctx) {
        TipoLA tipo = resolverTipo(ctx.tipo());
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            String nome = nomeDe(id);
            int linha = id.getStart().getLine();
            if (escopos.obterEscopoAtual().existe(nome)) {
                adicionarErro(linha, "identificador " + nome + " ja declarado anteriormente");
            } else {
                escopos.obterEscopoAtual().adicionar(nome, tipo);
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
            escopos.obterEscopoAtual().adicionar(nome, resolverTipoBasico(ctx.tipo_basico()));
        } else {
            escopos.obterEscopoAtual().adicionar(nome, resolverTipo(ctx.tipo()));
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        int linha = ctx.IDENT().getSymbol().getLine();
        if (escopos.obterEscopoAtual().existe(nome)) {
            adicionarErro(linha, "identificador " + nome + " ja declarado anteriormente");
        } else {
            escopos.obterEscopoAtual().adicionar(nome, TipoLA.INVALIDO);
        }

        escopos.criarNovoEscopo();
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext p : ctx.parametros().parametro()) {
                TipoLA tipo = resolverTipoEstendido(p.tipo_estendido());
                for (LAParser.IdentificadorContext id : p.identificador()) {
                    escopos.obterEscopoAtual().adicionar(nomeDe(id), tipo);
                }
            }
        }
        for (LAParser.Declaracao_localContext d : ctx.declaracao_local()) {
            visit(d);
        }
        for (LAParser.CmdContext c : ctx.cmd()) {
            visit(c);
        }
        escopos.abandonarEscopo();
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        TipoLA tipoExpressao = verificarTipo(ctx.expressao());
        LAParser.IdentificadorContext id = ctx.identificador();
        String nome = nomeDe(id);
        int linha = id.getStart().getLine();
        TipoLA tipoVariavel = buscarTipo(nome);
        if (tipoVariavel == null) {
            adicionarErro(linha, "identificador " + nome + " nao declarado");
        } else if (!compativel(tipoVariavel, tipoExpressao)) {
            adicionarErro(linha, "atribuicao nao compativel para " + nome);
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        for (LAParser.IdentificadorContext id : ctx.identificador()) {
            String nome = nomeDe(id);
            if (!declaradoEmAlgumEscopo(nome)) {
                adicionarErro(id.getStart().getLine(), "identificador " + nome + " nao declarado");
            }
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
        if (!declaradoEmAlgumEscopo(nome)) {
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
        String nome = ctx.IDENT().getText();
        if (!declaradoEmAlgumEscopo(nome)) {
            adicionarErro(ctx.IDENT().getSymbol().getLine(), "identificador " + nome + " nao declarado");
        }
        for (LAParser.ExpressaoContext e : ctx.expressao()) {
            verificarTipo(e);
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        verificarTipo(ctx.expressao());
        return null;
    }

    private TipoLA verificarTipo(LAParser.ExpressaoContext ctx) {
        List<LAParser.Termo_logicoContext> termos = ctx.termo_logico();
        TipoLA tipo = verificarTipo(termos.get(0));
        for (int i = 1; i < termos.size(); i++) {
            verificarTipo(termos.get(i));
            tipo = TipoLA.LOGICO;
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.Termo_logicoContext ctx) {
        List<LAParser.Fator_logicoContext> fatores = ctx.fator_logico();
        TipoLA tipo = verificarTipo(fatores.get(0));
        for (int i = 1; i < fatores.size(); i++) {
            verificarTipo(fatores.get(i));
            tipo = TipoLA.LOGICO;
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.Fator_logicoContext ctx) {
        TipoLA tipo = verificarTipo(ctx.parcela_logica());
        if (ctx.getChildCount() > 1) {
            return TipoLA.LOGICO;
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            return verificarTipo(ctx.exp_relacional());
        }
        return TipoLA.LOGICO;
    }

    private TipoLA verificarTipo(LAParser.Exp_relacionalContext ctx) {
        List<LAParser.Exp_aritmeticaContext> expressoes = ctx.exp_aritmetica();
        TipoLA tipo = verificarTipo(expressoes.get(0));
        if (expressoes.size() > 1) {
            verificarTipo(expressoes.get(1));
            return TipoLA.LOGICO;
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.Exp_aritmeticaContext ctx) {
        List<LAParser.TermoContext> termos = ctx.termo();
        TipoLA tipo = verificarTipo(termos.get(0));
        for (int i = 1; i < termos.size(); i++) {
            tipo = combina(tipo, verificarTipo(termos.get(i)));
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.TermoContext ctx) {
        List<LAParser.FatorContext> fatores = ctx.fator();
        TipoLA tipo = verificarTipo(fatores.get(0));
        for (int i = 1; i < fatores.size(); i++) {
            tipo = combina(tipo, verificarTipo(fatores.get(i)));
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.FatorContext ctx) {
        List<LAParser.ParcelaContext> parcelas = ctx.parcela();
        TipoLA tipo = verificarTipo(parcelas.get(0));
        for (int i = 1; i < parcelas.size(); i++) {
            tipo = combina(tipo, verificarTipo(parcelas.get(i)));
        }
        return tipo;
    }

    private TipoLA verificarTipo(LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            return verificarTipo(ctx.parcela_unario());
        }
        return verificarTipo(ctx.parcela_nao_unario());
    }

    private TipoLA verificarTipo(LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return TipoLA.INTEIRO;
        }
        if (ctx.NUM_REAL() != null) {
            return TipoLA.REAL;
        }
        if (ctx.IDENT() != null) {
            String nome = ctx.IDENT().getText();
            TipoLA tipo = buscarTipo(nome);
            if (tipo == null) {
                adicionarErro(ctx.IDENT().getSymbol().getLine(), "identificador " + nome + " nao declarado");
                tipo = TipoLA.INVALIDO;
            }
            for (LAParser.ExpressaoContext e : ctx.expressao()) {
                verificarTipo(e);
            }
            return tipo;
        }
        if (ctx.identificador() != null) {
            return verificarTipo(ctx.identificador());
        }
        return verificarTipo(ctx.expressao(0));
    }

    private TipoLA verificarTipo(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return TipoLA.LITERAL;
        }
        return verificarTipo(ctx.identificador());
    }

    private TipoLA verificarTipo(LAParser.IdentificadorContext ctx) {
        String nome = nomeDe(ctx);
        TipoLA tipo = buscarTipo(nome);
        if (tipo == null) {
            adicionarErro(ctx.getStart().getLine(), "identificador " + nome + " nao declarado");
            return TipoLA.INVALIDO;
        }
        if (ctx.dimensao() != null) {
            for (LAParser.Exp_aritmeticaContext e : ctx.dimensao().exp_aritmetica()) {
                verificarTipo(e);
            }
        }
        return tipo;
    }
}
