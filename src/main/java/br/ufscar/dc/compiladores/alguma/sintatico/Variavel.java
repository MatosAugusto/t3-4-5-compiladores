package br.ufscar.dc.compiladores.alguma.sintatico;

import java.util.ArrayList;

public class Variavel {

    public String varNome;
    public Tipo tipo;
    public Procedimento procedimento = null;
    public Registro registro = null;
    public Ponteiro ponteiro = null;
    public Funcao funcao = null;

    public Variavel(){
        this.varNome = "";
        this.tipo = null;
    }

    public Variavel(String nome, Tipo tipo) {
        this.varNome = nome;
        this.tipo = tipo;

        if (tipoNaoVazio(tipo)){
            Verifica(tipo);
        }
    }

     public Tipo getTipoPonteiroAninhado() {
        return ponteiro.getTipoAninhado();
    }
      public Registro getRegistro() {
        return registro;
    }

    public class Ponteiro {
        private final Tipo aponta;

        public Ponteiro (Tipo p) {
            this.aponta = p;
        }
        public Tipo getTipo() {
            return aponta.getTipo();
        }
        public Tipo getTipoAninhado() {
            return aponta.getTipoAninhado();
        }
    }

    public static boolean tipoNaoVazio(Tipo tipo) {
        return (tipo != null && tipo.aNativo != null);

    }

    public class Registro {
        private final ArrayList<Variavel> varRegistro = new ArrayList<>();

        public Variavel getVariavel (String nome) {
            for (Variavel v : varRegistro)
                if (v.varNome.equals(nome))
                    return v;

            return null;
        }



        public ArrayList<Variavel> getAll() {
            return varRegistro;
        }

        public void addRegistro(ArrayList<Variavel> aux) {
            varRegistro.addAll(aux);
        }


    }

    public final void Verifica(Tipo tipo){
         switch(tipo.aNativo){
                case PONTEIRO:
                    ponteiro = new Ponteiro(tipo.apontado);
                    break;
                case REGISTRO:
                    registro = new Registro();
                    break;
                case PROCEDIMENTO:
                    procedimento = new Procedimento();
                    break;
                case FUNCAO:
                    funcao = new Funcao();
                    break;

            }

    }

    public class Procedimento {
        private ArrayList<Variavel> local;
        private ArrayList<Variavel> parametros;

        public void setLocal(ArrayList<Variavel> local) {
            this.local = local;
        }

        public void setParametros(ArrayList<Variavel> parametros) {
            this.parametros = parametros;
        }

        public ArrayList<Variavel> getParametros() {
            return parametros;
        }

        public ArrayList<Variavel> getLocals() {
            return local;
        }
    }


    public class Funcao {
        private ArrayList<Variavel> local;
        private ArrayList<Variavel> parametros;
        private Tipo tipoRetorno;

        public void setTipoRetorno(Tipo tipoRetorno) {
            this.tipoRetorno = tipoRetorno;
        }

        public void setLocal(ArrayList<Variavel> local) {
            this.local = local;
        }

        public void setParametros(ArrayList<Variavel> parametros) {
            this.parametros = parametros;
        }

        public Tipo getTipoRetorno() {
            return tipoRetorno;
        }

        public ArrayList<Variavel> getParametros() {
            return parametros;
        }

        public ArrayList<Variavel> getLocal() {
            return local;
        }

        Iterable<AlgumaParser.Declaracao_localContext> getLocals() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    public void setRegistro(Registro registro) {
        this.registro = registro;
    }

    public Procedimento getProcedimento() {
        return procedimento;
    }

    public Funcao getFuncao() {
        return funcao;
    }
}
