package com.timfan.lox;

import java.util.List;

/**
 * LoxCallable
 */
interface LoxCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);
  String toString();  
}
