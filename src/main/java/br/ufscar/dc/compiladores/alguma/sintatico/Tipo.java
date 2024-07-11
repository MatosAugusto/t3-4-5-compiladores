package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.ArrayList;

public class Tipo {
    enum Nativo {INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        PONTEIRO,
        ENDERECO,
        REGISTRO,
        PROCEDIMENTO,
        FUNCAO,
        INVALIDO}

    public static ArrayList<String> Criados = new ArrayList<>();

    public Nativo aNativo = null;
    public String criados = null;
    public Tipo apontado = null;

    public Tipo(){
        aNativo = null;
        criados = null;
    }
    public Tipo(Nativo tipo) {
        aNativo = tipo;
    }


    public Tipo(String tipo) {
        criados = tipo;
    }

    public Tipo(Tipo filho) {
        aNativo = Nativo.PONTEIRO;
        apontado = filho;
    }

    public Tipo getTipo() {
        if (apontado != null)
            return apontado.getTipo();
        return this;
    }
    public boolean tipoVazio(){
        return (this != null && this.aNativo != null);
    }


    public static String getTipo(String tipo) {
        String existe = Criados.stream()
                .filter(str -> str.trim().contains(tipo))
                .findAny()
                .orElse("");
        if(!"".equals(existe))
            return existe;
        else
            return null;
    }
    public Tipo getTipoAninhado() {
        if (apontado == null)
            return this;

        Tipo tipo = apontado;
        while (tipo.apontado != null)
            tipo = tipo.getTipoAninhado();

        return tipo;
    }


    public Tipo validaTipo(Tipo tipo) {
        if (this.aNativo == Nativo.PONTEIRO && tipo.aNativo == Nativo.ENDERECO)
            return new Tipo(Nativo.PONTEIRO);
        else if ((this.aNativo == Nativo.REAL && (tipo.aNativo == Nativo.REAL || tipo.aNativo == Nativo.INTEIRO))
                || (this.aNativo == Nativo.INTEIRO && (tipo.aNativo == Nativo.REAL|| tipo.aNativo == Nativo.INTEIRO)))
            return new Tipo(Nativo.REAL);
        if (this.aNativo == Nativo.LITERAL && tipo.aNativo == Nativo.LITERAL)
            return new Tipo(Nativo.LITERAL);
        if (this.aNativo == Nativo.LOGICO && tipo.aNativo == Nativo.LOGICO)
            return new Tipo(Nativo.LOGICO);
        if (this.aNativo == Nativo.REGISTRO && tipo.aNativo == Nativo.REGISTRO)
            return new Tipo(Nativo.REGISTRO);
        return new Tipo(Nativo.INVALIDO);
    }

    public Tipo verificaEquivalenciaTipo(Tipo tipo) {

        if (this.aNativo == Nativo.ENDERECO && tipo.aNativo == Nativo.PONTEIRO)
            return new Tipo(Nativo.ENDERECO);
        if (this.aNativo == Nativo.REGISTRO && tipo.aNativo == Nativo.REGISTRO)
            return new Tipo(Nativo.REGISTRO);
        if (this.aNativo == Nativo.REAL && tipo.aNativo == Nativo.REAL)
            return validaTipo(tipo);
        return new Tipo(Nativo.INVALIDO);
    }

    public static void adicionaNovoTipo(String tipo) {
        Criados.add(tipo);
    }

    public String getFormat() {
        if (aNativo != null) {
            if(aNativo == Nativo.INTEIRO)
                return "int";
            if(aNativo == Nativo.REAL)
                return "float";
            if(aNativo == Nativo.LITERAL)
                return "char";
        }

        return criados;
    }

    public String getFormatSpec() {
        if (aNativo != null) {
            if(aNativo == Nativo.INTEIRO)
                return "%d";
            if(aNativo == Nativo.REAL)
                return "%f";
            if(aNativo == Nativo.LITERAL)
                return "%s";
        }
        return "";
    }
}
