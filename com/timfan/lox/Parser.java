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
    return peek().type == TokenType.EOF;
  }
  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) return varDeclaration();
      return statement();
    } catch (ParseError error) {
      // handle ParseError gracefully.
      synchronise();
      // it doesn't matter what we return, since we don't plan to 
      // execute the statements generated, (we made sure of this by 
      // reporting this parse error to the Lox instance).
      return null;
    }
  }
  /**
   * Used to handle ParseError gracefully. Instead of exiting the parser,
   * continue parsing from the next statement or declaration (marked by the next 
   * semicolon seen or the next start-of new declaration or statement seen).
   */
  private void synchronise() {
    advance();
    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) {
        // we have just moved past the next semicolon seen, continue parsing from here.
        return;
      }
      switch (peek().type) {
        // current is at the start of a new declaration or statement,
        case TokenType.CLASS:
        case TokenType.FUN:
        case TokenType.VAR:
        case TokenType.FOR:
        case TokenType.IF:
        case TokenType.WHILE:
        case TokenType.PRINT:
        case TokenType.RETURN:
          // continue parsing from here.
          return;
        default:
          break;
      }
      advance();
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
    return assignment();
  }
  private Expr assignment() {
    Expr expr = equality();
    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      if (expr instanceof Expr.Variable) {
        // the thing to the left hand side of the equals sign is an identifier.
        Token identifer = ((Expr.Variable)expr).identifier;
        return new Expr.Assign(identifer, value);
      } else {
        // we have managed to parse through the entire (invalid) assignment 
        // expression and now we are expecting the next token to be a semicolon, 
        // so there is no need to resynchronise no need to panic and throw a ParseError. 
        //
        // instead, we let the parser continue parsing from this point on to find 
        // if there are any more syntax errors, and just report to our Lox instance 
        // that there was a parse error so that it doesn't run the interpreter 
        // on the list of statements, some with invalid syntax, that we give to it. 
        //
        Lox.error(equals, "Invalid assignment target.");
        //
        // it doesn't really matter what we return here as it will not 
        // be ever used, the interpreter will not execute the statements 
        // we give it, because we reported to it that there was a parse error.
      }
    }
    return expr;
  }
  /**
   * e.g. 2 == 3.
   * equality -> comparison ( ( "!=" | "==" ) comparison )*
   */
  private Expr equality() {
    Expr expr = comparison();
    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous(); // operator is != or ==.
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
      Token operator = previous(); // operator is >=, <=, >, or <.
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
      Token operator = previous(); // operator is + or -.
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
      Token operator = previous(); // operator is * or /.
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
      Token operator = previous(); // operator is ! or -.
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
    if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      // once control reaches here, we have to confirm that the next token is a right paren.
      consume(TokenType.RIGHT_PAREN, "Expect ) after expression.");
      return new Expr.Grouping(expr);
    }
    if (match(TokenType.IDENTIFIER)) {
      // let the interpreter know that here, 
      // the user wants access to the variable with 
      // the name stored in previous().lexeme.
      return new Expr.Variable(previous());
    }
    Lox.error(peek(), "Expect expression.");
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
    if (peek().type == type) {
      return advance();
    }
    Lox.error(peek(), message);
    throw new ParseError();
  }
  /**
   * @param types Check if the current token matches any of the types in types.
   * @return If does match, then this method consumes current token and advances current.
   * If does not match, then this does not consume current token and does not advance current.
   */
  private boolean match(TokenType... types) {
    TokenType currType = peek().type;
    for (TokenType type : types) {
      if (currType == type) {
        // current token matches. consume token and advance current.
        advance();
        return true;
      }
    }
    // current token doesn't match any of the given types. 
    // don't consume token and don't advance current.
    return false;
  }
  /**
   * A wrapper around current++ that makes sure we don't advance past the end of tokens, 
   * that makes sure we don't advance past the EOF token.
   * @return The token that we just advanced past, which cannot be the EOF token 
   * since we cannot advance past the EOF token.
   */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
  /**
   * A wrapper around tokens.get(current - 1), try to abstract away operations involving current as much as possible.
   * @return Previous token.
   */
  private Token previous() {
    return tokens.get(current - 1);
  }
  /**
   * A wrapper around tokens.get(current), try to abstract away operations involving current as much as possible.
   * @return Current token.
   */
  private Token peek() {
    return tokens.get(current);
  }
}
