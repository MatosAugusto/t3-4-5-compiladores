package br.ufscar.dc.compiladores.alguma.sintatico;
import java.util.*;

public class Type {
    enum Natives {INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        PONTEIRO,
        ENDERECO,
        REGISTRO,
        PROCEDIMENTO,
        FUNCAO,
        INVALIDO}

    public static ArrayList<String> createdList = new ArrayList<>();

    public Natives natives = null;
    public String created = null;
    public Type pointed = null;

    public Type(){
        natives = null;
        created = null;
    }
    public Type(Natives type) {
        natives = type;
    }


    public Type(String type) {
        created = type;
    }

    public Type(Type child) {
        natives = Natives.PONTEIRO;
        pointed = child;
    }

    public Type getType() {
        if (pointed != null)
            return pointed.getType();
        return this;
    }
    public boolean nullType(){
        return (this != null && this.natives != null);
    }


    public static String getType(String type) {
        String exists = createdList.stream()
                .filter(str -> str.trim().contains(type))
                .findAny()
                .orElse("");
        if(!"".equals(exists))
            return exists;
        else
            return null;
    }
    public Type getNestedType() {
        if (pointed == null)
            return this;

        Type type = pointed;
        while (type.pointed != null)
            type = type.getNestedType();

        return type;
    }


    public Type checkType(Type type) {
        if (this.natives == Natives.PONTEIRO && type.natives == Natives.ENDERECO)
            return new Type(Natives.PONTEIRO);
        else if ((this.natives == Natives.REAL && (type.natives == Natives.REAL || type.natives == Natives.INTEIRO))
                || (this.natives == Natives.INTEIRO && (type.natives == Natives.REAL|| type.natives == Natives.INTEIRO)))
            return new Type(Natives.REAL);
        if (this.natives == Natives.LITERAL && type.natives == Natives.LITERAL)
            return new Type(Natives.LITERAL);
        if (this.natives == Natives.LOGICO && type.natives == Natives.LOGICO)
            return new Type(Natives.LOGICO);
        if (this.natives == Natives.REGISTRO && type.natives == Natives.REGISTRO)
            return new Type(Natives.REGISTRO);
        return new Type(Natives.INVALIDO);
    }

    public Type checkEquivalentType(Type type) {

        if (this.natives == Natives.ENDERECO && type.natives == Natives.PONTEIRO)
            return new Type(Natives.ENDERECO);
        if (this.natives == Natives.REGISTRO && type.natives == Natives.REGISTRO)
            return new Type(Natives.REGISTRO);
        if (this.natives == Natives.REAL && type.natives == Natives.REAL)
            return checkType(type);
        return new Type(Natives.INVALIDO);
    }

    public static void addNewType(String type) {
        createdList.add(type);
    }

    public String getFormat() {
        if (natives != null) {
            if(natives == Natives.INTEIRO)
                return "int";
            if(natives == Natives.REAL)
                return "float";
            if(natives == Natives.LITERAL)
                return "char";
        }

        return created;
    }

    public String getFormatSpec() {
        if (natives != null) {
            if(natives == Natives.INTEIRO)
                return "%d";
            if(natives == Natives.REAL)
                return "%f";
            if(natives == Natives.LITERAL)
                return "%s";
        }
        return "";
    }
}
