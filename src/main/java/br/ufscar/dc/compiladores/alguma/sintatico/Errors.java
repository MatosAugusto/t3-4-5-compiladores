package br.ufscar.dc.compiladores.alguma.sintatico;
import java.util.ArrayList;

public class Errors {

    private final ArrayList<String> errors = new ArrayList<>();

    public void addError(int id, int line, String name){ // Add errors on the list
        String base = "Linha " + line;
        switch(id) {
            case 0:
                errors.add(base + ": identificador " + name + " nao declarado");
                break;
            case 1:
                errors.add(base + ": identificador " + name + " ja declarado anteriormente");
                break;
            case 2:
                errors.add(base + ": atribuicao nao compativel para " + name);
                break;
            case 3:
                errors.add(base + ": tipo " + name + " nao declarado");
                break;
            case 4:
                errors.add(base + ": incompatibilidade de parametros na chamada de " + name);
                break;
            case 5:
                errors.add(base + ": comando retorne nao permitido nesse escopo");
                break;
            default:
        }
    }

    public ArrayList<String> getErrors(){
        return errors;
    } // Return errors of the list

}
