package com.timfan.lox;

import java.util.List;

public class Parser {
  private final List<Token> tokens;
  private int current = 0;
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  Expr parse() {
    return expression();
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
      // found a match. the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
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
      // found a match. the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
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
      // found a match. the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
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
      // found a match. the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
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
      // found a match. the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
      Token operator = tokens.get(current - 1); // operator is ! or -.
      Expr right = unary();
      return new Expr.Unary(operator, right);
    } else {
      // didn't find a match. so we assume that current token is start of a primary expression.
      return primary();
    }
  }
  /**
   * e.g. 2, "2", (a + b * c - d).
   * primary -> ( NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" )
   */
  private Expr primary() {

    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      // the matched token was consumed and current was advanced.
      // so the matched token we found is actually the previous token in tokens.
      return new Expr.Literal(tokens.get(current - 1).literal);
    }
    
    if (match(TokenType.LEFT_PAREN)) {
      // matched token was consumed and current was advanced.
      Expr expr = expression();
      //                           v control is here. found a right paren
      // ( [ ( [ ( [ a + b + c + d ] ) ] ) ]
      //                                      ^ when control reaches here and finds
      //                          no right paren, then we know that 
      // ^ this left paren has no matching right paren.                                     
      // once control reaches here, we have to confirm that the next token is a right paren. 
      if (match(TokenType.RIGHT_PAREN)) {
        // is a right parenthesis! matched token was consumed and current was advanced.
        return new Expr.Grouping(expr);
      } else {
        // TODO: detect syntax errors.
        Lox.error(-1, "Expect ) after expression!");
      }
    }
    // TODO: detect syntax errors.
    Lox.error(-1, "bad token");
    return null;
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
