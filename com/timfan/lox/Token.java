package com.timfan.lox;

class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line; 

  /**
   * 
   * @param type
   * @param lexeme the lexeme as string, e.g. "1234".
   * @param literal the lexeme as data, e.g. 1234.
   * @param line
   */
  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
