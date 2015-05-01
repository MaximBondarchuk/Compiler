package Compiler;

import java.util.Vector;


import Compiler.syntax_tree.*;
import Compiler.tables.TableErrors;
import Compiler.tables.TableTokens;


public class Parser {
    private Vector<String> tableReservedWords;
    private Vector<Character> tableOneSymbolTokens;
    private Vector<String> tableIdentifiers;
    private TableTokens tableTokens;
    public TableErrors tableErrors;
    int pos_in_tokens_table;
    private boolean was_error;
    public SyntaxTree syntaxTree;

    public Parser(Vector<String> tableReservedWords,
                  Vector<Character> tableOneSymbolTokens,
                  Vector<String> tableIdentifiers,
                  TableTokens tableTokens,
                  TableErrors tableErrors) {
        this.tableReservedWords = tableReservedWords;
        this.tableOneSymbolTokens = tableOneSymbolTokens;
        this.tableIdentifiers = tableIdentifiers;
        this.tableTokens = tableTokens;
        this.tableErrors = tableErrors;
        pos_in_tokens_table = -1;
        syntaxTree = new SyntaxTree();
        was_error = false;
    }

    // Adds error with <comment> to errors table
    private boolean addError(String comment) {
        if (!was_error) {
            if (pos_in_tokens_table < tableTokens.Tokens.size())  // if unexpected Token
                tableErrors.add_error(tableTokens.get(pos_in_tokens_table).file_row,
                        tableTokens.get(pos_in_tokens_table).file_column,
                        comment);
            else {  // if we don't have enough Tokens
                int len = 0;
                if (tableTokens.get(pos_in_tokens_table - 1).type == 0)
                    len = tableReservedWords.get(tableTokens.get(pos_in_tokens_table - 1).index_in_table).length();
                else if (tableTokens.get(pos_in_tokens_table - 1).type == 1)
                    len = 1;
                else if (tableTokens.get(pos_in_tokens_table - 1).type == 2)
                    len = tableIdentifiers.get(tableTokens.get(pos_in_tokens_table - 1).index_in_table).length();
                tableErrors.add_error(tableTokens.get(pos_in_tokens_table - 1).file_row,
                        tableTokens.get(pos_in_tokens_table - 1).file_column + len,
                        comment);
            }
        }
        was_error = true;
        return false;
    }

    protected String getToken(int type, int index) {
        if (type == 0)
            return tableReservedWords.get(index);
        if (type == 1)
            return tableOneSymbolTokens.get(index).toString();
        return tableIdentifiers.get(index);
    }

    private String getTokenByTokensTableIndex(int index) {
        if (index < tableTokens.Tokens.size())
            return getToken(tableTokens.get(index).type, tableTokens.get(index).index_in_table);
        return "";
    }

    private String getTokenByPos() {
        return getTokenByTokensTableIndex(++pos_in_tokens_table);
    }

    int get_token_index_in_table() {
        if (pos_in_tokens_table < tableTokens.Tokens.size())
            return tableTokens.get(pos_in_tokens_table).index_in_table;
        return -1;
    }

    public boolean parse() {
        return tableErrors.errors_rows.isEmpty() && program();
    }

    boolean program() {
        String _token = getTokenByPos();
        if (_token.equals("PROGRAM")) {    // PROGRAM <identifier> ; <block>.
            syntaxTree.programBranch = true;
            if (identifier()) {
                syntaxTree.identifier = get_token_index_in_table();
                if (getTokenByPos().equals(";")) {
                    syntaxTree.block = new Block();
                    return block(syntaxTree.block) && (getTokenByPos().equals(".") || addError("'.' expected"));
                } else return addError("';' expected");
            } else return addError("Identifier expected");
        } else if (_token.equals("PROCEDURE")) {   // PROCEDURE <identifier><parameters-list> ; <block> ;
            syntaxTree.programBranch = false;
            if (identifier()) {
                syntaxTree.identifier = get_token_index_in_table();
                syntaxTree._parametersList = new ParametersList();
                if (parameters_list(syntaxTree._parametersList))
                    if (getTokenByPos().equals(";")) {
                        syntaxTree.block = new Block();
                        return (block(syntaxTree.block) && getTokenByPos().equals(";") || addError("';' expected"));
                    } else return addError("';' expected");
                else return false;
            } else return addError("Identifier expected");
        }
        return addError("'PROGRAM' or 'PROCEDURE' expected");
    }

    boolean block(Block block) {
        if (getTokenByPos().equals("BEGIN")) {
            block.expressionsList = new ExpressionsList();
            if (expressionsList(block.expressionsList)) {
                block.empty = false;
                return getTokenByPos().equals("END") || addError("'END' expected");
            } else return false;
        } else return addError("'BEGIN' expected");
    }

    boolean expressionsList(ExpressionsList expressionsList) {
        if (identifier()) {
            pos_in_tokens_table--;
            expressionsList.expression = new Expression();
            int save_pos_in_tokens_table = pos_in_tokens_table;

            if (expression(expressionsList.expression)) {
                expressionsList.empty = false;
                expressionsList.expressionsList = new ExpressionsList();
                return expressionsList(expressionsList.expressionsList);
            } else pos_in_tokens_table = save_pos_in_tokens_table;
        }

        // Empty
        expressionsList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean expression(Expression expression) {
        if (identifier()) {
            expression.leftOperand = get_token_index_in_table();
            if (getTokenByPos().equals("=")) {
                if (identifier()) {
                    expression.middleOperand = get_token_index_in_table();
                    if (getTokenByPos().equals("+")) {
                        if (identifier()) {
                            expression.rightOperand = get_token_index_in_table();
                            return getTokenByPos().equals(";") || addError("';' expected");
                        } else return addError("Identifier expected");
                    } else return addError("'+' expected");
                } else return addError("Identifier expected");
            } else return addError("'=' expected");
        } else return addError("Identifier expected");
    }

    boolean parameters_list(ParametersList _parametersList) {     // (<declarations-list>)|<empty>
        if (getTokenByPos().equals("(")) {  // ( <declarations-list> ) | <empty>
            _parametersList._declarationsList = new DeclarationsList();
            if (declarations_list(_parametersList._declarationsList)) {
                if (getTokenByPos().equals(")")) {
                    _parametersList.empty = false;
                    return true;
                } else return addError("')' expected");
            } else return false;
        }

        // Empty
        _parametersList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean declarations_list(DeclarationsList _declarationsList) {   // <Declaration><declarations-list>|<empty>
        if (identifier()) {
            pos_in_tokens_table--;
            _declarationsList._declaration = new Declaration();
            int save_pos_in_tokens_table = pos_in_tokens_table;
            if (declaration(_declarationsList._declaration)) {
                _declarationsList.empty = false;
                _declarationsList._declarationsList = new DeclarationsList();
                return declarations_list(_declarationsList._declarationsList);
            } else pos_in_tokens_table = save_pos_in_tokens_table;
        }

        // Empty
        _declarationsList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean identifier() {
        pos_in_tokens_table++;
        return pos_in_tokens_table < tableTokens.Tokens.size()
                && tableTokens.get(pos_in_tokens_table).type == 2;
    }

    boolean declaration(Declaration _declaration) {     // <identifier><IdentifiersList>:<attribute><AttributesList>;
        if (identifier()) {
            _declaration.identifier = get_token_index_in_table();
            _declaration._identifiersList = new IdentifiersList();
            if (identifiers_list(_declaration._identifiersList)) {
                if (getTokenByPos().equals(":")) {
                    if (attribute()) {
                        _declaration.attribute = get_token_index_in_table();
                        _declaration._attributesList = new AttributesList();
                        return attributes_list(_declaration._attributesList) && (getTokenByPos().equals(";") || addError("';' expected"));
                    } else return addError("Attribute expected");
                } else return addError("':' expected");
            } else return false;
        } else return addError("Identifier expected");
    }

    boolean identifiers_list(IdentifiersList _identifiersList) {
        if (getTokenByPos().equals(",")) {
            if (identifier()) {
                _identifiersList.identifier = get_token_index_in_table();
                _identifiersList.empty = false;
                _identifiersList._identifiersList = new IdentifiersList();
                return identifiers_list(_identifiersList._identifiersList);
            } else return addError("Identifier expected");
        }

        // Empty
        _identifiersList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean attributes_list(AttributesList _attributesList) {
        if (attribute()) {
            _attributesList.attribute = get_token_index_in_table();
            _attributesList.empty = false;
            _attributesList._attributesList = new AttributesList();
            return attributes_list(_attributesList._attributesList);
        }

        // Empty
        _attributesList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean attribute() {
        String _token = getTokenByPos();
        return _token.equals("SIGNAL") || _token.equals("COMPLEX") || _token.equals("INTEGER")
                || _token.equals("FLOAT") || _token.equals("BLOCKFLOAT") || _token.equals("EXT");
    }
}
