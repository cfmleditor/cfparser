parser grammar  CFSCRIPTParser; 

options { tokenVocab=CFSCRIPTLexer; }

//Note: needs case insensitive stream: http://www.antlr.org/wiki/pages/viewpage.action?pageId=1782

scriptBlock
  :
  importStatement*
  componentDeclaration
  | interfaceDeclaration
  | ( element )*
  | cfscriptBlock* 
  | EOF
  ;

cfscriptBlock
  : SCRIPTOPEN scriptBlock SCRIPTCLOSE
  ;

// The inverse of cfscriptBlock: markup embedded in cfscript, between ``` fences, with
// #...# interpolation as in a string literal.
templateBlock
  : OPEN_TEMPLATE (templateLiteralPart | POUND_SIGN anExpression POUND_SIGN)* CLOSE_TEMPLATE
  ;

templateLiteralPart
  : TEMPLATE_LITERAL | DOUBLEHASH
  ;

componentDeclaration
  : componentModifier? COMPONENT componentAttribute* componentGuts //-> ( COMPDECL componentAttribute* componentGuts)
  ;
interfaceDeclaration
  : INTERFACE componentAttribute* componentGuts //-> ( COMPDECL componentAttribute* componentGuts)
  ;

staticBlock
  : STATIC LEFTCURLYBRACKET ( statement )* RIGHTCURLYBRACKET
  ;

element
  : functionDeclaration
  | staticBlock
  | statement
  ;

componentModifier
  : ABSTRACT
  | FINAL
  | STATIC
  ;

functionModifier
  : STATIC
  | ABSTRACT
  | FINAL
  ;

// `public static string function f()` and `static public string function f()` are the
// same declaration, so the access type may sit either side of the modifiers. Written as
// one optional accessType between two modifier lists rather than (functionModifier |
// accessType)*, which would also admit two access types.
functionDeclaration
  : functionModifier* accessType? functionModifier* typeSpec? FUNCTION identifier 
  	LEFTPAREN parameterList? RIGHTPAREN
  	functionAttribute* body=compoundStatement?
  ;
anonymousFunctionDeclaration
  : accessType? typeSpec? FUNCTION //identifier? 
  	LEFTPAREN parameterList? RIGHTPAREN
  	functionAttribute* body=compoundStatement 
  ;

lambdaDeclaration
  : LEFTPAREN parameterList? RIGHTPAREN
  	operator=(LAMBDAOP | THINARROW) (body=compoundStatement | simpleExpression =startExpression) 
  // A single parameter may drop the parentheses: t -> t.b(), target => target.name.
  | single=identifier
  	operator=(LAMBDAOP | THINARROW) (body=compoundStatement | simpleExpression =startExpression) 
  ;

accessType
	:PUBLIC | PRIVATE | REMOTE | PACKAGE
	;

typeSpec
  : (type
  | multipartIdentifier) array?
  | stringLiteral
  ;
  
array
  : LEFTBRACKET RIGHTBRACKET
  ; 
  
stringLiteral
  :  OPEN_STRING (stringLiteralPart | POUND_SIGN (anExpression) POUND_SIGN)* CLOSE_STRING;

stringLiteralPart
  :  STRING_LITERAL | DOUBLEHASH;
  

parameterList
  : parameter ( COMMA parameter)* COMMA?
  |
  ;
  
parameter
  : (REQUIRED)? (parameterType)? name=identifier ( EQUALSOP startExpression )? parameterAttribute* //-> ^(FUNCTION_PARAMETER (REQUIRED)? (parameterType)? identifier (EQUALSOP baseExpression)? parameterAttribute*)
  ;
  
parameterType
  : typeSpec //-> ^( PARAMETER_TYPE typeSpec )
  ;

componentAttribute
  : id=identifier //-> ^(COMPONENT_ATTRIBUTE identifier)
  | (prefix=identifier COLON)? id=identifier op=EQUALSOP startExpression //-> ^(COMPONENT_ATTRIBUTE identifier (COLON identifier)? baseExpression)
  ;
//i=identifier EQUALSOP^ v=baseExpression
   
functionAttribute
  : identifierWithColon op=EQUALSOP (value=identifier | valueString=constantExpression | function=simpleFunctionCall)
  | id=identifier ( op=(EQUALSOP|COLON) (value=identifier | valueString=constantExpression | function=simpleFunctionCall) )?
  ;

identifierWithColon
: identifier COLON identifier
;

parameterAttribute
  : identifier EQUALSOP startExpression //-> ^(PARAMETER_ATTRIBUTE identifier baseExpression)
  | identifier
  ;
  
compoundStatement
  : LEFTCURLYBRACKET ( statement )* RIGHTCURLYBRACKET
  ;
  
componentGuts
  : LEFTCURLYBRACKET componentDirective* ( element )* RIGHTCURLYBRACKET
  ;
  
componentDirective
  : PAGE_ENCODING stringLiteral
  ;
  
statement
  :   tryCatchStatement
  |   ifStatement
  |   whileStatement
  |   doWhileStatement
  |   forStatement
  |   switchStatement
         //Semicolon OR look for a newline as the next token (in the hidden channel)
  |   continueStatement endOfStatement
  |   breakStatement endOfStatement
  |   returnStatement endOfStatement
  |   tagOperatorStatement
  |   compoundStatement 
  |   localAssignmentExpression endOfStatement
  |   assignmentExpression endOfStatement
  |   startExpression SEMICOLON
  |   SEMICOLON // empty statement
  | functionCall // without semi
  | templateBlock
  ;
  
endOfStatement
   :
   // A newline stands in for the semicolon. So does end of input: a file whose last
   // statement ends in a block and carries no trailing newline has EOF here, not NEWLINE,
   // and without this it demands a semicolon the same text does not need one line up.
   {_input.get(_input.LT(-1).getTokenIndex()+1).getType()==NEWLINE
     || _input.get(_input.LT(-1).getTokenIndex()+1).getType()==Token.EOF}?
     semicolon = SEMICOLON?
   |
    semicolon = SEMICOLON; 
  
breakStatement 
  :BREAK ;
  
continueStatement 
  :CONTINUE ;  
   
condition
  : LEFTPAREN baseExpression RIGHTPAREN
  ;
  
returnStatement
  : RETURN (anExpression)?
  ;
  
ifStatement
  : IF condition statement ( ELSE statement )?
  ;

whileStatement
  : WHILE condition statement
  ;
 
doWhileStatement
  : DO statement WHILE condition endOfStatement
  ;
  
forStatement
  : FOR LEFTPAREN ( localAssignmentExpression | initExpression=assignmentExpression )? endOfStatement 
  	  ( condExpression=startExpression )? endOfStatement  
  	  ( incrExpression=startExpression | incrExpression2=assignmentExpression )? RIGHTPAREN statement
  | FOR LEFTPAREN forInKey IN inExpr=startExpression RIGHTPAREN statement
  ;
  
startExpression:
  baseExpression;

forInKey
  : VAR? multipartIdentifier
  ;

tryCatchStatement
  : TRY statement ( catchCondition )* finallyStatement?
  ;
  
catchCondition
  : CATCH LEFTPAREN typeSpec? multipartIdentifier RIGHTPAREN compoundStatement
  ;

finallyStatement
  : FINALLY compoundStatement
  ;
  
constantExpression
  : LEFTPAREN constantExpression RIGHTPAREN
  | MINUS? ( INTEGER_LITERAL | floatingPointExpression  )
  | stringLiteral
  | BOOLEAN_LITERAL
  ;
  
switchStatement
  : SWITCH condition LEFTCURLYBRACKET
    ( 
      caseStatement    
    )* 
    
    RIGHTCURLYBRACKET
  ;

caseStatement
  : ( CASE (constantExpression|memberExpression) COLON statement*) 
    | 
    ( DEFAULT COLON statement* ) 
  ;

tagOperatorStatement
  : includeStatement
  | importStatement
  | abortStatement
  | adminStatement
  | tagThrowStatement
  | throwStatement
  | rethrowStatment 
  | exitStatement
  | paramStatement
  | propertyStatement
  | lockStatement
  | threadStatement
  | transactionStatement
  | cfmlfunctionStatement
  | tagFunctionStatement
  | tagStatement
  ; 
  
rethrowStatment:
  lc=RETHROW endOfStatement ;

// include "a.cfm"; and include template="a.cfm" runOnce=true; are both <cfinclude>.
// The attribute form has no leading expression, so baseExpression cannot be required.
includeStatement
  : lc=INCLUDE (baseExpression (paramStatementAttributes)? | paramStatementAttributes) SEMICOLON
  ;

importStatement
  : lc=IMPORT componentPath (DOT all=STAR)? endOfStatement
  ;

transactionStatement
  : lc=TRANSACTION (paramStatementAttributes)? (compoundStatement)? 
  ;
  
cfmlfunctionStatement
  : cfmlFunction (LEFTPAREN tagAttributeList RIGHTPAREN | paramStatementAttributes)? (body=compoundStatement | SEMICOLON)
  ;

// Attributes of a parenthesised script-syntax tag: cffile( action="write" file=f ).
// At least one junction must be a space. A list separated entirely by commas is
// indistinguishable from an ordinary named-argument call -- update(id=1, name="x") is
// far more likely a user function than <cfupdate> -- so that stays the function call it
// already was. One space anywhere settles it, because argumentList requires a comma at
// every junction, so a mixed list cannot be a function call at all.
//
// Read as: a comma-separated prefix, then two params with no comma between them, then
// anything. cflog( file=f text="t", type="error" ) and
// cflog( file=f, text="t" type="error" ) both match; cflog( file=f, text="t" ) does not.
tagAttributeList
  : (param COMMA)* param param (COMMA? param)*
  ;

// A tag name called with comma-separated arguments is treated as an ordinary call, so it
// takes argumentList like any other. It used to take parameterList -- the rule for a
// function *declaration's* parameters -- whose trailing parameterAttribute* silently ate
// any argument that followed without a comma, and which turned a positional argument into
// a named one with no value. See #30.
tagFunctionStatement
  : cfmlFunction (LEFTPAREN argumentList RIGHTPAREN)? (body=compoundStatement | SEMICOLON)?
  ;

cfmlFunction
  : SAVECONTENT
  | APPLICATION
  | FILE
  | PROPERTY
  | DIRECTORY
  | LOOP 
  | SETTING
  | QUERY
  | LOG
  | APPLET
  | ASSOCIATE
  | AUTHENTICATE
  | CACHE
  | COL
  | COLLECTION
  | CONTENT
  | COOKIE
  | ERROR
  | EXECUTE
  | FORM
  | FTP
  | GRID
  | GRIDCOLUMN
  | GRIDROW
  | GRIDUPDATE
  | HEADER
  | HTMLHEAD
  | HTTP
  | CFHTTP
  | HTTPPARAM
  | CFHTTPPARAM
  | IMPERSONATE
  | INDEX
  | INPUT
  | INSERT
  | LDAP
  | LOCATION
  | MAIL
  | MAILPARAM
  | MODULE
  | OBJECT
  | OUTPUT
  | POP
  | PROCESSINGDIRECTIVE
  | PROCPARAM
  | PROCRESULT
  | QUERYPARAM
  | REGISTRY
  | REPORT
  | SCHEDULE
  | SCRIPT
  | SEARCH
  | SELECT
  | SERVLET
  | SERVLETPARAM
  | SET
  | SILENT
  | SLIDER
  | STOREDPROC
  | TABLE
  | TEXTINPUT
  | TREE
  | TREEITEM
  | UPDATE
  | WDDX
  | ZIP
  | CFCUSTOM_IDENTIFIER
  ;

lockStatement
  : lc=LOCK p=paramStatementAttributes cs=compoundStatement 
  ;

tagThrowStatement
  : lc=THROW p=paramStatementAttributes endOfStatement
  ;
tagStatement
  : lc=cfmlFunction p=paramStatementAttributes endOfStatement
  ;

threadStatement
  : lc=THREAD p=paramStatementAttributes (compoundStatement | SEMICOLON) 
  ;

abortStatement
  : lc=ABORT memberExpression? endOfStatement
  ;
  
adminStatement
  : lc=ADMIN p=paramStatementAttributes endOfStatement 
  ;

throwStatement
  : lc=THROW (stringLiteral | memberExpression)? endOfStatement 
  ;

exitStatement
  : lc=EXIT memberExpression? endOfStatement 
  ;

paramStatement
  : lc=PARAM (paramStatementAttributes | paramExpression) SEMICOLON //-> ^(PARAMSTATEMENT[$lc] paramStatementAttributes)
  ;
  
// `param string foo = "x";` gives the default with an equals sign; `param string foo
// default="x" max=100;` gives it, and anything else, as attributes. Both are the typed
// shorthand, as against the all-attributes `param name="foo" type="string" ...`.
paramExpression
  : type? multipartIdentifier EQUALSOP startExpression
  | type? multipartIdentifier paramStatementAttributes
  ;
// The typed shorthand may carry attributes too: `property string email default="";`.
propertyStatement
  : lc=PROPERTY paramStatementAttributes endOfStatement
  | lc=PROPERTY typeSpec? name=multipartIdentifier paramStatementAttributes? endOfStatement
  ;
  
paramStatementAttributes
  : param ( COMMA? param )*
  ;
  
param
  : i=multipartIdentifier EQUALSOP startExpression

  ;


//--- expression engine grammar rules (a subset of the cfscript rules)
  
expression 
	: localAssignmentExpression EOF
	|	assignmentExpression EOF
	|   startExpression EOF
	;

anExpression 
	: localAssignmentExpression
	|	assignmentExpression
	|   startExpression
	;

cfmlExpression 
	: localAssignmentExpression EOF
	|	assignmentExpression EOF
	|   startExpression EOF
	|   importStatement EOF
	;
	
// FINAL is Lucee's immutable local; it takes the same shape as VAR and may combine
// with it, as in `final var x = 1;`.
localAssignmentExpression 
	:	(VAR | FINAL VAR? | VAR FINAL) left=startExpression ( (EQUALSOP otherIdentifiers)* EQUALSOP right=startExpression )? //-> ^( VARLOCAL identifier ( EQUALSOP baseExpression )? )
	;
	
otherIdentifiers:
VAR? otherid=identifier;

assignmentExpression 
  :  left=startExpression
     ( ( (EQUALSOP (identifier EQUALSOP)*) | PLUSEQUALS | MINUSEQUALS | STAREQUALS | SLASHEQUALS | MODEQUALS | CONCATEQUALS )
     	right = startExpression
     )
  ;

ternaryExpression
    : QUESTIONMARK ternaryExpression1=startExpression COLON ternaryExpression2=startExpression
    ;

// ANTLR gives earlier alternatives of a left-recursive rule higher precedence, so the order
// below is the operator precedence table. elvis sits immediately above the ternary it is
// shorthand for: both bind loosest, so `a ?: b + 1` groups as `a ?: (b + 1)`.
baseExpression
	:
	 unaryOperator=(MINUS | PLUS) right=baseExpression
	| left=baseExpression powerOperator=POWER right=baseExpression
	| left=baseExpression multiplicativeOperator=(STAR|SLASH) right=baseExpression
	| left=baseExpression intDivOperator=BSLASH right=baseExpression
	| left=baseExpression mod_operator=MOD right=baseExpression
	| left=baseExpression concatenationOperator=CONCAT right=baseExpression
	| left=baseExpression addOperator=(PLUS|MINUS) right=baseExpression
	| notExpression
	| left=baseExpression operator=compareExpressionOperator right=baseExpression
	| left=baseExpression andOperator=(AND | ANDOPERATOR) right=baseExpression
	| left=baseExpression orOperator=(OR | OROPERATOR) right=baseExpression
	| notNotExpression
	| anonymousFunctionDeclaration
	| lambdaDeclaration
	| unaryExpression
	| left=baseExpression elvisOperator right=baseExpression
	| left=baseExpression ternaryExpression
	
	;
	
elvisOperator:
	QUESTIONMARK COLON;
	
compareExpression
	: (
	left=baseExpression 
		(operator=compareExpressionOperator right=compareExpression)?
	)
	;
	
compareExpressionOperator:
 EQV
 | XOR
    |EQ //-> ^(EQ)
    |   LT //-> ^(LT)
    |   LTE //-> ^(LTE)
    |   GT //-> ^(GT)
    |   GTE //-> ^(GTE)
    |   NEQ //-> ^(NEQ)
    |   CONTAINS //-> ^(CONTAINS)
    |   DOESNOTCONTAIN
    |   INSTANCEOF
    |   CT
    |   NCT
 ;
	

notExpression
	:	( NOT | NOTOP ) (unaryExpression|baseExpression)
	;
	
notNotExpression
	:	NOTNOTOP  unaryExpression 
	;	
equalityOperator1
    :
       EQ //-> ^(EQ)
    |   LT //-> ^(LT)
    |   LTE //-> ^(LTE)
    |   GT //-> ^(GT)
    |   GTE //-> ^(GTE)
    |   NEQ //-> ^(NEQ)
    |   CONTAINS //-> ^(CONTAINS)
    ;
    
unaryExpression
	: prefixop=(MINUSMINUS | PLUSPLUS) unaryExpression
  | memberExpression
  | innerExpression
  | unaryExpression postfixop=(MINUSMINUS | PLUSPLUS)
  | primaryExpression//-> ^(POSTMINUSMINUS memberExpression)
//  | atomicExpression
//  | notNotExpression
//  | notExpression
  ;

innerExpression:
	POUND_SIGN (anExpression) POUND_SIGN;

memberExpression
  : (functionCall
  	| newComponentExpression
    | firstidentifier=identifier
  	| parentheticalExpression
  	|arrayMemberExpression parentheticalMemberExpression?)
  ( 
    (DOT+|nullSafeOperator|DOUBLECOLUMN) qualifiedFunctionCall
    | arrayMemberExpression parentheticalMemberExpression?
    | (DOT+|nullSafeOperator|DOUBLECOLUMN) primaryExpressionIRW 
    | (DOT+|nullSafeOperator|DOUBLECOLUMN) identifier
  )*
;
  
identifierOrReservedWord:
identifier | reservedWord;

arrayMemberExpression
	:LEFTBRACKET startExpression RIGHTBRACKET 
	| arraySlice
	;

// Slicing: s[4:13], s[4:13:2], and either bound omitted -- s[:6], s[4:].
// Listed after the plain subscript so an ordinary index still takes that path.
// At least one bound is required, spelled as two alternatives rather than making both
// optional: [:] with neither is the empty ordered struct literal, not a slice, and a
// fully optional rule swallows it (structures/emptyOrderedStructColon.cfc catches this).
arraySlice
	: LEFTBRACKET from=startExpression COLON to=startExpression? (COLON by=startExpression?)? RIGHTBRACKET
	| LEFTBRACKET COLON to=startExpression (COLON by=startExpression?)? RIGHTBRACKET
	;

functionCall
    :(identifier | specialWord) LEFTPAREN argumentList RIGHTPAREN
    ;
simpleFunctionCall
    :(identifier | specialWord) LEFTPAREN argumentList RIGHTPAREN
    body=compoundStatement?
    ;
qualifiedFunctionCall
	:(identifier | reservedWord) LEFTPAREN argumentList RIGHTPAREN
	body=compoundStatement?
	;
	
	  
parentheticalMemberExpression
	:LEFTPAREN argumentList RIGHTPAREN 
	;
	
javaCallMemberExpression
	:primaryExpressionIRW LEFTPAREN argumentList RIGHTBRACKET 
	;	

indexSuffix
  : LEFTBRACKET  LT* (primaryExpression | parentheticalExpression) LT* RIGHTBRACKET 
  ; 
  
primaryExpressionIRW
	:	stringLiteral
	|	BOOLEAN_LITERAL
	|  INTEGER_LITERAL
	| implicitArray
    | implicitStruct
    | implicitOrderedStruct
	| reservedWord
	;
	
literalExpression
	:	stringLiteral
	|	BOOLEAN_LITERAL
	|  floatingPointExpression
	|  INTEGER_LITERAL;
	
floatingPointExpression
    : FLOATING_POINT_LITERAL
    | left=INTEGER_LITERAL? DOT right=INTEGER_LITERAL
    | leftonly=INTEGER_LITERAL DOT;
	
reservedWord
  : specialWord 
  | cfscriptKeywords 
  ;
specialWord
  : CONTAINS 
  | EQ | NEQ | GT | LT | GTE
  | LTE | NOT | AND
  | OR | XOR | EQV | IMP | MOD
  ;

argumentList
  : argument (COMMA argument)*
  | //-> ^(EMPTYARGS)
  ;

argument
  : ( (name=argumentName) COLON startExpression //-> ^( COLON identifier baseExpression ) 
  )
  | ( (name=argumentName) EQUALSOP startExpression //-> ^( COLON identifier baseExpression ) 
  )
  | anonymousFunctionDeclaration
  | lambdaDeclaration
  | startExpression
  ;
  
argumentName
  : identifier | stringLiteral
  ;

multipartIdentifier
	:
		identifier ((DOT|nullSafeOperator|DOUBLECOLUMN) identifierOrReservedWord)*;

nullSafeOperator
    :
QUESTIONMARK DOT;

identifier
	:	(COMPONENT
	| INTERFACE
	| IDENTIFIER
  | STATIC
  | CONTAIN
  | VAR
  | TO
  | DEFAULT // default is a cfscript keyword that's always allowed as a var name
  | INSTANCEOF // ColdBox's Matcher and TestBox's Assertion both declare function instanceOf()
  | FINAL     // modifiers, and ordinary names -- `final = 3;` is a variable called final
  | ABSTRACT
  | CT   // two-letter operator abbreviations; far too likely as ordinary names
  | NCT
  | INCLUDE
  | NEW
  | ABORT
  | ADMIN
  | THROW
  | RETHROW
  | PARAM
  | EXIT
  | THREAD
  | LOCK
  | TRANSACTION
  | PUBLIC
  | PRIVATE
  | REMOTE
  | PACKAGE
  | REQUIRED
  | FUNCTION
  | IMPORT
  | PAGE_ENCODING
  | cfmlFunction
  | type	)
	;

type
  : NUMERIC
  | STRING
  | BOOLEAN
  | COMPONENT
  | INTERFACE
  | ANY
  | ARRAY
  | STRUCT
  ;

cfscriptKeywords
  : IF
  | ELSE
  | BREAK
  | CONTINUE
  | FUNCTION
  | RETURN
  | WHILE
  | DO
  | FOR
  | IN
  | TRY
  | CATCH
  | FINALLY
  | SWITCH
  | CASE
  | DEFAULT
  | IMPORT
  ;
  
primaryExpression
	:	literalExpression
	| implicitArray
    | implicitStruct
    | implicitOrderedStruct
	|	identifier
	;
	
parentheticalExpression
	: LEFTPAREN startExpression RIGHTPAREN
;	

implicitArray
  : lc=LEFTBRACKET implicitArrayElements? RIGHTBRACKET //-> ^(IMPLICITARRAY[$lc] implicitArrayElements?) 
  ;
  
implicitArrayElements
  : startExpression ( COMMA startExpression )*
  ;
  
implicitStruct
  : lc=LEFTCURLYBRACKET implicitStructElements? RIGHTCURLYBRACKET //-> ^(IMPLICITSTRUCT[$lc] implicitStructElements?)
  ;
implicitOrderedStruct
  : lc=LEFTBRACKET (emptyDeclaration=( COLON | EQUALSOP )  | implicitStructElements) RIGHTBRACKET
  ;
  
implicitStructElements
  : implicitStructExpression ( COMMA implicitStructExpression )*
    unnecessaryComma=COMMA?
  ;

implicitStructExpression
  : implicitStructKeyExpression ( COLON | EQUALSOP ) baseExpression //unaryExpression
  ;
  
implicitStructKeyExpression
  : multipartIdentifier
 // | additiveExpression ( CONCAT additiveExpression )*
  | literalExpression
  | reservedWord
  ;

newComponentExpression
  : NEW componentPath LEFTPAREN argumentList RIGHTPAREN
  ;
  
// Lucee types the path being instantiated: new java:java.io.File(p), new cfml:foo.Bar().
componentPath
  : stringLiteral
  | (prefix=identifier COLON)? identifier
  | (prefix=identifier COLON)? multipartIdentifier
  ;