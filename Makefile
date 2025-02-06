run:
	javac ./com/timfan/lox/*.java;
	java com.timfan.lox.Lox file.lox; \
	ret=$$?; rm -f ./com/timfan/lox/*.class; exit $$ret;
