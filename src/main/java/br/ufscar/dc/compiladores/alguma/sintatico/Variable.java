package br.ufscar.dc.compiladores.alguma.sintatico;
import java.util.*;

public class Variable {

    public String name;
    public Type type;
    public Procedure procedure = null;
    public Register register = null;
    public Pointer pointer = null;
    public Function function = null;

    public Variable(){
        this.name = "";
        this.type = null;
    }

    public Variable(String name, Type type) {
        this.name = name;
        this.type = type;

        if (nonNullType(type)){
            Check(type);
        }
    }

    public Type getNestedPointerType() {
        return pointer.getNestedType();
    }
    public Register getRegister() {
        return register;
    }

    public class Pointer {
        private final Type auxPointer;

        public Pointer(Type p) {
            this.auxPointer = p;
        }
        public Type getType() {
            return auxPointer.getType();
        }
        public Type getNestedType() {
            return auxPointer.getNestedType();
        }
    }

    public static boolean nonNullType(Type type) {
        return (type != null && type.natives != null);

    }

    public class Register {
        private final ArrayList<Variable> register = new ArrayList<>();

        public Variable getVar(String name) {
            for (Variable v : register)
                if (v.name.equals(name))
                    return v;

            return null;
        }



        public ArrayList<Variable> getAll() {
            return register;
        }

        public void addRegister(ArrayList<Variable> aux) {
            register.addAll(aux);
        }


    }

    public final void Check(Type type){
        switch(type.natives){
            case PONTEIRO:
                pointer = new Pointer(type.pointed);
                break;
            case REGISTRO:
                register = new Register();
                break;
            case PROCEDIMENTO:
                procedure = new Procedure();
                break;
            case FUNCAO:
                function = new Function();
                break;

        }

    }

    public class Procedure {
        private ArrayList<Variable> local;
        private ArrayList<Variable> params;

        public void setLocal(ArrayList<Variable> local) {
            this.local = local;
        }

        public void setParams(ArrayList<Variable> params) {
            this.params = params;
        }

        public ArrayList<Variable> getParams() {
            return params;
        }

        public ArrayList<Variable> getLocals() {
            return local;
        }
    }


    public class Function{
        private ArrayList<Variable> local;
        private ArrayList<Variable> params;
        private Type responseType;

        public void setResponseType(Type responseType) {
            this.responseType = responseType;
        }

        public void setLocal(ArrayList<Variable> local) {
            this.local = local;
        }

        public void setParams(ArrayList<Variable> params) {
            this.params = params;
        }

        public Type getResponseType() {
            return responseType;
        }

        public ArrayList<Variable> getParams() {
            return params;
        }

        public ArrayList<Variable> getLocal() {
            return local;
        }

        Iterable<AlgumaParser.Declaracao_localContext> getLocals() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    public Procedure getProcedure() {
        return procedure;
    }

    public Function getFunction() {
        return function;
    }
}
