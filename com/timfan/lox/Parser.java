package com.timfan.lox;

import java.util.List;

public class Parser {
  private static class ParseError extends RuntimeException {}
  private final List<Token> tokens;
  private int current = 0;
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
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
      if (match(TokenType.RIGHT_PAREN)) {
        return new Expr.Grouping(expr);
      } else {
        Lox.error(tokens.get(current), "Expect ) after expression!");
        throw new ParseError();
      }
    }
    Lox.error(tokens.get(current), "Expect expression.");
    throw new ParseError();
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
