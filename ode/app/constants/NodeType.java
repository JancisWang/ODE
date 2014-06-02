package constants;

public enum NodeType {
    USER, FEATURE, VALUE, RULE, AVM, RHS, COMBINATION_GROUP,
    SLOT, OUTPUT_STRING, PART;

    @Override
    public String toString() {
        String name = this.name();
        if (name.equals("AVM") || name.equals("RHS")) {
            return name;
        } else if (name.equals("COMBINATION_GROUP")) {
            return "CombinationGroup";
        } else if (name.equals("OUTPUT_STRING")) {
            return "OutputString";
        }
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
