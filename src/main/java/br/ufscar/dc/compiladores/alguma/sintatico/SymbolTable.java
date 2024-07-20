package br.ufscar.dc.compiladores.alguma.sintatico;


import java.util.*;

public class SymbolTable {
    private final HashMap<String, Variable> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    public Type getType(String name) {
        return table.get(name).type;
    }

    public Variable getVar(String name) {
        return table.get(name);
    }

    public void add(Variable var) {
        table.put(var.name, var);
    }

    public boolean contains(String name) {
        return table.containsKey(name);
    }

    void add(AlgumaParser.Declaracao_localContext v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}