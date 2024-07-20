package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.*;

public class Scope {

    private final LinkedList<SymbolTable> symbolTableScope;

    public Scope() {
        symbolTableScope = new LinkedList<>();
        newScope();
    }

    public final void newScope() {
        symbolTableScope.push(new SymbolTable());
    }

    public List<SymbolTable> runScope() {
        return symbolTableScope;
    }

    public void leaveScope() {
        symbolTableScope.pop();
    }

    public SymbolTable lookScope() {
        return symbolTableScope.peek();
    }
}