## JLox

JLox is a basic [Lexer](com/timfan/lox/Scanner.java), recursive-descent [Parser](com/timfan/lox/Parser.java), and tree-walk [Interpreter](com/timfan/lox/Interpreter.java), for a simple minimal Javascript-like language called Lox, created by Robert Nystrom as part of their [Crafting Interpreters][1] book.

## Using JLox

Run in the terminal:

```
javac ./com/timfan/lox/*.java && java com.timfan.lox.Lox file.lox
```

Replacing file.lox with the Lox program that want to run.

An example file.lox is provided, which prints out the first 30 fibonacci numbers.
 
<p align="center">
  <img src="https://github.com/user-attachments/assets/2b89220e-04ac-4c54-a3b7-8dfd0eda6bc3" />
</p>

[1]: https://craftinginterpreters.com/the-lox-language.html