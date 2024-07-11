package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;


public class MyCustomErrorListener extends BaseErrorListener {

    public static final MyCustomErrorListener INSTANCE = new MyCustomErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
            int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException{

        Token token = (Token) offendingSymbol;


        if(checkError(token.getType())) {
            if (token.getType() == AlgumaLexer.Caracter_invalido) {
                throw new ParseCancellationException("Linha " + token.getLine() + ": " + token.getText() + " - simbolo nao identificado");
            }
            else if(AlgumaLexer.VOCABULARY.getSymbolicName(token.getType()).equals("CADEIA_SEM_FIM"))
            {
                throw new ParseCancellationException("Linha " + token.getLine() + ": " + "cadeia literal nao fechada");
            }
            else {
                throw new ParseCancellationException("Linha " + token.getLine() + ": " + "comentario nao fechado");
            }

        }
        else if (token.getType() == Token.EOF)// Checa erros nao-lexicos (sintaticos ou EOF)
                throw new ParseCancellationException("Linha " + token.getLine() + ": " + "erro sintatico proximo a EOF");
        else
                throw new ParseCancellationException("Linha " + token.getLine() + ": " + "erro sintatico proximo a " + token.getText());
        }

    private static Boolean checkError(int tokenType) {
        return tokenType == AlgumaLexer.CADEIA_SEM_FIM || tokenType == AlgumaLexer.COMENTARIO_SEM_FIM
                        || tokenType == AlgumaLexer.Caracter_invalido;
    }
}