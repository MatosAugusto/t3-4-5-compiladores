package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;


public class Visitor extends AlgumaBaseVisitor<Void> {
    public Errors errorlist = new Errors();
    private final Scope scope = new Scope();
    public static final Visitor VISITOR = new Visitor();

    private boolean returnAux;

    @Override
    public Void visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        ArrayList<Variavel> entry = new ArrayList<>();
        if (ctx.declaracao_local() != null)
            entry = checkLocal(scope, ctx.declaracao_local());
        else
            entry.add(checkGlobal(scope, ctx.declaracao_global()));
        addVarScope(entry);

        return null;
    }

    public Scope getScope() {
        return scope;
    }

    public void addVarScope(ArrayList<Variavel> var) {
        for(Variavel aux: var) {
            scope.lookScope().add(aux);
        }
    }

    @Override
    public Void visitCorpo(AlgumaParser.CorpoContext ctx) {

        ArrayList<Variavel> variableList = new ArrayList<>();

        for (AlgumaParser.Declaracao_localContext x : ctx.declaracao_local())
        {
            variableList.addAll(checkLocal(scope, x));
            addVarScope(variableList);
        }
        for (AlgumaParser.CmdContext cmd : ctx.cmd())
            checkCmd(scope.lookScope(), cmd);

        return null;
    }


    public ArrayList<Variavel> checkLocal(Scope scope, AlgumaParser.Declaracao_localContext ctx) {
        ArrayList<Variavel> result = new ArrayList<>();
        switch(Correspondence(ctx.getStart().getText())){

            case 1:
                Tipo type1 = checkTipo(ctx.tipo());
                if (type1.aNativo != null && type1.aNativo == Tipo.Nativo.INVALIDO) {
                    errorlist.addError(3,ctx.start.getLine(), ctx.tipo().getText());
                }else{
                    String name = ctx.IDENT().getText();
                    Tipo.adicionaNovoTipo(name);
                    Variavel newType = new Variavel(name, type1);

                    if (newType.tipo.aNativo == Tipo.Nativo.REGISTRO)
                        newType.registro = checkRegistro(scope, ctx.tipo().registro()).registro;

                    result.add(newType);
                }
            break;
            case 2:
                Tipo type2 = new Tipo(checkTipoNat(ctx.tipo_basico()));

                if (type2.aNativo != null && type2.aNativo == Tipo.Nativo.INVALIDO)
                    errorlist.addError(3,ctx.start.getLine(), ctx.tipo().getText());
                else
                    result.add(new Variavel(ctx.IDENT().getText(), type2));
            break;
            case 3:
                result = checkVar(scope, ctx.variavel());
            break;
        }
        return result;
    }

    public Variavel checkGlobal(Scope scope, AlgumaParser.Declaracao_globalContext ctx) {
        Variavel aux = null;

        switch(Correspondence(ctx.getStart().getText())){

            case 4:
                Tipo type4 = checkEstendido(ctx.tipo_estendido());
                scope.createNewScope();
                returnAux = true;
                aux = new Variavel(ctx.IDENT().getText(), new Tipo(Tipo.Nativo.FUNCAO));
                if (ctx.parametros() != null) {

                    ArrayList<Variavel> param = checkParams(scope, ctx.parametros());
                    aux.funcao.setParametros(param);
                    addVarScope(param);
                }
                aux.funcao.setTipoRetorno(type4);
                ArrayList<Variavel> declaration = new ArrayList<>();
                for (AlgumaParser.Declaracao_localContext auxDecl : ctx.declaracao_local())
                    declaration.addAll(checkLocal(scope, auxDecl));
                addVarScope(declaration);
                aux.funcao.setLocal(declaration);
                for (AlgumaParser.CmdContext cmd : ctx.cmd())
                    checkCmd(scope.lookScope(), cmd);
                scope.leaveScope();
                returnAux = false;
            break;

            case 5:
                scope.createNewScope();
                aux = new Variavel(ctx.IDENT().getText(), new Tipo(Tipo.Nativo.PROCEDIMENTO));

                if (ctx.declaracao_local() != null) {
                    ArrayList<Variavel> auxList = new ArrayList<>();

                    for (AlgumaParser.Declaracao_localContext auxDecl : ctx.declaracao_local()) {
                        auxList.addAll(checkLocal(scope, auxDecl));
                    }
                    addVarScope(auxList);
                    aux.procedimento.setLocal(auxList);
                }
                if (ctx.parametros() != null) {
                    ArrayList<Variavel> param = checkParams(scope, ctx.parametros());

                    addVarScope(param);
                    aux.procedimento.setParametros(param);
                }
                if (ctx.cmd() != null)
                    for (AlgumaParser.CmdContext cmd : ctx.cmd())
                        checkCmd(scope.lookScope(), cmd);

                scope.leaveScope();
            break;
        }
        return aux;
    }


    public Variavel checkRegistro(Scope scope, AlgumaParser.RegistroContext ctx) {

        Variavel register = new Variavel("", new Tipo(Tipo.Nativo.REGISTRO));
        scope.createNewScope();

        for (int i = 0; i < ctx.variavel().size(); i++) {
            register.registro.addRegistro(checkVar(scope, ctx.variavel(i)));
        }

        return register;
    }

    public int Correspondence(String receiver){
        switch(receiver){
            case "tipo": return 1;
            case "constante": return 2;
            case "declare": return 3;
            case "funcao": return 4;
            case "procedimento": return 5;
        }
    return 0;
    }

    public ArrayList<Variavel> checkParams(Scope scope, AlgumaParser.ParametrosContext ctx) {

        ArrayList<Variavel> result = new ArrayList<>();

        for (AlgumaParser.ParametroContext param : ctx.parametro()){
            ArrayList<Variavel> params = new ArrayList<>();
            Tipo tipo = checkEstendido(param.tipo_estendido());

            for (AlgumaParser.IdentificadorContext i : param.identificador()) {
                    Variavel auxvar = new Variavel(i.getText(), tipo);
                        for (SymbleTable st : scope.runScope()) {
                            Variavel aux = addNovoTipo(st, auxvar, tipo.criados);
                            if (aux.tipo != null)
                                auxvar = aux;
                        }
                    params.add(auxvar);
                    scope.lookScope().add(auxvar);
                }
            result.addAll(params);
        }

        return result;
    }

    public Tipo checkIdentacao(AlgumaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null)
            return new Tipo(checkTipoNat(ctx.tipo_basico()));

        if ((Tipo.getTipo(ctx.IDENT().getText()))!= null)
            return new Tipo(Tipo.getTipo(ctx.IDENT().getText()));

        return new Tipo(Tipo.Nativo.INVALIDO);
    }

    public void checkCmd(SymbleTable st, AlgumaParser.CmdContext ctx) {
        String base = "";
        if (ctx.cmdAtribuicao() != null){
            Variavel left = checkIdent(st, ctx.cmdAtribuicao().identificador());
            Tipo typeLeft = left.tipo;
            if (typeLeft == null) {
                errorlist.addError(0,ctx.cmdAtribuicao().identificador().start.getLine(), ctx.cmdAtribuicao().identificador().getText());
                return;
            }

            Tipo typeRight = checkExpressao(st, ctx.cmdAtribuicao().expressao());

            if (ctx.getChild(0).getText().contains("^")) {
                 base += "^";
                typeLeft = left.ponteiro.getTipo();
            }

             if (typeLeft.validaTipo(typeRight).aNativo == Tipo.Nativo.INVALIDO && typeLeft.aNativo != null)  {
                errorlist.addError(2,ctx.cmdAtribuicao().identificador().start.getLine(), base + ctx.cmdAtribuicao().identificador().getText());
            }
        }
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao())
                checkExpressao(st, exp);
            }
        else if (ctx.cmdLeia() != null){
                    for (AlgumaParser.IdentificadorContext i : ctx.cmdLeia().identificador()) {
                        Variavel aux = checkIdent(st, i);
                        if (aux != null && aux.tipo == null)
                            errorlist.addError(0,i.getStart().getLine(), i.getText());
                    }
        }
        else if (ctx.cmdEnquanto() != null)
            checkExpressao(st, ctx.cmdEnquanto().expressao());
        else if (ctx.cmdSe() != null){
            checkExpressao(st, ctx.cmdSe().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd())
                checkCmd(st, cmd);
        }
        else if (ctx.cmdFaca() != null){
            checkExpressao(st, ctx.cmdFaca().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                checkCmd(st, cmd);
        }
        else if (ctx.cmdRetorne() != null){
            if (!returnAux)
                errorlist.addError(5,ctx.start.getLine(),"");
        }
    }

    public ArrayList<Variavel> checkVar(Scope scope, AlgumaParser.VariavelContext ctx) {
        ArrayList<Variavel> result = new ArrayList<>();
        Tipo type = checkTipo(ctx.tipo());

        for (AlgumaParser.IdentificadorContext indent : ctx.identificador()){
            Variavel aux;
            aux = checkIdent(scope.lookScope(), indent);

            if (aux.tipo != null)
                errorlist.addError(1,indent.getStart().getLine(), indent.getText());
            else {
                aux = new Variavel(aux.varNome, type);
                if (type.criados != null){
                    aux = addNovoTipo(scope.lookScope(), aux, type.criados);
                }
                if (type.aNativo == Tipo.Nativo.REGISTRO){
                    aux.registro = checkRegistro(scope, ctx.tipo().registro()).registro;
                }
                scope.lookScope().add(aux);
                result.add(aux);
            }
        }

        if (type.aNativo != null && type.aNativo == Tipo.Nativo.INVALIDO)
            errorlist.addError(3,ctx.start.getLine(), ctx.tipo().getText());

        return result;
    }

    public Tipo checkTipo(AlgumaParser.TipoContext ctx) {
        return ((ctx.registro() != null) ? new Tipo(Tipo.Nativo.REGISTRO) : checkEstendido(ctx.tipo_estendido()));
    }

    public Tipo checkEstendido(AlgumaParser.Tipo_estendidoContext ctx) {
        return ((ctx.getChild(0).getText().contains("^")) ? new Tipo(checkIdentacao(ctx.tipo_basico_ident())): checkIdentacao(ctx.tipo_basico_ident()));
    }

    public Tipo checkLogica(SymbleTable st, AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null)
            return checkExpRelacional(st, ctx.exp_relacional());
        return new Tipo(Tipo.Nativo.LOGICO);
    }

    private Tipo.Nativo checkTipoNat(AlgumaParser.Tipo_basicoContext ctx) {

        if(ctx.getText().equals("inteiro"))
            return Tipo.Nativo.INTEIRO;
        if(ctx.getText().equals("real"))
            return Tipo.Nativo.REAL;
        if(ctx.getText().equals("literal"))
            return Tipo.Nativo.LITERAL;
        if(ctx.getText().equals("logico"))
            return Tipo.Nativo.LOGICO;

        return Tipo.Nativo.INVALIDO;
    }

    public Tipo checkExpressao(SymbleTable st, AlgumaParser.ExpressaoContext ctx) {
        Tipo type = checkTermosLogicos(st, ctx.termo_logico(0));
        if (ctx.termo_logico().size() > 1) {
            for (int i = 1; i < ctx.termo_logico().size(); i++) {
                type = type.validaTipo( checkTermosLogicos(st, ctx.termo_logico(i)));
            }
            if (type.aNativo != Tipo.Nativo.INVALIDO)
                type.aNativo = Tipo.Nativo.LOGICO;
        }
        return type;
    }

    public Tipo checkTermosLogicos(SymbleTable st, AlgumaParser.Termo_logicoContext ctx) {
        Tipo type = checkFatorLogico(st, ctx.fator_logico(0));

            for (int i = 1; i < ctx.fator_logico().size(); i++)
                type = type.validaTipo(checkFatorLogico(st, ctx.fator_logico(i)));
        return type;
    }

    public Tipo checkExpRelacional(SymbleTable st, AlgumaParser.Exp_relacionalContext ctx) {
        Tipo type = checkExpAritmetica(st, ctx.exp_aritmetica(0));

        if (ctx.exp_aritmetica().size() > 1) {
            type = type.validaTipo(checkExpAritmetica(st, ctx.exp_aritmetica(1)));

            if (type.aNativo != Tipo.Nativo.INVALIDO)
                type.aNativo = Tipo.Nativo.LOGICO;
        }

        return type;
    }

    public Tipo checkFatorLogico(SymbleTable st, AlgumaParser.Fator_logicoContext ctx) {
        Tipo type = checkLogica(st, ctx.parcela_logica());
            return ((ctx.getChild(0).getText().contains("nao"))? type.validaTipo(new Tipo(Tipo.Nativo.LOGICO)): type);
    }



    public Tipo checkExpAritmetica(SymbleTable st, AlgumaParser.Exp_aritmeticaContext ctx) {
        Tipo type = checkTermo(st, ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++)
            type = type.validaTipo(checkTermo(st, ctx.termo(i)));

        return type;
    }

    public Tipo checkTermo(SymbleTable st, AlgumaParser.TermoContext ctx) {
        Tipo type = checkFator(st, ctx.fator(0));
            for (int i = 1; i < ctx.fator().size(); i++)
                type = type.validaTipo(checkFator(st, ctx.fator(i)));

        return type;
    }

    public Tipo checkFator(SymbleTable st, AlgumaParser.FatorContext ctx) {
        Tipo type = checkParcela(st, ctx.parcela(0));
            for (int i = 1; i < ctx.parcela().size(); i++)
                type = type.validaTipo(checkParcela(st, ctx.parcela(i)));

        return type;
    }

    public Tipo checkParcela(SymbleTable st, AlgumaParser.ParcelaContext ctx) {

        if (ctx.parcela_unario() != null) {
            Tipo type = checkParcelaSimples(st, ctx.parcela_unario());
            if (ctx.op_unario() != null) {
                if (type.aNativo != Tipo.Nativo.INTEIRO && type.aNativo != Tipo.Nativo.REAL)
                    return new Tipo(Tipo.Nativo.INVALIDO);
                return type;
            }
            return type;
        }
        return checkParcelaUnaria(st, ctx.parcela_nao_unario());
    }

    public Variavel checkIdent(SymbleTable st, AlgumaParser.IdentificadorContext ctx) {
        String name = ctx.IDENT(0).getText();

        if (st.include(name)) {
            Variavel result = st.getVar(name);
            if (ctx.IDENT().size() > 1) {
                result = result.registro.getVariavel(ctx.IDENT(1).getText());
                if (result == null)
                    errorlist.addError(0,ctx.start.getLine(), ctx.getText());
            }
            return result;
        }

        return new Variavel(Converter(ctx,name), null);
    }

    public Tipo checkMetodo(SymbleTable st, TerminalNode IDENT, List<AlgumaParser.ExpressaoContext> exprs) {

        Tipo result = null;
        Variavel method = st.getVar(IDENT.getText());

            for (AlgumaParser.ExpressaoContext exp : exprs) {
                Tipo tipoExp = checkExpressao(st, exp);
                if (result == null || result.aNativo != Tipo.Nativo.INVALIDO)
                    result = tipoExp.verificaEquivalenciaTipo(method.funcao.getTipoRetorno());
            }

        if (result.aNativo == Tipo.Nativo.INVALIDO){
            errorlist.addError(4,IDENT.getSymbol().getLine(), IDENT.getText());
            return new Tipo(Tipo.Nativo.INVALIDO);
        }

        return result;
    }

    public Tipo checkParcelaSimples(SymbleTable st, AlgumaParser.Parcela_unarioContext ctx) {

        if (ctx.NUM_INT() != null)
            return new Tipo(Tipo.Nativo.INTEIRO);
        if (ctx.NUM_REAL() != null)
            return new Tipo(Tipo.Nativo.REAL);
        if (ctx.IDENT() != null)
            return checkMetodo(st, ctx.IDENT(), ctx.expressao());

        if (ctx.identificador() != null) {
            Variavel indent = checkIdent(st, ctx.identificador());

            if (indent.tipo == null) {
                errorlist.addError(0,ctx.identificador().start.getLine(), indent.varNome);
                return new Tipo(Tipo.Nativo.INVALIDO);
            }

            return indent.tipo;
        }


        Tipo type = checkExpressao(st, ctx.expressao(0));
            for (int i = 1; i < ctx.expressao().size(); i++)
                type = type.validaTipo( checkExpressao(st, ctx.expressao(i)));

        return type;
    }

    public Tipo checkParcelaUnaria(SymbleTable st, AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null)
            return new Tipo(Tipo.Nativo.LITERAL);
        else {
            if (ctx.getChild(0).getText().contains("&"))
                return new Tipo(Tipo.Nativo.ENDERECO);

            Variavel indent = checkIdent(st, ctx.identificador());
            if (indent.tipo == null) {
                errorlist.addError(0,ctx.identificador().start.getLine(), indent.varNome);
                return new Tipo(Tipo.Nativo.INVALIDO);
            }
            return indent.tipo;
        }
    }



   public Variavel addNovoTipo(SymbleTable st, Variavel aux, String name) {
        if (st.include(name)) {
            Variavel model = st.getVar(name);
            if (model.tipo.aNativo == Tipo.Nativo.REGISTRO) {
                Variavel result = new Variavel(aux.varNome, new Tipo(Tipo.Nativo.REGISTRO));
                result.setRegistro(model.registro);
                result.tipo = aux.tipo;
                return result;
            }
        }
        return new Variavel(null, null);
    }

    public String Converter(AlgumaParser.IdentificadorContext base, String name){
        for (int i = 1; i < base.IDENT().size(); i++)
            name += "." + base.IDENT(i);
    return name;

    }
}