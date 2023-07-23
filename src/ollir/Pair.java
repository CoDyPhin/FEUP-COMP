package ollir;

public class Pair<JmmNode,String> {
    private final JmmNode key;
    private final String value;

    public Pair(JmmNode key, String value) {
        this.key = key;
        this.value = value;
    }

    public JmmNode getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}