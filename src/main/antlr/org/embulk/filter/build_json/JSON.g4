
/** Taken from "The Definitive ANTLR 4 Reference" by Terence Parr */

// Derived from http://json.org
grammar JSON;

@header {
package org.embulk.filter.build_json;
}



json
   : value
   ;

object
   : '{' pair (',' pair)* '}' # ObjectWithPair
   | '{' '}'                  # EmptyObject
   ;

pair
   : STRING ':' value
   ;

array
   : '[' value (',' value)* ']' # ArrayWithValue
   | '[' ']'                    # EmptyArray
   ;

value
   : STRING     # StringValue
   | NUMBER     # NumberValue
   | object     # ObjectValue
   | array      # ArrayValue
   | 'true'     # TrueValue
   | 'false'    # FalseValue
   | 'null'     # NullValue
   | reference  # ReferenceValue
   ;


STRING
   : '"' (ESC | ~ ["\\])* '"'
   ;


fragment ESC
   : '\\' (["\\/bfnrt] | UNICODE)
   ;


fragment UNICODE
   : 'u' HEX HEX HEX HEX
   ;


fragment HEX
   : [0-9a-fA-F]
   ;


NUMBER
   : '-'? INT '.' [0-9] + EXP? | '-'? INT EXP | '-'? INT
   ;


fragment INT
   : '0' | [1-9] [0-9]*
   ;

// no leading zeros

fragment EXP
   : [Ee] [+\-]? INT
   ;

reference
   : '!' ID
   ;

// \- since - means "range" inside [...]

WS
   : [ \t\n\r] + -> skip
   ;

ID
   : [A-Za-z][A-Za-z0-9_\-]*
   ;
