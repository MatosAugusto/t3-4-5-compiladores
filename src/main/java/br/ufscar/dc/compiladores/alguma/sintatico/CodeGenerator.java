package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.ArrayList;

public class CodeGenerator extends AlgumaBaseVisitor<Void> {
    private final Scope scope;
    public final StringBuilder finalOutput = new StringBuilder();
    private Variavel aux;

    public CodeGenerator(Scope scope){
        this.scope = scope;
    }

    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) {
        finalOutput.append("#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n#include <math.h>\n\n");
        if (!ctx.declaracoes().isEmpty())
            for (AlgumaParser.Decl_local_globalContext declaration : ctx.declaracoes().decl_local_global())
                visitDecl_local_global(declaration);
        finalOutput.append("int main() {\n");
        for (AlgumaParser.Declaracao_localContext declaration : ctx.corpo().declaracao_local())
            visitDeclaracao_local(declaration);
        for (AlgumaParser.CmdContext cmd : ctx.corpo().cmd())
            visitCmd(cmd);
        finalOutput.append("return 0;\n}\n");
        return null;
    }

    @Override
    public Void visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null)
            visitDeclaracao_local(ctx.declaracao_local());
        else
            visitDeclaracao_global(ctx.declaracao_global());
        return null;
    }

    @Override
    public Void visitDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        if(ctx.getChild(0).getText().equals("funcao")){
            Variavel function = scope.lookScope().getVar(ctx.IDENT().getText());
            finalOutput.append(function.getFuncao().getTipoRetorno().getFormat()).append(" ").append(function.varNome).append("(");
            ArrayList<Variavel> parameters = function.getFuncao().getParametros();

            if (parameters.get(0).tipo.aNativo == Tipo.Nativo.LITERAL)
                finalOutput.append(parameters.get(0).tipo.getFormat()).append(" *").append(parameters.get(0).varNome);
            else
                finalOutput.append(parameters.get(0).tipo.getFormat()).append(" ").append(parameters.get(0).varNome);

            for (int i = 1; i < parameters.size(); i++) {
                finalOutput.append(", ");
                if (parameters.get(i).tipo.aNativo == Tipo.Nativo.LITERAL)
                    finalOutput.append(parameters.get(i).tipo.getFormat()).append(" *").append(parameters.get(i).varNome);
                else
                    geraVariavel(parameters.get(i));
            }
            finalOutput.append(") {\n");
            for (AlgumaParser.Declaracao_localContext declaration : ctx.declaracao_local())
                visitDeclaracao_local(declaration);

            scope.createNewScope();

            for (Variavel v : parameters) {
                scope.lookScope().add(v);
            }

            for (AlgumaParser.Declaracao_localContext v : function.getFuncao().getLocals())
                scope.lookScope().add(v);

            for (AlgumaParser.CmdContext cmd : ctx.cmd())
                visitCmd(cmd);

            scope.leaveScope();
            finalOutput.append("}\n");
        }   //Vê se é um procedimento e faz as devidas verificações
        else if(ctx.getChild(0).getText().equals("procedimento")){
            Variavel proc = scope.lookScope().getVar(ctx.IDENT().getText());
            finalOutput.append("void ").append(proc.varNome).append("(");
            ArrayList<Variavel> parametros = proc.getProcedimento().getParametros();
            if (Tipo.Nativo.LITERAL == parametros.get(0).tipo.aNativo)
                finalOutput.append(parametros.get(0).tipo.getFormat()).append(" *").append(parametros.get(0).varNome);
            else
                finalOutput.append(parametros.get(0).tipo.getFormat()).append(" ").append(parametros.get(0).varNome);
            for (int i = 1; i < parametros.size(); i++) {
                finalOutput.append(", ");
                if (parametros.get(i).tipo.aNativo == Tipo.Nativo.LITERAL)
                    finalOutput.append(parametros.get(i).tipo.getFormat()).append(" *").append(parametros.get(0).varNome);
                else
                    geraVariavel(parametros.get(i));
            }
            finalOutput.append(") {\n");
            for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local())
                visitDeclaracao_local(declaracao);
            scope.createNewScope();
            for (Variavel v : parametros)
                scope.lookScope().add(v);
            for (Variavel v : proc.getProcedimento().getLocals())
                scope.lookScope().add(v);
            for (AlgumaParser.CmdContext cmd : ctx.cmd())
                visitCmd(cmd);
            scope.leaveScope();
            finalOutput.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(AlgumaParser.Declaracao_localContext ctx) { // Constante, Tipo, ou Declare.
        switch (ctx.getChild(0).getText()) {
            case "constante":
                finalOutput.append("#define ").append(ctx.IDENT().getText()).append(" ");
                visitValor_constante(ctx.valor_constante());
                break;
            case "tipo":
                finalOutput.append("typedef struct {\n");
                this.aux = scope.lookScope().getVar(ctx.IDENT().getText());
                if (ctx.tipo().registro() != null)
                    for (Variavel v : aux.getRegistro().getAll()) {
                        geraVariavel(v);
                        finalOutput.append(";\n");
                    }
                finalOutput.append("} ").append(ctx.IDENT().getText()).append(";\n");
                break;
            case "declare":
                visitVariavel(ctx.variavel());
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public Void visitVariavel(AlgumaParser.VariavelContext ctx) {
        for (AlgumaParser.IdentificadorContext id : ctx.identificador()) {
            String varNome = id.IDENT(0).getText();
            for (int i = 1; i < id.IDENT().size(); i++)
                varNome += "." + id.IDENT(i).getText();
            Variavel ident = scope.lookScope().getVar(varNome);
            geraVariavel(ident);
            if (!id.dimensao().exp_aritmetica().isEmpty())
                visitDimensao(id.dimensao());
            finalOutput.append(";\n");
        }
        return null;
    }
    @Override
    public Void visitCmd(AlgumaParser.CmdContext ctx) {
        if (ctx.cmdLeia() != null){
            Variavel ident = scope.lookScope().getVar(ctx.cmdLeia().identificador(0).getText());
            finalOutput.append(String.format("scanf(\"%s\", &%s);\n", ident.tipo.getFormatSpec(), ident.varNome));}
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao()) {
                Tipo tipoExp = Visitor.VISITOR.checkExpressao(scope.lookScope(), exp);
                finalOutput.append(String.format("printf(\"%s\", ", tipoExp.getFormatSpec()));
                visitExpressao(exp);
                finalOutput.append(");\n");
            }
        }
        else if (ctx.cmdSe() != null) {
            finalOutput.append("if (");
            visitExpressao(ctx.cmdSe().expressao());
            finalOutput.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd())
                visitCmd(cmd);
            finalOutput.append("}\n");
            if (ctx.cmdSe().cmd().size() > 0) {
                finalOutput.append("else {\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd())
                    visitCmd(cmd);
                finalOutput.append("}\n");
            }
        }
        else if (ctx.cmdAtribuicao() != null){
            if (ctx.cmdAtribuicao().getChild(0).getText().equals("^"))
                finalOutput.append("*");
            Variavel ident = Visitor.VISITOR.checkIdent(scope.lookScope(), ctx.cmdAtribuicao().identificador());
            if (ident.tipo != null && ident.tipo.aNativo != Tipo.Nativo.LITERAL) {
                visitIdentificador(ctx.cmdAtribuicao().identificador());
                finalOutput.append(" = ");
                visitExpressao(ctx.cmdAtribuicao().expressao());
            } else {
                finalOutput.append("strcpy(").append(ctx.cmdAtribuicao().identificador().getText()).append(",");
                visitExpressao(ctx.cmdAtribuicao().expressao());
                finalOutput.append(")");
            }
            finalOutput.append(";\n");
        }
        else if (ctx.cmdCaso() != null){
            finalOutput.append("switch (");
            visitExp_aritmetica(ctx.cmdCaso().exp_aritmetica());
            finalOutput.append(") {\n");
            for (AlgumaParser.Item_selecaoContext i : ctx.cmdCaso().selecao().item_selecao()) {
                visitConstantes(i.constantes());
                for (AlgumaParser.CmdContext cmd : i.cmd())
                    visitCmd(cmd);
                finalOutput.append("break;\n");
            }
            if (!ctx.cmdCaso().cmd().isEmpty()) {
                finalOutput.append("default:\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdCaso().cmd())
                    visitCmd(cmd);
            }
            finalOutput.append("}\n");
        }
        else if (ctx.cmdPara() != null) {
            finalOutput.append("for (");
            Variavel ident = scope.lookScope().getVar(ctx.cmdPara().IDENT().getText());
            finalOutput.append(ident.varNome).append(" = ");
            visitExp_aritmetica(ctx.cmdPara().exp_aritmetica(0));
            finalOutput.append("; ");
            finalOutput.append(ident.varNome).append(" <= ");
            visitExp_aritmetica(ctx.cmdPara().exp_aritmetica(1));
            finalOutput.append("; ");
            finalOutput.append(ident.varNome).append("++) {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdPara().cmd())
                visitCmd(cmd);
            finalOutput.append("}\n");
        }
        else if (ctx.cmdEnquanto() != null){
            finalOutput.append("while (");
            visitExpressao(ctx.cmdEnquanto().expressao());
            finalOutput.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdEnquanto().cmd())
                visitCmd(cmd);
            finalOutput.append("}\n");
        }
        else if (ctx.cmdFaca() != null){
            finalOutput.append("do {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                visitCmd(cmd);
            finalOutput.append("} while (");
            visitExpressao(ctx.cmdFaca().expressao());
            finalOutput.append(");\n");
        }
        else if (ctx.cmdChamada() != null){
            finalOutput.append(ctx.cmdChamada().IDENT().getText()).append("(");
            visitExpressao(ctx.cmdChamada().expressao(0));
            for (int i = 1; i < ctx.cmdChamada().expressao().size(); i++) {
                finalOutput.append(", ");
                visitExpressao(ctx.cmdChamada().expressao(i));
            }
            finalOutput.append(");\n");
        }
        else if (ctx.cmdRetorne() != null){
            finalOutput.append("return ");
            visitExpressao(ctx.cmdRetorne().expressao());
            finalOutput.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitConstantes(AlgumaParser.ConstantesContext ctx) {
        int inicio = ctx.numero_intervalo(0).op_unario() != null ? -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText()); // Corrigido aqui
        int fim;
        if (ctx.numero_intervalo(0).op_unario(1) != null) // Corrigido aqui
            fim = -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
            fim = Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else
            fim = inicio;
        for (int i = inicio; i <= fim; i++)
            finalOutput.append("case ").append(i).append(":\n");

        for (int i = 1; i < ctx.numero_intervalo().size(); i++) {
            inicio = ctx.numero_intervalo(i).op_unario() != null ? -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText()); // Corrigido aqui
            if (ctx.numero_intervalo(i).op_unario(1) != null) // Corrigido aqui
                fim = -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
                fim = Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else
                fim = inicio;
            for (int j = inicio; i <= fim; i++)
                finalOutput.append("case ").append(j).append(":\n");
        }
        return null;
    }

    @Override
    public Void visitTermo_logico(AlgumaParser.Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));
        for (int i = 0; i < ctx.op_logico_2().size(); i++) {
            finalOutput.append(" && ");
            visitFator_logico(ctx.fator_logico(i + 1));
        }
        return null;
    }

    @Override
    public Void visitFator_logico(AlgumaParser.Fator_logicoContext ctx) {
        // Se o operador for não, imprime '!' no arquivo final
        if (ctx.getChild(0).getText().equals("nao"))
            finalOutput.append("!");
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

    @Override
    public Void visitParcela_logica(AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) {
            visitExp_relacional(ctx.exp_relacional());
            return null;
        }
        if (ctx.getText().equals("verdadeiro"))
            finalOutput.append(" true ");
        else
            finalOutput.append(" false ");

        return null;
    }

    @Override
    public Void visitIdentificador(AlgumaParser.IdentificadorContext ctx) {
        finalOutput.append(ctx.IDENT(0).getText());
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            finalOutput.append(".");
            finalOutput.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao().getChild(0) != null)
            visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(AlgumaParser.DimensaoContext ctx) {
        finalOutput.append("[");
        for (AlgumaParser.Exp_aritmeticaContext exp : ctx.exp_aritmetica())
            visitExp_aritmetica(exp);
        finalOutput.append("]");
        return null;
    }

    @Override
    public Void visitExp_relacional(AlgumaParser.Exp_relacionalContext ctx) {
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        if (ctx.op_relacional() != null) {
            visitOp_relacional(ctx.op_relacional());
            visitExp_aritmetica(ctx.exp_aritmetica(1));
        }
        return null;
    }

    @Override
    public Void visitOp_relacional(AlgumaParser.Op_relacionalContext ctx){ // Equivalencia do operador de Java para C
        switch (ctx.getText()){
            case "=":
                finalOutput.append(" == ");
                break;
            case "<>":
                finalOutput.append(" != ");
                break;
            default:
                finalOutput.append(ctx.getText());
                break;
        }
        return null;
    }

    @Override
    public Void visitTermo(AlgumaParser.TermoContext ctx) {
        visitFator(ctx.fator(0));
        for (int i = 0; i < ctx.op2().size(); i++) {
            finalOutput.append(" ").append(ctx.op2(i).getText()).append(" ");
            visitFator(ctx.fator(i + 1));
        }

        return null;
    }

    @Override
    public Void visitFator(AlgumaParser.FatorContext ctx) {
        visitParcela(ctx.parcela(0));
        for (int i = 0; i < ctx.op3().size(); i++) {
            finalOutput.append(" ").append(ctx.op3(i).getText()).append(" ");
            visitParcela(ctx.parcela(i));
        }
        return null;
    }

    @Override
    public Void visitParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            if (ctx.op_unario() != null)
                finalOutput.append(" ").append(ctx.op_unario().getText());
            visitParcela_unario(ctx.parcela_unario());
        } else
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        return null;
    }

    @Override
    public Void visitExp_aritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        visitTermo(ctx.termo(0));
        for (int i = 0; i < ctx.op1().size(); i++) {
            finalOutput.append(" ").append(ctx.op1(i).getText()).append(" ");
            visitTermo(ctx.termo(i + 1));
        }
        return null;
    }

    @Override
    public Void visitParcela_unario(AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("^"))
                finalOutput.append("*");
            visitIdentificador(ctx.identificador());
        } else if (ctx.IDENT() != null) {
            finalOutput.append(ctx.IDENT().getText()).append("(");
            visitExpressao(ctx.expressao(0));
            for (int i = 1; i < ctx.expressao().size(); i++) {
                finalOutput.append(", ");
                visitExpressao(ctx.expressao(i));
            }
            finalOutput.append(")");
        }
        else if (ctx.NUM_INT() != null)
            finalOutput.append(ctx.NUM_INT().getText());
        else if (ctx.NUM_REAL() != null)
            finalOutput.append(ctx.NUM_REAL().getText());
        else {
            finalOutput.append("(");
            for (AlgumaParser.ExpressaoContext exp : ctx.expressao())
                visitExpressao(exp);

            finalOutput.append(")");
        }
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("&"))
                finalOutput.append("&");
            visitIdentificador(ctx.identificador());
        } else
            finalOutput.append(ctx.CADEIA().getText());
        return null;
    }

    public void geraVariavel(Variavel v) {
        if (v.tipo != null && v.tipo.aNativo != null) {
            if(null != v.tipo.aNativo)
                switch (v.tipo.aNativo) {
                    case LITERAL:
                        finalOutput.append(String.format("%s %s[100]", v.tipo.getFormat(), v.varNome));
                        break;
                    case PONTEIRO:
                        finalOutput.append(String.format("%s *%s", v.getTipoPonteiroAninhado().getFormat(), v.varNome));
                        break;
                    case REGISTRO:
                        finalOutput.append("struct {\n");
                        for (Variavel i : v.getRegistro().getAll()) {
                            geraVariavel(i);
                            finalOutput.append(";\n");
                        }   finalOutput.append("} ").append(v.varNome);
                        break;
                    case INTEIRO:
                        finalOutput.append(String.format("%s %s", v.tipo.getFormat(), v.varNome));
                        break;
                    case REAL:
                        finalOutput.append(String.format("%s %s", v.tipo.getFormat(), v.varNome));
                        break;
                    default:
                        break;
                }
        } else
            finalOutput.append(String.format("%s %s", v.tipo.criados, v.varNome));
    }

    @Override
    public Void visitValor_constante(AlgumaParser.Valor_constanteContext ctx) { // Equivalencia do operador de Java para C
        if (ctx.CADEIA() != null)
            finalOutput.append("\"").append(ctx.CADEIA().getText()).append("\"\n");
        else if (ctx.NUM_INT() != null)
            finalOutput.append(Integer.parseInt(ctx.NUM_INT().getText())).append("\n");
        else if (ctx.NUM_REAL() != null)
            finalOutput.append(Float.parseFloat(ctx.NUM_REAL().getText())).append("\n");
        else if (ctx.getChild(0).getText().equals("verdadeiro"))
            finalOutput.append("1\n");
        else
            finalOutput.append("0\n");

        return null;
    }

    @Override
    public Void visitExpressao(AlgumaParser.ExpressaoContext ctx) {
        visitTermo_logico(ctx.termo_logico(0));
        for (int i = 0; i < ctx.op_logico_1().size(); i++) {
            finalOutput.append(" || ");
            visitTermo_logico(ctx.termo_logico(i + 1));
        }
        return null;
    }

}
