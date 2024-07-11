package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.LinkedList;
import java.util.List;

public class Scope {

    private final LinkedList<SymbleTable> symbleTabScope;

    public Scope() {
        symbleTabScope = new LinkedList<>();
        createNewScope();
    }

    public final void createNewScope() {
        symbleTabScope.push(new SymbleTable());
    }

    public List<SymbleTable> runScope() {
        return symbleTabScope;
    }

    public void leaveScope() {
        symbleTabScope.pop();
    }

    public SymbleTable lookScope() {
        return symbleTabScope.peek();
    }
}