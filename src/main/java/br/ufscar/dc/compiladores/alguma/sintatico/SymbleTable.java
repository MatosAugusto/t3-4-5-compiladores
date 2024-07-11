package br.ufscar.dc.compiladores.alguma.sintatico;


import java.util.HashMap;

public class SymbleTable {
    private final HashMap<String, Variavel> table;

    public SymbleTable() {
        table = new HashMap<>();
    }

    public Tipo getType(String name) {
        return table.get(name).tipo;
    }

    public Variavel getVar(String name) {
        return table.get(name);
    }
    public void add(Variavel foo) {
        table.put(foo.varNome, foo);
    }
    public boolean include(String name) {
        return table.containsKey(name);
    }

    void add(AlgumaParser.Declaracao_localContext v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}