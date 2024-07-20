package br.ufscar.dc.compiladores.alguma.sintatico;


import java.io.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;


public class Principal {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Falha na execuÃ§Ã£o.\nNÃºmero de parÃ¢metros invÃ¡lidos.");
            System.exit(0);
        }

        AlgumaLexer lexer = new AlgumaLexer(CharStreams.fromFileName(args[0]));
        AlgumaParser parser = new AlgumaParser(new CommonTokenStream(lexer));

        parser.removeErrorListeners();
        parser.addErrorListener(MyCustomErrorListener.INSTANCE);

        Visitor visitor = new Visitor();

        try (PrintWriter pw = new PrintWriter(args[1])){

            try{
                AlgumaParser.ProgramaContext ctx = parser.programa();
                visitor.visitPrograma(ctx);

                if (visitor.errorListener.getErrors().isEmpty()) {
                    CodeGenerator generator = new CodeGenerator(visitor.getScope());
                    generator.visit(ctx);
                    pw.print(generator.finalResponse.toString());
                }
                else{
                    for (String response : visitor.errorListener.getErrors())
                        pw.println(response);
                    pw.println("Fim da compilacao");
                }
                pw.close();
            }

            catch(ParseCancellationException exception) {
                pw.println(exception.getMessage());
                pw.println("Fim da compilacao");
                pw.close();
            }
        }
        catch(IOException exception){
            System.out.println("Falha na execuÃ§Ã£o.\nO programa nÃ£o conseguiu abrir o arquivo: " + args[1]+ ".");
        }

    }
}