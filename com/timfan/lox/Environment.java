package com.timfan.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a hashmap of declarations and their associated values.
 * The interpreter will have its own singular global environment accessible by all statements,
 * and then each code block, like the if-then-else code blocks, the function code blocks, etc.,
 * will have their own environments, environments only accessible to 
 * statements within those code blocks.
 */
class Environment {
  final Environment parent;
  private final Map<String, Object> values = new HashMap<>();
  Environment() {
    // the global variable environment.
    parent = null;
  }
  Environment(Environment parent) {
    // walking up the chain of parent environments will reach the global variable environment.
    this.parent = parent;
  }
  void define(String name, Object value) {
    values.put(name, value);
  }
  /**
   * Assign identifier.lexeme to be mapped to value in this environment,
   * only if identifier.lexeme has been defined in this environment before already.
   */
  void assign(Token identifier, Object value) {
    String name = identifier.lexeme;
    if (values.containsKey(name)) {
      values.put(name, value);
      return;
    }
    // this environment doesn't have this identifier. check the parent environment, 
    if (parent != null) {
      // recursively walking up the chain trying each parent environment, until we reach the global environment.
      parent.assign(identifier, value);
    } else /* if parent is null */ {
      // we are in the global variable environment and still haven't found.
      throw new RuntimeError(identifier, "Undefined variable '" + name + "'.");
    }
  }
  /**
   * @throws RuntimeError If is undefined variable.
   */
  Object get(Token identifier) {
    String name = identifier.lexeme;
    if (values.containsKey(name)) {
      return values.get(name);
    }
    // this environment doesn't have this identifier. check the parent environment, 
    if (parent != null) {
      // recursively walking up the chain trying each parent environment, until we reach the global environment.
      return parent.get(identifier);
    } else /* if parent is null */ {
      // we are in the global variable environment and still haven't found.
      throw new RuntimeError(identifier, "Undefined variable '" + name + "'.");
    }
  }
}
