package Compiler;

import Compiler.syntax_tree.*;
import Compiler.tables.TableErrors;
import Compiler.tables.TableTokens;

import java.util.Vector;

public class CodeGenerator extends Parser {
    public Vector<String> asmCode;

    private Vector<String> parametersIdentifiers;
    private Vector<String> parametersAttributes;

    private int sizeSIGNAL = 4;     // As a link
    private int sizeCOMPLEX = 8;    // For two integers
    private int sizeINTEGER = 4;
    private int sizeFLOAT = 4;
    private int sizeBLOCKFLOAT = 8;
    private int sizeEXT = 8;
    private int parammeterOffset = 8;

    public CodeGenerator(Vector<String> tableReservedWords,
                         Vector<Character> tableOneSymbolTokens,
                         Vector<String> tableIdentifiers,
                         TableTokens tableTokens,
                         TableErrors tableErrors,
                         SyntaxTree syntaxTree) {
        super(tableReservedWords, tableOneSymbolTokens, tableIdentifiers, tableTokens, tableErrors);
        this.syntaxTree = syntaxTree;
        asmCode = new Vector<String>();
    }

    private void addError(int indexInTokensTable, String comment) {
        if (indexInTokensTable == -1)
            tableErrors.addError(-1, -1, comment);
        else
            tableErrors.addError(tableTokens.getTokenFileRow(indexInTokensTable),
                    tableTokens.getTokenFileColumn(indexInTokensTable) - 1, comment);
//        return false;
    }

    public void generate() {
        asmCode.add(".386\n");

        if (syntaxTree.programBranch) {
            if (!syntaxTree.block.expressionsList.empty) {
                addError(syntaxTree.identifier + 3, "Expressions in block are forbidden.");
                return;
            }

            asmCode.add(String.format("%-8s%s\n", "TITLE", getTokenByTokensTableIndex(syntaxTree.identifier)));
            asmCode.add(".CODE\n");
            asmCode.add(String.format("%-8s%s", "?BEGIN:", "NOP"));
            asmCode.add(String.format("%-8s%s", "END", "?BEGIN"));
            return;
        }

        asmCode.add(".CODE\n");
        asmCode.add(String.format("%-8s%s", getTokenByTokensTableIndex(syntaxTree.identifier), "PROC"));
        generateParametersList(syntaxTree.parametersList);
        asmCode.add(String.format("%-8s%s", getTokenByTokensTableIndex(syntaxTree.identifier), "ENDP"));
    }

    private void generateParametersList(ParametersList parametersList) {
        if (parametersList.empty || parametersList.declarationsList.empty)
            return;

        asmCode.add(String.format("\t\t%-8s", "; Save registers for getting parameters"));
        asmCode.add(String.format("\t\t%-8s%s", "PUSH", "EBP"));
        asmCode.add(String.format("\t\t%-8s%s", "MOV", "EBP, ESP"));
        asmCode.add(String.format("\t\t%-8s%s\n", "PUSH", "ESI"));

        asmCode.add(String.format("\t\t%-8s", "; Get parameters"));
        parametersIdentifiers = new Vector<String>();
        parametersAttributes = new Vector<String>();
        generateDeclarationsList(parametersList.declarationsList);

        asmCode.add(String.format("\n\t\t%-8s", "; Restore registers"));
        asmCode.add(String.format("\t\t%-8s%s", "POP", "ESI"));
        asmCode.add(String.format("\t\t%-8s%s", "POP", "EBP"));
        asmCode.add(String.format("\t\t%-8s", "RET"));
    }

    private void generateDeclarationsList(DeclarationsList declarationsList) {
        if (declarationsList.empty)
            return;

        generateDeclaration(declarationsList.declaration);
        generateDeclarationsList(declarationsList.declarationsList);
    }

    private void generateDeclaration(Declaration declaration) {
        generateIdentifiersAttributesLists(declaration.identifiersList, declaration.attributesList);
    }

    private void generateIdentifiersAttributesLists(IdentifiersList identifiersList, AttributesList attributesList) {
        if (identifiersList.empty && attributesList.empty)
            return;
        if (!identifiersList.empty && attributesList.empty) {
            addError(identifiersList.identifier, "Extra identifier");
            return;
        }
        if (identifiersList.empty) {
            addError(attributesList.attribute, "Extra attribute");
            return;
        }

        String identifier = getTokenByTokensTableIndex(identifiersList.identifier);
        String attribute = getTokenByTokensTableIndex(attributesList.attribute);
        if (parametersIdentifiers.contains(identifier)) {
            addError(identifiersList.identifier, String.format("Repeat of parameter: %s", identifier));
            return;
        }

        parametersIdentifiers.add(identifier);
        parametersAttributes.add(attribute);
        asmCode.add(String.format("\t\t%-8s%-8s%s", identifier, "EQU", String.format("[EBP + %d]", parammeterOffset)));
        parammeterOffset += parameterSize(identifier);
        generateIdentifiersAttributesLists(identifiersList.identifiersList, attributesList.attributesList);
    }

    // Returns parameter size by attribute (e.g. INTEGER - 4)
    private int parameterSize(String parameterAttribute) {
        if (parameterAttribute.equals("SIGNAL"))
            return sizeSIGNAL;
        if (parameterAttribute.equals("COMPLEX"))
            return sizeCOMPLEX;
        if (parameterAttribute.equals("INTEGER"))
            return sizeINTEGER;
        if (parameterAttribute.equals("FLOAT"))
            return sizeFLOAT;
        if (parameterAttribute.equals("BLOCKFLOAT"))
            return sizeBLOCKFLOAT;
        return sizeEXT;
    }
}
