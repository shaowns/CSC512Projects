import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Owner: ShaownS
 * File: Parser.java
 * Package: 
 * Project: CSC512Parser
 * Email: ssarker@ncsu.edu
 */

/**
 * 
 */
public class Parser {
	
	private Scanner scanner;
	private ArrayList<Scanner.Token> lookAheadTokens;
	private Scanner.Token word;
	
	// Parsing counters.
	int numVar, numFunc, numStatement;
	
	public Parser(String fileName) throws FileNotFoundException, IOException {
		this.scanner = new Scanner(fileName);
		this.lookAheadTokens = new ArrayList<Scanner.Token>();
		
		this.numVar = 0;
		this.numFunc = 0;
		this.numStatement = 0;
	}
	
	private Scanner.Token peekNextToken() {
		Scanner.Token token = getNextToken();
		lookAheadTokens.add(token);
		return token;
	}
	
	/*
	 * First checks the look ahead collection, if it's empty - requests the scanner for a new token.
	 */
	public Scanner.Token nextWord() {
		Scanner.Token token = null;		
		if (lookAheadTokens.size() > 0) {
			token = lookAheadTokens.get(0);
			lookAheadTokens.remove(0);
		} else {
			token = getNextToken();
		}
		return token;
	}
	
	/*
	 * Get next token that is not meta-statement from scanner, not used directly.
	 * Called by getNextWord when there are no more look ahead tokens.
	 * Called by peekNextToken to get a look ahead token.
	 */
	private Scanner.Token getNextToken() {
		Scanner.Token token = null;
		// Get the non-meta statement tokens only.
		do {
			if (scanner.hasMoreTokens()) {
				token = scanner.getNextToken();
			}
		} while (token != null && token.getTokenType() == Scanner.TokenType.META_STATEMENT);		
		
		return token;
	}
	
	public void fail() {
		System.out.println("Fail");
		System.out.println("Failed token: " + word.getTokenName());		
	}
	
	public void success() {
		System.out.println("Pass. " + "variable " + Integer.toString(numVar) + " function " + Integer.toString(numFunc) 
							+ " statement " + Integer.toString(numStatement));
	}
	
	/*
	 * <program> --> <data decls> <func list>
	 */
	public void program() {
		word = nextWord();
		if (data_decls()) {
			if (!func_list()) {
				fail();
				return;
			}
			
			// Check if input is exhausted.
			if (word == null) {
				success();
				return;
			} else {
				fail();
				return;
			}
			
		} else {
			fail();
			return;
		}
	}
	
	/*
	<func list> --> int ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
	<func list> --> void ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
	<func list> --> binary ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
	<func list> --> decimal ID left_parenthesis <parameter list> right_parenthesis <func prime> <func list>
	<func list> --> empty
	*/
	public boolean func_list() {
		if (word == null) {
			// <func list> --> empty
			// First+: {eof}
			return true;
		} else if (word.getTokenName().equals("int") || word.getTokenName().equals("void") 
					|| word.getTokenName().equals("binary") || word.getTokenName().equals("decimal")) {
				word = nextWord();
				if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
					return false;
				}
				word = nextWord();
				
				if (!word.getTokenName().equals("(")) {
					return false;
				}			
				word = nextWord();
				
				if (!parameter_list()) {
					return false;
				}
				
				if (!word.getTokenName().equals(")")) {
					return false;
				}			
				word = nextWord();
				
				if (!func_prime()) {
					return false;
				}
				
				return func_list();
		} else  {
			return false;
		}
	}
	
	/*
	<func prime> --> semicolon 
	<func prime> --> left_brace <data decls> <statements> right_brace
	*/
	public boolean func_prime() {
		if (word.getTokenName().equals(";")) {
			word = nextWord();
			return true;
		} else if (word.getTokenName().equals("{")) {
			word = nextWord();
			if (!data_decls()) {
				return false;
			}
			
			if (!statements()) {
				return false;
			}
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}
			word = nextWord();
			
			// The right brace concludes the function definition, increase the function count.
			numFunc++;
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<parameter list> --> void <parameter list prime>
	<parameter list> --> int ID <ne list prime>
	<parameter list> --> binary ID <ne list prime>
	<parameter list> --> decimal ID <ne list prime>
	<parameter list> --> empty
	*/
	public boolean parameter_list() {
		if (word.getTokenName().equals("void")) {
			word = nextWord();
			return parameter_list_prime();
		} else if (word.getTokenName().equals("int") || word.getTokenName().equals("binary") || word.getTokenName().equals("decimal")) {
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			word = nextWord();
			return ne_list_prime();
		} else if (word.getTokenName().equals(")")) {
			// <parameter list> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<parameter list prime> --> ID <ne list prime>
	<parameter list prime> --> empty
	*/
	public boolean parameter_list_prime() {
		if (word.getTokenType() == Scanner.TokenType.IDENTIFIER) {
			word = nextWord();
			return ne_list_prime();
		} else if (word.getTokenName().equals(")")) {
			// <parameter list prime> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<ne list prime> --> comma <ne list part>
	<ne list prime> --> empty
	*/
	public boolean ne_list_prime() {
		if (word.getTokenName().equals(",")) {
			word = nextWord();
			return ne_list_part();
		} else if (word.getTokenName().equals(")")) {
			// <ne list prime> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<ne list part> --> int ID <ne list prime>
	<ne list part> --> void ID <ne list prime>
	<ne list part> --> binary ID <ne list prime>
	<ne list part> --> decimal ID <ne list prime>
	*/
	public boolean ne_list_part() {
		if (word.getTokenName().equals("int") || word.getTokenName().equals("void") 
				|| word.getTokenName().equals("binary") || word.getTokenName().equals("decimal")) {
			word = nextWord();
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			word = nextWord();
			return ne_list_prime();
		} else {
			return false;
		}
	}
	
	/*
	<data decls> --> int ID <id prime> <id list prime> semicolon <data decls>
	<data decls> --> void ID <id prime> <id list prime> semicolon <data decls>
	<data decls> --> binary ID <id prime> <id list prime> semicolon <data decls>
	<data decls> --> decimal ID <id prime> <id list prime> semicolon <data decls>
	<data decls> --> empty
	*/
	
	public boolean data_decls() {
		if (word == null || word.getTokenType() == Scanner.TokenType.IDENTIFIER || word.getTokenName().equals("if")
				|| word.getTokenName().equals("while") || word.getTokenName().equals("return")
				|| word.getTokenName().equals("break") || word.getTokenName().equals("continue") || word.getTokenName().equals("read")
				|| word.getTokenName().equals("write") || word.getTokenName().equals("print") || word.getTokenName().equals("}")) {
			// <data decls> --> empty
			// First+: {empty, int, void, binary, decimal, ID, if, while, return, break, continue, read, write, print, right_brace, eof}
			// Among these int, void, binary, decimal have been handled as part of the look ahead below.
			return true;
		} else if (word.getTokenName().equals("int") || word.getTokenName().equals("void") ||
					word.getTokenName().equals("binary") || word.getTokenName().equals("decimal")) {
				/*
				 *  Match without consuming, unless we are sure this is a data declaration.
				 *  Need to peek ahead two tokens, if we get identifier followed by left_parenthesis then
				 *  we are matching a func_list and should return true without consuming anything. 
				 *  Otherwise we keep on matching more data_decls. This is part of the rule: <data decls> --> empty
				 *  The rest of the empty expansion is handled below.
				 */
				Scanner.Token peek1 = peekNextToken();
				Scanner.Token peek2 = peekNextToken();
				if (peek1 == null || peek2 == null) {
					return false;
				}
				if (peek1.getTokenType() == Scanner.TokenType.IDENTIFIER && peek2.getTokenName().equals("(")) {
					return true;
				}
				
				// Safe to consume word.
				word = nextWord();
				if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
					return false;
				}
				
				// We have read an identifier inside data declaration, increase the variable count.
				numVar++;
				word = nextWord();			
				
				if (!id_prime()) {
					return false;
				}
				
				if (!id_list_prime()) {
					return false;
				}
				
				if (!word.getTokenName().equals(";")) {
					return false;
				}
				word = nextWord();
				
				return data_decls();
		} else  {
			return false;
		}
	}
	
	/*
	<id prime> --> left_bracket <expression> right_bracket
	<id prime> --> empty
	*/
	public boolean id_prime() {
		if (word.getTokenName().equals("[")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}			
			word = nextWord();
			
			return true;
		} else if (word.getTokenName().equals(",") || word.getTokenName().equals(";")) {
			// <id prime> --> empty
			// First+: {empty, comma, semicolon}
			return true;			
		} else {
			return false;
		}
	}
	
	/*
	<id list prime> --> comma ID <id prime> <id list prime>
	<id list prime> --> empty
	*/
	public boolean id_list_prime() {
		if (word.getTokenName().equals(",")) {
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			
			// We have parsed a variable declaration separated by comma, increase the variable count.
			numVar++;
			word = nextWord();
			
			if (!id_prime()) {
				return false;
			}
			
			return id_list_prime();
		} else if (word.getTokenName().equals(";")) {
			// <id list prime> --> empty
			// First+: {empty, semicolon}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<statements> --> ID <statement prime> <statements>
	<statements> --> if left_parenthesis <condition expression> right_parenthesis left_brace <statements> right_brace <statements>
	<statements> --> while left_parenthesis <condition expression> right_parenthesis left_brace <statements> right_brace <statements>
	<statements> --> return <return statement prime> <statements>
	<statements> --> break semicolon <statements>
	<statements> --> continue semicolon <statements>
	<statements> --> read left_parenthesis  ID right_parenthesis semicolon <statements>
	<statements> --> write left_parenthesis <expression> right_parenthesis semicolon <statements>
	<statements> --> print left_parenthesis  STRING right_parenthesis semicolon <statements>
	<statements> --> empty
	*/
	public boolean statements() {
		if (word.getTokenType() == Scanner.TokenType.IDENTIFIER) {
			word = nextWord();
			
			if (!statement_prime()) {
				return false;
			}
			
			return statements();
		} else if (word.getTokenName().equals("if")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (!condition_expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("{")) {
				return false;
			}
			word = nextWord();
			
			if (!statements()) {
				return false;
			}
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}
			
			// if statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			
			return statements();
		} else if (word.getTokenName().equals("while")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (!condition_expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("{")) {
				return false;
			}
			word = nextWord();
			
			if (!statements()) {
				return false;
			}
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}
			// while statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			
			return statements();
		} else if (word.getTokenName().equals("return")) {
			word = nextWord();
			
			if (!return_statement_prime()) {
				return false;
			}
			
			return statements();
		} else if (word.getTokenName().equals("break")) {
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// break statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			return statements();
		} else if (word.getTokenName().equals("continue")) {
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// continue statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			return statements();
		} else if (word.getTokenName().equals("read")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// read statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			
			return statements();
		} else if (word.getTokenName().equals("write")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// write statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			
			return statements();
		} else if (word.getTokenName().equals("print")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.STRING) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// print statement parsed, increase the statement counter.
			numStatement++;
			word = nextWord();
			
			return statements();
		} else if (word.getTokenName().equals("}")) {
			// <statements> --> empty
			// First+: {empty, right_brace}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<statement prime> --> equal_sign <expression> semicolon
	<statement prime> --> left_bracket <expression> right_bracket equal_sign <expression> semicolon
	<statement prime> --> left_parenthesis <expr list> right_parenthesis semicolon
	*/
	public boolean statement_prime() {
		if (word.getTokenName().equals("=")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			// Reached semicolon of a statement, increase the counter.
			numStatement++;
			word = nextWord();
			
			return true;
		} else if (word.getTokenName().equals("[")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("=")) {
				return false;
			}
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Reached semicolon of a statement, increase the counter.
			numStatement++;
			word = nextWord();
			
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			if (!expr_list()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Reached semicolon of a statement, increase the counter.
			numStatement++;
			word = nextWord();
			
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<expr list> --> <expression> <ne expr list prime>
	<expr list> --> empty
	*/
	public boolean expr_list() {
		if (expression()) {
			return ne_expr_list_prime();
		} else if (word.getTokenName().equals(")")) {
			// <expr list> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<ne expr list prime> --> comma <expression> <ne expr list prime>
	<ne expr list prime> --> empty
	*/
	public boolean ne_expr_list_prime() {
		if (word.getTokenName().equals(",")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			return ne_expr_list_prime();
		} else if (word.getTokenName().equals(")")) {
			// <ne expr list prime> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<condition expression> -->  <condition> <condition expression prime>
	*/
	public boolean condition_expression() {
		if (condition()) {
			return condition_expression_prime();
		} else {
			return false;
		}
	}
	
	/*
	<condition expression prime> --> double_and_sign <condition> 
	<condition expression prime> --> double_or_sign <condition> 
	<condition expression prime> --> empty
	*/
	public boolean condition_expression_prime() {
		if (word.getTokenName().equals("&&") || word.getTokenName().equals("||")) {
			word = nextWord();
			return condition();
		} else if (word.getTokenName().equals(")")) {
			// <condition expression prime> --> empty
			// First+: {empty, right_parenthesis}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<condition> --> <expression> <condition prime>
	*/
	public boolean condition() {
		if (expression()) {
			return condition_prime();
		} else {
			return false;
		}
	}
	
	/*
	<condition prime> --> ==  <expression>
	<condition prime> --> !=  <expression>
	<condition prime> --> >  <expression>
	<condition prime> --> >=  <expression>
	<condition prime> --> <  <expression>
	<condition prime> --> <=  <expression>
	*/
	public boolean condition_prime() {
		if (word.getTokenName() .equals("==") || word.getTokenName().equals("!=")
				|| word.getTokenName().equals(">") || word.getTokenName().equals(">=")
				|| word.getTokenName().equals("<") || word.getTokenName().equals("<=")) {
			word = nextWord();
			
			return expression();
		} else {
			return false;
		}
	}
	
	/*
	<return statement prime> --> <expression> semicolon
	<return statement prime> --> semicolon
	*/
	public boolean return_statement_prime() {
		if (expression()) {
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Reached semicolon of a return statement, increase the counter.
			numStatement++;
			word = nextWord();
			
			return true;
		} else if (word.getTokenName().equals(";")) {
			// Reached semicolon of a return statement, increase the counter.
			numStatement++;
			word = nextWord();
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<expression> --> <term> <expression prime>
	*/
	public boolean expression() {
		if (term()) {
			return expression_prime();
		} else {
			return false;
		}
	}
	
	/*
	<expression prime> --> plus_sign <term> <expression prime>
	<expression prime> --> minus_sign <term> <expression prime>
	<expression prime> --> empty
	*/
	public boolean expression_prime() {
		if (word.getTokenName().equals("+") || word.getTokenName().equals("-")) {
			word = nextWord();
			
			if (!term()) {
				return false;
			}
			
			return expression_prime();
		} else if (word.getTokenName().equals(";") || word.getTokenName().equals("&&")
					|| word.getTokenName().equals("||") || word.getTokenName().equals(")")
					|| word.getTokenName().equals("==") || word.getTokenName().equals("!=")
					|| word.getTokenName().equals(">") || word.getTokenName().equals(">=")
					|| word.getTokenName().equals("<") || word.getTokenName().equals("<=")
					|| word.getTokenName().equals(",") || word.getTokenName().equals("]")) {
			// <expression prime> --> empty
			// First+: {empty, semicolon, double_and_sign, double_or_sign, right_parenthesis, 
			//			==, !=, >, >=, <, <=, comma, right_bracket}
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<term> --> <factor> <term prime>
	*/
	public boolean term() {
		if (factor()) {
			return term_prime();
		} else {
			return false;
		}
	}
	
	/*
	<term prime> --> star_sign <factor> <term prime>
	<term prime> --> forward_slash <factor> <term prime>
	<term prime> --> empty
	*/
	public boolean term_prime() {
		if (word.getTokenName().equals("*") || word.getTokenName().equals("/")) {
			word = nextWord();
			
			if (!factor()) {
				return false;
			}
			
			return term_prime();
		} else if (word.getTokenName().equals("+") || word.getTokenName().equals("-")
					|| word.getTokenName().equals(";") || word.getTokenName().equals("&&")
					|| word.getTokenName().equals("||") || word.getTokenName().equals(")")
					|| word.getTokenName().equals("==") || word.getTokenName().equals("!=")
					|| word.getTokenName().equals(">") || word.getTokenName().equals(">=")
					|| word.getTokenName().equals("<") || word.getTokenName().equals("<=")
					|| word.getTokenName().equals(",") || word.getTokenName().equals("]")) {
				// <term prime> --> empty
				// First+: {empty, plus_sign, minus_sign, semicolon, double_and_sign, double_or_sign, right_parenthesis, 
				//			==, !=, >, >=, <, <=, comma, right_bracket}
				return true;
		} else {
			return false;
		}
	}
	
	/*
	<factor> --> ID <factor prime>
	<factor> --> NUMBER
	<factor> --> minus_sign NUMBER
	<factor> --> left_parenthesis <expression> right_parenthesis
	*/
	public boolean factor() {
		if (word.getTokenType() == Scanner.TokenType.IDENTIFIER) {
			word = nextWord();			
			return factor_prime();
		} else if (word.getTokenType() == Scanner.TokenType.NUMBER) {
			word = nextWord();
			
			return true;
		} else if (word.getTokenName().equals("-")) {
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.NUMBER) {
				return false;
			}			
			word = nextWord();
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}			
			word = nextWord();
			return true;			
		} else {
			return false;
		}
	}
	
	/*
	<factor prime> --> left_bracket <expression> right_bracket
	<factor prime> --> left_parenthesis <expr list> right_parenthesis
	<factor prime> --> empty
	*/
	public boolean factor_prime() {
		if (word.getTokenName().equals("[")) {
			word = nextWord();
			
			if (!expression()) {
				return false;
			}
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}			
			word = nextWord();
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			if (!expr_list()) {
				return false;
			}
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}			
			word = nextWord();
			return true;
		} else if (word.getTokenName().equals("*") || word.getTokenName().equals("/") 
				|| word.getTokenName().equals("+") || word.getTokenName().equals("-")
				|| word.getTokenName().equals(";") || word.getTokenName().equals("&&")
				|| word.getTokenName().equals("||") || word.getTokenName().equals(")")
				|| word.getTokenName().equals("==") || word.getTokenName().equals("!=")
				|| word.getTokenName().equals(">") || word.getTokenName().equals(">=")
				|| word.getTokenName().equals("<") || word.getTokenName().equals("<=")
				|| word.getTokenName().equals(",") || word.getTokenName().equals("]")) {
			// <factor prime> --> empty
			// First+: {empty, star_sign, forward_slash, plus_sign, minus_sign, semicolon, 
			//			double_and_sign, double_or_sign, right_parenthesis, ==, !=, >, >=, <, <=, comma, right_bracket}
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String[] args) {
		// Basic sanity check.
		if (args.length == 0) {
			System.out.println("Must give the input file location as parameter.");
			return;
		}
		
		// Create parser and start parsing.
		try {
			Parser parser = new Parser(args[0]);
			
			// Call with the starting non-terminal symbol.
			parser.program();
			
		} catch (IOException e) {
			System.out.println("Error reading input file. " + e.getMessage());
			System.exit(1);
		}
	}

}
