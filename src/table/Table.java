package table;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import table.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Table implements SymbolTable {
    private List<String> imports;
    private String className;
    private String superClassName;
    private List<Symbol> fields;
    private List<Method> methods;
    private HashMap<String, Integer> arrayInitValue;

    public Table() {
        this.imports = new ArrayList<String>();
        this.fields = new ArrayList<Symbol>();
        this.methods = new ArrayList<Method>();
        this.arrayInitValue = new HashMap<>();
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public void addImport(String name) {
        this.imports.add(name);
    }

    public boolean addField(Symbol field) {
        if (!this.fields.contains(field))
            return this.fields.add(field);
        return false;
    }

    public boolean addMethod(Method method) {
        if (!this.methods.contains(method))
            return this.methods.add(method);
        return false;
    }

    public List<Method> getMethodsObjects() {
        return this.methods;
    }

    public List<String> getParametersString(String methodName) {
        List<String> result = new ArrayList<>();
        for (Symbol s: this.getParameters(methodName)) {
            if (s == null)
                result.add("|");
            else {
                if (s.getType().isArray())
                    result.add("Int Array");
                else
                    result.add(s.getType().getName());
            }
        }
        return result;
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return this.className;
    }

    @Override
    public String getSuper() {
        return this.superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        List<String> result = new ArrayList<>();
        for (Method method: this.methods)
            result.add(method.getName());
        return new ArrayList<>(new HashSet<>(result));
    }

    @Override
    public Type getReturnType(String methodName) {
        for (Method method: this.methods) {
            if (method.getName().equals(methodName))
                return method.getReturnType();
        }
        if (this.getSuper() != null)
            return new Type(this.getSuper(), false);
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        List<Symbol> res = new ArrayList<>();
        int cnt = 0;
        for (Method method: this.methods) {
            if (method.getName().equals(methodName)) {
                res.addAll(method.getParameters());
                res.add(null);
                cnt++;
            }
        }
        return res;
        //throw new RuntimeException("There is no method with that name!");
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        List<Symbol> res = new ArrayList<>();
        int cnt = 0;
        for (Method method: this.methods) {
            if (method.getName().equals(methodName)) {
                res.addAll(method.getLocalVariables());
                res.add(null);
                cnt++;
            }
        }
        return res;
        //throw new RuntimeException("There is no method with that name!");
    }

    @Override
    public String toString() {
        return "Table{" +
                "imports=" + imports +
                ", className='" + className + '\'' +
                ", superClassName='" + superClassName + '\'' +
                ", fields=" + fields +
                ", methods=" + methods +
                '}';
    }

    public void wasInitialized(String functionName, JmmNode node) {
        List<Symbol> symbols = this.getLocalVariables(functionName);
    }
}
