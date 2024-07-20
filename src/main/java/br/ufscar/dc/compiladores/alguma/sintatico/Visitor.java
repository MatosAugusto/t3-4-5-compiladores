package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.*;
import org.antlr.v4.runtime.tree.TerminalNode;


public class Visitor extends AlgumaBaseVisitor<Void> {
    public Errors errorListener = new Errors();
    private final Scope scope = new Scope();
    public static final Visitor I = new Visitor();

    private boolean retAux;

    @Override
    public Void visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        ArrayList<Variable> entry = new ArrayList<>();
        if (ctx.declaracao_local() != null)
            entry = checkLocal(scope, ctx.declaracao_local());
        else
            entry.add(checkGlobal(scope, ctx.declaracao_global()));

        addScopeVar(entry);

        return null;
    }

    public Scope getScope() {
        return scope;
    }

    public void addScopeVar(ArrayList<Variable> var) {
        for(Variable auxVar: var) {
            scope.lookScope().add(auxVar);
        }
    }

    @Override
    public Void visitCorpo(AlgumaParser.CorpoContext ctx) {

        ArrayList<Variable> variableList = new ArrayList<>();

        for (AlgumaParser.Declaracao_localContext x : ctx.declaracao_local())
        {
            variableList.addAll(checkLocal(scope, x));
            addScopeVar(variableList);
        }
        for (AlgumaParser.CmdContext cmd : ctx.cmd())
            checkCmd(scope.lookScope(), cmd);

        return null;
    }


    public ArrayList<Variable> checkLocal(Scope scope, AlgumaParser.Declaracao_localContext ctx) {
        ArrayList<Variable> response = new ArrayList<>();
        switch(Equivalence(ctx.getStart().getText())){

            case 1:
                Type type1 = checkType(ctx.tipo());
                if (type1.natives != null && type1.natives == Type.Natives.INVALIDO) {
                    errorListener.addError(3,ctx.start.getLine(), ctx.tipo().getText());
                }else{
                    String name = ctx.IDENT().getText();
                    Type.addNewType(name);
                    Variable newType = new Variable(name, type1);

                    if (newType.type.natives == Type.Natives.REGISTRO)
                        newType.register = checkRegister(scope, ctx.tipo().registro()).register;

                    response.add(newType);
                }
                break;
            case 2:
                Type type2 = new Type(checkNativeType(ctx.tipo_basico()));

                if (type2.natives != null && type2.natives == Type.Natives.INVALIDO)
                    errorListener.addError(3,ctx.start.getLine(), ctx.tipo().getText());
                else
                    response.add(new Variable(ctx.IDENT().getText(), type2));
                break;
            case 3:
                response = checkVar(scope, ctx.variavel());
                break;
        }
        return response;
    }

    public Variable checkGlobal(Scope scope, AlgumaParser.Declaracao_globalContext ctx) {
        Variable auxVar = null;

        switch(Equivalence(ctx.getStart().getText())){

            case 4:
                Type returnType = checkExtended(ctx.tipo_estendido());
                scope.newScope();
                retAux = true;
                auxVar = new Variable(ctx.IDENT().getText(), new Type(Type.Natives.FUNCAO));
                if (ctx.parametros() != null) {

                    ArrayList<Variable> param = checkParameters(scope, ctx.parametros());
                    auxVar.function.setParams(param);
                    addScopeVar(param);
                }
                auxVar.function.setResponseType(returnType);
                ArrayList<Variable> declare = new ArrayList<>();
                for (AlgumaParser.Declaracao_localContext declaration : ctx.declaracao_local())
                    declare.addAll(checkLocal(scope, declaration));
                addScopeVar(declare);
                auxVar.function.setLocal(declare);
                for (AlgumaParser.CmdContext cmd : ctx.cmd())
                    checkCmd(scope.lookScope(), cmd);
                scope.leaveScope();
                retAux = false;
                break;

            case 5:
                scope.newScope();
                auxVar = new Variable(ctx.IDENT().getText(), new Type(Type.Natives.PROCEDIMENTO));

                if (ctx.declaracao_local() != null) {
                    ArrayList<Variable> auxDec = new ArrayList<>();

                    for (AlgumaParser.Declaracao_localContext declaration : ctx.declaracao_local()) {
                        auxDec.addAll(checkLocal(scope, declaration));
                    }
                    addScopeVar(auxDec);
                    auxVar.procedure.setLocal(auxDec);
                }
                if (ctx.parametros() != null) {
                    ArrayList<Variable> params = checkParameters(scope, ctx.parametros());

                    addScopeVar(params);
                    auxVar.procedure.setParams(params);
                }
                if (ctx.cmd() != null)
                    for (AlgumaParser.CmdContext cmd : ctx.cmd())
                        checkCmd(scope.lookScope(), cmd);

                scope.leaveScope();
                break;
        }
        return auxVar;
    }


    public Variable checkRegister(Scope scope, AlgumaParser.RegistroContext ctx) {

        Variable auxReg = new Variable("", new Type(Type.Natives.REGISTRO));
        scope.newScope();

        for (int i = 0; i < ctx.variavel().size(); i++) {
            auxReg.register.addRegister(checkVar(scope, ctx.variavel(i)));
        }

        return auxReg;
    }



    public int Equivalence(String receptor){
        switch(receptor){
            case "tipo": return 1;
            case "constante": return 2;
            case "declare": return 3;
            case "funcao": return 4;
            case "procedimento": return 5;
        }
        return 0;
    }

    public ArrayList<Variable> checkParameters(Scope scope, AlgumaParser.ParametrosContext ctx) {

        ArrayList<Variable> response = new ArrayList<>();

        for (AlgumaParser.ParametroContext param : ctx.parametro()){
            ArrayList<Variable> parameters = new ArrayList<>();
            Type type = checkExtended(param.tipo_estendido());

            for (AlgumaParser.IdentificadorContext i : param.identificador()) {
                Variable auxvar = new Variable(i.getText(), type);
                for (SymbolTable st : scope.runScope()) {
                    Variable aux = addNewType(st, auxvar, type.created);
                    if (aux.type != null)
                        auxvar = aux;
                }
                parameters.add(auxvar);
                scope.lookScope().add(auxvar);
            }
            response.addAll(parameters);
        }

        return response;
    }

    public Type checkIndentType(AlgumaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null)
            return new Type(checkNativeType(ctx.tipo_basico()));

        if ((Type.getType(ctx.IDENT().getText()))!= null)
            return new Type(Type.getType(ctx.IDENT().getText()));

        return new Type(Type.Natives.INVALIDO);
    }

    public void checkCmd(SymbolTable st, AlgumaParser.CmdContext ctx) {
        String base = "";
        if (ctx.cmdAtribuicao() != null){
            Variable left = checkIndent(st, ctx.cmdAtribuicao().identificador());
            Type leftType = left.type;
            if (leftType == null) {
                errorListener.addError(0,ctx.cmdAtribuicao().identificador().start.getLine(), ctx.cmdAtribuicao().identificador().getText());
                return;
            }

            Type rightType = checkExpression(st, ctx.cmdAtribuicao().expressao());

            if (ctx.getChild(0).getText().contains("^")) {
                base += "^";
                leftType = left.pointer.getType();
            }

            if (leftType.checkType(rightType).natives == Type.Natives.INVALIDO && leftType.natives != null)  {
                errorListener.addError(2,ctx.cmdAtribuicao().identificador().start.getLine(), base + ctx.cmdAtribuicao().identificador().getText());
            }
        }
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao())
                checkExpression(st, exp);
        }
        else if (ctx.cmdLeia() != null){
            for (AlgumaParser.IdentificadorContext i : ctx.cmdLeia().identificador()) {
                Variable auxVar = checkIndent(st, i);
                if (auxVar != null && auxVar.type == null)
                    errorListener.addError(0,i.getStart().getLine(), i.getText());
            }
        }
        else if (ctx.cmdEnquanto() != null)
            checkExpression(st, ctx.cmdEnquanto().expressao());
        else if (ctx.cmdSe() != null){
            checkExpression(st, ctx.cmdSe().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd())
                checkCmd(st, cmd);
        }
        else if (ctx.cmdFaca() != null){
            checkExpression(st, ctx.cmdFaca().expressao());
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                checkCmd(st, cmd);
        }
        else if (ctx.cmdRetorne() != null){
            if (!retAux)
                errorListener.addError(5,ctx.start.getLine(),"");
        }
    }

    public ArrayList<Variable> checkVar(Scope scope, AlgumaParser.VariavelContext ctx) {
        ArrayList<Variable> response = new ArrayList<>();
        Type type = checkType(ctx.tipo());

        for (AlgumaParser.IdentificadorContext ident : ctx.identificador()){
            Variable auxVar;
            auxVar = checkIndent(scope.lookScope(), ident);

            if (auxVar.type != null)
                errorListener.addError(1,ident.getStart().getLine(), ident.getText());
            else {
                auxVar = new Variable(auxVar.name, type);
                if (type.created != null){
                    auxVar = addNewType(scope.lookScope(), auxVar, type.created);
                }
                if (type.natives == Type.Natives.REGISTRO){
                    auxVar.register = checkRegister(scope, ctx.tipo().registro()).register;
                }
                scope.lookScope().add(auxVar);
                response.add(auxVar);
            }
        }

        if (type.natives != null && type.natives == Type.Natives.INVALIDO)
            errorListener.addError(3,ctx.start.getLine(), ctx.tipo().getText());

        return response;
    }

    public Type checkType(AlgumaParser.TipoContext ctx) {
        return ((ctx.registro() != null) ? new Type(Type.Natives.REGISTRO) : checkExtended(ctx.tipo_estendido()));
    }

    public Type checkExtended(AlgumaParser.Tipo_estendidoContext ctx) {
        return ((ctx.getChild(0).getText().contains("^")) ? new Type(checkIndentType(ctx.tipo_basico_ident())): checkIndentType(ctx.tipo_basico_ident()));
    }

    public Type checkLogicalPortion(SymbolTable st, AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null)
            return checkRelationalExpression(st, ctx.exp_relacional());
        return new Type(Type.Natives.LOGICO);
    }

    private Type.Natives checkNativeType(AlgumaParser.Tipo_basicoContext ctx) {

        if(ctx.getText().equals("inteiro"))
            return Type.Natives.INTEIRO;
        if(ctx.getText().equals("real"))
            return Type.Natives.REAL;
        if(ctx.getText().equals("literal"))
            return Type.Natives.LITERAL;
        if(ctx.getText().equals("logico"))
            return Type.Natives.LOGICO;

        return Type.Natives.INVALIDO;
    }

    public Type checkExpression(SymbolTable st, AlgumaParser.ExpressaoContext ctx) {
        Type type = checkLogicalTerms(st, ctx.termo_logico(0));
        if (ctx.termo_logico().size() > 1) {
            for (int i = 1; i < ctx.termo_logico().size(); i++) {
                type = type.checkType( checkLogicalTerms(st, ctx.termo_logico(i)));
            }
            if (type.natives != Type.Natives.INVALIDO)
                type.natives = Type.Natives.LOGICO;
        }
        return type;
    }

    public Type checkLogicalTerms(SymbolTable st, AlgumaParser.Termo_logicoContext ctx) {
        Type type = checkLogicalCoefficient(st, ctx.fator_logico(0));

        for (int i = 1; i < ctx.fator_logico().size(); i++)
            type = type.checkType(checkLogicalCoefficient(st, ctx.fator_logico(i)));
        return type;
    }

    public Type checkRelationalExpression(SymbolTable st, AlgumaParser.Exp_relacionalContext ctx) {
        Type type = checkArithmeticExpression(st, ctx.exp_aritmetica(0));

        if (ctx.exp_aritmetica().size() > 1) {
            type = type.checkType(checkArithmeticExpression(st, ctx.exp_aritmetica(1)));

            if (type.natives != Type.Natives.INVALIDO)
                type.natives = Type.Natives.LOGICO;
        }

        return type;
    }

    public Type checkLogicalCoefficient(SymbolTable st, AlgumaParser.Fator_logicoContext ctx) {
        Type type = checkLogicalPortion(st, ctx.parcela_logica());
        return ((ctx.getChild(0).getText().contains("nao"))? type.checkType(new Type(Type.Natives.LOGICO)): type);
    }



    public Type checkArithmeticExpression(SymbolTable st, AlgumaParser.Exp_aritmeticaContext ctx) {
        Type type = checkTerm(st, ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++)
            type = type.checkType(checkTerm(st, ctx.termo(i)));

        return type;
    }

    public Type checkTerm(SymbolTable st, AlgumaParser.TermoContext ctx) {
        Type type = checkCoefficient(st, ctx.fator(0));
        for (int i = 1; i < ctx.fator().size(); i++)
            type = type.checkType(checkCoefficient(st, ctx.fator(i)));

        return type;
    }

    public Type checkCoefficient(SymbolTable st, AlgumaParser.FatorContext ctx) {
        Type type = checkPortion(st, ctx.parcela(0));
        for (int i = 1; i < ctx.parcela().size(); i++)
            type = type.checkType(checkPortion(st, ctx.parcela(i)));

        return type;
    }

    public Type checkPortion(SymbolTable st, AlgumaParser.ParcelaContext ctx) {

        if (ctx.parcela_unario() != null) {
            Type type = checkSimplePortion(st, ctx.parcela_unario());
            if (ctx.op_unario() != null) {
                if (type.natives != Type.Natives.INTEIRO && type.natives != Type.Natives.REAL)
                    return new Type(Type.Natives.INVALIDO);
                return type;
            }
            return type;
        }
        return checkSinglePortion(st, ctx.parcela_nao_unario());
    }

    public Variable checkIndent(SymbolTable st, AlgumaParser.IdentificadorContext ctx) {
        String name = ctx.IDENT(0).getText();

        if (st.contains(name)) {
            Variable response = st.getVar(name);
            if (ctx.IDENT().size() > 1) {
                response = response.register.getVar(ctx.IDENT(1).getText());
                if (response == null)
                    errorListener.addError(0,ctx.start.getLine(), ctx.getText());
            }
            return response;
        }

        return new Variable(Suiting(ctx,name), null);
    }

    public Type checkProcess(SymbolTable st, TerminalNode IDENT, List<AlgumaParser.ExpressaoContext> expression) {

        Type response = null;
        Variable process = st.getVar(IDENT.getText());

        for (AlgumaParser.ExpressaoContext exp : expression) {
            Type expType = checkExpression(st, exp);
            if (response == null || response.natives != Type.Natives.INVALIDO)
                response = expType.checkEquivalentType(process.function.getResponseType());
        }

        if (response.natives == Type.Natives.INVALIDO){
            errorListener.addError(4,IDENT.getSymbol().getLine(), IDENT.getText());
            return new Type(Type.Natives.INVALIDO);
        }

        return response;
    }

    public Type checkSimplePortion(SymbolTable st, AlgumaParser.Parcela_unarioContext ctx) {

        if (ctx.NUM_INT() != null)
            return new Type(Type.Natives.INTEIRO);
        if (ctx.NUM_REAL() != null)
            return new Type(Type.Natives.REAL);
        if (ctx.IDENT() != null)
            return checkProcess(st, ctx.IDENT(), ctx.expressao());

        if (ctx.identificador() != null) {
            Variable indent = checkIndent(st, ctx.identificador());

            if (indent.type == null) {
                errorListener.addError(0,ctx.identificador().start.getLine(), indent.name);
                return new Type(Type.Natives.INVALIDO);
            }

            return indent.type;
        }


        Type type = checkExpression(st, ctx.expressao(0));
        for (int i = 1; i < ctx.expressao().size(); i++)
            type = type.checkType( checkExpression(st, ctx.expressao(i)));

        return type;
    }

    public Type checkSinglePortion(SymbolTable st, AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null)
            return new Type(Type.Natives.LITERAL);
        else {
            if (ctx.getChild(0).getText().contains("&"))
                return new Type(Type.Natives.ENDERECO);

            Variable indent = checkIndent(st, ctx.identificador());
            if (indent.type == null) {
                errorListener.addError(0,ctx.identificador().start.getLine(), indent.name);
                return new Type(Type.Natives.INVALIDO);
            }
            return indent.type;
        }
    }



    public Variable addNewType(SymbolTable st, Variable auxVar, String name) {
        if (st.contains(name)) {
            Variable template = st.getVar(name);
            if (template.type.natives == Type.Natives.REGISTRO) {
                Variable response = new Variable(auxVar.name, new Type(Type.Natives.REGISTRO));
                response.setRegister(template.register);
                response.type = auxVar.type;
                return response;
            }
        }
        return new Variable(null, null);
    }

    public String Suiting(AlgumaParser.IdentificadorContext base, String name){
        for (int i = 1; i < base.IDENT().size(); i++)
            name += "." + base.IDENT(i);
        return name;

    }
}