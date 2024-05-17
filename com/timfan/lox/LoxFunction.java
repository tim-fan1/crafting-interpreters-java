package com.timfan.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  // private final Environment closure;
  LoxFunction(Stmt.Function declaration/*, Environment closure*/) {
    this.declaration = declaration;
    // this.closure = closure;
  }
  @Override
  public int arity() {
    return declaration.params.size();
  }
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment local = new Environment(interpreter.globals);
    for (int i = 0; i < declaration.params.size(); i++) {
      local.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body, local);
    } catch (Return returnException) {
      return returnException.value;
    }
    // function call finished without any explicit return statement.
    return null;
  }
  @Override
  public String toString() {
    return "<fn " + declaration.identifier.lexeme + ">";
  }
}
