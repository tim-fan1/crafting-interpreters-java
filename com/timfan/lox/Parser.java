package com.timfan.lox;

import java.util.List;
import java.util.ArrayList;

public class Parser {
  private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  List<Stmt> parse() {
    // a program is a sequence of statements (ending in an EOF token).
    // walk through tokens and keep forming statements, constructing a list of statement objects.
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }
  private boolean isAtEnd() {
    return tokens.get(current).type == TokenType.EOF;
  }
  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      throw error;
      // TODO: synchronise(); // handle ParseError gracefully.
      // return null;
    }
  }
  private Stmt varDeclaration() {
    Token identifier = consume(TokenType.IDENTIFIER, "Expect variable name.");
    Expr initialiser = null; 
    if (match(TokenType.EQUAL)) {
      initialiser = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ; after variable declaration.");
    return new Stmt.VarDeclaration(identifier, initialiser);
  }
  private Stmt statement() {
    if (match(TokenType.PRINT)) {
      Stmt s = new Stmt.Print(expression());
      consume(TokenType.SEMICOLON, "Expect ; after expression.");
      return s;
    } else /* if is expression statement. */ {
      Stmt s = new Stmt.Expression(expression());
      consume(TokenType.SEMICOLON, "Expect ; after expression.");
      return s;
    }
  }
  private Expr expression() {
    return equality();
  }
  /**
   * e.g. 2 == 3.
   * equality -> comparison ( ( "!=" | "==" ) comparison )*
   */
  private Expr equality() {
    Expr expr = comparison();
    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = tokens.get(current - 1); // operator is != or ==.
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
  /**
   * e.g. 2 >= 3.
   * comparison -> term ( ( ">=" | "<=" | ">" | "<" ) term )*
   */
  private Expr comparison() {
    Expr expr = term();
    while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
      Token operator = tokens.get(current - 1); // operator is >=, <=, >, or <.
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
  /**
   * e.g. 2 + 3.
   * term -> factor ( ( "+" | "-" ) factor )*
   */
  private Expr term() {
    Expr expr = factor();
    while (match(TokenType.PLUS, TokenType.MINUS)) {
      Token operator = tokens.get(current - 1); // operator is + or -.
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
  /**
   * e.g. 2 * 3.
   * factor -> unary ( ( "*" | "/" ) unary )*
   */
  private Expr factor() {
    Expr expr = unary();
    while (match(TokenType.STAR, TokenType.SLASH)) {
      Token operator = tokens.get(current - 1); // operator is * or /.
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
  /**
   * e.g. !2.
   * unary -> ( primary ) | ( ( "!" | "-" ) unary )
   */
  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = tokens.get(current - 1); // operator is ! or -.
      Expr right = unary();
      return new Expr.Unary(operator, right);
    } else {
      return primary();
    }
  }
  /**
   * e.g. 2, "2", (a + b * c - d).
   * primary -> ( NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" )
   */
  private Expr primary() {
    if (match(TokenType.TRUE)) return new Expr.Literal(Boolean.valueOf(true));
    if (match(TokenType.FALSE)) return new Expr.Literal(Boolean.valueOf(false));
    if (match(TokenType.NIL)) return new Expr.Literal(null);
    if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(tokens.get(current - 1).literal);
    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      // once control reaches here, we have to confirm that the next token is a right paren.
      consume(TokenType.RIGHT_PAREN, "Expect ) after expression.");
      return new Expr.Grouping(expr);
    }
    if (match(TokenType.IDENTIFIER)) {
      // let the interpreter know that here, 
      // the user wants access to the variable with 
      // the name stored in tokens.get(current - 1).lexeme.
      return new Expr.Variable(tokens.get(current - 1));
    }
    Lox.error(tokens.get(current), "Expect expression.");
    throw new ParseError();
  }
  /**
   * Consumes current token if matches the given type, and returns the consumed token.
   * If current token doesn't match the given type, then don't consume token, and throw error.
   * @param type
   * @param message The error message to show to user.
   * @throws ParseError If current token does not match given type. 
   * @return The consumed token.
   */
  private Token consume(TokenType type, String message) {
    if (tokens.get(current).type != type) {
      Lox.error(tokens.get(current), message);
      throw new ParseError();
    } else {
      return tokens.get(current++);
    }
  }
  /**
   * @param types Check if the current token matches any of the types in types.
   * @return If does match, then this method consumes current token and advances current.
   * If does not match, then this does not consume current token and does not advance current.
   */
  private boolean match(TokenType... types) {
    TokenType currType = tokens.get(current).type;
    for (TokenType type : types) {
      if (currType == type) {
        // current token matches. consume token and advance current.
        current++;
        return true;
      }
    }
    // current token doesn't match any of the given types. 
    // don't consume token and don't advance current.
    return false;
  }
}
