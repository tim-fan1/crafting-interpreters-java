package com.timfan.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure; // which environment does this function declaration belong to?
                                     // what environment closes over this function declaration?
                                     // because, if this function declaration's body code relies on 
                                     // definitions contained within that environment, the environment 
                                     // that closes around this function -- so, if this function relies 
                                     // on definitions which are not contained within the body code 
                                     // itself -- it would need a reference to that environment in order 
                                     // to work properly, it would need a reference to that environment 
                                     // that closes over it to work properly, it would need a reference 
                                     // to its closure.
  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }
  @Override
  public int arity() {
    return declaration.params.size();
  }
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment local = new Environment(closure);
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
