How to compile and run:
=======================

The source is a single file named Scanner.java. To compile, open terminal (or command line) and type the following: (the % is to denote the terminal, it is not an actual input)

% javac Scanner.java

This will produce the Scanner java class. To run the program:

% java Scanner /path/to/input/file

If the input file is a directory, or does not exist, or can not be read, the program will show error and quit. Trying to run the program without an input file will also quit the program. The output file is produced in the same directory as the Scanner class. The output file is named with a _gen suffix. So, foo.c becomes foo_gen.c as in the project requirements description.

The source is written for java 1.7 and up. It has been tested with OpenJDK equivalent.

Program characteristics:
========================
- If an invald token according to the language is encountered then the program prints the invalid token and stops execution. Examples:
	- A string token spanning over multiple lines.
	- An identifier containing anything other than letters (includes underscore) and digits (the test program parse2.c has this issue with the identifier bar@, which is also not an identifier according to the c syntax)
	- If the program contains a single meta statement at the end of the file without the new line, the program considers that as an valid meta statement token.