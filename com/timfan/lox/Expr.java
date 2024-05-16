package com.timfan.lox;

import java.util.List;
abstract class Expr {
  /**
   * the client offers the customer the whole menu,
   * and asks the customer to choose what they want from that menu.
   *
   * for (Person customer : customers) {
   *   Choice choice = customer.choose(menu);
   * }
   *
   * and then each customer chooses what they want from the menu.
   *
   * Person Alice() {
   *   Choice choose(Menu menu) {
   *     // Alice chooses apple from the given menu.
   *      return menu.apple();
   *    }
   *  }
   *
   *  Person Bob() {
   *    Choice choose(Menu menu) {
   *      // Bob chooses banana from the given menu.
   *      return menu.banana();
   *    }
   *  }
   */
  interface Visitor<R> {
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitAssignExpr(Assign expr);
    R visitLogicExpr(Logic expr);
  }
  abstract <R> R accept(Visitor<R> visitor);
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
    final Expr left;
    final Token operator;
    final Expr right;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }
  }
  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }
    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }
  }
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }
    final Expr expression;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }
    final Object value;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }
    final Token operator;
    final Expr right;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }
  }
  static class Variable extends Expr {
    Variable(Token identifier) {
      this.identifier = identifier;
    }
    final Token identifier;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }
  }
  static class Assign extends Expr {
    Assign(Token identifier, Expr value) {
      this.identifier = identifier;
      this.value = value;
    }
    final Token identifier;
    final Expr value;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }
  }
  static class Logic extends Expr {
    Logic(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }
    final Expr left;
    final Token operator;
    final Expr right;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicExpr(this);
    }
  }
}
