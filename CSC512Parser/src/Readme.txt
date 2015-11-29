A. Modified grammar used for the Parser:
=========================================
Following is the modified grammar used for the parser code submitted. This modification involves removal of all direct and indirect left recursion, left factoring, and some substitution to reduce non-terminals. 

No. of rules in grammar: 75
No. of non-terminals in grammar: 25

Grammar (leftmost column just lists the no. of rules)
----------------------------------------------------- 
1.  <program> --> <data decls> <func list>

2.  <func list> --> int ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
3.  <func list> --> void ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
4.  <func list> --> binary ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
5.  <func list> --> decimal ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
6.  <func list> --> empty 

7.  <func prime> --> semicolon 
8.  <func prime> --> left_brace <data decls> <statements> right_brace 

9.  <parameter list> --> void <parameter list prime>
10. <parameter list> --> int ID <ne list prime>
11. <parameter list> --> binary ID <ne list prime>
12. <parameter list> --> decimal ID <ne list prime>
13. <parameter list> --> empty

14. <parameter list prime> --> ID <ne list prime>
15. <parameter list prime> --> empty

16. <ne list prime> --> comma <ne list part>
17. <ne list prime> --> empty

18. <ne list part> --> int ID <ne list prime>
19. <ne list part> --> void ID <ne list prime>
20. <ne list part> --> binary ID <ne list prime>
21. <ne list part> --> decimal ID <ne list prime>

22. <data decls> --> int ID <id prime> <id list prime> semicolon <data decls>
23. <data decls> --> void ID <id prime> <id list prime> semicolon <data decls>
24. <data decls> --> binary ID <id prime> <id list prime> semicolon <data decls>
25. <data decls> --> decimal ID <id prime> <id list prime> semicolon <data decls>
26. <data decls> --> empty

27. <id prime> --> left_bracket <expression> right_bracket
28. <id prime> --> empty

29. <id list prime> --> comma ID <id prime> <id list prime>
30. <id list prime> --> empty

31. <statements> --> ID <statement prime> <statements>
32. <statements> --> if left_parenthesis <condition expression> right_parenthesis left_brace <statements> right_brace <statements>
33. <statements> --> while left_parenthesis <condition expression> right_parenthesis left_brace <statements> right_brace <statements>
34. <statements> --> return <return statement prime> <statements>
35. <statements> --> break semicolon <statements>
36. <statements> --> continue semicolon <statements>
37. <statements> --> read left_parenthesis  ID right_parenthesis semicolon <statements>
38. <statements> --> write left_parenthesis <expression> right_parenthesis semicolon <statements>
39. <statements> --> print left_parenthesis  STRING right_parenthesis semicolon <statements>
40. <statements> --> empty

41. <statement prime> --> equal_sign <expression> semicolon
42. <statement prime> --> left_bracket <expression> right_bracket equal_sign <expression> semicolon
43. <statement prime> --> left_parenthesis <expr list> right_parenthesis semicolon 

44. <expr list> --> <expression> <ne expr list prime>
45. <expr list> --> empty

46. <ne expr list prime> --> comma <expression> <ne expr list prime>
47. <ne expr list prime> --> empty

48. <condition expression> -->  <condition> <condition expression prime>

49. <condition expression prime> --> double_and_sign <condition> 
50. <condition expression prime> --> double_or_sign <condition> 
51. <condition expression prime> --> empty

52. <condition> --> <expression> <condition prime>

53. <condition prime> --> ==  <expression>
54. <condition prime> --> !=  <expression>
55. <condition prime> --> >  <expression>
56. <condition prime> --> >=  <expression>
57. <condition prime> --> <  <expression>
58. <condition prime> --> <=  <expression>

59. <return statement prime> --> <expression> semicolon
60. <return statement prime> --> semicolon 

61. <expression> --> <term> <expression prime>

62. <expression prime> --> plus_sign <term> <expression prime>
63. <expression prime> --> minus_sign <term> <expression prime>
64. <expression prime> --> empty

65. <term> --> <factor> <term prime>

66. <term prime> --> star_sign <factor> <term prime>
67. <term prime> --> forward_slash <factor> <term prime>
68. <term prime> --> empty

69. <factor> --> ID <factor prime>
70. <factor> --> NUMBER
71. <factor> --> minus_sign NUMBER
72. <factor> --> left_parenthesis <expression> right_parenthesis

73. <factor prime> --> left_bracket <expression> right_bracket
74. <factor prime> --> left_parenthesis <expr list> right_parenthesis
75. <factor prime> --> empty

However, the grammar is not fully LL(1), below are the First and Follow sets for all 25 non-terminals in the grammar:

First and Follow sets:
----------------------
1. <program> 
First: {int, void, binary, decimal, empty}
Follow: {eof}

2. <func list>
First: {int, void, binary, decimal, empty}
Follow: {eof}

3. <func prime>
First: {semicolon, left_brace}
Follow: {int, void, binary, decimal, eof}

4. <parameter list> 
First: {int, void, binary, decimal, empty}
Follow: {right_parenthesis}

5. <parameter list prime>
First: {ID, empty}
Follow: {right_parenthesis}

6. <ne list prime> 
First: {comma, empty}
Follow: {right_parenthesis}

7. <ne list part>
First: {int, void, binary, decimal}
Follow: {right_parenthesis}

8. <data decls> 
First: {int, void, binary, decimal, empty}
Follow: {int, void, binary, decimal, ID, if, while, return, break, continue, read, write, print, right_brace, eof}

9. <id prime> 
First: {left_bracket, empty}
Follow: {comma, semicolon}

10. <id list prime>
First: {comma, empty}
Follow: {semicolon}

11. <statements> 
First: {ID, if, while, return, break, continue, read, write, print, empty}
Follow: {right_brace}

12. <statement prime>
First: {equal_sign, left_bracket, left_parenthesis}
Follow: {ID, if, while, return, break, continue, read, write, print, right_brace}

13. <expr list> 
First: {ID, NUMBER, minus_sign, left_parenthesis, empty}
Follow: {right_parenthesis}

14. <ne expr list prime>
First: {comma, empty}
Follow: {right_parenthesis}

15. <condition expression> 
First: {ID, NUMBER, minus_sign, left_parenthesis}
Follow: {right_parenthesis}

16. <condition expression prime>
First: {double_and_sign, double_or_sign, empty}
Follow: {right_parenthesis}

17. <condition> 
First: {ID, NUMBER, minus_sign, left_parenthesis}
Follow: {double_and_sign, double_or_sign, right_parenthesis}

18. <condition prime>
First: {==, !=, >, >=, <, <=}
Follow: {double_and_sign, double_or_sign, right_parenthesis}

19. <return statement prime>
First: {ID, NUMBER, minus_sign, left_parenthesis, semicolon}
Follow: {ID, if, while, return, break, continue, read, write, print, right_brace}

20. <expression> 
First: {ID, NUMBER, minus_sign, left_parenthesis}
Follow: {semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

21. <expression prime>
First: {plus_sign, minus_sign, empty}
Follow: {semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

22. <term> 
First: {ID, NUMBER, minus_sign, left_parenthesis}
Follow: {plus_sign, minus_sign, semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

23. <term prime>
First: {star_sign, forward_slash, empty}
Follow: {plus_sign, minus_sign, semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

24. <factor> 
First: {ID, NUMBER, minus_sign, left_parenthesis}
Follow: {star_sign, forward_slash, plus_sign, minus_sign, semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

25. <factor prime>
First: {left_bracket, left_parenthesis, empty}
Follow: {star_sign, forward_slash, plus_sign, minus_sign, semicolon, double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}

It can be seen from these sets that the non-terminal <data decls> (no. 8 in the above list) has First+ sets for it's expansion rules that are not disjoint. So, this grammar is not strong LL(1). To accomodate the <data decls> rule, the code utilizes more than one word look ahead which is explained in the program characteristis section and also commented in the code.

B. How to compile and run:
==========================

The source consists of two java source files named Scanner.java and Parser.java. The scanner is the minutely modified scanner submitted for the scanner project previously, the modifications are just so that it can be used by the parser I wrote.

 To compile, open terminal (or command line) and type the following: (the % is to denote the terminal, it is not an actual input)

% javac Parser.java Scanner.java

This will produce the Parser java class. To run the program:

% java Parser /path/to/input/file

If the input file is a directory, or does not exist, or can not be read, the program will show error and quit. Trying to run the program without an input file will also quit the program. 

The output is either Pass or Fail from the parser. Scanner errors are prefixed by 'Error in Scanner'.

If the input program is parsed successfully according to the grammar above, it will also show the variable count (global and local), function definition count (not function declarations), and the number of statements according to the <statements> rule in the above grammar.

If the input program parsing is unsuccessful, it additionally only shows the word that could not be parsed according to the grammar.

The source is written for java 1.7 and up. It has been tested with OpenJDK equivalent.

C. Program characteristics:
========================
- My parser code is a slightly modified recursive descent parser to accommodate the <data decls> rule expansion. Each non-terminal symbol in the grammar (see list above) has it's own method in the code, which recursively expands the corresponding rules. The code includes the grammar rules for each non-terminal method as comment above and the First+ set where appropriate.

- Every non-terminal symbol's function expands based on the next word read from the scanner. If the function is able to expand the rule successfully it returns true, otherwise false.

- The only exclusion of the single word look ahead is the <data decls> rule, where we need more than a single word look ahead to determine the next rule. This is done by peeking ahead in the scanner token stream for words without consuming them, once we are certain of the rule, then we begin to consume the words. Otherwise the next appropriate rule makes use of the words. A very primitive queue is used for to this look ahead.

- The program terminates when the starting symbol's function returns. If it exhausted the input tokens and the function returned true - denoting all the rules encountered have been successfully expanded, the input program parsing is successful, otherwise the parsing has failed.

- The program counts the total no. of variables - both global and local in the program based on the <data decls> rule. Note, function parameters are not counted as local variable.

- The program counts no. of function definitions, it excludes any function declarations from this count. It utilizes the closing braces for function scope to count the function definitions.

- The program also counts all statements according to the <statements> rule. The statement count includes statements scoped within another statement (statements inside block of another statement such as if or while).