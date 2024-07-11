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

        // Lê 'entrada.txt'
        AlgumaLexer lexer = new AlgumaLexer(CharStreams.fromFileName(args[0]));
        AlgumaParser parser = new AlgumaParser(new CommonTokenStream(lexer));


        parser.removeErrorListeners();
        parser.addErrorListener(MyCustomErrorListener.INSTANCE);

        Visitor analisador = new Visitor();


        // Abre 'saida.txt'
        try (PrintWriter saida = new PrintWriter(args[1])){

            try{
                AlgumaParser.ProgramaContext c = parser.programa();
                analisador.visitPrograma(c);

                if (analisador.errorlist.getErrors().isEmpty()) {
                    CodeGenerator gerador = new CodeGenerator(analisador.getScope());
                    gerador.visit(c);
                    saida.print(gerador.finalOutput.toString());
                }
                else{
                    for (String retorno : analisador.errorlist.getErrors())
                        saida.println(retorno);
                    saida.println("Fim da compilacao");
                }
                saida.close();
            }

            catch(ParseCancellationException exception) {
                saida.println(exception.getMessage());
                saida.println("Fim da compilacao");
                saida.close();
            }
        }
        catch(IOException exception){
            System.out.println("Falha na execuÃ§Ã£o.\nO programa nÃ£o conseguiu abrir o arquivo: " + args[1]+ ".");
        }

    }
}