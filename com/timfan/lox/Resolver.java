package com.timfan.lox;
import java.util.Stack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    MAIN, LOCAL
  }
  /**
   * for each variable declaration, add into the scope stored at top of stack 
   * (if there is an scope at top of stack, this is the current local scope).
   * 
   * and then, for each variable reference, walk through stack (starting from the top of the stack),
   * and find out how deep in the stack this variable was declared in, which variable declaration this
   * variable reference wants to refer to,
   * 
   * which variable declaration the interpreter should use when it visits this variable reference.
   * 
   * we will call interpreter.resolve(varRefExpr, depth) for each variable reference, 
   * so that the interpreter knows which variable declaration for each variable reference.
   */
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.MAIN; // initially we are in the main function. change
                                                            // when enter local functions, non-main functions.
  private final Interpreter interpreter;
  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
  /**
   * resolve handles both declarations and references.
   * 
   * for each declaration, it adds that declaration to the environment at the top of stack.
   * 
   * and for each reference, it searches through the stack starting from the top of stack,
   * for where the declaration this reference wants to use is. and after finding it, it 
   * lets the interpreter know how deep to look to find that declaration.
   */
  void resolve(Stmt stmt) {
    stmt.accept(this);
  }
  void resolve(Expr expr) {
    expr.accept(this);
  }
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }
  void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }
  void endScope() {
    scopes.pop();
  }
  void declare(Token identifier) {
    if (scopes.empty()) return; // we are in the global environment, no need for resolver.
    if (scopes.peek().containsKey(identifier.lexeme)) {
      Lox.error(identifier, "Already a variable with this name in this scope.");
      // might as well re-assign it for now? regardless, the source code won't be run by interpreter, 
      // so continue resolving to see if there are any more detectable reference declaration errors.
    }
    // declare this variable in the current local environment (it has not yet been initialised).
    scopes.peek().put(identifier.lexeme, false);
  }
  void define(Token identifier) {
    if (scopes.empty()) return; // we are in the global environment, no need for resolver.
    // define this variable in the current local environment (it has now been initialised).
    scopes.peek().put(identifier.lexeme, true);
  }
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    // starting a new block, so push a new environment to top of stack
    // (the top of stack is now our current local environment).
    beginScope();
    resolve(stmt.statements);
    // we are exiting block, so return to previous environment.
    endScope();
    return null;
  }
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // add the name of this function to the current local scope.
    declare(stmt.identifier);
    define(stmt.identifier);
    // let our resolver know that we are now entering the body of a local function.
    FunctionType previous = currentFunction;
    currentFunction = FunctionType.LOCAL;
    beginScope();
    // then walk through its params code.
    for (Token param : stmt.params) {
      declare(param);
      define(param);
    }
    // then walk through its body code, where return statements in the body 
    // code are ok because the currentFunction is a local function, not the main.
    resolve(stmt.body);
    endScope();
    // now revert back current function, which could be the main, but also a non-main.
    currentFunction = previous;
    return null;
  }
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
  @Override
  public Void visitVarDeclarationStmt(Stmt.VarDeclaration stmt) {
    // add the name of this variable to the current local environment.
    declare(stmt.identifier);
    // and then resolve its initialiser, if there is one.
    if (stmt.initialiser != null) {
      resolve(stmt.initialiser);
    }
    // variable has now also been defined, mark as defined.
    define(stmt.identifier);
    return null;
  }
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenStmt);
    if (stmt.elseStmt != null) resolve(stmt.elseStmt);
    return null;
  }
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.MAIN) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }
    if (stmt.value != null) {
      resolve(stmt.value);
    }
    return null;
  }
  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }
  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    return null;
  }
  @Override
  public Void visitLambdaExpr(Expr.Lambda expr) {
    // let our resolver know that we are now entering the body of a local function.
    FunctionType previous = currentFunction;
    currentFunction = FunctionType.LOCAL;
    beginScope();
    // then walk through its params code.
    for (Token param : expr.function.params) {
      declare(param);
      define(param);
    }
    // then walk through its body code, where return statements in the body 
    // code are ok because the currentFunction is a local function, not the main.
    resolve(expr.function.body);
    endScope();
    // now revert back current function, which could be the main, but also a non-main.
    currentFunction = previous;
    return null;
  }
  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.empty() && scopes.peek().get(expr.identifier.lexeme) == Boolean.FALSE) {
      // for an edge case like 
      //
      // while (true) {
      //   var a = 2;
      //   if (!false) { 
      //     var a = a + 1; 
      //             ^ the variable reference.
      //         ^ because of static scoping, we take the user to mean that 
      //           this is the declaration they are intending to use. but this
      //           declaration hasn't even finished being initialised yet!
      //   }
      // }
      //
      // where the user is trying to reference a variable before it has even been initalised yet,
      // they are trying to use the value of a local variable in the initialiser of its own declaration.
      //
      // of course, if the intended declaration is not in the current scope, then 
      // the scopes.peek().get call will return null, and so the comparison will fail.
      Lox.error(expr.identifier, "Can't use the value of a local variable in the initialiser of its own declaration.");
      return null;
    }
    // searching for the variable reference's intended declaration, and letting the interpreter know.
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(expr.identifier.lexeme)) {
        int depth = (scopes.size() - 1) - (i); // if i == scopes.size() - 1 [the intended declaration is in the current local environment], then depth == 0. 
        interpreter.resolve(expr, depth); // when visiting this specific varRefExpr node during interpretation, 
                                          // the interpreter will know they should search depth number of levels up the 
                                          // environment chain to find the declaration the user wants to use.
        break;
      }
    }
    return null;
  }
  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr argument : expr.arguments) {
      resolve(argument);
    }
    return null;
  }
  @Override
  public Void visitArrayExpr(Expr.Array expr) {
    for (Expr value : expr.values) {
      resolve(value);
    }
    return null;
  }
  @Override
  public Void visitDictionaryExpr(Expr.Dictionary expr) {
    for (Expr value : expr.dictionary) {
      resolve(value);
    }
    return null;
  }
  @Override
  public Void visitSubscriptExpr(Expr.Subscript expr) {
    resolve(expr.subscriptee);
    resolve(expr.index);
    return null;
  }
  @Override
  public Void visitSubscriptAssignExpr(Expr.SubscriptAssign expr) {
    resolve(expr.subscriptee);
    resolve(expr.index);
    resolve(expr.value);
    return null;
  }
  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }
  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }
  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    // first resolve the re-initialiser, the new value we want to assign expr to.
    resolve(expr.value);
    // the interpreter needs to know which variable declaration to assign to, 
    // within which environment should the variable of expr's name be reassinged.
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(expr.identifier.lexeme)) {
        int depth = (scopes.size() - 1) - (i); // if i == scopes.size() - 1 [the intended declaration is in the current local environment], then depth == 0. 
        interpreter.resolve(expr, depth); // when visiting this specific varAssignExpr node during interpretation, 
                                          // the interpreter will know they should search depth number of levels up the 
                                          // environment chain to find the declaration the user wants to use.
      }
    }
    return null;
  }
  @Override
  public Void visitLogicExpr(Expr.Logic expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
}
