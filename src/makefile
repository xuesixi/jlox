file: Lox.java test.lox
	@javac -d out *.java
	@java -cp out Lox test.lox

p: Lox.java
	@javac -d out *.java
	@java -cp out Lox

codegen: CodeGen.java
	@java ./CodeGen.java .

printer: ./other.AstPrinter.java
	@javac -d out *.java
	@java -cp out other.AstPrinter
