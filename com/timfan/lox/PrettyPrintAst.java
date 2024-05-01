package com.timfan.lox;

import com.timfan.lox.Expr.Binary;
import com.timfan.lox.Expr.Grouping;
import com.timfan.lox.Expr.Literal;
import com.timfan.lox.Expr.Unary;

public class PrettyPrintAst implements Expr.Visitor<String> {
  @Override
  public String visitBinaryExpr(Binary expr) {
    StringBuilder string = new StringBuilder();
    string.append("(");
    string.append(expr.operator.lexeme);
    string.append(" ");
    string.append(expr.left.accept(this));
    string.append(" ");
    string.append(expr.right.accept(this));
    string.append(")");
    return string.toString();
  }
  @Override
  public String visitGroupingExpr(Grouping expr) {
    StringBuilder string = new StringBuilder();
    string.append("[");
    string.append(expr.expression.accept(this));
    string.append("]");
    return string.toString();
  }
  @Override
  public String visitLiteralExpr(Literal expr) {
    return expr.value.toString();
  }
  @Override
  public String visitUnaryExpr(Unary expr) {
    StringBuilder string = new StringBuilder();
    string.append("{");
    string.append(expr.operator.lexeme);
    string.append(expr.right.accept(this));
    string.append("}");
    return string.toString();
  }
}
