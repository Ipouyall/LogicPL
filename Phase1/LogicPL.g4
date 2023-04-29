grammar LogicPL;

logicPL
    : (comment | function)* main comment*
    ;

function
    : FUNCTION_DEC IDENTIFIER {System.out.println("FunctionDec: " + $IDENTIFIER.text);} LPAR function_dec_args RPAR COLON data_type  LBRACE body RBRACE
    ;

function_dec_args
    : (data_type IDENTIFIER COMMA {System.out.println("ArgumentDec: " + $IDENTIFIER.text);})* (data_type IDENTIFIER {System.out.println("ArgumentDec: " + $IDENTIFIER.text);})
    | /* epsilon */
    ;

main
    : MAIN {System.out.println("MainBody");} LBRACE main_body RBRACE
    ;

main_body
    : body
    ;

loop
    : FOR {System.out.println("Loop: for");} LPAR IDENTIFIER COLON IDENTIFIER RPAR LBRACE body RBRACE
    ;

body
    : (statement | comment)*
    ;

statements
    : (statement)+
    ;

statement
    : variable
    | array
    | variable_assignment
    | predicate
    | array_assignment
    | loop
    | print
    | implication
    | return
    | {System.out.println("FunctionCall");} function_call SEMICOLON
    ;

variable
    : data_type IDENTIFIER ASSIGN {System.out.println("VarDec: " + $IDENTIFIER.text);} relational_expressions SEMICOLON
    | data_type IDENTIFIER SEMICOLON {System.out.println("VarDec: " + $IDENTIFIER.text);}
    ;

function_call
    : IDENTIFIER LPAR function_call_args RPAR
    ;

function_call_args
    : (function_call_arg COMMA)* function_call_arg
    | /* epsilon */
    ;

function_call_arg
    : var_or_data
    | array_element
    ;

array
    : data_type LBRACKET NN RBRACKET IDENTIFIER ASSIGN LBRACKET {System.out.println("VarDec: " + $IDENTIFIER.text);} array_args RBRACKET SEMICOLON
    | data_type LBRACKET NN RBRACKET IDENTIFIER SEMICOLON {System.out.println("VarDec: " + $IDENTIFIER.text);}
    ;

array_args
    : (var_or_data COMMA)* var_or_data
    ;

predicate
    : PREDICATE_NAME {System.out.println("Predicate: " + $PREDICATE_NAME.text);}  LPAR (array_element | IDENTIFIER) RPAR SEMICOLON
    ;

variable_assignment
    : IDENTIFIER ASSIGN relational_expressions SEMICOLON
    ;

array_assignment
    : array_element ASSIGN relational_expressions SEMICOLON
    ;

implication
    : LPAR {System.out.println("Implication");} relational_expressions RPAR IMPLIES LPAR statements RPAR
    ;

print
    : PRINT {System.out.println("Built-in: print");} LPAR (query_type1 | query_type2 | array_element | IDENTIFIER) RPAR SEMICOLON
    ;

query_type1
    : LBRACKET QUESTION PREDICATE_NAME {System.out.println("Predicate: " + $PREDICATE_NAME.text);} LPAR (array_element | IDENTIFIER) RPAR RBRACKET
    ;

query_type2
    : LBRACKET PREDICATE_NAME {System.out.println("Predicate: " + $PREDICATE_NAME.text);} LPAR QUESTION RPAR RBRACKET
    ;

return
    : RETURN {System.out.println("Return");} (var_or_data | /* epsilon */) SEMICOLON
    ;

data_type
    : FLOAT_DEC
    | INT_DEC
    | BOOLEAN_DEC
    ;

var_or_data
    : data
    | negative_num
    | IDENTIFIER
    ;


relational_expressions
    : r_term1 relational_expressions_
    ;

relational_expressions_
    : OR r_term1 relational_expressions_ {System.out.println("Operator: ||");}
    | /* epsilon */
    ;

r_term1
    : r_term2 r_term1_
    ;

r_term1_
    : AND r_term2 r_term1_ {System.out.println("Operator: &&");}
    | /* epsilon */
    ;

r_term2
    : r_term3 r_term2_
    ;
r_term2_
    : EQ r_term3 r_term2_ {System.out.println("Operator: ==");}
    | NEQ r_term3 r_term2_ {System.out.println("Operator: !=");}
    | /* epsilon */
    ;

r_term3
    : r_factor r_term3_
    ;

r_term3_
    : GTE r_factor r_term3_ {System.out.println("Operator: >=");}
    | GT r_factor r_term3_ {System.out.println("Operator: >");}
    | LTE r_factor r_term3_ {System.out.println("Operator: <=");}
    | LT r_factor r_term3_  {System.out.println("Operator: <");}
    | /* epsilon */
    ;

r_factor
    : LPAR relational_expressions RPAR
    | NOT LPAR relational_expressions RPAR
    | expressions
    ;

expressions
    : term expressions_
    ;
expressions_
    : SUM term expressions_ {System.out.println("Operator: +");}
    | MINUS term expressions_ {System.out.println("Operator: -");}
    | /* epsilon */
    ;

term
    : factor term_
    ;

term_
    : MULT factor term_ {System.out.println("Operator: *");}
    | DIV factor term_ {System.out.println("Operator: /");}
    | REMAINDER factor term_ {System.out.println("Operator: %");}
    | /* epsilon */
    ;

factor
    : MINUS factor {System.out.println("Operator: -");}
    | SUM factor {System.out.println("Operator: +");}
    | NOT factor {System.out.println("Operator: !");}
    | primary
    ;

primary
    : LPAR expressions RPAR
    | function_call
    | query_type1
    | array_element
    | TRUE
    | FALSE
    | IDENTIFIER
    | int
    | FLOAT
    ;

array_element
    : IDENTIFIER LBRACKET expressions RBRACKET
    ;

data: FLOAT | int | boolean;
boolean: TRUE | FALSE;

negative_num
    : MINUS int {System.out.println("Operator: -");}
    | MINUS FLOAT {System.out.println("Operator: -");}
    ;
int
    : NN
    | ZERO
    ;
comment: COMMENT;

FLOAT: NN '.'[0-9]+ | ZERO'.'[0-9]+;
NN: [1-9][0-9]*;
ZERO: [0];
FUNCTION_DEC: 'function';
INT_DEC: 'int';
FLOAT_DEC: 'float';
BOOLEAN_DEC: 'boolean';
LBRACKET: '[';
RBRACKET: ']';
LBRACE: '{';
RBRACE: '}';
LPAR: '(';
RPAR: ')';
SEMICOLON: ';';
TRUE: 'true';
FALSE: 'false';
COMMA: ',';
SUM: '+';
MINUS: '-';
AND: '&&';
OR: '||';
MULT: '*';
DIV: '/';
REMAINDER: '%';
GTE: '>=';
LTE: '<=';
LT: '<';
GT: '>';
EQ: '==';
NEQ: '!=';
NOT: '!';
ASSIGN: '=';
COLON: ':';
FOR: 'for';
PRINT: 'print';
MAIN: 'main';
RETURN: 'return';
QUESTION: '?';
IMPLIES: '=>';




COMMENT:  '#' ~( '\r' | '\n' )*;
PREDICATE_NAME: [A-Z][A-Za-z0-9]*;
IDENTIFIER: [a-z][A-Za-z0-9_]*;
WS  : [ \t\r\n]+ -> skip ;