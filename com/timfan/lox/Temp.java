package com.timfan.lox;

public class Temp {
  public static void main(String[] args) {
    Expr rootExpr = new Expr.Binary(
      new Expr.Binary(
        new Expr.Literal(6), 
        new Token(TokenType.MINUS, "-", null, 0), 
        new Expr.Grouping(
          new Expr.Binary(
            new Expr.Unary(
              new Token(TokenType.MINUS, "-", null, 0),
              new Expr.Literal(17)
            ),
            new Token(TokenType.PLUS, "+", null, 0), 
            new Expr.Literal(2)
          )
        )
      ),
      new Token(TokenType.PLUS, "+", null, 0), 
      new Expr.Literal(3)
    );
    PrettyPrintAst prettyPrintAstVisitor = new PrettyPrintAst();
    System.out.println(rootExpr.accept(prettyPrintAstVisitor));
  }
}
