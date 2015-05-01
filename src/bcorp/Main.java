﻿package bcorp;

import bcorp.tables.TableErrors;
import bcorp.tables.TableTokens;

import java.io.IOException;
import java.io.PrintWriter;
//import java.util.Scanner;
import java.util.Vector;

public class Main {

    public static void main(String[] args) throws IOException {
        String file_name = "src/bcorp/text";
        FileChecker fileChecker = new FileChecker(file_name);
//        System.out.format("%-15s%s", "123", "45\n");

        if (fileChecker.check_for_utf_16()) {
            Scanner scanner = new Scanner("src/bcorp/text");
            if (scanner.scan())
                System.out.println("No lexical errors\n");

            Vector<String> reserved_words_table = scanner.tableReservedWords;
            for (int i = 0; i < reserved_words_table.size(); i++)
                System.out.println(i + " " + reserved_words_table.get(i));
            System.out.println();

            Vector<Character> one_symbol_tokens_table = scanner.tableOneSymbolTokens;
            for (int i = 0; i < one_symbol_tokens_table.size(); i++)
                System.out.println(i + " " + one_symbol_tokens_table.get(i));
            System.out.println();

            Vector<String> identifiers_table = scanner.tableIdentifiers;
            for (int i = 0; i < identifiers_table.size(); i++)
                System.out.println(i + " " + identifiers_table.get(i));
            System.out.println();

            TableTokens _tableTokens = scanner.tableTokens;
            for (int i = 0; i < scanner.tableTokens.size(); i++)
                System.out.format("%4d.%4s%4s%4s%4s\n", i, scanner.tableTokens.get_token_type(i), scanner.tableTokens.get_token_index_in_table(i), scanner.tableTokens.get_token_file_row(i), scanner.tableTokens.get_token_file_column(i));
//                System.out.println(scanner.tableTokens.get_token_type(i) + " " + scanner.tableTokens.get_token_index_in_table(i) + " " + scanner.tableTokens.get_token_file_row(i) + " " + scanner.tableTokens.get_token_file_column(i));
            System.out.println();

            TableErrors _tableErrors = scanner.tableErrors;

            Parser parser = new Parser(reserved_words_table, one_symbol_tokens_table, identifiers_table, _tableTokens, _tableErrors);
            if (!parser.parse()) {
                System.out.println("Syntax errors");
                for (int i = 0; i < scanner.tableErrors.size(); i++)
                    System.out.format("%4s%4s %s", scanner.tableErrors.getErrorRow(i), scanner.tableErrors.get_error_column(i), scanner.tableErrors.get_error_comment(i));
                return;
            }
            System.out.println("No syntax errors\n");
            parser.syntaxTree.print();
            System.out.println();

            CodeGenerator codeGenerator = new CodeGenerator(scanner.tableReservedWords, scanner.tableOneSymbolTokens, scanner.tableIdentifiers, scanner.tableTokens, parser.tableErrors, parser.syntaxTree);
            if (codeGenerator.generate()) {
//                for (int i = 0; i < codeGenerator.asmCode.size(); i++)
//                    System.out.println(codeGenerator.asmCode.get(i));

                PrintWriter asmFile = new PrintWriter("asm");
                for (int i = 0; i < codeGenerator.asmCode.size(); i++)
                    asmFile.println(codeGenerator.asmCode.get(i));
                asmFile.close();
            }
        } else
            System.out.println("Use UTF-16BE");
    }
}