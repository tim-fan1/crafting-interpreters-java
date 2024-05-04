package com.timfan.lox;

abstract class Stmt {
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
    R visitExpressionStmt(Expression stmt);
    R visitPrintStmt(Print stmt);
  }
  abstract <R> R accept(Visitor<R> visitor);
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }
    final Expr expression;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }
  }
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }
    final Expr expression;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }
  }
}
