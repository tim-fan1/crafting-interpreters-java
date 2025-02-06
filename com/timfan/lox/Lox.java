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
  static boolean hadRuntimeError = false;
  static Interpreter interpreter = new Interpreter(); // for runPrompt, rather than make a new interpreter every loop 
                                                      // (as we would be doing if we make the interpreter in run),
                                                      // we instead make it on Lox bootup, which also allows for global variables 
                                                      // to be used in runPrompt (instead of all variables expiring 
                                                      // at the end of each loop).
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
    if (hadRuntimeError) System.exit(70);
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
    List<Token> tokens = new Scanner(source).scanTokens();
    List<Stmt> statements = new Parser(tokens).parse();
    if (hadError) return;
    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);
    if (hadError) return;
    // all syntax is good, no scanning errors or parsing errors reported. 
    interpreter.interpret(statements);
  }
  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
  /**
   * Used by Interpreter to report to Lox that there were RuntimeErrors.
   */
  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }
  /**
   * Used by Parser to report to Lox that there were ParseErrors.
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
   * Used by Scanner to report to Lox that there was an error during Scanning.
   */
  static void error(int line, String message) {
    report(line, "", message);
  }
}