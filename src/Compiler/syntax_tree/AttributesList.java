package Compiler.syntax_tree;

public class AttributesList {  // <attribute><AttributesList>|<empty>
    public int attribute;   // Index in reserved words table
    public AttributesList attributesList;
    public boolean empty;
}
