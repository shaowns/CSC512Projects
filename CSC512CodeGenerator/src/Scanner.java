import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Owner: ShaownS
 * File: Scanner.java
 * Package: 
 * Project: CSC512Scanner
 * Email: ssarker@ncsu.edu
 */

/**
 * Simple lexical Scanner for CSC512 class, identifies token is input file and
 * outputs in a result file augmenting identifier tokens with the cs512 prefix.
 */
public class Scanner {
	
	public static String scanner_error_prefix = "Error in Scanner: ";
	
	public static enum TokenType {
        // Our language has these token types.
        IDENTIFIER, NUMBER, RESERVED_WORD, SYMBOL, STRING, META_STATEMENT;
    }
	
	// Our simple token class, has a type and token string or name.
    public static class Token {
        private TokenType type;
        private String tokenString;
        
        public Token() {
        	
        }
        
        public Token(TokenType t, String s) {
            this.type = t;
            this.tokenString = s;
        }
        
        public TokenType getTokenType() {
        	return type;
        }
        
        public String getTokenName() {
        	return tokenString;
        }
    }

    // Constructor. Must give a file name, throws exceptions based on the file
    // reading or attribute issues like file is a directory and does not exist.
	public Scanner(String srcFilename) throws FileNotFoundException, IOException {
    	this.curTokenBuilder = new StringBuilder();
    	this.nextTokenChar = Character.MIN_VALUE;
    	this.nextToken = null;
    	
    	// Start processing the input file.
    	InputStream srcStream = new FileInputStream(srcFilename);
        InputStreamReader streamReader = new InputStreamReader(srcStream, Charset.defaultCharset());
        
        // Use a buffered reader for efficiency.
        inputReader = new BufferedReader(streamReader);
    }
	
	// Looks ahead for tokens in file, if there is one caches it.
	// Can be called multiple times, would not lose tokens. Must
	// call before calling getNextToken, otherwise program quits
	// with error code. This is done to enforce the coupling between
	// these two functions. If this function returns true, then the
	// next getNextToken is guaranteed to return a valid token.
	public boolean hasMoreTokens() {
    	// Check if there is already a token cached or not.
		if (this.nextToken == null) {    		
    		// Scan the next token in file.
    		Token nextTokenInFile = scanNextTokenInFile();
    		if (nextTokenInFile == null) {
	    		// We have reached the white spaces
				// at the end of the file while scanning.
				return false;
    		}
    		
    		// Otherwise cache it for easy retrieval.
    		this.nextToken = nextTokenInFile;
		}
		
		return true;
    }
	
	// Should only be called after checking by hasMoreTokens.
	// Otherwise will stop the program with error code. Gets
	// the cached token that was fetched by the hasMoreTokens call.
	public Token getNextToken() {
		if (this.nextToken == null) {
			// Someone called this without checking for hasMoreTokens.
			// Should not have done that.
			System.exit(1);
		}
		
		// Return the cached one and empty the cache. So, that next
		// call to hasMoreTokens retrieves and caches the next token.
		Token retToken = this.nextToken;
		this.nextToken = null;		
		return retToken;
	}
    
	// Retrieves the next token in source file. Will
	// return null, if the end of the file has been 
	// reached and we don't have a token. Will start
	// with either the character saved from last matching
	// or the next character in the source file.
    private Token scanNextTokenInFile() {
    	Token nextToken = null;
    	int readValue = -1;
    	while (nextToken == null) {
	    	// Check if we have a character left from previous
	    	// match. Otherwise read one from the file.
    		char ch;
    		if (nextTokenChar != Character.MIN_VALUE) {
    			ch = nextTokenChar;
    		} else {
    			readValue = getNextCharValue();
    			if (readValue == -1) {
    				// Reached end of file. Don't have any more to scan.
    				break;
    			}
    			ch = (char) readValue;
    		}
    			
			// Get the next token starting from the initial state.
			nextToken = scanTokenFromStart(ch);		
    	}
    	
    	return nextToken;
    }

    // Will get the next token matching with the starting 
    // character. Returns null if the end of file has been
    // reached. Functionally, the starting state of all tokens, 
    // goes into different matching states based on the character
    // read. If the first non-whitespace character is not one
    // of the characters that begins a token, then it's an 
    // invalid one and the program halts.
	private Token scanTokenFromStart(char ch) {
		// Consume white space characters.
		while (isWhitespace(ch)) {
			int readValue = getNextCharValue();
			if (readValue == -1) {
				// Reached end of file while reading white spaces.
				return null;
			}			
			ch = (char) readValue;
		}
		
		// Clear the token builder, since we are starting a new match.
		curTokenBuilder.setLength(0);
		
		// Append the character to the token builder and start matching.
		curTokenBuilder.append(ch);		
		
		if (isLetter(ch)) {
			// Identifiers or reserved words.
			return matchIdentifierOrReserved();
		} else if (isDigit(ch)) {
			// Numbers.
			return matchNumber();
		} else if (ch == '"') {
			// Strings.
			return matchStrings();
		} else if (ch == '#') {
			// Meta statement.
			return matchMetaStatements();
		} else if (ch == '/') {
			// Can be meta statement if there is another '/' or a single forward slash symbol.
			return matchMetaStatementOrSymbol();
		}  else if (isSingleCharSymbol(ch)) {
			/*
			 * Single character symbols that can not be followed by anything.
			 * left_parenthesis | right_parenthesis | left_brace | right_brace | left_bracket | right_bracket | 
			 * comma | semicolon | plus_sign | minus_sign | star_sign | forward_slash
			 */
			// Token was matched, no more characters for the next token.
			this.nextTokenChar = Character.MIN_VALUE;
			return new Token(TokenType.SYMBOL, curTokenBuilder.toString());
		} else if (isAssignOperatorSymbol(ch)) {
			/*
			 * Operator symbols that can be followed by = or not.
			 * assign (=) | greater_than (<) || lesser_than(>)
			 */
			return matchAssignOperatorSymbols();
		} else if (ch == '!') {
			/*
			 * ! can only be followed by =.
			 * Anything else makes an invalid token.
			 */
			return matchNotEqualSymbol();
		} else if (ch == '&') {
			/*
			 * & can only be followed by another &.
			 * Anything else makes an invalid token.
			 */
			return matchLogicalAnd();
		} else if (ch == '|') {
			/*
			 * | can only be followed by another |.
			 * Anything else makes an invalid token.
			 */
			return matchLogicalOr();
		} 
		else {
			// Not identified character. Reject token.
			System.out.println(scanner_error_prefix + "Invalid token in source file " + Character.toString(ch));
			System.exit(1);
		}
		
		// Should never reach this.
		return null;
	}
    
	/* Each match*** function is a FA that matches certain type(s) of tokens.
	 * When they are done, the scanner is ready for another scan for new token.
	 * The next scan can have one character that was read from the last matching,
	 * which is cached in the nextTokenChar member.
	 */
	
	// Matches &&, a single & is not a symbol in our language. So
    // if a & is not followed by another, then it's an invalid token.
    private Token matchLogicalAnd() {
    	// Flag to check if the contents of the builder is valid or not.
    	boolean tokenMatched = false;
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	if (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// If this character is a '&' then it's logical and symbol,
    		// otherwise this is an invalid token.
        	if (ch == '&') {
				curTokenBuilder.append(ch);
				tokenMatched = true;
				
				// Token was matched, no more characters left for the next one.
				this.nextTokenChar = Character.MIN_VALUE;
			} else {
				// Character for next token.
				this.nextTokenChar = ch;
			}
    	}
    	
    	// We have found a valid token or reached the end of file. If there was a match then
    	// return the token. Otherwise if there is content in the token builder, it's an invalid token.
    	if (tokenMatched) {
    		matchedToken = new Token(TokenType.SYMBOL, curTokenBuilder.toString());
		} else if (curTokenBuilder.length() > 0) {
			System.out.println(scanner_error_prefix + "Invalid token in source file: " + curTokenBuilder.toString());
			System.exit(1);
		}
    	
    	return matchedToken;
    }
    
    // Matches ||, a single | is not a symbol in our language. So
    // if a | is not followed by another, then it's an invalid token.
    private Token matchLogicalOr() {
    	// Flag to check if the contents of the builder is valid or not.
    	boolean tokenMatched = false;    	
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	if (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// If this character is a '|' then it's logical or symbol,
    		// otherwise this is an invalid token.
        	if (ch == '|') {
				curTokenBuilder.append(ch);
    			tokenMatched = true;
    			
    			// Token was matched, no more characters left for the next one.
				this.nextTokenChar = Character.MIN_VALUE;
			} else {
				// Character for next token.
				this.nextTokenChar = ch;
    		}
    	}
    	
    	// We have found a valid token or reached the end of file. If there was a match then
    	// return the token. Otherwise if there is content in the token builder, it's an invalid token.
    	if (tokenMatched) {
    		matchedToken = new Token(TokenType.SYMBOL, curTokenBuilder.toString());
		} else if (curTokenBuilder.length() > 0) {
			System.out.println(scanner_error_prefix + "Invalid token in source file: " + curTokenBuilder.toString());
			System.exit(1);
		}
    	
    	return matchedToken;
    }
    
    // Matches !=, in our language ! is not a symbol. So, any !
    // encountered must be followed by a =, otherwise it's invalid.
    private Token matchNotEqualSymbol() {
    	// Flag to check if the contents of the builder is valid or not.
    	boolean tokenMatched = false;
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	if (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// If this character is a '=' then it's not equal symbol,
    		// otherwise this is an invalid token.
        	if (ch == '=') {
				curTokenBuilder.append(ch);
				tokenMatched = true;
    			
				// Token was matched, no more characters left for the next one.
				this.nextTokenChar = Character.MIN_VALUE;
			} else {
				// Character for next token.
				this.nextTokenChar = ch;
    		}
    	}
    	
    	// We have found a valid token or reached the end of file. If there was a match then
    	// return the token. Otherwise if there is content in the token builder, it's an invalid token.
    	if (tokenMatched) {
    		matchedToken = new Token(TokenType.SYMBOL, curTokenBuilder.toString());
		} else if (curTokenBuilder.length() > 0) {
			System.out.println(scanner_error_prefix + "Invalid symbol token in file: " + curTokenBuilder.toString());
			System.exit(1);
		}
    	
    	return matchedToken;
    }
    
    // Matches those symbols that can be a symbol on their own,
    // or have a = followed (=, <, >). So, if these are followed
    // by a =, we take the longest match. Otherwise we keep the 
    // read character for the next token, and have the previous
    // one returned as a symbol token.
    private Token matchAssignOperatorSymbols() {
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	if (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// If this character is a '=' then it's assign operator symbol,
    		// otherwise the previous character was a single symbol.
        	if (ch == '=') {
				curTokenBuilder.append(ch);
				
				// Token was matched, no more characters left for the next one.
				this.nextTokenChar = Character.MIN_VALUE;
    							
			} else {    			
    			// Character for next token.
				this.nextTokenChar = ch;
    		}
    	}
    	
    	// We have found a valid token or reached the end of file.
    	matchedToken = new Token(TokenType.SYMBOL, curTokenBuilder.toString());
		return matchedToken;
    }
    
    // Matches meta statements that begins with // or the symbol '/'.
    private Token matchMetaStatementOrSymbol() {
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	if (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// If this character is also a '/' then it's a meta statement,
    		// otherwise the previous character was a forward slash symbol.
        	if (ch == '/') {
        		curTokenBuilder.append(ch);
        		
        		// The rest is same as  a meta statement, match it there.
				return matchMetaStatements();
			} else {
    			// The first '/' was a symbol. This character is for next token.
 				this.nextTokenChar = ch;
    		}
    	}
    	// The previous '/' was a symbol, return it.
		matchedToken = new Token(TokenType.SYMBOL, curTokenBuilder.toString());
    	return matchedToken;
    }
    
    // Matches the meta statements starting with #, to the end of the line included.
    private Token matchMetaStatements() {
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	while (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// Read until the end of line.
    		if (ch != '\n') {
    			curTokenBuilder.append(ch);
    		} else {
    			break;
    		}
    		readValue = getNextCharValue();
    	}
    	
    	// We have found the end of the file or a new line. The contents of
    	// the builder is a meta statement.
		matchedToken = new Token(TokenType.META_STATEMENT, curTokenBuilder.toString());
    	return matchedToken;
    }
    
    // Match the longest string between double quotes ("),
    // Strings can not have new lines in between the quotes,
    // so a string with beginning quote spanning over more 
    // than one line is considered an invalid token.
    private Token matchStrings() {
    	// Flag to check if the contents of the builder is valid or not.
    	boolean tokenMatched = false;
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	while (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// Anything that is not another double quote or a newline
    		// gets appended to the string.
    		if (ch != '"' && ch != '\n') {
    			curTokenBuilder.append(ch);
    		} else if (ch == '"') {
    			// We have found the end of the identified string token.
    			curTokenBuilder.append(ch);
    			tokenMatched = true;
    			
    			// Token was matched, no more characters left for the next one.
				this.nextTokenChar = Character.MIN_VALUE;				
    			break;
    		}    		
    		else if (ch == '\n') {
    			// Have found a newline without the matching double quote.
    			curTokenBuilder.append(ch);
    			
    			// No more characters left for the next one.
    			this.nextTokenChar = Character.MIN_VALUE;
				break;
    		}
    		
    		readValue = getNextCharValue();
    	}
    	
    	// We have found a valid token or reached the end of file. If there was a match then
    	// return the token. Otherwise if there is content in the token builder, it's an invalid token.
    	if (tokenMatched) {
    		matchedToken = new Token(TokenType.STRING, curTokenBuilder.toString());
		} else if (curTokenBuilder.length() > 0) {
			System.out.println(scanner_error_prefix + "Invalid token in file: " + curTokenBuilder.toString());
			System.exit(1);
		}
    	return matchedToken;
    }
    
    // Match the longest running number.
    private Token matchNumber() {
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	while (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// Any following digits.
    		if (isDigit(ch)) {
    			curTokenBuilder.append(ch);
    		} else {
    			// We have found the end of the identified token.
    			// Character for next token.
				this.nextTokenChar = ch;
				break;
    		}
    		
    		readValue = getNextCharValue();
    	}
    	
    	// We have found a valid token or reached the end of file. The contents of the builder is
    	// the longest matched number we have found.
    	matchedToken = new Token(TokenType.NUMBER, curTokenBuilder.toString());		
    	return matchedToken;
	}

	// Matches longest running identifier token or reserve words.
    private Token matchIdentifierOrReserved() {
    	Token matchedToken = null;
    	int readValue = getNextCharValue();
    	while (readValue != -1) {
    		char ch = (char) readValue;
    		
    		// Any following letter or digits, append.
    		if (isLetter(ch) || isDigit(ch)) {
    			curTokenBuilder.append(ch);
    		} else {
    			// Character for next token.
				this.nextTokenChar = ch;
				break;
    		}
    		
    		readValue = getNextCharValue();
    	}
    	
    	// We have found a valid token or reached the end of file. The contents of the builder
    	// is the longest matching identifier or a reserved word.
    	if (isReserved(curTokenBuilder.toString())) {
    		// It is a reserved word.  		
			matchedToken = new Token(TokenType.RESERVED_WORD, curTokenBuilder.toString());
		} else {
			matchedToken = new Token(TokenType.IDENTIFIER, curTokenBuilder.toString());
		}
		
		return matchedToken;
    }    	
    
    // Utility functions.
    public static boolean isLetter(char ch) {
    	return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }
    
    public static boolean isDigit(char ch) {
    	return (ch >= '0' && ch <= '9');
    }
    
    // White space characters that our scanner can consume freely.
    public static boolean isWhitespace(char ch) {
    	return (ch == ' ') || (ch == '\r') || (ch =='\n') || (ch == '\t');
    }
    
    // Operator symbols that can be followed by an assign sign (=). 
    public static boolean isAssignOperatorSymbol(char ch) {
    	return (ch == '=') || (ch == '<') || (ch == '>');
    }
    
    // Symbols that are single characters only.
    public static boolean isSingleCharSymbol(char ch) {
    	return (ch == '(') || (ch == ')') || (ch == '{') || (ch == '}') || (ch == '[') || (ch == ']') ||
    			(ch == ',') || (ch == ';') || (ch == '+') || (ch == '-') || (ch == '*') || (ch == '/');
    }
    
    // Wrapper function for reading next char value from stream, so that 
    // don't have to write try/catch block every time a character is read.
    private int getNextCharValue() {
    	int readValue = -1;
    	try {
			readValue = inputReader.read();			
		} catch (IOException e) {
			// Error reading the buffer, should not happen.
			System.out.println(e.getMessage());
			System.exit(1);
		}
    	return readValue;
    }
    
    // Utility function to give the generated file name.
    public static String getResultFilename(String filename) {
    	File srcFile = new File(filename);
    	String srcFilename = srcFile.getName();
    	
    	// See if there's an extension.
    	int extPos = srcFilename.lastIndexOf(".");
    	if (extPos > 0) {
    	    String srcFilenameWithoutExt = srcFilename.substring(0, extPos);
    	    String extensionWithDot = srcFilename.substring(extPos, srcFilename.length());
    	    return srcFilenameWithoutExt + "_gen" + extensionWithDot;
    	}
    	
    	// No extension, append _gen and return.
    	return srcFilename + "_gen";
    }
    
    // List of reserved words.
    private static final String[] reservedWords = {"int", "void", "if", "while", "return", "read", 
													"write", "print", "continue", "break", "binary", "decimal"};
    
    // Utility function to check if an identifier is a reserved word.
    private boolean isReserved(String tokenString) {
    	for (int i = 0; i < reservedWords.length; i++) {
			if (tokenString.equals(reservedWords[i])) {
				return true;
			}
		}
    	return false;
    }
    
    private BufferedReader inputReader;
	private StringBuilder curTokenBuilder;
	private char nextTokenChar;
	private Token nextToken;
}
