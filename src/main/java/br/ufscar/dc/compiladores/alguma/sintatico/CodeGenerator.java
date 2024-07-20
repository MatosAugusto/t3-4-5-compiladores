package br.ufscar.dc.compiladores.alguma.sintatico;
import java.util.*;

public class CodeGenerator extends AlgumaBaseVisitor<Void> {
    private final Scope scope;
    public final StringBuilder finalResponse = new StringBuilder();
    private Variable auxVar;

    public CodeGenerator(Scope scope){
        this.scope = scope;
    }


    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) {
        finalResponse.append("#include <stdio.h>\n#include <stdlib.h>\n#include <string.h>\n#include <math.h>\n\n");
        if (!ctx.declaracoes().isEmpty())
            for (AlgumaParser.Decl_local_globalContext declaration : ctx.declaracoes().decl_local_global())
                visitDecl_local_global(declaration);
        finalResponse.append("int main() {\n");
        for (AlgumaParser.Declaracao_localContext declaration : ctx.corpo().declaracao_local())
            visitDeclaracao_local(declaration);
        for (AlgumaParser.CmdContext cmd : ctx.corpo().cmd())
            visitCmd(cmd);
        finalResponse.append("return 0;\n}\n");
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
            Variable auxFunction = scope.lookScope().getVar(ctx.IDENT().getText());
            finalResponse.append(auxFunction.getFunction().getResponseType().getFormat()).append(" ").append(auxFunction.name).append("(");
            ArrayList<Variable> params = auxFunction.getFunction().getParams();

            if (params.get(0).type.natives == Type.Natives.LITERAL)
                finalResponse.append(params.get(0).type.getFormat()).append(" *").append(params.get(0).name);
            else
                finalResponse.append(params.get(0).type.getFormat()).append(" ").append(params.get(0).name);

            for (int i = 1; i < params.size(); i++) {
                finalResponse.append(", ");
                if (params.get(i).type.natives == Type.Natives.LITERAL)
                    finalResponse.append(params.get(i).type.getFormat()).append(" *").append(params.get(i).name);
                else
                    varGenerator(params.get(i));
            }
            finalResponse.append(") {\n");
            for (AlgumaParser.Declaracao_localContext declaration : ctx.declaracao_local())
                visitDeclaracao_local(declaration);

            scope.newScope();

            for (Variable v : params) {
                scope.lookScope().add(v);
            }

            for (AlgumaParser.Declaracao_localContext v : auxFunction.getFunction().getLocals())
                scope.lookScope().add(v);

            for (AlgumaParser.CmdContext cmd : ctx.cmd())
                visitCmd(cmd);

            scope.leaveScope();
            finalResponse.append("}\n");
        }
        else if(ctx.getChild(0).getText().equals("procedimento")){
            Variable procedure = scope.lookScope().getVar(ctx.IDENT().getText());
            finalResponse.append("void ").append(procedure.name).append("(");
            ArrayList<Variable> params = procedure.getProcedure().getParams();
            if (Type.Natives.LITERAL == params.get(0).type.natives)
                finalResponse.append(params.get(0).type.getFormat()).append(" *").append(params.get(0).name);
            else
                finalResponse.append(params.get(0).type.getFormat()).append(" ").append(params.get(0).name);
            for (int i = 1; i < params.size(); i++) {
                finalResponse.append(", ");
                if (params.get(i).type.natives == Type.Natives.LITERAL)
                    finalResponse.append(params.get(i).type.getFormat()).append(" *").append(params.get(0).name);
                else
                    varGenerator(params.get(i));
            }
            finalResponse.append(") {\n");
            for (AlgumaParser.Declaracao_localContext declaracao : ctx.declaracao_local())
                visitDeclaracao_local(declaracao);
            scope.newScope();
            for (Variable v : params)
                scope.lookScope().add(v);
            for (Variable v : procedure.getProcedure().getLocals())
                scope.lookScope().add(v);
            for (AlgumaParser.CmdContext cmd : ctx.cmd())
                visitCmd(cmd);
            scope.leaveScope();
            finalResponse.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(AlgumaParser.Declaracao_localContext ctx) {
        switch (ctx.getChild(0).getText()) {
            case "constante":
                finalResponse.append("#define ").append(ctx.IDENT().getText()).append(" ");
                visitValor_constante(ctx.valor_constante());
                break;
            case "tipo":
                finalResponse.append("typedef struct {\n");
                this.auxVar = scope.lookScope().getVar(ctx.IDENT().getText());
                if (ctx.tipo().registro() != null)
                    for (Variable v : auxVar.getRegister().getAll()) {
                        varGenerator(v);
                        finalResponse.append(";\n");
                    }
                finalResponse.append("} ").append(ctx.IDENT().getText()).append(";\n");
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
            String name = id.IDENT(0).getText();
            for (int i = 1; i < id.IDENT().size(); i++)
                name += "." + id.IDENT(i).getText();
            Variable indent = scope.lookScope().getVar(name);
            varGenerator(indent);
            if (!id.dimensao().exp_aritmetica().isEmpty())
                visitDimensao(id.dimensao());
            finalResponse.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitCmd(AlgumaParser.CmdContext ctx) {
        if (ctx.cmdLeia() != null){
            Variable indent = scope.lookScope().getVar(ctx.cmdLeia().identificador(0).getText());
            finalResponse.append(String.format("scanf(\"%s\", &%s);\n", indent.type.getFormatSpec(), indent.name));}
        else if (ctx.cmdEscreva() != null){
            for (AlgumaParser.ExpressaoContext exp : ctx.cmdEscreva().expressao()) {
                Type expressionType = Visitor.I.checkExpression(scope.lookScope(), exp);
                finalResponse.append(String.format("printf(\"%s\", ", expressionType.getFormatSpec()));
                this.visitExpressao(exp);
                finalResponse.append(");\n");
            }
        }
        else if (ctx.cmdSe() != null){
            finalResponse.append("if (");
            this.visitExpressao(ctx.cmdSe().expressao());
            finalResponse.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd1)
                visitCmd(cmd);
            finalResponse.append("}\n");
            if (ctx.cmdSe().cmd2.size() > 0) {
                finalResponse.append("else {\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdSe().cmd2)
                    visitCmd(cmd);
                finalResponse.append("}\n");
            }
        }
        else if (ctx.cmdAtribuicao() != null){
            if (ctx.cmdAtribuicao().getChild(0).getText().equals("^"))
                finalResponse.append("*");
            Variable indent = Visitor.I.checkIndent(scope.lookScope(), ctx.cmdAtribuicao().identificador());
            if (indent.type != null && indent.type.natives != Type.Natives.LITERAL) {
                visitIdentificador(ctx.cmdAtribuicao().identificador());
                finalResponse.append(" = ");
                this.visitExpressao(ctx.cmdAtribuicao().expressao());
            } else {
                finalResponse.append("strcpy(").append(ctx.cmdAtribuicao().identificador().getText()).append(",");
                this.visitExpressao(ctx.cmdAtribuicao().expressao());
                finalResponse.append(")");
            }
            finalResponse.append(";\n");
        }
        else if (ctx.cmdCaso() != null){
            finalResponse.append("switch (");
            visitExp_aritmetica(ctx.cmdCaso().exp_aritmetica());
            finalResponse.append(") {\n");
            for (AlgumaParser.Item_selecaoContext i : ctx.cmdCaso().selecao().item_selecao()) {
                this.visitConstantes(i.constantes());
                for (AlgumaParser.CmdContext cmd : i.cmd())
                    visitCmd(cmd);
                finalResponse.append("break;\n");
            }
            if (!ctx.cmdCaso().cmd().isEmpty()) {
                finalResponse.append("default:\n");
                for (AlgumaParser.CmdContext cmd : ctx.cmdCaso().cmd())
                    visitCmd(cmd);
            }
            finalResponse.append("}\n");
        }
        else if (ctx.cmdPara() != null){
            finalResponse.append("for (");
            Variable indent = scope.lookScope().getVar(ctx.cmdPara().IDENT().getText());
            finalResponse.append(indent.name).append(" = ");
            visitExp_aritmetica(ctx.cmdPara().a);
            finalResponse.append("; ");
            finalResponse.append(indent.name).append(" <= ");
            visitExp_aritmetica(ctx.cmdPara().b);
            finalResponse.append("; ");
            finalResponse.append(indent.name).append("++) {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdPara().cmd())
                visitCmd(cmd);
            finalResponse.append("}\n");
        }
        else if (ctx.cmdEnquanto() != null){
            finalResponse.append("while (");
            this.visitExpressao(ctx.cmdEnquanto().expressao());
            finalResponse.append(") {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdEnquanto().cmd())
                visitCmd(cmd);
            finalResponse.append("}\n");
        }
        else if (ctx.cmdFaca() != null){
            finalResponse.append("do {\n");
            for (AlgumaParser.CmdContext cmd : ctx.cmdFaca().cmd())
                visitCmd(cmd);
            finalResponse.append("} while (");
            this.visitExpressao(ctx.cmdFaca().expressao());
            finalResponse.append(");\n");
        }
        else if (ctx.cmdChamada() != null){
            finalResponse.append(ctx.cmdChamada().IDENT().getText()).append("(");
            this.visitExpressao(ctx.cmdChamada().expressao(0));
            for (int i = 1; i < ctx.cmdChamada().expressao().size(); i++) {
                finalResponse.append(", ");
                this.visitExpressao(ctx.cmdChamada().expressao(i));
            }
            finalResponse.append(");\n");
        }
        else if (ctx.cmdRetorne() != null){
            finalResponse.append("return ");
            this.visitExpressao(ctx.cmdRetorne().expressao());
            finalResponse.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitConstantes(AlgumaParser.ConstantesContext ctx) {
        int begin = ctx.numero_intervalo(0).opu1 != null ? -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(0).getText());
        int end;
        if (ctx.numero_intervalo(0).opu2 != null)
            end = -Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
            end = Integer.parseInt(ctx.numero_intervalo(0).NUM_INT(1).getText());
        else
            end = begin;
        for (int i = begin; i <= end; i++)
            finalResponse.append("case ").append(i).append(":\n");

        for (int i = 1; i < ctx.numero_intervalo().size(); i++){
            begin = ctx.numero_intervalo(i).opu1 != null ? -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText()) : Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(0).getText());
            if (ctx.numero_intervalo(i).opu2 != null)
                end = -Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else if (ctx.numero_intervalo(0).NUM_INT(1) != null)
                end = Integer.parseInt(ctx.numero_intervalo(i).NUM_INT(1).getText());
            else
                end = begin;
            for (int j = begin; i <= end; i++)
                finalResponse.append("case ").append(j).append(":\n");
        }
        return null;
    }

    @Override
    public Void visitTermo_logico(AlgumaParser.Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));
        for (int i = 0; i < ctx.op_logico_2().size(); i++) {
            finalResponse.append(" && ");
            visitFator_logico(ctx.fator_logico(i + 1));
        }
        return null;
    }

    @Override
    public Void visitFator_logico(AlgumaParser.Fator_logicoContext ctx) {
        if (ctx.getChild(0).getText().equals("nao"))
            finalResponse.append("!");
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
            finalResponse.append(" true ");
        else
            finalResponse.append(" false ");

        return null;
    }

    @Override
    public Void visitIdentificador(AlgumaParser.IdentificadorContext ctx) {
        finalResponse.append(ctx.IDENT(0).getText());
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            finalResponse.append(".");
            finalResponse.append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao().getChild(0) != null)
            visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(AlgumaParser.DimensaoContext ctx) {
        finalResponse.append("[");
        for (AlgumaParser.Exp_aritmeticaContext exp : ctx.exp_aritmetica())
            visitExp_aritmetica(exp);
        finalResponse.append("]");
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
    public Void visitOp_relacional(AlgumaParser.Op_relacionalContext ctx){
        switch (ctx.getText()){
            case "=":
                finalResponse.append(" == ");
                break;
            case "<>":
                finalResponse.append(" != ");
                break;
            default:
                finalResponse.append(ctx.getText());
                break;
        }
        return null;
    }

    @Override
    public Void visitTermo(AlgumaParser.TermoContext ctx) {
        this.visitFator(ctx.fator(0));
        for (int i = 0; i < ctx.op2().size(); i++) {
            finalResponse.append(" ").append(ctx.op2(i).getText()).append(" ");
            this.visitFator(ctx.fator(i + 1));
        }

        return null;
    }

    @Override
    public Void visitFator(AlgumaParser.FatorContext ctx) {
        this.visitParcela(ctx.parcela(0));
        for (int i = 0; i < ctx.op3().size(); i++) {
            finalResponse.append(" ").append(ctx.op3(i).getText()).append(" ");
            this.visitParcela(ctx.parcela(i));
        }
        return null;
    }

    @Override
    public Void visitParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            if (ctx.op_unario() != null)
                finalResponse.append(" ").append(ctx.op_unario().getText());
            this.visitParcela_unario(ctx.parcela_unario());
        } else
            this.visitParcela_nao_unario(ctx.parcela_nao_unario());
        return null;
    }

    @Override
    public Void visitExp_aritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        this.visitTermo(ctx.termo(0));
        for (int i = 0; i < ctx.op1().size(); i++) {
            finalResponse.append(" ").append(ctx.op1(i).getText()).append(" ");
            this.visitTermo(ctx.termo(i + 1));
        }
        return null;
    }

    @Override
    public Void visitParcela_unario(AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("^"))
                finalResponse.append("*");
            visitIdentificador(ctx.identificador());
        } else if (ctx.IDENT() != null) {
            finalResponse.append(ctx.IDENT().getText()).append("(");
            this.visitExpressao(ctx.expressao(0));
            for (int i = 1; i < ctx.expressao().size(); i++) {
                finalResponse.append(", ");
                this.visitExpressao(ctx.expressao(i));
            }
            finalResponse.append(")");
        }
        else if (ctx.NUM_INT() != null)
            finalResponse.append(ctx.NUM_INT().getText());
        else if (ctx.NUM_REAL() != null)
            finalResponse.append(ctx.NUM_REAL().getText());
        else {
            finalResponse.append("(");
            for (AlgumaParser.ExpressaoContext exp : ctx.expressao())
                this.visitExpressao(exp);

            finalResponse.append(")");
        }
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            if (ctx.getChild(0).getText().equals("&"))
                finalResponse.append("&");
            visitIdentificador(ctx.identificador());
        } else
            finalResponse.append(ctx.CADEIA().getText());
        return null;
    }


    public void varGenerator(Variable v) {
        if (v.type != null && v.type.natives != null) {
            if(null != v.type.natives)
                switch (v.type.natives) {
                    case LITERAL:
                        finalResponse.append(String.format("%s %s[100]", v.type.getFormat(), v.name));
                        break;
                    case PONTEIRO:
                        finalResponse.append(String.format("%s *%s", v.getNestedPointerType().getFormat(), v.name));
                        break;
                    case REGISTRO:
                        finalResponse.append("struct {\n");
                        for (Variable i : v.getRegister().getAll()) {
                            varGenerator(i);
                            finalResponse.append(";\n");
                        }   finalResponse.append("} ").append(v.name);
                        break;
                    case INTEIRO:
                        finalResponse.append(String.format("%s %s", v.type.getFormat(), v.name));
                        break;
                    case REAL:
                        finalResponse.append(String.format("%s %s", v.type.getFormat(), v.name));
                        break;
                    default:
                        break;
                }
        } else
            finalResponse.append(String.format("%s %s", v.type.created, v.name));
    }

    @Override
    public Void visitValor_constante(AlgumaParser.Valor_constanteContext ctx) {
        if (ctx.CADEIA() != null)
            finalResponse.append("\"").append(ctx.CADEIA().getText()).append("\"\n");
        else if (ctx.NUM_INT() != null)
            finalResponse.append(Integer.parseInt(ctx.NUM_INT().getText())).append("\n");
        else if (ctx.NUM_REAL() != null)
            finalResponse.append(Float.parseFloat(ctx.NUM_REAL().getText())).append("\n");
        else if (ctx.getChild(0).getText().equals("verdadeiro"))
            finalResponse.append("1\n");
        else
            finalResponse.append("0\n");

        return null;
    }


    @Override
    public Void visitExpressao(AlgumaParser.ExpressaoContext ctx) {
        this.visitTermo_logico(ctx.termo_logico(0));
        for (int i = 0; i < ctx.op_logico_1().size(); i++) {
            finalResponse.append(" || ");
            this.visitTermo_logico(ctx.termo_logico(i + 1));
        }
        return null;
    }

}
