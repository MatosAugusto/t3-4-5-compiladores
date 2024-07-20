package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
// Outros imports vão ser necessários aqui. O NetBeans ou IntelliJ fazem isso automaticamente
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;


public class MyCustomErrorListener extends BaseErrorListener {

    public static final MyCustomErrorListener INSTANCE = new MyCustomErrorListener();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                            int charPositionInLine, String msg, RecognitionException e)
            throws ParseCancellationException{

        Token token = (Token) offendingSymbol;

        String base = "Linha " + token.getLine() + ": ";
        if(isError(token.getType())) {
            if (token.getType() == AlgumaLexer.Caracter_invalido) {
                throw new ParseCancellationException(base + token.getText() + " - simbolo nao identificado");
            }
            else if(AlgumaLexer.VOCABULARY.getSymbolicName(token.getType()).equals("CADEIA_SEM_FIM"))
            {
                throw new ParseCancellationException(base + "cadeia literal nao fechada");
            }
            else {
                throw new ParseCancellationException(base + "comentario nao fechado");
            }

        }
        else if (token.getType() == Token.EOF)
            throw new ParseCancellationException(base + "erro sintatico proximo a EOF");
        else
            throw new ParseCancellationException(base + "erro sintatico proximo a " + token.getText());
    }

    private static Boolean isError(int tkType) {
        return tkType == AlgumaLexer.CADEIA_SEM_FIM || tkType == AlgumaLexer.COMENTARIO_SEM_FIM
                || tkType == AlgumaLexer.Caracter_invalido;
    }
}