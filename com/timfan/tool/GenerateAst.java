package com.timfan.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];
    // write source code into a file called Expr.java, which contains class Expr.
    // contained inside Expr (as static classes), are subclasses Binary, Grouping, Literal, Unary,
    // each with their own fields (e.g., Object value, for the Literal class)
    // and own constructors and own methods.
    defineAst(outputDir, "Expr", Arrays.asList(
      "Binary    : Expr left, Token operator, Expr right",
      "Call      : Expr callee, Token paren, List<Expr> arguments",
      "Grouping  : Expr expression",
      "Literal   : Object value",
      "Unary     : Token operator, Expr right",
      "Variable  : Token identifier",
      "Assign    : Token identifier, Expr value",
      "Logic     : Expr left, Token operator, Expr right",
      "Array     : List<Expr> values",
      "Subscript : Expr subscriptee, Token bracket, Expr index",
      "SubscriptAssign : Expr subscriptee, Token bracket, Expr index, Expr value",
      "Lambda    : Stmt.Function function",
      "Dictionary: List<Expr> dictionary"
    ));
    defineAst(outputDir, "Stmt", Arrays.asList(
      "Expression       : Expr expression",
      "Function         : Token identifier, List<Token> params, List<Stmt> body",
      "Print            : Expr expression", 
      "VarDeclaration   : Token identifier, Expr initialiser",
      "Block            : List<Stmt> statements",
      "If               : Expr condition, Stmt thenStmt, Stmt elseStmt",
      "While            : Expr condition, Stmt body",
      "Return           : Token keyword, Expr value",
      "Break            : Token keyword",
      "Continue         : Token keyword"
    ));
  }
  private static void defineAst(
    String outputDir, String baseName, List<String> types)
    throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.timfan.lox;");
    writer.println();
    writer.println("import java.util.List;");
    // writer.println();
    writer.println("abstract class " + baseName + " {");
    
    // making the visitor interface (how clients, like the parser or interpreter, can interact with Expr objects).
    defineVisitor(writer, baseName, types);
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    // making subclasses for each type of subexpression.
    for (String type : types) {
      // for this subclass.
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      // write in constructor, fields, methods.
      defineType(writer, baseName, className, fields);
    }
    writer.println("}");
    writer.close();
  }
  private static void defineType(
      PrintWriter writer, String baseName,
      String className, String fieldList) {
    writer.println("  static class " + className + " extends " +
        baseName + " {");

    // Constructor.
    writer.println("    " + className + "(" + fieldList + ") {");

    // Store parameters in fields.
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    // Fields.
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");
    writer.println("  }");
  }
  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    {
      String s = 
        "  /**\n" +
        "   * the client offers the customer the whole menu,\n" +
        "   * and asks the customer to choose what they want from that menu.\n" +
        "   *\n" +
        "   * for (Person customer : customers) {\n" +
        "   *   Choice choice = customer.choose(menu);\n" +
        "   * }\n" +
        "   *\n" +
        "   * and then each customer chooses what they want from the menu.\n" +
        "   *\n" +
        "   * Person Alice() {\n" +
        "   *   Choice choose(Menu menu) {\n" +
        "   *     // Alice chooses apple from the given menu.\n" +
        "   *      return menu.apple();\n" +
        "   *    }\n" +
        "   *  }\n" +
        "   *\n" +
        "   *  Person Bob() {\n" +
        "   *    Choice choose(Menu menu) {\n" +
        "   *      // Bob chooses banana from the given menu.\n" +
        "   *      return menu.banana();\n" +
        "   *    }\n" +
        "   *  }\n" +
        "   */";
      writer.println(s);
    }
    // first, each customer needs to be able to read the same menu.
    writer.println("  interface Visitor<R> {");
    // making a menu item for each kind of customer.
    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
    }
    writer.println("  }");
    // then, each kind of customer should choose which menu item they would choose.
  }
}
