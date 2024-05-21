package com.timfan.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreter is a client that wants to operate on both Stmt objects, 
 * and also the Expr objects within those Stmt objects.
 */
class Interpreter implements Stmt.Visitor<Void>, Expr.Visitor<Object> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>(); // Resolver letting us know how deep in the environment 
                                                             // chain to search to find the intended variable declaration 
                                                             // for each variable reference in the source code.
  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }
      @Override
      public String toString() { return "<native fn>"; }
    });
  }
  public void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }
  public void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    } 
  }
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    // return to the instruction that called the function we are in right now.
    throw new Return(evaluate(stmt.value));
  }
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    Expr condition = stmt.condition;
    Stmt body = stmt.body;
    while (isTruthy(evaluate(condition))) {
      execute(body);
    }
    return null;
  }
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    Object conditionResult = evaluate(stmt.condition);
    if (isTruthy(conditionResult)) {
      // if is true execute the then statement.
      execute(stmt.thenStmt);
    } else if (stmt.elseStmt != null) {
      // else execute the else statement, if there is one.
      execute(stmt.elseStmt);
    }
    return null;
  }
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // this function belongs to the current environment, 
    // its closure is this current environment.
    LoxFunction function = new LoxFunction(stmt, environment);
    // to interpret the given function declaration,
    // add it to the current namespace, so that it is ready in a visitCallExpr().
    environment.define(stmt.identifier.lexeme, function);
    return null;
  }
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    // this block will run in an environment that has this.environment as its parent.
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }
  public void executeBlock(List<Stmt> statements, Environment local) {
    /**
     * it's tempting to instead write:
     * 
     * Environment previous = local.parent;
     * 
     * instead of:
     * 
     * Environment previous = this.environment;
     * 
     * given that in visitBlockStmt, local.parent is set to be 
     * the current environment which is this.environment.
     * 
     * the issue is, is that when functions are called through LoxFunction.call():
     * 
     * (1) the functions should have their parent be the global environment, 
     *     so that we can allow for multiple instances of the same function running
     *     simultaneouly, and not have any name collisions between those instances, and;
     * 
     * (2) when we return from finishing calling a function,
     *     we want to return to the environment of the code that called the function,,, 
     *     which is not necessarily the global environment!!!
     * 
     * so we can't just blindly return to the function's parent environment, which will
     * always be the global environment! instead we should return to the environment 
     * that the code that called the function is in, in other words, saving the current
     * this.environment, and reverting back to it after finishing calling the function,
     * after finishing executing the block statement. 
     */
    Environment previous = this.environment;
    try {
      this.environment = local;
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      // finished executing block, revert environment back.
      this.environment = previous;
    }
  } 
  @Override
  public Void visitVarDeclarationStmt(Stmt.VarDeclaration stmt) {
    String name = stmt.identifier.lexeme;
    Object value = null;
    if (stmt.initialiser != null) {
      value = evaluate(stmt.initialiser);
    }
    environment.define(name, value);
    return null;
  }
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    // evaluate the expression within this statement.
    evaluate(stmt.expression);
    return null;
  }
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    // evaluate the expression within this statement, and print out the result.
    System.out.println(stringify(evaluate(stmt.expression)));
    return null;
  }
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }
    LoxCallable function = (LoxCallable)callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }
    return function.call(this, arguments);
  }
  @Override
  public Object visitLogicExpr(Expr.Logic expr) {
    // make sure evaluate either left or right only once.
    Object left = evaluate(expr.left);
    // first check if we can short circuit.
    if (expr.operator.type == TokenType.AND) {
      if (!isTruthy(left)) return Boolean.FALSE;
    } else /* if type is OR */ {
      if (isTruthy(left)) return Boolean.TRUE;
    }
    // we can't short circuit.
    return isTruthy(evaluate(expr.right));
  }
  private Environment ancestor(int depth) {
    // go depth levels into the environment chain, 
    // and return that depth'st level environment.
    Environment result = environment;
    for (int i = 0; i < depth; i++) {
      result = result.parent;
    }
    return result;
  }
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Token identifier = expr.identifier;
    Object value = evaluate(expr.value);
    Integer depth = locals.get(expr);
    if (depth == null) {
      // try the global environment.
      globals.assign(identifier, value);
    } else {
      // go to the specific local environment that the user intends.
      ancestor(depth).assign(identifier, value);
    }
    return value;
  }
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Token identifier = expr.identifier;
    Integer depth = locals.get(expr);
    if (depth == null) {
      // try the global environment.
      return globals.get(expr.identifier);
    } else {
      // go to the specific local environment that the user intends.
      return ancestor(depth).get(identifier);
    }
  }
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    Token operator = expr.operator;
    switch (operator.type) {
      case TokenType.PLUS:
        if ((left instanceof String) && (right instanceof String)) {
          return (String)left + (String)right;
        }
        if ((left instanceof Double) && (right instanceof Double)) {
          return (double)left + (double)right;
        }
        throw new RuntimeError(operator, "Can only add two numbers or two strings together");
      case TokenType.MINUS:
        checkNumberOperand(operator, left, right);
        return (double)left - (double)right;
      case TokenType.STAR:
        checkNumberOperand(operator, left, right);
        return (double)left * (double)right;
      case TokenType.SLASH:
        checkNumberOperand(operator, left, right);
        return (double)left / (double)right;
      case TokenType.LESS:
        checkNumberOperand(operator, left, right);
        return (double)left < (double)right;
      case TokenType.GREATER:
        checkNumberOperand(operator, left, right);
        return (double)left > (double)right;
      case TokenType.LESS_EQUAL:
        checkNumberOperand(operator, left, right);
        return (double)left <= (double)right;
      case TokenType.GREATER_EQUAL:
        checkNumberOperand(operator, left, right);
        return (double)left >= (double)right;
      case TokenType.EQUAL_EQUAL:
        checkNumberOperand(operator, left, right);
        return (double) left == (double)right;
      default:
        break;
    }
    // control should not reach here...
    return null;
  }
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object value = expr.right.accept(this);
    switch (expr.operator.type) {
      case TokenType.MINUS:
        // make sure value is a Double.
        checkNumberOperand(expr.operator, value);
        return Double.valueOf(-(double)value);
      case TokenType.BANG:
        // in Lox all objects are either truthy or falsey, so can negate value no matter what object it is.
        return Boolean.valueOf(!isTruthy(value));
      default:
        break;
    }
    // control should not reach here...
    return null;
  }
  /**
   * Used for validating operands before an arithmetic unary operation.
   * @throws RuntimeError When operand is not a Double.
   */
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    // although really it is a bad operand (e.g. a string when should be a number) that caused 
    // the error (rather than a bad operator), telling the user that the string "abc" caused the error 
    // doesn't really help the user find where the error happened in the source code,
    // since that operand could have been used with any number of operators in the source code.
    // so, specifying which operator it was that operated on the operand that caused the error 
    // is meant to help the user find where the error happened in the source code.
    throw new RuntimeError(operator, "Operand must be a number.");
  }
  /**
   * Used for validating operands before an arithmetic binary operation.
   * @throws RuntimeError When either operand is not a Double.
   */
  private void checkNumberOperand(Token operator, Object leftOperand, Object rightOperand) {
    if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) return;
    throw new RuntimeError(operator, "Both operands must be numbers.");
  }
  /**
   * In Lox all values -- in jLox represented as java.lang.Object objects -- have truthiness.
   * @return if value is truthy.
   */
  private boolean isTruthy(Object value) {
    if (value instanceof Boolean) {
      Boolean booleanValue = (Boolean) value;
      return booleanValue.booleanValue();
    }
    if (value == null) return false;
    return true;
  }
  /**
   * We don't want to show the user java's string representation of these java objects.
   * Instead, we show the user lox's string representation of lox's values.
   * @return Lox's string representation of the Lox value given.
   */
  String stringify(Object value) {
    if (value == null) return "nil";
    if (value instanceof Double) {
      String string = value.toString();
      if (string.endsWith(".0")) {
        // though this Lox value is being stored in a java.lang.Double,
        // the underlying value is really a Lox integer.
        // to not confuse the user, pretend as if the value was stored in a java.lang.Integer.
        return string.substring(0, string.length() - 2);
      }
    }
    return value.toString();
  }
  private void execute(Stmt stmt) {
    stmt.accept(this);
  }
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
}
