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
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Unary    : Token operator, Expr right"
    ));
  }
  private static void defineAst(
    String outputDir, String baseName, List<String> types)
    throws IOException {
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");
    // root node has no fields. so skip to making subclasses.
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
  private static void defineType(PrintWriter writer, String baseName, String className, String fields) {
    writer.println("  static class " + className + " extends " + baseName + " {");
  }
}
