package Compiler;

import java.io.IOException;

import Compiler.tables.TableErrors;
import Compiler.tables.TableTokens;

import java.io.PrintWriter;
import java.util.Vector;

// I`m master
public class Main {

    public static void main(String[] args) throws IOException {
        FileChecker fileChecker = new FileChecker("src/Compiler/text");

        if (!fileChecker.check_for_utf_16())
            System.out.println("Use UTF-16BE");

        Scanner scanner = new Scanner("src/Compiler/text");
        if (!scanner.scan()) {
            for (int i = 0; i < scanner.tableErrors.size(); i++)
                System.out.format("%4s%4s %s", scanner.tableErrors.getErrorRow(i), scanner.tableErrors.get_error_column(i), scanner.tableErrors.get_error_comment(i));
            return;
        }

        for (int i = 0; i < scanner.tableReservedWords.size(); i++)
            System.out.format("%4d %s\n", i, scanner.tableReservedWords.get(i));
        System.out.println();

        for (int i = 0; i < scanner.tableOneSymbolTokens.size(); i++)
            System.out.format("%4d %s\n", i, scanner.tableOneSymbolTokens.get(i));
        System.out.println();

        for (int i = 0; i < scanner.tableIdentifiers.size(); i++)
            System.out.format("%4d %s\n", i, scanner.tableIdentifiers.get(i));
        System.out.println();

        for (int i = 0; i < scanner.tableTokens.size(); i++)
            System.out.format("%4d.%4s%4s%4s%4s\n", i, scanner.tableTokens.get_token_type(i), scanner.tableTokens.get_token_index_in_table(i), scanner.tableTokens.get_token_file_row(i), scanner.tableTokens.get_token_file_column(i));
        System.out.println();

        Parser parser = new Parser(scanner.tableReservedWords, scanner.tableOneSymbolTokens, scanner.tableIdentifiers, scanner.tableTokens, scanner.tableErrors);
        if (!parser.parse()) {
            for (int i = 0; i < scanner.tableErrors.size(); i++)
                System.out.format("%4s%4s %s", scanner.tableErrors.getErrorRow(i), scanner.tableErrors.get_error_column(i), scanner.tableErrors.get_error_comment(i));
            return;
        }

        parser.syntaxTree.print();
        System.out.println();

        CodeGenerator codeGenerator = new CodeGenerator(scanner.tableReservedWords, scanner.tableOneSymbolTokens, scanner.tableIdentifiers, scanner.tableTokens, parser.tableErrors, parser.syntaxTree);
        if (codeGenerator.generate()) {
            PrintWriter asmFile = new PrintWriter("asm");
            for (int i = 0; i < codeGenerator.asmCode.size(); i++)
                asmFile.println(codeGenerator.asmCode.get(i));
            asmFile.close();
        }


    }
}