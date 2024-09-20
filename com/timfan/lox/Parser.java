package com.timfan.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
  private Stmt declaration() {
    try {
      if (match(TokenType.VAR)) return varDeclaration();
      else if (match(TokenType.FUN)) return funDeclaration("function");
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
        case TokenType.LAMBDA:
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
  /**
   * @param kind the kind of function, (function | class).
   */
  private Stmt funDeclaration(String kind) {
    Token identifier = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
    consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (peek().type != TokenType.RIGHT_PAREN) {
      do {
        if (parameters.size() >= 255) {
          Lox.error(peek(), "Can't have more than 255 parameters");
          throw new ParseError();
        }
        parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
      } while (match(TokenType.COMMA));
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
    consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(identifier, parameters, body);
  }
  /**
   * Used to handle ParseError gracefully. Instead of exiting the parser,
   * continue parsing from the next statement or declaration (marked by the next 
   * semicolon seen or the next start-of new declaration or statement seen).
   */
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
      return printStatement();
    } else if (match(TokenType.IF)) {
      return ifStatement();
    } else if (match(TokenType.RETURN)) {
      return returnStatement();
    } else if (match(TokenType.BREAK)) {
      return breakStatement();
    } else if (match(TokenType.CONTINUE)) {
      return continueStatement();
    } else if (match(TokenType.FOR)) {
      return forStatement();
    } else if (match(TokenType.WHILE)) {
      return whileStatement();
    } else if (match(TokenType.LEFT_BRACE)) {
      return new Stmt.Block(block());
    } else /* if is expression statement. */ {
      return expressionStatement();
    }
  }
  private Stmt breakStatement() {
    Token keyword = previous(); // is the break token.
    consume(TokenType.SEMICOLON, "Expect ; after break statement.");
    return new Stmt.Break(keyword);
  }
  private Stmt continueStatement() {
    Token keyword = previous(); // is the continue token.
    consume(TokenType.SEMICOLON, "Expect ; after continue statement.");
    return new Stmt.Continue(keyword);
  }
  private Stmt returnStatement() {
    Token keyword = previous(); // is the return token.
    Expr value = null;
    if (peek().type != TokenType.SEMICOLON) {
      value = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ; after return value.");
    return new Stmt.Return(keyword, value);
  }
  private Stmt forStatement() {
    consume(TokenType.LEFT_PAREN, "Expect ( after for keyword.");

    // creating the parts of a for loop statement.

    // - initialiser. for (var i = 0; ), for (i = 0; )
    Stmt initialiser;
    if (match(TokenType.SEMICOLON)) {
      initialiser = null;
    } else if (match(TokenType.VAR)) {
      initialiser = varDeclaration();
    } else {
      initialiser = expressionStatement();
    }

    // - condition. for (var i = 0; i < 5; ), for (;;)
    Expr condition;
    if (match(TokenType.SEMICOLON)) {
      condition = new Expr.Literal(true);
    } else {
      condition = expression();
      consume(TokenType.SEMICOLON, "Expect ; after condition in for loop.");
    }

    // - step. for (var i = 0; i < 5; i = i + 1)
    Expr step;
    if (match(TokenType.RIGHT_PAREN)) {
      step = null;
    } else {
      step = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ) at end of for loop step.");
    }

    // - body. for (var i = 0; i < 5; i = i + 1) print i;
    Stmt body = statement();

    // finished making the parts of the for loop.
    // now using a Stmt.Block to group together these parts of a for loop.
    List<Stmt> forStatements = new ArrayList<>();
    if (initialiser != null) {
      // run the initiliaser first.
      forStatements.add(initialiser);
    }
    forStatements.add(
      // and then run a while loop on the condition given and body given,
      new Stmt.While(
        condition, 
        // adding in the step to the end of the body given, if there is a step.
        (step == null) ? body : new Stmt.Block(Arrays.asList(body, new Stmt.Expression(step)))
      )
    );
    return new Stmt.Block(forStatements);
  }
  private Stmt whileStatement() {
    consume(TokenType.LEFT_PAREN, "Expect ( after if keyword.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ) after expression.");
    Stmt body = statement();
    return new Stmt.While(condition, body);
  }
  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect ( after if keyword.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ) after expression.");
    Stmt thenStmt = statement();
    Stmt elseStmt = null;
    if (match(TokenType.ELSE)) {
      elseStmt = statement();
    }
    return new Stmt.If(condition, thenStmt, elseStmt);
  }
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();
    while (peek().type != TokenType.RIGHT_BRACE && !isAtEnd()) {
      statements.add(declaration());
    }
    consume(TokenType.RIGHT_BRACE, "Expect } at end of block.");
    return statements;
  }
  private Stmt printStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ; after expression.");
    return new Stmt.Print(expr);
  }
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ; after expression.");
    return new Stmt.Expression(expr);
  }
  private Expr expression() {
    return assignment();
  }
  /**
   * assignment -> IDENTIFIER "=" assignment | logicOr
   */
  private Expr assignment() {
    Expr expr = logicOr();
    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();
      if (expr instanceof Expr.Variable) {
        // the thing to the left hand side of the equals sign is an identifier.
        Token identifer = ((Expr.Variable)expr).identifier;
        return new Expr.Assign(identifer, value);
      } else if (expr instanceof Expr.Subscript) {
        Expr subscriptee = ((Expr.Subscript)expr).subscriptee;
        Expr index = ((Expr.Subscript)expr).index;
        Token bracket = ((Expr.Subscript)expr).bracket;
        return new Expr.SubscriptAssign(subscriptee, bracket, index, value);
      } else /* if the thing to the left hand side is not an identifier. */ {
        // we have managed to parse through the entire (invalid) assignment 
        // expression and now we are expecting the next token to be a semicolon, 
        // so there is no need to resynchronise no need to panic and throw a ParseError. 
        //
        // instead, we let the parser continue parsing from this point on to find 
        // if there are any more syntax errors, and just report to our Lox instance that there 
        // was a parse error so that it doesn't run the interpreter on this erroneous code.
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
   * logicOr -> logicAnd ( "or" logicAnd )*
   */
  private Expr logicOr() {
    Expr expr = logicAnd();
    while (match(TokenType.OR)) {
      Token operator = previous(); // the OR token.
      Expr right = logicAnd();
      return new Expr.Logic(expr, operator, right);
    } 
    return expr;
  }
  private Expr logicAnd() {
    Expr expr = equality();
    while (match(TokenType.AND)) {
      Token operator = previous(); // the AND token.
      Expr right = equality();
      return new Expr.Logic(expr, operator, right);
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
      return call();
    }
  }
  /**
   * e.g. fib(n).
   * call -> primary ( "[" expression "]" ) | "(" arguments? ")" )
   * arguments -> expression ( "," expression )*
   */
  private Expr call() {
    // Get the expression that we will be (call)-ing  or [subscript]-ing.
    Expr expr = primary();
    while (true) {
      // And keep calling or subscripting while we can.
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(TokenType.LEFT_BRACKET)) {
        expr = finishSubscript(expr);
      } else {
        break;
      }
    }
    return expr;
  }
  private Expr finishSubscript(Expr array) {
    Expr index = expression();
    Token bracket = consume(TokenType.RIGHT_BRACKET, "Expect ] after array indexing.");
    return new Expr.Subscript(array, bracket, index);
  }
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (peek().type != TokenType.RIGHT_PAREN) {
      // there is at least one argument.
      do {
        // add the first, and then keep adding more while next token is comma.
        if (arguments.size() >= 255) {
          Lox.error(peek(), "Can't have more than 255 arguments.");
          throw new ParseError();
        }
        arguments.add(expression());
      } while (match(TokenType.COMMA));
    }
    Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
    return new Expr.Call(callee, paren, arguments);
  }
  /**
   * e.g. 2, "2", (a + b * c - d), [1, 2, 3, 4].
   * primary -> ( NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | "[" expressions? "]" )
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
    if (match(TokenType.LEFT_BRACKET)) {
      List<Expr> values = new ArrayList<>();
      if (peek().type != TokenType.RIGHT_BRACKET) {
        values.add(expression());
        while (match(TokenType.COMMA)) {
          values.add(expression());
        }
      }
      consume(TokenType.RIGHT_BRACKET, "Expect ] to close array declaration.");
      return new Expr.Array(values);
    }
    if (match(TokenType.LEFT_BRACE)) {
      List<Expr> dictionary = new ArrayList<>();
      if (peek().type != TokenType.RIGHT_BRACE) {
        // "key" : value
        Expr key = expression();
        consume(TokenType.COLON, "Expect : after key in dictionary.");
        Expr value = expression();
        dictionary.add(key);
        dictionary.add(value);
        // while (, "key2" : value2)
        while (match(TokenType.COMMA)) {
          key = expression();
          consume(TokenType.COLON, "Expect : after key in dictionary.");
          value = expression();
          dictionary.add(key);
          dictionary.add(value);
        }
      }
      consume(TokenType.RIGHT_BRACE, "Expect } to close dictionary declaration.");
      return new Expr.Dictionary(dictionary);
    }
    if (match(TokenType.IDENTIFIER)) {
      // let the interpreter know that here, 
      // the user wants access to the variable with 
      // the name stored in previous().lexeme.
      return new Expr.Variable(previous());
    }
    if (match(TokenType.LAMBDA)) {
      // Get lambda keyword token.
      Token token = previous();

      // Get parameters.
      List<Token> parameters = new ArrayList<>();
      consume(TokenType.LEFT_PAREN, "Expect '(' after lambda keyword.");
      if (peek().type != TokenType.RIGHT_PAREN) {
        do {
          if (parameters.size() >= 255) {
            Lox.error(peek(), "Can't have more than 255 parameters");
            throw new ParseError();
          }
          parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
        } while (match(TokenType.COMMA));
      }
      consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

      // Lambda declarations will use arrow function notation.
      consume(TokenType.EQUAL, "Expect => arrow after lambda parameters.");
      consume(TokenType.GREATER, "Expect => arrow after lambda parameters.");

      // Get body.
      consume(TokenType.LEFT_BRACE, "Expect '{' before lambda body.");
      List<Stmt> body = block();

      // Return lambda expression.
      return new Expr.Lambda(new Stmt.Function(token, parameters, body));
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
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }
}
