package com.timfan.lox;

class Interpreter implements Expr.Visitor<Object> {

  public void interpret(Expr expr) {
    try {
      Object value = expr.accept(this);
      System.out.println(value.toString());
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  // TODO:
  String stringify(Object value) {
    return value.toString();
  }
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = expr.left.accept(this);
    Object right = expr.right.accept(this);
    Token operator = expr.operator;
    switch (operator.type) {
      case TokenType.PLUS:
        if ((left instanceof String) && (right instanceof String)) {
          return (String)left + (String)right;
        }
        if ((left instanceof Double) && (right instanceof Double)) {
          return (Double)left + (Double)right;
        }
        throw new RuntimeError(operator, "Can only add two numbers or two strings together");
      case TokenType.MINUS:
        checkNumberOperand(operator, left, right);
        return (Double)left - (Double)right;
      case TokenType.STAR:
        checkNumberOperand(operator, left, right);
        return (Double)left * (Double)right;
      case TokenType.SLASH:
        checkNumberOperand(operator, left, right);
        return (Double)left / (Double)right;
      default:
        break;
    }
    // control should not reach here...
    return null;
  }
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return expr.expression.accept(this);
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
        break;
      case TokenType.BANG:
        // in Lox all objects are either truthy or falsey, so can negate value no matter what object it is.
        return Boolean.valueOf(isTruthy(value));
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
}
