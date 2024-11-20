
all:
	@javac -d out *.java

f: file

file: Lox.java test.lox
	@javac -d out *.java
	@java -cp out Lox test.lox

p: prompt

prompt: Lox.java
	@javac -d out *.java
	@java -cp out Lox

codegen: CodeGen.java
	@java ./CodeGen.java .

printer: ./other.AstPrinter.java
	@javac -d out *.java
	@java -cp out other.AstPrinter

jar: all Lox.java manifest.txt
	cd out && jar cfm ../myjlox.jar ../manifest.txt *.class resources

