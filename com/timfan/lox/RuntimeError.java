package com.timfan.lox;

class RuntimeError extends RuntimeException {
  final Token token;
  RuntimeError(String message) {
    super(message);
    this.token = new Token(TokenType.NIL, message, null, 0);
  }
  RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }
}
