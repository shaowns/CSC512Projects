import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * Owner: ShaownS
 * File: Parser.java
 * Package: 
 * Project: CSC512CodeGenerator
 * Email: ssarker@ncsu.edu
 */

/**
 * 
 */
public class Parser {
	
	/*
	 * Class to hold the location of a particular symbol.
	 * Gives us the index and which array to look into.
	 */
	private static class SymbolLocation {
		private int index;
        private boolean isGlobal;
        
        public SymbolLocation(int i, boolean b) {
            this.index = i;
            this.isGlobal = b;
        }
        
        public int getIndex() {
        	return index;
        }
        
        public boolean isGlobal() {
        	return isGlobal;
        }
        
        public String getSymbolString() {
        	if (index != -1) {
	        	if (isGlobal) {
	        		return "global[" + Integer.toString(index) + "]";
	        	}
	        	return "local[" + Integer.toString(index) + "]";
        	}
        	return "N/A";
        }
	}
	
	/*
	 * Class for generated code for the function body.
	 * This contains a string builder which accumulates
	 * the intermediate code and when applicable the
	 * equivalent symbol for an expression.
	 */
	public static class GeneratedCode {
		private StringBuilder code;
	    private String equivalent;
	    
	    public GeneratedCode() {
	    	this.code = new StringBuilder();
	    	this.equivalent = "";
	    }
	    
	    public String getCode() {
	    	return code.toString();
	    }
	    
	    public String getEquivalent() {
	    	return equivalent;
	    }
	    
	    public void addCode(String s) {
	    	if (s.length() > 0) {
	    		code.append(s);
	    	}
	    }
	    
	    public void setEquivalent(String s) {
	    	equivalent = s;
	    }
	}
	
	private Scanner scanner;
	private ArrayList<Scanner.Token> lookAheadTokens;
	private Scanner.Token word;
	
	// Input file name.
	private String inputFile;
	
	// Builder for the generated code. We print this to the file.
	private StringBuilder outputCode;
	
	// Container for global variables.
	private ArrayList<String> globals;
	
	// Container for local variables, should be cleared when exiting function scope.
	private ArrayList<String> locals;
	
	// Maintains the current while loops' (possibly nested) labels in code. Should be synchronized all the time.
	// We need both starts and ends of while loop, so that continue and break statements can be translated.
	ArrayList<String> loopStarts;
	ArrayList<String> loopEnds;
	
	// Check if are in a loop.
	private boolean isInsideLoop() {
		return loopStarts.size() > 0 && loopEnds.size() > 0;
	}
	
	// Add a new loop labels.
	private void addLoopLabels(String start, String end) {
		loopStarts.add(start);
		loopEnds.add(end);
	}
	
	// Remove a the last loops labels when it's scope ends.
	private void removeLastLoop() {
		loopStarts.remove(loopStarts.size() - 1);
		loopEnds.remove(loopEnds.size() - 1);
	}
	
	// Label counter.
	private int labelCounter;
	
	// Flag to determine whether to wrap numerical factor in local variable.
	private boolean wrapFactorInLocalVar;
	
	public Parser(String fileName) throws FileNotFoundException, IOException {
		this.inputFile = fileName;
		this.scanner = new Scanner(fileName);
		this.lookAheadTokens = new ArrayList<Scanner.Token>();
		this.outputCode = new StringBuilder();
		
		this.globals = new ArrayList<String>();
		this.locals = new ArrayList<String>();
		this.labelCounter = 0;
		this.wrapFactorInLocalVar = false;
		
		this.loopStarts = new ArrayList<String>();
		this.loopEnds = new ArrayList<String>();
	}
	
	// Get the next valid label to be used.
	private String getNextLabel() {
		return "c" + Integer.toString(++labelCounter);
	}
	
	// Finds the given id or expression in locals.
	private SymbolLocation findVarIndex(String code, boolean isArray) {
		if (isArray) {
			// Find the first index position.
			code += "[0]"; 
		}
		
		// Check the locals.
		for (int i = 0; i < locals.size(); i++) {
			if (locals.get(i).equals(code)) {
				return new SymbolLocation(i, false);
			}
		}
		
		for (int i = 0; i < globals.size(); i++) {
			if (globals.get(i).equals(code)) {
				return new SymbolLocation(i, true);
			}
		}
		
		// Add it to the locals then.
		addSymbol(code, false);		
		return new SymbolLocation(locals.size() - 1, false);
	}
	
	// Adds the symbol (identifier or expression) to the local or global symbol list.
	// Also gives the assignment code necessary for this.
	private String addSymbol(String s, boolean isGlobal) {
		String code = "";
		if (isGlobal) {
			globals.add(s);
			code += System.lineSeparator() + "global[" + Integer.toString(globals.size() - 1) + "] = " + s + ";";
		} else {
			locals.add(s);
			code += System.lineSeparator() + "local[" + Integer.toString(locals.size() - 1) + "] = " + s + ";";
		}
		
		return code; 
	}
	
	// Print local variable at specific position.
	private String printLocalVariable(int i) {
		return System.lineSeparator() + "local[" + Integer.toString(i) + "] = " + locals.get(i) + ";";
	}
	
	// Print last local variable.
	private String getLastLocalVariable() {
		return "local[" + Integer.toString(locals.size() - 1) + "]";
	}
	
	// Returns all local variable statements, assuming no arrays in locals at this point.
	private String localVarStatements() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < locals.size(); i++) {
			sb.append(printLocalVariable(i));
		}
		return sb.toString();
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
				
				// Copy over the meta statements.
				if (token.getTokenType() == Scanner.TokenType.META_STATEMENT) {
					outputCode.append(token.getTokenName() + System.lineSeparator());
				}								
			}
		} while (token != null && token.getTokenType() == Scanner.TokenType.META_STATEMENT);
		
		return token;
	}
	
	public void fail() {
		System.out.println("Fail");
		System.out.println("Failed token: " + word.getTokenName());		
	}
	
	public void success() {
		// Writer for the generated output file.
		PrintWriter outputWriter = null;		
		
		try {
			// Output file writer. Note this will wipe out the file if exists, so every
			// new scanner run will produce the output file again.
			outputWriter = new PrintWriter(Scanner.getResultFilename(inputFile));
			outputWriter.write(getGeneratedCode());			
		} catch (IOException e) {
			System.out.println("Error reading input file. " + e.getMessage());
			System.exit(1);
		} finally {
			outputWriter.close();
		}
	}
	
	/*
	 * <program> --> <data decls> <func list>
	 */
	public void program() {
		word = nextWord();
		
		// Generated code from each rule.
		if (data_decls(true)) {
			
			// Add the global data declaration code to the generated code.
			if (globals.size() > 0) {
				outputCode.append("int global[" + globals.size() + "];" + System.lineSeparator());
			}
			
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
				
				// Add the function declaration to generated code verbatim.
				outputCode.append(word.getTokenName());
				word = nextWord();
				
				if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
					return false;
				}
				
				outputCode.append(" " + word.getTokenName());
				word = nextWord();
				
				if (!word.getTokenName().equals("(")) {
					return false;
				}			
				
				outputCode.append(" " + word.getTokenName());
				word = nextWord();
				
				if (!parameter_list()) {
					return false;
				}
				
				if (!word.getTokenName().equals(")")) {
					return false;
				}			
				
				outputCode.append(" " + word.getTokenName());
				word = nextWord();
				
				if (!func_prime()) {
					return false;
				}
				
				// Clear out the local variables as we exit function scope.
				locals.clear();
				
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
			outputCode.append(" " + word.getTokenName() + System.lineSeparator());
			word = nextWord();
			
			// Clear out the local variables if any were added, since this is a function declaration rather than definition.
			locals.clear();
			
			return true;
		} else if (word.getTokenName().equals("{")) {
			outputCode.append(System.lineSeparator() + word.getTokenName() + System.lineSeparator());
			word = nextWord();
			
			// Use a local builder.
			StringBuilder funcSb = new StringBuilder();						
			
			// Add the local variable declarations for parameters so far for the parameters.
			funcSb.append(localVarStatements());
			
			if (!data_decls(false)) {
				return false;
			}
			
			// Get the generated code for statements.
			GeneratedCode funcCode = new GeneratedCode();
			if (!statements(funcCode)) {
				return false;
			}
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}
			
			if (locals.size() > 0) {
				funcSb.insert(0, "int local[" + Integer.toString(locals.size()) + "];");
			}
			
			// Add the generated code to the builder.
			funcSb.append(funcCode.getCode());
			
			// Append the function contents to the original.
			outputCode.append(funcSb.toString());
			
			outputCode.append(System.lineSeparator() + word.getTokenName() + System.lineSeparator() + System.lineSeparator());
			word = nextWord();
			
			// Clear out the local variables if any were added, 
			// since we are going out of the scope.
			locals.clear();
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
			// Add to the output code.
			outputCode.append(" " + word.getTokenName());
			word = nextWord();
			
			return parameter_list_prime();
		} else if (word.getTokenName().equals("int") || word.getTokenName().equals("binary") || word.getTokenName().equals("decimal")) {
			
			outputCode.append(" " + word.getTokenName());
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			
			outputCode.append(" " + word.getTokenName());

			// Add this to the list of locals.
			addSymbol(word.getTokenName(), false);
			
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
			outputCode.append(" " + word.getTokenName());
			
			// Add this to the list of locals.
			addSymbol(word.getTokenName(), false);
			
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
			outputCode.append(word.getTokenName());
			
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
			
			outputCode.append(" " + word.getTokenName());
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			
			outputCode.append(" " + word.getTokenName());
			
			// Add this to the list of locals.
			addSymbol(word.getTokenName(), false);
			
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
	
	public boolean data_decls(boolean isGlobal) {
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
				if (peek1.getTokenType() == Scanner.TokenType.IDENTIFIER && peek2.getTokenName().equals("(")) {
					return true;
				}
				
				// Safe to consume word.
				word = nextWord();
				
				if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
					return false;
				}
				
				// Get the identifier name so that we can pass it down.
				String id = word.getTokenName();
				
				// Consume the word.
				word = nextWord();			
				
				if (!id_prime(id, isGlobal)) {
					return false;
				}
				
				if (!id_list_prime(isGlobal)) {
					return false;
				}
				
				if (!word.getTokenName().equals(";")) {
					return false;
				}
				
				word = nextWord();
				
				return data_decls(isGlobal);
		} else  {
			return false;
		}
	}
	
	/*
	<id prime> --> left_bracket <expression> right_bracket
	<id prime> --> empty
	*/
	public boolean id_prime(String id, boolean isGlobal) {
		if (word.getTokenName().equals("[")) {
			word = nextWord();
			
			// Get the expression value.
			GeneratedCode eg = new GeneratedCode();
			if (!expression(eg)) {
				return false;
			}
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}			
			word = nextWord();
			
			try {
				// If we are given a number as the expression equivalent,
				// then we are okay, otherwise we must exit and show error.
				int arraySize = Integer.parseInt(eg.getEquivalent());
				
				// Add each of the array member to the locals.
				for (int i = 0; i < arraySize; i++) {
					addSymbol(id + "[" + Integer.toString(i) + "]", isGlobal);
				}
			} catch (NumberFormatException e) {
				System.out.println("Non integer array size used for array: " + id);
			}
			
			return true;
		} else if (word.getTokenName().equals(",") || word.getTokenName().equals(";")) {
			// <id prime> --> empty
			// First+: {empty, comma, semicolon}
			
			// Add the symbol.
			addSymbol(id, isGlobal);
			
			return true;			
		} else {
			return false;
		}
	}
	
	/*
	<id list prime> --> comma ID <id prime> <id list prime>
	<id list prime> --> empty
	*/
	public boolean id_list_prime(boolean isGlobal) {
		if (word.getTokenName().equals(",")) {
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			
			// Get the identifier name so that we can pass it down.
			String id = word.getTokenName();
			
			// Consume the word.
			word = nextWord();
			
			if (!id_prime(id, isGlobal)) {
				return false;
			}
			
			return id_list_prime(isGlobal);
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
	public boolean statements(GeneratedCode g) {
		if (word.getTokenType() == Scanner.TokenType.IDENTIFIER) {
			// Get the identifier.
			String id = word.getTokenName();
			
			word = nextWord();
			
			// Get the generated code for the rule.
			if (!statement_prime(id, g)) {
				return false;
			}
			
			// Continuously add the code for other statements.
			return statements(g);
		} else if (word.getTokenName().equals("if")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			// Get the generated code for condition expression.
			if (!condition_expression(g)) {
				return false;
			}
			
			// Get the conditional equivalent.
			String condEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("{")) {
				return false;
			}
			word = nextWord();
			
			// Also get the labels for the if statement.
			String ifLabel = getNextLabel();
			String ifNotLabel = getNextLabel();
			
			// Add code for the if statement.
			g.addCode(System.lineSeparator() + "if ( " + condEquivalent + " ) goto " + ifLabel + ";");
			g.addCode(System.lineSeparator() + "goto " + ifNotLabel + ";");
			g.addCode(System.lineSeparator() + ifLabel + ": ;");
			
			// Get the generated code for the statements inside if.
			if (!statements(g)) {
				return false;
			}
			
			g.addCode(System.lineSeparator() + ifNotLabel + ": ;");
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}			
			
			// Continuously add the code for other statements.
			word = nextWord();
			return statements(g);
		} else if (word.getTokenName().equals("while")) {
			// Get the labels for the equivalent if statement and the loop start.
			// The label for if not is the current loop's end label.
			String loopStart = getNextLabel();
			String ifLabel = getNextLabel();
			String ifNotLabel = getNextLabel();
			
			// Add the loop.
			addLoopLabels(loopStart, ifNotLabel);
			
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			// Add the code for the while loop, so that it encapsulates the contents of the conditional expression.
			g.addCode(System.lineSeparator() + loopStart + ": ;");
			
			// Get the conditional expressions generated code.
			if (!condition_expression(g)) {
				return false;
			}
			
			// Get the conditional expression equivalent.
			String condEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("{")) {
				return false;
			}
			word = nextWord();
			
			// Add the if condition for the loop.
			g.addCode(System.lineSeparator() + "if ( " + condEquivalent + " ) goto " + ifLabel + ";");
			g.addCode(System.lineSeparator() + "goto " + ifNotLabel + ";");
			g.addCode(System.lineSeparator() + ifLabel + ": ;");
			
			// Get the generated code for while statements.
			if (!statements(g)) {
				return false;
			}
			
			// Add the rest of the loop.
			g.addCode(System.lineSeparator() + "goto " + loopStart + ";");
			g.addCode(System.lineSeparator() + ifNotLabel + ": ;");
			
			if (!word.getTokenName().equals("}")) {
				return false;
			}
			
			// Our loop has ended, remove the loop's labels.
			removeLastLoop();
			
			// Continuously add the code for other statements.
			word = nextWord();
			return statements(g);
		} else if (word.getTokenName().equals("return")) {
			word = nextWord();
			
			// Get the generated code for return.
			if (!return_statement_prime(g)) {
				return false;
			}
			
			// Continuously add the code for other statements.
			return statements(g);
		} else if (word.getTokenName().equals("break")) {
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Found a break. Check if we are inside a loop.
			if (isInsideLoop()) {
				String loopEnd = loopEnds.get(loopEnds.size() - 1);
				g.addCode(System.lineSeparator() + "goto " + loopEnd + ";");
			} else {
				System.out.println("break statement used outside loop. Quiting.");
				System.exit(1);
			}
			
			word = nextWord();
			
			// Continuously add the code for other statements.
			return statements(g);
		} else if (word.getTokenName().equals("continue")) {
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Found a break. Check if we are inside a loop.
			if (isInsideLoop()) {
				String loopStart = loopStarts.get(loopStarts.size() - 1);
				g.addCode(System.lineSeparator() + "goto " + loopStart + ";");
			} else {
				System.out.println("continue statement used outside loop. Quiting.");
				System.exit(1);
			}
			
			word = nextWord();
			
			// Continuously add the code for other statements.
			return statements(g);
		} else if (word.getTokenName().equals("read")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.IDENTIFIER) {
				return false;
			}
			
			// Save the identifier.
			String id = word.getTokenName();
			word = nextWord();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Get the identifier location in locals.
			SymbolLocation pos = findVarIndex(id, false);
			
			// Add the code for the read statement.
			g.addCode(System.lineSeparator() + "read ( " + pos.getSymbolString() + " );");
			
			word = nextWord();
			
			// Continuously add generated code over statements.
			return statements(g);
		} else if (word.getTokenName().equals("write")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			// Get the expression code.
			if (!expression(g)) {
				return false;
			}
			
			// Get the write parameter equivalent.
			String writeParam = g.getEquivalent();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Add the write statement code.
			g.addCode(System.lineSeparator() + "write ( " + writeParam + " );");
			
			word = nextWord();
			
			// Continuously add generated code over statements.
			return statements(g);
		} else if (word.getTokenName().equals("print")) {
			word = nextWord();
			
			if (!word.getTokenName().equals("(")) {
				return false;
			}
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.STRING) {
				return false;
			}
			
			// Save the string to print.
			String printStr =  word.getTokenName();
			word = nextWord();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Add the code for print string.
			g.addCode(System.lineSeparator() + "print ( " + printStr + " );");
			
			word = nextWord();
			
			// Continuously add generated code over statements.
			return statements(g);
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
	public boolean statement_prime(String id, GeneratedCode g) {
		if (word.getTokenName().equals("=")) {
			word = nextWord();
			
			// Generated code for expression.
			if (!expression(g)) {
				return false;
			}
			
			// Get the right hand side expression equivalent.
			String rhsEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Assigning an expression to the identifier.
			// Get the identifiers location.
			SymbolLocation pos = findVarIndex(id, false);
			
			// Add the code for the assignment.
			g.addCode(System.lineSeparator() + pos.getSymbolString() + " = " + rhsEquivalent + ";");			
			
			word = nextWord();			
			return true;
		} else if (word.getTokenName().equals("[")) {
			word = nextWord();
			
			// The array offset.
			if (!expression(g)) {
				return false;
			}
			
			// Get the array offset expression equivalent.
			String arrayOffset = g.getEquivalent();
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals("=")) {
				return false;
			}
			word = nextWord();
			
			// The expression to be assigned generated code.
			if (!expression(g)) {
				return false;
			}
			
			// Get the right hand side expression equivalent to be used.
			String  rhsEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Assigning expression to an array offset.
			// Find the base of the array.
			SymbolLocation base = findVarIndex(id, true);
			
			// Add base + expression equivalent to local variables.
			String code = addSymbol(Integer.toString(base.getIndex()) + " + " + arrayOffset, false);
			g.addCode(code);
			
			// Now dereference the array element.
			String lhsSymbol;
			if (base.isGlobal()) {
				lhsSymbol = "global";
			} else {
				lhsSymbol = "local";
			}
			
			lhsSymbol += "[" + getLastLocalVariable()  + "]";			
			
			// Finally add the code for the expression assignment.
			g.addCode(System.lineSeparator() + lhsSymbol + " = " + rhsEquivalent + ";");
			
			word = nextWord();			
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			// Turn the flag on so that all code generated are local variables.
			wrapFactorInLocalVar = true;
			
			// Expression list generated code.
			if (!expr_list(g)) {
				return false;
			}
			
			// Get the function call parameter list equivalent.
			String paramList = g.getEquivalent();
			
			// Turn the flag off, we are done.
			wrapFactorInLocalVar = false;
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}
			word = nextWord();
			
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// An stand-alone function call.
			g.addCode(System.lineSeparator() + id + " ( " + paramList + " );");
			
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
	public boolean expr_list(GeneratedCode g) {
		if (expression(g)) {
			return ne_expr_list_prime(g);
		} else if (word.getTokenName().equals(")")) {
			// <expr list> --> empty
			// First+: {empty, right_parenthesis}
			
			// Clear out the equivalent if any, since this was empty.
			g.setEquivalent("");
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<ne expr list prime> --> comma <expression> <ne expr list prime>
	<ne expr list prime> --> empty
	*/
	public boolean ne_expr_list_prime(GeneratedCode g) {
		if (word.getTokenName().equals(",")) {
			word = nextWord();
			
			// Save the current expression list.
			String currentList = g.getEquivalent();
			
			// Get the next expression in the list.
			if (!expression(g)) {
				return false;
			}
			
			// Add the generated code to the left side generated code.
			g.setEquivalent(currentList + ", " + g.getEquivalent());
			
			return ne_expr_list_prime(g);
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
	public boolean condition_expression(GeneratedCode g) {
		if (condition(g)) {
			return condition_expression_prime(g);
		} else {
			return false;
		}
	}
	
	/*
	<condition expression prime> --> double_and_sign <condition> 
	<condition expression prime> --> double_or_sign <condition> 
	<condition expression prime> --> empty
	*/
	public boolean condition_expression_prime(GeneratedCode g) {
		if (word.getTokenName().equals("&&") || word.getTokenName().equals("||")) {
			// Save the operator.
			String op = word.getTokenName();
			
			word = nextWord();
			
			// Save the left hand side equivalent.
			String lhsEquivalent = g.getEquivalent();
			
			// Get the right hand side code and equivalent.
			if (condition(g)) {
				// Add the code for the conditional operation.
				String rhsEquivalent = g.getEquivalent();
				String code = addSymbol("( " + lhsEquivalent + " " + op + " " + rhsEquivalent + " )", false);
				g.addCode(code);
				g.setEquivalent(getLastLocalVariable());
				
				return true;
			}
			return false;							
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
	public boolean condition(GeneratedCode g) {
		if (expression(g)) {
			return condition_prime(g);
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
	public boolean condition_prime(GeneratedCode g) {
		if (word.getTokenName().equals("==") || word.getTokenName().equals("!=")
				|| word.getTokenName().equals(">") || word.getTokenName().equals(">=")
				|| word.getTokenName().equals("<") || word.getTokenName().equals("<=")) {
			
			// Save the operator.
			String op = word.getTokenName();
			
			word = nextWord();
			
			// Save the left hand side equivalent.
			String lhsEquivalent = g.getEquivalent();
			
			// The right hand side generated code.
			if (expression(g)) {
				// Add the code for the conditional operation.
				String rhsEquivalent = g.getEquivalent();
				String code = addSymbol("( " + lhsEquivalent + " " + op + " " + rhsEquivalent + " )", false);
				g.addCode(code);
				g.setEquivalent(getLastLocalVariable());
				
				return true;
			}
			return false;				
		} else {
			return false;
		}
	}
	
	/*
	<return statement prime> --> <expression> semicolon
	<return statement prime> --> semicolon
	*/
	public boolean return_statement_prime(GeneratedCode g) {
		if (expression(g)) {
			if (!word.getTokenName().equals(";")) {
				return false;
			}
			
			// Add the expression code.
			String retEquivalent = g.getEquivalent();
			
			// Wrap the equivalent into another local variable.
			String code = addSymbol(retEquivalent, false);
			g.addCode(code);			
			g.addCode(System.lineSeparator() + "return " + getLastLocalVariable() + ";");
			
			word = nextWord();			
			return true;
		} else if (word.getTokenName().equals(";")) {
			// Simple return statement.
			g.addCode(System.lineSeparator() + "return;");
			
			word = nextWord();			
			return true;
		} else {
			return false;
		}
	}
	
	/*
	<expression> --> <term> <expression prime>
	*/
	public boolean expression(GeneratedCode g) {
		if (term(g)) {
			return expression_prime(g);
		} else {
			return false;
		}
	}
	
	/*
	<expression prime> --> plus_sign <term> <expression prime>
	<expression prime> --> minus_sign <term> <expression prime>
	<expression prime> --> empty
	*/
	public boolean expression_prime(GeneratedCode g) {
		if (word.getTokenName().equals("+") || word.getTokenName().equals("-")) {
			
			// Save the operator.
			String op = word.getTokenName();
			
			word = nextWord();
			
			// Save the left hand side equivalent.
			String lhsEquivalent = g.getEquivalent();
			
			// Get the right hand side.
			if (!term(g)) {
				return false;
			}
			
			// Add the code for addition/subtraction on the left hand side.
			String rhsEquivalent = g.getEquivalent();
			String code = addSymbol(lhsEquivalent + " " + op + " " + rhsEquivalent, false);
			g.addCode(code);
			g.setEquivalent(getLastLocalVariable());
			
			return expression_prime(g);
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
	public boolean term(GeneratedCode g) {
		if (factor(g)) {
			return term_prime(g);
		} else {
			return false;
		}
	}
	
	/*
	<term prime> --> star_sign <factor> <term prime>
	<term prime> --> forward_slash <factor> <term prime>
	<term prime> --> empty
	*/
	public boolean term_prime(GeneratedCode g) {
		if (word.getTokenName().equals("*") || word.getTokenName().equals("/")) {
			// Save the operator.
			String op = word.getTokenName();
			
			word = nextWord();
			
			// Save the left hand side.
			String lhsEquivalent = g.getEquivalent();
			
			// Get the right hand side.
			if (!factor(g)) {
				return false;
			}
			
			// Add the code for multiplication/division to the left hand side.
			String rhsEquivalent = g.getEquivalent();
			String code = addSymbol(lhsEquivalent + " " + op + " " + rhsEquivalent, false);
			g.addCode(code);
			g.setEquivalent(getLastLocalVariable());
			
			return term_prime(g);
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
	public boolean factor(GeneratedCode g) {
		if (word.getTokenType() == Scanner.TokenType.IDENTIFIER) {
			// Array dereference, function call, or just identifier.

			// Get the identifier part.
			String id = word.getTokenName();
			
			// Consume the word.
			word = nextWord();	
			
			return factor_prime(id, g);
		} else if (word.getTokenType() == Scanner.TokenType.NUMBER) {
			
			// Check if we have to wrap the number in a local variable.
			if (wrapFactorInLocalVar) {
				String code = addSymbol(word.getTokenName(), false);
				g.addCode(code);
				g.setEquivalent(getLastLocalVariable());
			} else {
				// This is a number, place it in the equivalent directly.
				g.setEquivalent(word.getTokenName());
			}
			
			word = nextWord();			
			return true;
		} else if (word.getTokenName().equals("-")) {
			word = nextWord();
			
			if (word.getTokenType() != Scanner.TokenType.NUMBER) {
				return false;
			}
			
			// Check if we have to wrap the number in a local variable.
			if (wrapFactorInLocalVar) {
				String code = addSymbol("-" + word.getTokenName(), false);
				g.addCode(code);
				g.setEquivalent(getLastLocalVariable());
			} else {
				// This is a negative number, place it in the equivalent directly.
				g.setEquivalent("-" + word.getTokenName());
			}
			
			word = nextWord();
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			// This is an expression, directly pass the generated code.
			if (!expression(g)) {
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
	public boolean factor_prime(String id, GeneratedCode g) {
		if (word.getTokenName().equals("[")) {
			
			// Array dereference. 
			word = nextWord();
			
			if (!expression(g)) {
				return false;
			}
			
			// Get the expression equivalent.
			String exprEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals("]")) {
				return false;
			}			
			word = nextWord();
			
			// Find the base of the array.
			SymbolLocation base = findVarIndex(id, true);
			
			// Add base + expression equivalent to local variables.
			String code = addSymbol(Integer.toString(base.getIndex()) + " + " + exprEquivalent, false);
			g.addCode(code);
			
			// Now dereference the array element in a new local variable.
			String symbolToAdd;
			if (base.isGlobal()) {
				symbolToAdd = "global";
			} else {
				symbolToAdd = "local";
			}
			
			symbolToAdd += "[" + getLastLocalVariable() + "]";			
			code = addSymbol(symbolToAdd, false);
			g.addCode(code);
			g.setEquivalent(getLastLocalVariable());
			
			return true;
		} else if (word.getTokenName().equals("(")) {
			word = nextWord();
			
			if (!expr_list(g)) {
				return false;
			}
			
			// Get the expression list equivalent.
			String exprListEquivalent = g.getEquivalent();
			
			if (!word.getTokenName().equals(")")) {
				return false;
			}			
			word = nextWord();
			
			// Is a function call, add it to the locals.
			String code = addSymbol(id + " ( " + exprListEquivalent + " ) ", false);
			g.addCode(code);
			g.setEquivalent(getLastLocalVariable());
			
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
			
			// Just an identifier, return the corresponding local variable.
			SymbolLocation index = findVarIndex(id, false);
			g.setEquivalent(index.getSymbolString());			
			return true;
		} else {
			return false;
		}
	}
	
	public String getGeneratedCode() {
		return outputCode.toString();
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
