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
  private final Map<String, Object> values = new HashMap<>();
  void define(String name, Object value) {
    values.put(name, value);
  }
  /**
   * Assign identifier.lexeme to be mapped to value in this environment,
   * only if identifier.lexeme has been defined in this environment before already.
   */
  void assign(Token identifer, Object value) {
    String name = identifer.lexeme;
    if (values.containsKey(name)) {
      values.put(name, value);
      return;
    }
    throw new RuntimeError(identifer, "Undefined variable '" + name + "'.");
  }
  /**
   * @throws RuntimeError If is undefined variable.
   */
  Object get(Token identifier) {
    String name = identifier.lexeme;
    if (values.containsKey(name)) {
      return values.get(name);
    } else {
      throw new RuntimeError(identifier, "Undefined variable '" + name + "'.");
    }
  }
}
