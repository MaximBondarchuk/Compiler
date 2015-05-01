package bcorp;

import java.util.Vector;


import bcorp.syntax_tree.*;
import bcorp.tables.TableErrors;
import bcorp.tables.TableTokens;


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
    boolean add_error(String comment) {
        if (!was_error) {
            if (pos_in_tokens_table < tableTokens.Tokens.size())  // if unexpected Token
                tableErrors.add_error(tableTokens.get(pos_in_tokens_table).file_row,
                        tableTokens.get(pos_in_tokens_table).file_column,
                        comment);
            else {  // if no enough Tokens
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

    String get_token() {
        pos_in_tokens_table++;
        if (pos_in_tokens_table < tableTokens.Tokens.size()) {
            if (tableTokens.get(pos_in_tokens_table).type == 0)
                return tableReservedWords.get(tableTokens.get(pos_in_tokens_table).index_in_table);
            if (tableTokens.get(pos_in_tokens_table).type == 1)
                return tableOneSymbolTokens.get(tableTokens.get(pos_in_tokens_table).index_in_table).toString();
            return tableIdentifiers.get(tableTokens.get(pos_in_tokens_table).index_in_table);
        }
        return "";
    }

    int get_token_index_in_table() {
        if (pos_in_tokens_table < tableTokens.Tokens.size())
            return tableTokens.get(pos_in_tokens_table).index_in_table;
        return -1;
    }

    public boolean parse() {
        if (!tableErrors.errors_rows.isEmpty()) {
            System.out.println("First remove lexical errors");
            return false;
        }
        return program();
    }

    boolean program() {
        String _token = get_token();
        if (_token.equals("PROGRAM")) {    // PROGRAM <identifier> ; <block>.
            syntaxTree.programBranch = true;
            if (identifier()) {
                syntaxTree.identifier = get_token_index_in_table();
                if (get_token().equals(";")) {
                    syntaxTree.block = new Block();
                    return block(syntaxTree.block) && (get_token().equals(".") || add_error("'.' expected"));
                } else return add_error("';' expected");
            } else return add_error("Identifier expected");
        } else if (_token.equals("PROCEDURE")) {   // PROCEDURE <identifier><parameters-list> ; <block> ;
            syntaxTree.programBranch = false;
            if (identifier()) {
                syntaxTree.identifier = get_token_index_in_table();
                syntaxTree._parametersList = new ParametersList();
                if (parameters_list(syntaxTree._parametersList))
                    if (get_token().equals(";")) {
                        syntaxTree.block = new Block();
                        return (block(syntaxTree.block) && get_token().equals(";") || add_error("';' expected"));
                    } else return add_error("';' expected");
                else return false;
            } else return add_error("Identifier expected");
        }
        return add_error("'PROGRAM' or 'PROCEDURE' expected");
    }

    boolean block(Block block) {
//        block.empty = true;
        if (get_token().equals("BEGIN")) {
//            int rollbackIndex = pos_in_tokens_table;
            block.expressionsList = new ExpressionsList();
            if (expressionsList(block.expressionsList)) {
                block.empty = false;
                return get_token().equals("END") || add_error("'END' expected");
            } else return false;

//            if (get_token().equals("END")) {
//                block.empty = true;
//            } else {
//                block.empty = false;
//                return expressionsList(block.expressionsList);
//            }
//            return get_token().equals("END") || add_error("'END' expected");
        } else return add_error("'BEGIN' expected");
    }

    boolean expressionsList(ExpressionsList expressionsList) {
        if (identifier()) {
            pos_in_tokens_table--;
            expressionsList.expression = new Expression();
            //            _declarationsList._declaration = new Declaration();
            int save_pos_in_tokens_table = pos_in_tokens_table;

            if (expression(expressionsList.expression)) {
                expressionsList.empty = false;
                expressionsList.expressionsList = new ExpressionsList();
                return expressionsList(expressionsList.expressionsList);
            } else pos_in_tokens_table = save_pos_in_tokens_table;
//            if (Declaration(_declarationsList._declaration)) {
//                _declarationsList.empty = false;
//                _declarationsList._declarationsList = new DeclarationsList();
//                return DeclarationsList(_declarationsList._declarationsList);
//            } else pos_in_tokens_table = save_pos_in_tokens_table;
        }

        // Empty
        expressionsList.empty = true;
        pos_in_tokens_table--;
        return true;
    }

    boolean expression(Expression expression) {
        if (identifier()) {
            expression.leftOperand = get_token_index_in_table();
            if (get_token().equals("=")) {
                if (identifier()) {
                    expression.middleOperand = get_token_index_in_table();
                    if (get_token().equals("+")) {
                        if (identifier()) {
                            expression.rightOperand = get_token_index_in_table();
                            return get_token().equals(";") || add_error("';' expected");
                        } else return add_error("Identifier expected");
                    } else return add_error("'+' expected");
                } else return add_error("Identifier expected");
            } else return add_error("'=' expected");
        } else return add_error("Identifier expected");
    }

    boolean parameters_list(ParametersList _parametersList) {     // (<declarations-list>)|<empty>
        if (get_token().equals("(")) {  // ( <declarations-list> ) | <empty>
            _parametersList._declarationsList = new DeclarationsList();
            if (declarations_list(_parametersList._declarationsList)) {
                if (get_token().equals(")")) {
                    _parametersList.empty = false;
                    return true;
                } else return add_error("')' expected");
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
                if (get_token().equals(":")) {
                    if (attribute()) {
                        _declaration.attribute = get_token_index_in_table();
                        _declaration._attributesList = new AttributesList();
                        return attributes_list(_declaration._attributesList) && (get_token().equals(";") || add_error("';' expected"));
                    } else return add_error("Attribute expected");
                } else return add_error("':' expected");
            } else return false;
        } else return add_error("Identifier expected");
    }

    boolean identifiers_list(IdentifiersList _identifiersList) {
        if (get_token().equals(",")) {
            if (identifier()) {
                _identifiersList.identifier = get_token_index_in_table();
                _identifiersList.empty = false;
                _identifiersList._identifiersList = new IdentifiersList();
                return identifiers_list(_identifiersList._identifiersList);
            } else return add_error("Identifier expected");
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
        String _token = get_token();
        return _token.equals("SIGNAL") || _token.equals("COMPLEX") || _token.equals("INTEGER")
                || _token.equals("FLOAT") || _token.equals("BLOCKFLOAT") || _token.equals("EXT");
    }
}
