A. How to compile and run:
==========================
The source consists of two java source files named Scanner.java and Parser.java. The 
scanner is the minutely modified scanner submitted for the scanner project previously, 
the modifications are just so that it can be used by the parser I wrote.

The parser is the same recursive descent parser I submitted for parser project previously, modified so
that it produces the generated code - which is written in an output file when the parser succeeds.

 To compile, open terminal (or command line) and type the following: (the % is to denote the terminal, 
 it is not an actual input)

% javac Parser.java Scanner.java

This will produce the Parser java class. To run the program:

% java Parser /path/to/input/file

If the input file is a directory, or does not exist, or can not be read, the program will 
show error and quit. Trying to run the program without an input file will also quit the program. 

The outcome is either success or fail from the parser. If the parser succeeds then there is 
no output printed and a file with suffix '_gen' is created with the generated code.

On failure, the parser prints 'Fail' and gives the token that caused the failure. Scanner 
errors are printed and are prefixed by 'Error in Scanner' in the output, if any.

An example:

% java Parser foo.c

Upon success this will create a foo_gen.c with the generated code in the same directory where the class file is.

The source is written for java 1.7 and up. It has been tested with OpenJDK equivalent.

B. Program functionality and brief description:
===============================================
The following structures have been added to the parser to get the generated code, the 
rest of the parser code is as it was from the parser project (additional comments have
been put in places to increase readability of the newer operations):

- Two array structures are maintained for the global and local variables. The local array
is cleared out whenever we go out of a functions scope.

- Static class SymbolLocation within the Parser - container of a particular symbol 
in our two array system. Holds the location and which array to look into.

- Static class GeneratedCode within the Parser - this is a basic class that contains
the generated code for a function body and an string to represent the expression equivalent
that are encountered throughout the statements. For example, when evaluating a operation
with multiple operands and operator, the equivalent string holds the previously parsed expression
equivalent of the left hand side expression to the point of parsing, so that the generated code 
can use it when needed.

- Arrays to maintain the labels which are need to jump to for break and continue statements.
Whenever a loop is encountered we add the starting and exiting labels of the loop to these arrays
and whenever a loop is finished we remove the last labels, thus supporting nested loops.

Program assumptions:
- Only integer type variables are used throughout the input program. The code does not generate code
for any other type data.

- The program will give error if break and continue statements are used outside a loop body, which is
conforming to the gcc standard.

- No array can be used as a function parameter, which is conforming to the original grammar used with
the parser.

 