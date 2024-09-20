run:
	javac ./com/timfan/lox/*.java;
	java com.timfan.lox.Lox examples/example.lox; \
	ret=$$?; rm -f ./com/timfan/lox/*.class; exit $$ret;
