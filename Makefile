run:
	javac ./com/timfan/lox/*.java
	java com.timfan.lox.Lox file.lox
	rm -f ./com/timfan/lox/*.class