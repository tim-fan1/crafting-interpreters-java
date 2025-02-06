package com.timfan.lox;

import java.util.ArrayList;
import java.util.Arrays;
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
    // clock();
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
    // str(object);
    globals.define("str", new LoxCallable() {
      @Override
      public int arity() { return 1; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return stringify(arguments.get(0));
      }
      @Override
      public String toString() { return "<native fn>"; }
    });
    // len(array);
    globals.define("len", new LoxCallable() {
      @Override
      public int arity() { return 1; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        // Parse array list.
        if (!(arguments.get(0) instanceof LoxArray)) {
          throw new RuntimeError("First argument to len must be an array.");
        }
        LoxArray array = (LoxArray)arguments.get(0);

        // Get its length.
        return (double)array.list.size();
      }
      @Override
      public String toString() { return "<native fn>"; }
    });
    // map(map, array);
    globals.define("map", new LoxCallable() {
      @Override
      public int arity() { return 2; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        // Parse map function.
        if (!(arguments.get(0) instanceof LoxFunction)) {
          throw new RuntimeError("First argument to map must be a function.");
        }
        LoxFunction map = (LoxFunction)arguments.get(0);

        // Map function must have exactly one parameter.
        if (map.arity() != 1) {
          throw new RuntimeError("Map function must take exactly one argument.");
        }

        // Parse array list.
        if (!(arguments.get(1) instanceof LoxArray)) {
          throw new RuntimeError("Second argument to map must be an array.");
        }
        LoxArray array = (LoxArray)arguments.get(1);

        // Do the map.
        List<Object> appliedList = new ArrayList<>();
        for (Object item : array.list) {
          appliedList.add(map.call(interpreter, Arrays.asList(item)));
        }
        return new LoxArray(appliedList);
      }
      @Override
      public String toString() { return "<native fn>"; }
    });
    // filter(filter, array);
    globals.define("filter", new LoxCallable() {
      @Override
      public int arity() { return 2; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        // Parse filter function.
        if (!(arguments.get(0) instanceof LoxFunction)) {
          throw new RuntimeError("First argument to filter must be a function.");
        }
        LoxFunction filter = (LoxFunction)arguments.get(0);

        // Filter function must have exactly one parameter.
        if (filter.arity() != 1) {
          throw new RuntimeError("Filter function must take exactly one argument.");
        }

        // Parse array list.
        if (!(arguments.get(1) instanceof LoxArray)) {
          throw new RuntimeError("Second argument to filter must be an array.");
        }
        LoxArray array = (LoxArray)arguments.get(1);

        // Do the filter.
        List<Object> filteredList = new ArrayList<>();
        for (Object item : array.list) {
          if (isTruthy(filter.call(interpreter, Arrays.asList(item)))) {
            filteredList.add(item);
          }
        }
        return new LoxArray(filteredList);
      }
      @Override
      public String toString() { return "<native fn>"; }
    });
    // reduce(reduce, array);
    globals.define("reduce", new LoxCallable() {
      @Override
      public int arity() { return 2; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        // Parse reduce function.
        if (!(arguments.get(0) instanceof LoxFunction)) {
          throw new RuntimeError("First argument to reduce must be a function.");
        }
        LoxFunction reduce = (LoxFunction)arguments.get(0);

        // Reducer function must have exactly two parameters.
        if (reduce.arity() != 2) {
          throw new RuntimeError("Reducer function must take exactly two arguments.");
        }

        // Parse array list.
        if (!(arguments.get(1) instanceof LoxArray)) {
          throw new RuntimeError("Second argument to reduce must be an array.");
        }
        LoxArray array = (LoxArray)arguments.get(1);
        List<Object> list = array.list;

        // Do the reduce.
        if (list.size() == 0) {
          return null;
        }
        if (list.size() == 1) {
          return list.get(0);
        }
        Object result = list.get(0);
        for (int i = 1; i < list.size(); i++) {
          result = reduce.call(interpreter, Arrays.asList(result, list.get(i)));
        }
        return result;
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
    // See LoxFunction.java/call.
    throw new Return(evaluate(stmt.value));
  }
  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new Break();
  }
  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    throw new Continue();
  }
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    Expr condition = stmt.condition;
    Stmt body = stmt.body;
    while (isTruthy(evaluate(condition))) {
      try {
        execute(body);
      } catch (Break breakException) {
        break;
      } catch (Continue continueException) {
        continue;
      }
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
     * the issue is that this same executeBlock is also used by LoxFunction.call. and 
     * when it is invoked by LoxFunction.call, the local environment that is passed in 
     * is a local environment that has LoxFunction.closure as its parent. and so in the 
     * specific case where executeBlock is being invoked by LoxFunction.call: 
     * 
     * if we were to return to local.parent, we will be returning to the LoxFunction's 
     * closure - the environment in which the LoxFunction was *declared* in - when what 
     * we want to return to is the local block's environment - the environment in which 
     * the LoxFunction was *invoked* in.
     *
     * to allow for this executeBlock() to be used by both visitBlockStmt and ALSO 
     * LoxFunction.call, we have executeBlock() remember the current environment in a 
     * local variable stored on its stack, and revert to the saved current environment 
     * after returning from finishing execute the block's code, whether that block is a 
     * normal block like in an if statement, or a function's body code executed as a block.
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
  public Object visitLambdaExpr(Expr.Lambda expr) {
    return new LoxFunction((Stmt.Function)expr.function, environment);
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
  public Object visitSubscriptAssignExpr(Expr.SubscriptAssign expr) {
    // parse subscriptee.
    Object subscripteeObject = evaluate(expr.subscriptee);
    if (!(subscripteeObject instanceof LoxArray) && !(subscripteeObject instanceof LoxDictionary)) {
      throw new RuntimeError(expr.bracket, "Can only use subscript operator [] on arrays or dictionaries.");
    }
    if (subscripteeObject instanceof LoxArray) {
      List<Object> list = ((LoxArray)subscripteeObject).list;

      // parse index.
      Object indexObject = evaluate(expr.index);
      if (!(indexObject instanceof Double)) {
        throw new RuntimeError(expr.bracket, "Can only use subscript operator [] with integers.");
      }
      Double index = (Double)indexObject;
      if (Math.floor(index) != index) {
        throw new RuntimeError(expr.bracket, "Can only use subscript operator [] with integers.");
      }
      if (index.intValue() < 0 || index.intValue() >= list.size()) {
        throw new RuntimeError(expr.bracket, "Array index out of bounds.");
      }

      // parse value.
      Object value = evaluate(expr.value);

      // assign array at index to value.
      list.set(index.intValue(), value);
      return value;
    } else if (subscripteeObject instanceof LoxDictionary) {
      Map<Object, Object> dictionary = ((LoxDictionary)subscripteeObject).dictionary;

      // parse index.
      Object index = evaluate(expr.index);

      // parse value.
      Object value = evaluate(expr.value);

      // insert into (or update) dictionary.
      dictionary.put(index, value);
      return value;
    }
    // Control should not reach here...
    throw new RuntimeError(expr.bracket, "Control should not reach here... Check if the thing that used [] on was an array or dictionary.");
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
        if ((left instanceof LoxArray) && (right instanceof LoxArray)) {
          List<Object> list = new ArrayList<>();
          list.addAll(((LoxArray)left).list);
          list.addAll(((LoxArray)right).list);
          return new LoxArray(list);
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
      case TokenType.BANG_EQUAL:
        checkNumberOperand(operator, left, right);
        return (double) left != (double)right;
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
  @Override
  public Object visitArrayExpr(Expr.Array expr) {
    List<Object> list = new ArrayList<>();
    for (Expr value : expr.values) {
      list.add(evaluate(value));
    }
    return new LoxArray(list);
  }

  @Override
  public Object visitDictionaryExpr(Expr.Dictionary expr) {
    Map<Object, Object> dictionary = new HashMap<>();
    for (int i = 0; i < expr.dictionary.size() / 2; i++) {
      Object key = evaluate(expr.dictionary.get(2 * i));
      Object value = evaluate(expr.dictionary.get((2 * i) + 1));
      dictionary.put(key, value);
    }
    return new LoxDictionary(dictionary);
  }

  @Override
  public Object visitSubscriptExpr(Expr.Subscript expr) {
    // parse subscriptee.
    Object subscripteeObject = evaluate(expr.subscriptee);
    if (!(subscripteeObject instanceof LoxArray) && !(subscripteeObject instanceof LoxDictionary)) {
      throw new RuntimeError(expr.bracket, "Can only use subscript operator [] on arrays or dictionaries.");
    }
    if (subscripteeObject instanceof LoxArray) {
      List<Object> list = ((LoxArray)subscripteeObject).list;

      // parse index.
      Object indexObject = evaluate(expr.index);
      if (!(indexObject instanceof Double)) {
        throw new RuntimeError(expr.bracket, "Can only use subscript operator [] with integers.");
      }
      Double index = (Double)indexObject;
      if (Math.floor(index) != index) {
        throw new RuntimeError(expr.bracket, "Can only use subscript operator [] with integers.");
      }
      if (index.intValue() < 0 || index.intValue() >= list.size()) {
        throw new RuntimeError(expr.bracket, "Array index out of bounds.");
      }
      return list.get(index.intValue());
    } else if (subscripteeObject instanceof LoxDictionary) {
      Map<Object, Object> dictionary = ((LoxDictionary)subscripteeObject).dictionary;

      // parse index.
      Object index = evaluate(expr.index);
      if (!dictionary.containsKey(index)) {
        throw new RuntimeError(expr.bracket, "Dictionary does not contain given key.");
      }
      return dictionary.get(index);
    }

    // Control should not reach here...
    throw new RuntimeError(expr.bracket, "Control should not reach here... Check if the thing that used [] on was an array or dictionary.");
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
