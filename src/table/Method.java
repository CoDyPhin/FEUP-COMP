package table;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.Objects;

public class Method {
    private String name;
    private Type returnType;
    private List<Symbol> parameters;
    private List<Symbol> localVariables;

    public Method(String name, Type returnType, List<Symbol> parameters, List<Symbol> localVariables) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
    }

    public String getName() {
        return this.name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    @Override
    public String toString() {
        return "Method{" +
                "returnType=" + returnType +
                ", parameters=" + parameters +
                ", localVariables=" + localVariables +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;

        boolean sameParameters = false;
        if (this.parameters.size() == ((Method) o).getParameters().size()) {
            for (int x = 0; x < this.parameters.size(); x++) {
                if (this.parameters.get(x).getType().equals(((Method) o).getParameters().get(x).getType())) {
                    sameParameters = true;
                    break;
                }
            }
        }

        return Objects.equals(name, method.name) && Objects.equals(returnType, method.returnType) && sameParameters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, parameters, localVariables);
    }
}
