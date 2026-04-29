grammar Logo;

// ═══════════════════════════════
//  PARSER RULES
// ═══════════════════════════════

program
    : line* EOF
    ;

line
    : NEWLINE
    | statement NEWLINE*
    ;

statement
    : procedureDefinition
    | command
    ;

procedureDefinition
    : TO IDENT parameter* NEWLINE* statement* END
    ;

parameter
    : COLON IDENT
    ;

command
    : builtinCommand
    | repeatCommand
    | ifCommand
    | makeCommand
    | procedureCall
    ;

builtinCommand
    : FORWARD expression
    | BACK expression
    | RIGHT expression
    | LEFT expression
    | PENUP
    | PENDOWN
    | HOME
    | CLEARSCREEN
    | PRINT expression
    | SHOW expression
    ;

repeatCommand
    : REPEAT expression LBRACKET NEWLINE* statement* NEWLINE* RBRACKET
    ;

ifCommand
    : IF expression LBRACKET NEWLINE* statement* NEWLINE* RBRACKET
      (ELSE LBRACKET NEWLINE* statement* NEWLINE* RBRACKET)?
    ;

makeCommand
    : MAKE QUOTE IDENT expression
    ;

procedureCall
    : IDENT expression*
    ;

expression
    : NUMBER                                                    # numberExpr
    | COLON IDENT                                               # variableExpr
    | QUOTE IDENT                                               # stringExpr
    | LPAREN expression RPAREN                                  # parenExpr
    | expression op=(PLUS|MINUS|STAR|SLASH) expression          # binaryExpr
    | expression op=(LT|GT|EQ) expression                       # compareExpr
    ;

// ═══════════════════════════════
//  LEXER RULES
// ═══════════════════════════════

TO          : [Tt][Oo] ;
END         : [Ee][Nn][Dd] ;
REPEAT      : [Rr][Ee][Pp][Ee][Aa][Tt] ;
IF          : [Ii][Ff] ;
ELSE        : [Ee][Ll][Ss][Ee] ;
MAKE        : [Mm][Aa][Kk][Ee] ;
FORWARD     : [Ff][Oo][Rr][Ww][Aa][Rr][Dd] | [Ff][Dd] ;
BACK        : [Bb][Aa][Cc][Kk] | [Bb][Kk] ;
RIGHT       : [Rr][Ii][Gg][Hh][Tt] | [Rr][Tt] ;
LEFT        : [Ll][Ee][Ff][Tt] | [Ll][Tt] ;
PENUP       : [Pp][Ee][Nn][Uu][Pp] | [Pp][Uu] ;
PENDOWN     : [Pp][Ee][Nn][Dd][Oo][Ww][Nn] | [Pp][Dd] ;
HOME        : [Hh][Oo][Mm][Ee] ;
CLEARSCREEN : [Cc][Ll][Ee][Aa][Rr][Ss][Cc][Rr][Ee][Ee][Nn] | [Cc][Ss] ;
PRINT       : [Pp][Rr][Ii][Nn][Tt] ;
SHOW        : [Ss][Hh][Oo][Ww] ;

COLON       : ':' ;
QUOTE       : '"' ;
LBRACKET    : '[' ;
RBRACKET    : ']' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
PLUS        : '+' ;
MINUS       : '-' ;
STAR        : '*' ;
SLASH       : '/' ;
LT          : '<' ;
GT          : '>' ;
EQ          : '=' ;

NUMBER      : '-'? [0-9]+ ('.' [0-9]+)? ;
IDENT       : [a-zA-Z][a-zA-Z0-9_]* ;

NEWLINE     : [\r\n]+ ;
WS          : [ \t]+ -> skip ;
COMMENT     : ';' ~[\r\n]* -> skip ;