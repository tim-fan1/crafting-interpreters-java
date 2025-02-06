package com.timfan.lox;

import java.util.List;
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
    R visitFunctionStmt(Function stmt);
    R visitPrintStmt(Print stmt);
    R visitVarDeclarationStmt(VarDeclaration stmt);
    R visitBlockStmt(Block stmt);
    R visitIfStmt(If stmt);
    R visitWhileStmt(While stmt);
    R visitReturnStmt(Return stmt);
    R visitBreakStmt(Break stmt);
    R visitContinueStmt(Continue stmt);
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
  static class Function extends Stmt {
    Function(Token identifier, List<Token> params, List<Stmt> body) {
      this.identifier = identifier;
      this.params = params;
      this.body = body;
    }
    final Token identifier;
    final List<Token> params;
    final List<Stmt> body;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
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
  static class VarDeclaration extends Stmt {
    VarDeclaration(Token identifier, Expr initialiser) {
      this.identifier = identifier;
      this.initialiser = initialiser;
    }
    final Token identifier;
    final Expr initialiser;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarDeclarationStmt(this);
    }
  }
  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }
    final List<Stmt> statements;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }
  }
  static class If extends Stmt {
    If(Expr condition, Stmt thenStmt, Stmt elseStmt) {
      this.condition = condition;
      this.thenStmt = thenStmt;
      this.elseStmt = elseStmt;
    }
    final Expr condition;
    final Stmt thenStmt;
    final Stmt elseStmt;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }
  }
  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }
    final Expr condition;
    final Stmt body;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }
  }
  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }
    final Token keyword;
    final Expr value;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }
  }
  static class Break extends Stmt {
    Break(Token keyword) {
      this.keyword = keyword;
    }
    final Token keyword;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }
  }
  static class Continue extends Stmt {
    Continue(Token keyword) {
      this.keyword = keyword;
    }
    final Token keyword;
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }
  }
}
