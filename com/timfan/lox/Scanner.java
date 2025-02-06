package com.timfan.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
  private final String source; // the source code we are scanning through. 
                               // provided to us on Scanner object instantiation.
  private final List<Token> tokens = new ArrayList<>();

  private int start = 0; // first character of current lexeme.
  private int current = 0; // current character of current lexeme.
  private int line = 1; // which line our current lexeme is on.

  /**
   * when we have found that a lexeme is just 
   * a string of alphanumeric characters,
   * (so no opening double quotes!), otherwise known as an identifier:
   * 
   * check if that lexeme matches one of the keywords here.
   * if it does, then we should generate a special token
   * matching that keyword, e.g., the lexeme "and" 
   * should generate a token with type TokenType.AND.
   */
  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and",      TokenType.AND);
    keywords.put("break",    TokenType.BREAK);
    keywords.put("class",    TokenType.CLASS);
    keywords.put("continue", TokenType.CONTINUE);
    keywords.put("else",     TokenType.ELSE);
    keywords.put("false",    TokenType.FALSE);
    keywords.put("for",      TokenType.FOR);
    keywords.put("fun",      TokenType.FUN);
    keywords.put("if",       TokenType.IF);
    keywords.put("lambda",   TokenType.LAMBDA);
    keywords.put("nil",      TokenType.NIL);
    keywords.put("or",       TokenType.OR);
    keywords.put("print",    TokenType.PRINT);
    keywords.put("return",   TokenType.RETURN);
    keywords.put("super",    TokenType.SUPER);
    keywords.put("this",     TokenType.THIS);
    keywords.put("true",     TokenType.TRUE);
    keywords.put("var",      TokenType.VAR);
    keywords.put("while",    TokenType.WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  /**
   * Usable only by files in this package com.timfan.lox.
   * This function is used by com.timfan.lox.Lox.run() to scan through 
   * the source code give to us on Scanner object instantiation.
   */
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // we haven't reached the end of our source code file.
      // let's find the start of the next lexeme.
      // we maintain this loop where we have the loop invariant that:
      // at the start of each loop, we are at the start of the next lexeme.
      // in other words, current is the next character to be processed, 
      // so set start to be current, the start of the next lexeme.
      start = current;
      scanToken();
    }
    // finished scanning through all the source code. append end-of-file token to end of tokens.
    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }
  /**
   * Start and current is at the start of next lexeme. 
   * Figure out which lexeme this is, and generate token accordingly, 
   * (adding it to our list of tokens).
   */
  private void scanToken() {
    // figure out what character current is pointing to.
    char c = advance();
    switch (c) {
      case '[': addToken(TokenType.LEFT_BRACKET); break;
      case ']': addToken(TokenType.RIGHT_BRACKET); break;
      case '(': addToken(TokenType.LEFT_PAREN); break;
      case ')': addToken(TokenType.RIGHT_PAREN); break;
      case '{': addToken(TokenType.LEFT_BRACE); break;
      case '}': addToken(TokenType.RIGHT_BRACE); break;
      case ',': addToken(TokenType.COMMA); break;
      case '.': addToken(TokenType.DOT); break;
      case '-': addToken(TokenType.MINUS); break;
      case '+': addToken(TokenType.PLUS); break;
      case ':': addToken(TokenType.COLON); break;
      case ';': addToken(TokenType.SEMICOLON); break;
      case '*': addToken(TokenType.STAR); break; 
      case '!': addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG); break;
      case '=': addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL); break;
      case '<': addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS); break;
      case '>': addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER); break;
      case '/':
        if (match('/')) {
          // the rest of this line is a comment. keep skipping characters until newline.
          while (peek() != '\n' && !isAtEnd()) advance();
          // assert(peek() == '\n') // the next character consumed by scanToken() will be a newline.
          // OR assert(isAtEnd()) // we have reached the end of source code.
        } else {
          // is a normal slash, like for arithmetic division.
          addToken(TokenType.SLASH);
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        // skipping whitespace.
        break;
      case '\n':
        line++;
        break;
      case '"': 
        // is a string (though it could be an unterminated string). 
        // find out what string this is, and add to tokens.
        string();
        break;
      default:
        if (isDigit(c)) {
          // is a digit. find out what number this is, and add to tokens.
          number();
        } else if (isAlpha(c))  {
          // is a letter. could be an identifier like b, or a keyword like var.
          identifier();
        } else /* if is a invalid character */ {
          // set hadError to be true, and still continue scanning source.
          Lox.error(line, "Unexpected character");
        }
    }
  }
  private void identifier() {
    while (isAlphaNumeric(peek())) advance();
    // figure out if this string of alphanumeric characters (so only [a-zA-Z][a-zA-Z0-9]*),
    // is a keyword (like var), or is it just a normal identifer (like b).
    TokenType type = keywords.get(source.substring(start, current));
    if (type == null) type = TokenType.IDENTIFIER;
    addToken(type);
  }
  private boolean isAlphaNumeric(char c) {
    return (isDigit(c) || isAlpha(c));
  }
  private boolean isAlpha(char c) {
    return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_');
  }
  private boolean isDigit(char c) {
    return (c >= '0' && c <= '9');
  }
  /**
   * Find what number the source code wants us to store as data 
   * for use during interpretation, and add to our list of tokens.
   * 
   * Both 12345 and 12345.6789 are allowed (both will be stored as double float).
   * But weird things like 12345. are not allowed. 
   * In other words, the . should be treated as a member access of 12345, 
   * and not as a part of the number. So in this case, this function will not consume the .
   * 
   * @return On return, current will be at the character just after the last digit of this number.
   */
  private void number() {
    while (isDigit(peek())) advance();
    // assert(!isDigit(peek()));
    if (peek() == '.' && isDigit(peekNext())) {
      // since the next character is a ., and the character after that is a number, 
      // then the . is to mark a fraction (rather than some weird member access thing).
      // consume the . and read the rest of the fractional part of this number.
      advance();
      while (isDigit(peek())) advance();
      // current is now on the character (or EOF) just after the last digit.
    }
    // convert the string to its equivalent double, 
    // and then ALSO convert that double to a Double,
    // wrap the double in a Double wrapper, a Double object.
    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));;
  }
  /**
   * Find what string the source code wants us to store as data 
   * for use during interpretation, and add to our list of tokens.
   * 
   * @return On return, current will be at the character just after the second " character 
   * (which marks the end of this string that we want to add as a token).
   */
  private void string() {
    while (peek() != '"' && !isAtEnd()) advance();
    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }
    // assert(peek() == '"'); 
    advance();
    // create token with the string (excluding the opening and closing quotes) as a data literal.
    addToken(TokenType.STRING, source.substring(start + 1, current - 1));
  }
  /**
   * Used for lexemes that will not be interpreted as data.
   */
  private void addToken(TokenType type) {
    tokens.add(new Token(type, source.substring(start, current), null, line));
  }
  /**
   * Used for lexemes that will be interpreted as data.
   */
  private void addToken(TokenType type, Object literal) {
    tokens.add(new Token(type, source.substring(start, current), literal, line));
  }
  /**
   * @return The character after the next character. Does not advance current.
   */
  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }
  /**
   * @return The next character. Does not advance current.
   */
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }
  /**
   * Moves current forwards by one place.
   * @return The character in source we just moved forwards from, the "old" current character.
   */
  private char advance() {
    return source.charAt(current++);
  }
  /**
   * Checks if the next character in source code matches given character. 
   * If it does, will move current forwards. Otherwise, will leave current where it is.
   */
  private boolean match(char c) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != c) return false;
    current++;
    return true;
  }
  /**
   * @return True iff this scanner instance has reached the end of its source code file.
   */
  private boolean isAtEnd() {
    return current >= source.length();
  }
}
