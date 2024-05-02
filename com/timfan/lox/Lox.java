package com.timfan.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  static boolean hadError = false;
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64); 
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError) System.exit(65);
  }
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) { 
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
  }
  private static void run(String source) {
    Scanner scanner = new Scanner(source);
    // scan through source code to generate tokens.
    List<Token> tokens = scanner.scanTokens();
    // parse through the tokens and generate a syntax tree.
    Parser parser = new Parser(tokens);
    // the root of the syntax tree.
    Expr expr = parser.parse();
    if (expr == null) return; // syntax error.
    System.out.println(new AstPrinter().print(expr));
  }
  /**
   * Used by Parser.
   */
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      // error at which part of the source code? the part of the source code that caused the error 
      // is stored as the token's lexeme, the token's source code representation
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }
  /**
   * Used by Scanner.
   */
  static void error(int line, String message) {
    report(line, "", message);
  }
  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
}