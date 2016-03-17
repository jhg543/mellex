/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 by Bart Kiers
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Project      : sqlite-parser; an ANTLR4 grammar for SQLite
 *                https://github.com/bkiers/sqlite-parser
 * Developed by : Bart Kiers, bart@big-o.nl
 */
grammar DefaultSQLP;
@header {
  import io.github.jhg543.mellex.ASTHelper.*;
  import java.util.Stack;
}
parse
 : ( (statement)? ';' | exec_plsql_buffer )* EOF
 ;

exec_plsql_buffer:
   '/'
;

statement
 :  sql_stmt_ext | bteq
 ;

bteq
 : DOT ( any_name | DOT | LT | GT | NOT_EQ2 | DIV | ',' | signed_number )*
 ;

sql_stmt_ext
 :  sql_stmt | begin_tx_stmt |  end_tx_stmt | set_stmt | database_stmt | create_procedure
 ;

begin_tx_stmt
 : K_BT
 ;

K_BT
 : B T
 ;

end_tx_stmt
 : K_ET
 ;

K_ET
 : E T
 ;

set_stmt
 : K_SET any_name ASSIGN any_name ( K_FOR any_name )?
 ;

database_stmt
 : K_DATABASE expr
 ;
error
 : UNEXPECTED_CHAR
   {
     throw new RuntimeException("UNEXPECTED_CHAR=" + $UNEXPECTED_CHAR.text);
   }
 ;


create_procedure :
        K_CREATE ( K_OR K_REPLACE )? (K_PROCEDURE | K_FUNCTION) object_name
        parameter_declarations? (K_RETURN datatype)?
        ( K_IS | K_AS )
        declare_section? body
    ;


procedure_or_function_declaration :
        (K_PROCEDURE | K_FUNCTION) any_name parameter_declarations? (K_RETURN datatype)? ( K_DETERMINISTIC | K_PIPELINED | K_PARALLEL_ENABLE | K_RESULT_CACHE )*
    ;

procedure_or_function_definition :
        procedure_or_function_declaration
        ( K_IS | K_AS ) declare_section? body
	;

body 	:
	K_BEGIN multiple_plsql_stmt_list ( K_EXCEPTION exception_handler+ )? K_END any_name?
	;

parameter_declarations :
        (   '('  parameter_declaration ( ','  parameter_declaration )* ')' )
    ;

parameter_declaration :
        any_name  K_IN? K_OUT? K_NOCOPY?  datatype
        ( ( ':=' | K_DEFAULT ) expr )?
    ;

declare_section :
    ( declare_section_oneline ';' )+
    ;

declare_section_oneline:
    type_definition
    | subtype_definition
    | cursor_definition
    | variable_declaration
    | exception_declaration
    | procedure_or_function_declaration
    | procedure_or_function_definition
;

cursor_definition :
        K_CURSOR any_name parameter_declarations? K_IS select_stmt
    ;


variable_declaration :
        any_name K_CONSTANT? datatype (  (  K_NOT K_NULL )? (  ':='  | K_DEFAULT ) expr  )?
    ;


exception_declaration :
        any_name K_EXCEPTION
    ;

type_definition :
        K_TYPE any_name K_IS ( record_type_definition | collection_type_definition | ref_cursor_type_definition )
    ;

subtype_definition :
        K_SUBTYPE any_name K_IS datatype ( K_NOT K_NULL )?
    ;

record_type_definition :
	K_RECORD '(' record_field_declaration ( ',' record_field_declaration )* ')'
    ;

record_field_declaration :
	any_name datatype ( ( K_NOT K_NULL )? ( ':=' | K_DEFAULT ) expr )?
    ;

collection_type_definition
	:	varray_type_definition
	|	nested_table_type_definition
	;

varray_type_definition
	:	( K_VARYING K_ARRAY? | K_VARRAY ) '(' NUMERIC_LITERAL ')' K_OF datatype ( K_NOT K_NULL )?
	;

nested_table_type_definition
	:	K_TABLE K_OF datatype ( K_NOT K_NULL )? ( K_INDEX K_BY associative_index_type )?
	;

associative_index_type
	:	datatype
	;

ref_cursor_type_definition
	:	K_REF K_CURSOR ( K_RETURN datatype )?
	;

datatype
    : ( K_REF )? any_name ( '.' any_name )? ( '(' NUMERIC_LITERAL ( ',' NUMERIC_LITERAL )* ')' | '%' ( K_TYPE | K_ROWTYPE ) )?
    ;




exception_handler
	:	K_WHEN ( qual_id  ( K_OR qual_id  )* | K_OTHERS )
		K_THEN multiple_plsql_stmt_list
	;

multiple_plsql_stmt_list :
    ( plsql_statement ';' )+
;

plsql_statement :
    label* plsql_statement_nolabel;

plsql_statement_nolabel:
      case_statement
    | close_statement
    | continue_statement
    | basic_loop_statement
    | execute_immediate_statement
    | exit_statement
    | fetch_statement
    | for_loop_statement
    | forall_statement
    | goto_statement
    | if_statement
    | null_statement
    | open_statement
    | plsql_block
    | raise_statement
    | return_statement
    | sql_stmt
    | while_loop_statement
    | assign_or_call_statement
;
/*
lvalue
    : call ( '.' call )*
    ;

assign_or_call_statement
    : lvalue ( '.' delete_call | ':=' expr )?
    ;

call
    : COLON? any_name ( '(' ( parameter ( ',' parameter )* )? ')' )?
    ;

delete_call
    : K_DELETE ( '(' parameter? ')' )?
    ;
*/

assign_or_call_statement:
    expr
    | expr ':=' expr
    ;

basic_loop_statement :
        K_LOOP multiple_plsql_stmt_list K_END K_LOOP label_name?
    ;

case_statement :
        K_CASE selector=expr?
        ( K_WHEN selector_vals+=expr K_THEN then_stmts+=multiple_plsql_stmt_list )+
        ( K_ELSE else_stmts=multiple_plsql_stmt_list )?
        K_END K_CASE label_name?
    ;

close_statement :
        K_CLOSE any_name ( '.' any_name )?
    ;

continue_statement :
        K_CONTINUE any_name? ( K_WHEN expr )?
    ;

execute_immediate_statement :
        K_EXECUTE K_IMMEDIATE expr (
        ( into_clause | bulk_collect_into_clause) using_clause?
        | using_clause dynamic_returning_clause?
        | dynamic_returning_clause
        )?
    ;

exit_statement :
        K_EXIT any_name? ( K_WHEN expr )?
    ;

fetch_statement :
        K_FETCH qual_id ( into_clause | bulk_collect_into_clause ( K_LIMIT expr )? )
    ;

into_clause :
        K_INTO expr ( ',' expr )*
    ;

bulk_collect_into_clause :
        K_BULK K_COLLECT K_INTO expr ( ',' expr )*

    ;

using_clause :
        K_USING param_modifiers? expr ( ',' param_modifiers? expr )*
    ;

param_modifiers
	: K_IN K_OUT? | K_OUT
	;

dynamic_returning_clause :
        ( K_RETURNING | K_RETURN ) ( into_clause | bulk_collect_into_clause )

    ;

for_loop_statement :

        K_FOR any_name K_IN ( ~(K_LOOP) )+ K_LOOP multiple_plsql_stmt_list K_END K_LOOP label_name?
    ;

forall_statement :
        K_FORALL any_name K_IN bounds_clause plsql_statement ( K_SAVE K_EXCEPTIONS )?
    ;

while_loop_statement :
        K_WHILE expr K_LOOP multiple_plsql_stmt_list K_END K_LOOP label_name?
    ;

bounds_clause
    : expr '..' expr
    | K_INDICES K_OF expr ( K_BETWEEN expr K_AND expr )?
    | K_VALUES K_OF expr
    ;

goto_statement :
        K_GOTO label_name

    ;

if_statement :
        K_IF expr K_THEN multiple_plsql_stmt_list
        ( K_ELSIF expr K_THEN multiple_plsql_stmt_list )*
        ( K_ELSE multiple_plsql_stmt_list )?
        K_END K_IF
    ;

null_statement :
        K_NULL
    ;

open_statement :
        K_OPEN expr ( K_FOR (select_stmt | any_name) )?

    ;


raise_statement :
        K_RAISE ( any_name ( '.' any_name )* )?
    ;

return_statement :
        K_RETURN expr?
    ;

plsql_block :
        ( K_DECLARE declare_section )? body

    ;

label :
        '<<' label_name '>>'
    ;

qual_id :
	':'? any_name ( '.' ':'? any_name )*
    ;

label_name:
    any_name;


sql_stmt
locals [ Stack<SubQuery> cte_stack = new Stack<>() ]
 : ( K_EXPLAIN ( K_QUERY K_PLAN )? )? ( alter_table_stmt
                                      | analyze_stmt
                                      | attach_stmt
                                      | begin_stmt
                                      | commit_stmt
/*                                      | compound_select_stmt*/
                                      | create_index_stmt
                                      | create_sequence_stmt
                                      | create_table_stmt
                                      | create_trigger_stmt
                                      | create_view_stmt
                                      | create_virtual_table_stmt
                                      | delete_stmt
/*                                      | delete_stmt_limited*/
                                      | detach_stmt
                                      | drop_index_stmt
                                      | drop_table_stmt
                                      | drop_trigger_stmt
                                      | drop_view_stmt
//                                      | factored_select_stmt
                                      | insert_stmt
                                      | pragma_stmt
                                      | reindex_stmt
                                      | release_stmt
                                      | rollback_stmt
                                      | savepoint_stmt
/*                                      | simple_select_stmt*/
                                      | select_stmt
                                      | update_stmt
/*                                      | update_stmt_limited*/
                                      | vacuum_stmt
                                      | comment_stmt
                                      | other_stmt
                                      | call_stmt
                                      | rename_table_stmt
                                      | drop_macro_stmt
                                      | create_macro_stmt
                                      | truncate_table_stmt
                                      )
 ;

call_stmt:
 K_CALL expr
 ;

rename_table_stmt:
 K_RENAME ( K_TABLE | K_VIEW ) object_name (K_TO | K_AS) object_name
 ;

 other_stmt:
 (K_COLLECT | K_GRANT | K_SHOW) ( any_name | ',' | '(' | ')' | '.' | literal_value )+
 ;

alter_table_stmt
 : K_ALTER K_TABLE ( database_name '.' )? table_name
   alter_table_change (',' alter_table_change)*
 ;

alter_table_change
 :  K_RENAME column_name? K_TO new_table_name
    | K_ADD K_COLUMN? column_def
    | K_DROP column_name
 ;

analyze_stmt
 : K_ANALYZE ( database_name | table_or_index_name | database_name '.' table_or_index_name )?
 ;

attach_stmt
 : K_ATTACH K_DATABASE? expr K_AS database_name
 ;

begin_stmt
 : K_BEGIN ( K_DEFERRED | K_IMMEDIATE | K_EXCLUSIVE )? ( K_TRANSACTION transaction_name? | any_name )?
 ;

commit_stmt
 : ( K_COMMIT | K_END ) ( K_TRANSACTION transaction_name? )?
 ;

/*
compound_select_stmt
 : ( K_WITH K_RECURSIVE? common_table_expression ( ',' common_table_expression )* )?
   select_core ( ( K_UNION K_ALL? | K_INTERSECT | K_EXCEPT ) select_core )+
   ( K_ORDER K_BY ordering_term ( ',' ordering_term )* )?
   ( K_LIMIT expr ( ( K_OFFSET | ',' ) expr )? )?
 ;
*/
create_index_stmt
 : K_CREATE K_UNIQUE? K_INDEX
  /*( K_IF K_NOT K_EXISTS )?
   ( database_name '.' )? index_name K_ON table_name '(' indexed_column ( ',' indexed_column )* ')'
   ( K_WHERE expr )?
   */
   swallow_to_semi
 ;


create_sequence_stmt
 : K_CREATE K_SEQUENCE
  /*( K_IF K_NOT K_EXISTS )?
   ( database_name '.' )? index_name K_ON table_name '(' indexed_column ( ',' indexed_column )* ')'
   ( K_WHERE expr )?
   */
   swallow_to_semi
 ;

create_table_stmt
returns [ CreateTableStmt stmt = new CreateTableStmt(), boolean isvolatile = false, InsertStmt insert ]
 : K_CREATE (( K_MULTISET | K_SET ) | ( K_VOLATILE {$isvolatile = true;} | (K_GLOBAL? ( K_TEMPORARY | K_TEMP ) {$isvolatile = true;}) ))* K_TABLE ( K_IF K_NOT K_EXISTS )?
   obj=object_name create_table_options (def=column_defs | st=create_source_table )+  create_table_stmt_C_to_D
 ;

column_defs
returns [List<String> colnames]
: '(' cd+=column_def  ( ',' cd+=column_def )* ( ',' table_constraint )* ')'
 ;

create_source_table
returns [ SubQuery q, boolean nodata = true ]
 : K_AS ( obj=object_name  | '(' ss=select_stmt ')' /*TD*/ | ss=select_stmt /*GP*/ ) ( K_WITH {$nodata = false;} ( K_NO {$nodata = true;} )?  K_DATA /*TD*/)?
 | '(' K_LIKE obj=object_name ')' /* GP */
 ;
create_table_options
 : ( ',' create_table_option )*
 ;

create_table_option
 :K_NO? K_LOG | K_CHECKSUM ASSIGN any_name | K_NO? ( K_BEFORE | K_AFTER )? K_JOURNAL | K_NO? K_FALLBACK | K_DEFAULT any_name /* MERGEBLOCKRATIO */
 ;

/*
create_table_stmt_C_to_D
 :( K_UNIQUE? K_PRIMARY K_INDEX ( any_name )? '(' column_name ( ',' column_name )* ')' )? (K_PARTITION K_BY )? ( K_ON K_COMMIT (K_DELETE | K_PRESERVE) K_ROWS )?
 ;
*/
create_table_stmt_C_to_D
 :
  //( K_UNIQUE | K_INDEX | K_PRIMARY | K_PARTITION | K_BY | K_ON | K_COMMIT | K_DELETE | K_PRESERVE | K_ROWS | any_name | '(' | ')' | ',' | '=' | literal_value )*
  ( ~ ';')*
 ;

create_trigger_stmt
 : K_CREATE ( K_TEMP | K_TEMPORARY )? K_TRIGGER ( K_IF K_NOT K_EXISTS )?
   ( database_name '.' )? trigger_name ( K_BEFORE  | K_AFTER | K_INSTEAD K_OF )?
   ( K_DELETE | K_INSERT | K_UPDATE ( K_OF column_name ( ',' column_name )* )? ) K_ON ( database_name '.' )? table_name
   ( K_FOR K_EACH K_ROW )? ( K_WHEN expr )?
   K_BEGIN ( ( update_stmt | insert_stmt | delete_stmt | select_stmt ) ';' )+ K_END
 ;

create_view_stmt
returns [ CreateTableStmt stmt = new CreateTableStmt(), InsertStmt insert ]
 : ( K_CREATE | K_REPLACE | K_CREATE K_OR K_REPLACE /*  POSTGRES */ ) ( K_TEMP | K_TEMPORARY )? K_VIEW ( K_IF K_NOT K_EXISTS )?
   obj=object_name ( '(' cn+=column_name ( ',' cn+=column_name )* ')' )? K_AS? /*TD = MUST AS GP = NO AS*/ locking_option* ss=select_stmt
 ;

locking_option
 : (K_LOCK | K_LOCKING ) ( ( K_DATABASE | K_TABLE | K_COLUMN | K_VIEW )? /* WTF */ object_name | K_ROW ) ( K_FOR | K_IN )? ( K_ACCESS )
 ;
create_virtual_table_stmt
 : K_CREATE K_VIRTUAL K_TABLE ( K_IF K_NOT K_EXISTS )?
   ( database_name '.' )? table_name
   K_USING module_name ( '(' module_argument ( ',' module_argument )* ')' )?
 ;

delete_stmt
 : with_clause? (K_DELETE | K_DEL) object_name? K_FROM? qualified_table_name (K_AS? table_alias)? ( (',' | K_USING /* LAZY */) qualified_table_name (K_AS? table_alias)? )*
   ( (K_WHERE expr) | K_ALL  )?
 ;

/*
delete_stmt_limited
 : with_clause? K_DELETE K_FROM qualified_table_name
   ( K_WHERE expr )?
   ( ( K_ORDER K_BY ordering_term ( ',' ordering_term )* )?
     K_LIMIT expr ( ( K_OFFSET | ',' ) expr )?
   )?
 ;
*/

detach_stmt
 : K_DETACH K_DATABASE? database_name
 ;

drop_index_stmt
 : K_DROP K_INDEX ( K_IF K_EXISTS )? ( database_name '.' )? index_name
 ;

truncate_table_stmt
 : K_TRUNCATE K_TABLE ( database_name '.' )? table_name
 ;

drop_table_stmt
 : K_DROP K_TABLE ( K_IF K_EXISTS )? ( database_name '.' )? table_name ( K_ALL | K_CASCADE /* postgres */ ) ?
 ;

drop_trigger_stmt
 : K_DROP K_TRIGGER ( K_IF K_EXISTS )? ( database_name '.' )? trigger_name
 ;

drop_view_stmt
 : K_DROP K_VIEW ( K_IF K_EXISTS )? ( database_name '.' )? view_name ( K_CASCADE? /* postgres */ )
 ;

drop_macro_stmt
 : K_DROP K_MACRO ( database_name '.' )? trigger_name
 ;


create_macro_stmt
 : ( K_CREATE | K_REPLACE ) K_MACRO
   ( database_name '.' )? view_name column_defs K_AS '(' select_stmt ';' ')'
 ;
/*
factored_select_stmt
 : ( K_WITH K_RECURSIVE? common_table_expression ( ',' common_table_expression )* )?
   select_core ( compound_operator select_core )*
   ( K_ORDER K_BY ordering_term ( ',' ordering_term )* )?
   ( K_LIMIT expr ( ( K_OFFSET | ',' ) expr )? )?
 ;
*/

insert_stmt
returns [ InsertStmt stmt = new InsertStmt() ]
 : //with_clause?
 ( K_INSERT
                | K_REPLACE
                | K_INSERT K_OR K_REPLACE
                | K_INSERT K_OR K_ROLLBACK
                | K_INSERT K_OR K_ABORT
                | K_INSERT K_OR K_FAIL
                | K_INSERT K_OR K_IGNORE ) K_INTO?
   obj = object_name
   //( database_name '.' )? table_name
   ( '(' cn+=column_name ( ',' cn+=column_name )* ')' )?
   ( K_VALUES '(' ex+=expr ( ',' ex+=expr )* ')'  | ss=select_stmt  )
 ;

pragma_stmt
 : K_PRAGMA ( database_name '.' )? pragma_name ( '=' pragma_value
                                               | '(' pragma_value ')' )?
 ;

reindex_stmt
 : K_REINDEX ( collation_name
             | ( database_name '.' )? ( table_name | index_name )
             )?
 ;

release_stmt
 : K_RELEASE K_SAVEPOINT? savepoint_name
 ;

rollback_stmt
 : K_ROLLBACK ( K_TRANSACTION transaction_name? )? ( K_TO K_SAVEPOINT? savepoint_name )?
 ;

savepoint_stmt
 : K_SAVEPOINT savepoint_name
 ;

/*
simple_select_stmt
 : ( K_WITH K_RECURSIVE? common_table_expression ( ',' common_table_expression )* )?
   select_core ( K_ORDER K_BY ordering_term ( ',' ordering_term )* )?
   ( K_LIMIT expr ( ( K_OFFSET | ',' ) expr )? )?
 ;
*/

select_stmt
returns [ SubQuery q = new SubQuery() ]
 : ( K_WITH K_RECURSIVE? c+=common_table_expression  ( ',' c+=common_table_expression )* )?
   sv+=select_or_values ( compound_operator sv+=select_or_values )*
/*   ( K_LIMIT expr ( ( K_OFFSET | ',' ) expr )? )?*/
 ;

/*
select_or_values
 : K_SELECT ( K_DISTINCT | K_ALL )? result_column ( ',' result_column )*
   ( K_FROM ( table_or_subquery ( ',' table_or_subquery )* | join_clause ) )?
   ( K_WHERE expr )?
   ( K_GROUP K_BY expr ( ',' expr )* ( K_HAVING expr  )? )?
 | K_VALUES '(' expr ( ',' expr )* ')' ( ',' '(' expr ( ',' expr )* ')' )*
 ;
*/
select_or_values
returns [ SubQuery q ]
 : sc=select_core #select_or_valuesSelectCore
 | '(' ss=select_or_values ')' #select_or_valuesSelectValue
 ;

comment_stmt
 : K_COMMENT K_ON? object_kind? object_name ( K_AS | K_IS )? STRING_LITERAL
 ;

object_kind
 : K_TABLE | K_COLUMN | K_VIEW
 ;

update_stmt
returns [ UpdateStmt q = new UpdateStmt() ]
 : K_UPDATE tobj=object_name ( K_AS? ta=table_alias  )?
  ( f=update_stmt_from )? s=update_stmt_set ( f=update_stmt_from )?
  ( K_WHERE wex=expr | K_ALL )?

 ;

update_stmt_from
returns [ List<SubQuery> tables ]
 : K_FROM ts+=table_or_subquery  ( ',' ts+=table_or_subquery )*
 ;

update_stmt_set
returns [ List<ResultColumn> columns ]
 : K_SET (any_name '.')? cn+=column_name '=' ex+=expr ( ',' (any_name '.')? cn+=column_name '=' ex+=expr)*
 ;
 /*
update_stmt
 : with_clause? K_UPDATE ( K_OR K_ROLLBACK
                         | K_OR K_ABORT
                         | K_OR K_REPLACE
                         | K_OR K_FAIL
                         | K_OR K_IGNORE )? qualified_table_name
   K_SET column_name '=' expr ( ',' column_name '=' expr )* ( K_WHERE expr )?
 ;


update_stmt_limited
 : with_clause? K_UPDATE ( K_OR K_ROLLBACK
                         | K_OR K_ABORT
                         | K_OR K_REPLACE
                         | K_OR K_FAIL
                         | K_OR K_IGNORE )? qualified_table_name
   K_SET column_name '=' expr ( ',' column_name '=' expr )* ( K_WHERE expr )?
   ( ( K_ORDER K_BY ordering_term ( ',' ordering_term )* )?
     K_LIMIT expr ( ( K_OFFSET | ',' ) expr )?
   )?
 ;
*/

vacuum_stmt
 : K_VACUUM
 ;

column_def
returns [String colname]
 : cn=column_name type_name? column_constraint*
 ;

type_name
 : ( name+?  ( '(' signed_number (',' signed_number| K_BYTE | K_CHAR)? ')' )?   )
 ;


date_type
 : K_DATE ( K_FORMAT STRING_LITERAL )?
 ;

column_constraint
 : ( K_CONSTRAINT name )?
   ( K_PRIMARY K_KEY ( K_ASC | K_DESC )? conflict_clause K_AUTOINCREMENT?
   | K_NOT? K_NULL conflict_clause
   | K_UNIQUE conflict_clause
   | K_CHECK '(' expr ')'
   | K_DEFAULT coldef_value_expr
   | K_COLLATE collation_name
   | foreign_key_clause
   | K_CHARACTER K_SET any_name
   | K_TITLE STRING_LITERAL
   | K_NOT? K_CASESPECIFIC
   | K_COMPRESS ( coldef_value_expr | ( '(' coldef_value_expr ( ',' coldef_value_expr)* ')' ) )?
   | K_FORMAT STRING_LITERAL
   | K_GENERATED ( K_BY K_DEFAULT | any_name /* K_ALWAYS */ ) K_AS any_name /* k_identity */ ( '(' (any_name | literal_value)* ')' )?
   )
 ;

coldef_value_expr
 : ( K_DATE | K_USER | K_TIME | K_TIMESTAMP )? (signed_number | literal_value | '(' expr ')' | any_name)
 ;
conflict_clause
 : ( K_ON K_CONFLICT ( K_ROLLBACK
                     | K_ABORT
                     | K_FAIL
                     | K_IGNORE
                     | K_REPLACE
                     )
   )?
 ;

/*
    SQLite understands the following binary operators, in order from highest to
    lowest precedence:
    ||
    *    /    %
    +    -
    <<   >>   &    |
    <    <=   >    >=
    =    ==   !=   <>   IS   IS NOT   IN   LIKE   GLOB   MATCH   REGEXP
    AND
    OR
*/
expr
returns [ Influences inf , ObjectName objname ]
 : literal_value /* Nothing */ #exprLiteral
// | BIND_PARAMETER

 | unary_operator operand1=expr /*unary*/  #expr1
 | operand1=expr '||' operand2=expr /*arithmetic*/  #exprOR
 | operand1=expr '*' '*' operand2=expr /*arithmetic*/  #expr2
 | operand1=expr ( '*' | '/' | K_MOD ) operand2=expr /*arithmetic*/  #expr2
 | operand1=expr ( '+' | '-' ) operand2=expr /*arithmetic*/  #expr2
 | operand1=expr ( '<<' | '>>' | '&' | '|' ) operand2=expr /*arithmetic*/  #expr2
 | operand1=expr ( '<' | '<=' | '>' | '>=' ) operand2=expr /*logical*/  #expr2poi
 | operand1=expr ( '=' | '==' | '!=' | '<>' | K_IS K_NOT? | K_NOT? K_IN | K_NOT? K_LIKE K_ANY? ) operand2=expr /*logical*/  #expr2poi
 /* td p495,508*/
 | operand1=expr K_AND operand2=expr /*logical*/  #expr2poi
 | operand1=expr K_OR operand2=expr /*logical*/  #expr2poi
 | '(' operand1=expr ')' /*unary*/  #expr1
// | cast_expr
 | K_CAST '(' operand1=expr K_AS ( type_name | data_attribute  )+ ')'/*unary*/  #expr1
 | sp = special_function /*unary*/  #exprSpecialFunction
 | function_name? '(' ( K_DISTINCT? ex+=expr ( ',' ex+=expr )* | '*' )? ')' (wx=window)? /*function as all direct*/ #exprFunction
 | operand1=expr K_COLLATE collation_name /*unary*/  #expr1
// | expr K_NOT? ( K_LIKE ( K_ANY )?  | K_GLOB | K_REGEXP | K_MATCH ) operand2=expr /*logical*/ {$opt=2;$inf.copypoi($operand1.ctx.inf); $inf.copypoi($operand2.ctx.inf);}
 | operand1=expr ( K_ISNULL | K_NOTNULL | K_NOT K_NULL ) /*unary*/ #expr1
 //| operand1=expr K_IS K_NOT? operand2=expr /*logical*/ {$opt=2;$inf.copypoi($operand1.ctx.inf); $inf.copypoi($operand2.ctx.inf);}
 | operand1=expr K_NOT? K_BETWEEN operand2=expr K_AND operand3=expr /*logical*/ #exprBetween
 | operand1=expr K_NOT? K_IN operand2=expr #expr2poi
//  '('  (
//           ss=select_stmt { $inf.copypoiselect($select_stmt.q); }
//        |  ex+=expr ( ',' ex+=expr )*  {  for (int i=0;i<$ex.size();++i)  {    $inf.copypoi($ex.get(i).inf); }    }
//  )  ')' #exprIn

 | ( ( K_NOT )? isexists=K_EXISTS )? '(' ss=select_stmt ')' #exprExists
 | K_CASE ex+=expr? ( K_WHEN ex+=expr K_THEN ax+=expr )+ ( K_ELSE ax+=expr )? K_END #exprCase

 | operand1=expr '(' ( K_CASESPECIFIC  | K_TITLE STRING_LITERAL )')' /*unary*/ #expr1
 | operand1=expr interval_def /* WTF IS THIS DATE CONVERSION */ /*unary*/ #expr1
 | operand1=expr '(' data_attribute ')' /*unary*/  #expr1
 | operand1=expr '(' ( K_MONTH | K_DATE | K_CHAR | K_INTEGER | K_DECIMAL ) ('(' NUMERIC_LITERAL( ',' NUMERIC_LITERAL)? ')' )? ')'/*unary*/  #expr1 // IMPLICIT CONVERSION
 | expr '%' ( K_ISOPEN | K_FOUND | K_NOTFOUND | K_ROWCOUNT) #exprCursorAttribute // PLSQL
 | obj = object_name  #exprObject
 //| raise_function
 //| analytic_expr
 ;
/*
object_name
 :( ( database_name '.' )? table_name '.' )? column_name
 ;
 */


interval_def
: ( K_MONTH | K_SECOND | K_HOUR ) ( '(' NUMERIC_LITERAL ')' )? ( K_TO ( K_MONTH | K_SECOND | K_HOUR ) ( '(' NUMERIC_LITERAL ')' )? )?
;


special_function
returns [ Influences inf ]
: K_TRIM '(' (any_name | literal_value )*? K_FROM? operand1=expr ')'/*unary*/ #special_function1
 | K_EXTRACT '(' any_name K_FROM operand1=expr ')'/*unary*/ #special_function1
 | K_RANK '(' operand1=expr ( K_ASC | K_DESC )? ')'/*unary*/ #special_function1
 | K_SUBSTRING '(' operand1=expr K_FROM operand2=expr (K_FOR operand3=expr)? ')' /*arithmetic3*/ #special_functionSubString
 | K_TRANSLATE_CHK '(' operand1=expr K_USING IDENTIFIER ')' /*unary*/ #special_function1
 | ( K_DATE | K_TIME | K_INTERVAL ) STRING_LITERAL /*nothing*/  #special_functionDateTime

 ;


object_name
returns [ ObjectName objname ]
 : ( d+=any_name '.' )* d+=any_name
 ;

/*
cast_expr
 : K_CAST '(' expr K_AS ( type_name | data_attribute  )+ ')'
 ;
*/

data_attribute
 : K_FORMAT literal_value
 ;

foreign_key_clause
 : K_REFERENCES foreign_table ( '(' column_name ( ',' column_name )* ')' )?
   ( ( K_ON ( K_DELETE | K_UPDATE ) ( K_SET K_NULL
                                    | K_SET K_DEFAULT
                                    | K_CASCADE
                                    | K_RESTRICT
                                    | K_NO K_ACTION )
     | K_MATCH name
     )
   )*
   ( K_NOT? K_DEFERRABLE ( K_INITIALLY K_DEFERRED | K_INITIALLY K_IMMEDIATE )? )?
 ;

/*
raise_function
 : K_RAISE '(' ( K_IGNORE
               | ( K_ROLLBACK | K_ABORT | K_FAIL ) ',' error_message )
           ')'
 ;
*/

indexed_column
 : column_name ( K_COLLATE collation_name )? ( K_ASC | K_DESC )?
 ;

table_constraint
 : ( K_CONSTRAINT name )?
   ( ( K_PRIMARY K_KEY | K_UNIQUE ) '(' indexed_column ( ',' indexed_column )* ')' conflict_clause
   | K_CHECK '(' expr ')'
   | K_FOREIGN K_KEY '(' column_name ( ',' column_name )* ')' foreign_key_clause
   )
 ;

with_clause
 : K_WITH K_RECURSIVE? cte_table_name K_AS '(' select_stmt ')' ( ',' cte_table_name K_AS '(' select_stmt ')' )*
 ;

qualified_table_name
 : ( database_name '.' )? table_name ( K_INDEXED K_BY index_name
                                     | K_NOT K_INDEXED )?
 ;

ordering_term_window
returns [ Influences inf ]
 : operand1=expr ( K_COLLATE collation_name )? ( K_ASC | K_DESC )?
 ;

pragma_value
 : signed_number
 | name
 | STRING_LITERAL
 ;

common_table_expression
returns [ SubQuery q ]
 : tn=table_name ( '(' cn+=column_name ( ',' cn+=column_name )* ')' )? K_AS '(' ss=select_stmt ')'

 ;

result_column
returns [ ResultColumn rc ]
 : '*' #result_columnAsterisk
 | tn=table_name '.' '*' #result_columnTableAsterisk
 | ex=expr ( K_AS? ca=column_alias )? #result_columnExpr
 ;

table_or_subquery
returns [ SubQuery q = new SubQuery() ]
 : ( dn=database_name '.' )? tn=table_name ( K_AS? ta=table_alias )? #table_or_subqueryTable
 | '(' ss=select_stmt ')' ( K_AS? ta=table_alias ( '(' cn+=column_name (',' cn+=column_name)* ')' )? )? #table_or_subquerySubQuery
 ;

join_clause
returns [ List<SubQuery> tables, Influences join_constraints ]
 : ts += table_or_subquery ( join_operator ts+= table_or_subquery ( K_ON ex += expr )? )*
 ;

join_operator
 : ','
 | K_NATURAL? (  K_INNER | K_CROSS | ( K_FULL | K_RIGHT|  K_LEFT ) K_OUTER? )? K_JOIN
 ;

/*
join_constraint
 : ( K_ON expr
   //| K_USING '(' column_name ( ',' column_name )* ')'
   )?
 ;
*/

select_core
returns [ SubQuery q ]
//locals [ List<SubQuery> tables, List<Integer> groupbypositions = new ArrayList<>()  ]
 : ( K_SELECT | K_SEL ) ( K_UNIQUE /* WTF */ | K_DISTINCT | K_ALL | K_TOP NUMERIC_LITERAL K_PERCENT? (K_WITH K_TIES)? )?
    r+=result_column  ( ',' r+=result_column  )*
   ( K_INTO variable_name ( ',' variable_name)* )? /* PL/SQL SELECT INTO */
   ( K_FROM jc=join_clause )?
   ( g1=grouping_by_clause )? /* WTF */
   ( w1=where_clause )?
   ( g2=grouping_by_clause )?
   ( h1=having_clause | q1=qualify_clause )?
   ( K_SAMPLE NUMERIC_LITERAL )?
   ( o1=order_by_clause )?
 //| K_VALUES '(' expr ( ',' expr )* ')' ( ',' '(' expr ( ',' expr )* ')' )*
 ;


order_by_clause
returns [ Influences inf , List<Integer> positions ]
 :  K_ORDER K_BY (  (nx+=NUMERIC_LITERAL | ex+=expr )  ( ',' (nx+=NUMERIC_LITERAL | ex+=expr ) )* ) ( K_ASC | K_DESC )?
 ;


where_clause
returns [ Influences inf ]
 : K_WHERE ex=expr
 ;

grouping_by_clause
returns [ Influences inf, List<Integer> positions ]
 :  K_GROUP K_BY (  (nx+=NUMERIC_LITERAL | ex+=expr )  ( ',' (nx+=NUMERIC_LITERAL | ex+=expr ) )* )
 ;

having_clause
returns [ Influences inf ]
 : K_HAVING ex=expr
 ;

/* TD FUNCTION P962 */
qualify_clause
returns [ Influences inf ]
 : K_QUALIFY ex=expr
 ;

/*
analytic_expr
 : function_name '(' ( ( object_name | literal_value ) ( ',' ( object_name | literal_value ) )* )? ')' window
 ;
*/

window
returns [ Influences inf ]
 : K_OVER '(' ( K_PARTITION K_BY ex+=expr ( ',' ex+=expr)* )? ( K_ORDER K_BY ox+=ordering_term_window ( ',' ox+=ordering_term_window)* )? ( K_ROWS any_name+ )? ')'
 ;


/*
window
 : K_OVER '(' window_0a? window_ab? window_b0? ')'
 ;


window_0a
 : K_PARTITION K_BY expr ( ',' expr)*
 ;

window_ab
 : K_ORDER K_BY ordering_term ( ',' ordering_term)*
 ;

window_b0
 : K_ROWS any_name+
 ;
*/

compound_operator
 : K_UNION
 | K_UNION K_ALL
 | K_INTERSECT
 | K_EXCEPT
 | K_MINUS
 ;

cte_table_name
 : table_name ( '(' column_name ( ',' column_name )* ')' )?
 ;

signed_number
 : ( '+' | '-' )? NUMERIC_LITERAL
 ;

literal_value
 : NUMERIC_LITERAL
 | STRING_LITERAL
 | BLOB_LITERAL
 | K_NULL
 | K_CURRENT_TIME
 | K_CURRENT_DATE
 | K_CURRENT_TIMESTAMP
 //| ( K_DATE | K_TIME ) STRING_LITERAL
 ;

unary_operator
 : '-'
 | '+'
 | '~'
 | K_NOT
 | K_UNIQUE
 ;

error_message
 : STRING_LITERAL
 ;

module_argument // TODO check what exactly is permitted here
 : expr
 | column_def
 ;

column_alias
 : IDENTIFIER
 | STRING_LITERAL
 ;

keyword
 : K_ABORT
 | K_ACTION
 | K_ADD
 | K_AFTER
 | K_ALL
 | K_ALTER
 | K_ANALYZE
 | K_AND
 | K_AS
 | K_ASC
 | K_ATTACH
 | K_AUTOINCREMENT
 | K_BEFORE
 | K_BEGIN
 | K_BETWEEN
 | K_BY
 | K_CASCADE
// | K_CASE
// | K_CAST
 | K_CHECK
 | K_COLLATE
 | K_COLUMN
 | K_COMMIT
 | K_CONFLICT
 | K_CONSTRAINT
// | K_CREATE
 | K_CROSS
 | K_CURRENT_DATE
 | K_CURRENT_TIME
 | K_CURRENT_TIMESTAMP
 | K_DATABASE
 | K_DEFAULT
 | K_DEFERRABLE
 | K_DEFERRED
 | K_DELETE
 | K_DESC
 | K_DETACH
 | K_DISTINCT
 | K_DROP
 | K_EACH
// | K_ELSE
 | K_END
 | K_ESCAPE
 | K_EXCEPT
 | K_EXCLUSIVE
 | K_EXISTS
 | K_EXPLAIN
 | K_FAIL
 | K_FOR
 | K_FOREIGN
// | K_FROM
 | K_FULL
 | K_GLOB
// | K_GROUP
// | K_HAVING
 | K_IF
 | K_IGNORE
 | K_IMMEDIATE
 | K_IN
 | K_INDEX
 | K_INDEXED
 | K_INITIALLY
 | K_INNER
 | K_INSERT
 | K_INSTEAD
 | K_INTERSECT
 | K_INTO
 | K_IS
 | K_ISNULL
 | K_JOIN
 | K_KEY
 | K_LEFT
 | K_LIKE
 | K_LIMIT
 | K_MATCH
 | K_NATURAL
 | K_NO
 | K_NOT
 | K_NOTNULL
 | K_NULL
 | K_OF
 //| K_OFFSET
 | K_ON
 | K_OR
 | K_ORDER
 | K_OUTER
 | K_PLAN
 | K_PRAGMA
 | K_PRIMARY
 | K_QUERY
 | K_RAISE
 | K_RECURSIVE
 | K_REFERENCES
 | K_REGEXP
 | K_REINDEX
 | K_RELEASE
 | K_RENAME
 | K_REPLACE
 | K_RESTRICT
 | K_RIGHT
 | K_ROLLBACK
 | K_ROW
 | K_SAVEPOINT
// | K_SELECT
 | K_SET
 | K_TABLE
 | K_TEMP
 | K_TEMPORARY
 | K_THEN
 | K_TO
 | K_TRANSACTION
 | K_TRIGGER
 | K_UNION
 | K_UNIQUE
 | K_UPDATE
 | K_USING
 | K_VACUUM
 | K_VALUES
 | K_VIEW
 | K_VIRTUAL
 | K_WHEN
// | K_WHERE
 | K_WITH
 | K_WITHOUT
 | K_TIMESTAMP
 | K_DATE
 | K_TIME
 | K_MONTH
 | K_RANK
 | K_DEL
 | K_CHARACTER
 | K_CHAR
 | K_INTEGER
 | K_DECIMAL
 | K_HOUR
 | K_SECOND
 | K_SAMPLE
 | K_INTERVAL
 | K_TITLE
 | K_BT
 | K_TYPE

 ;

// TODO check all names below

name
 : any_name
 ;

function_name
 : object_name
 ;

database_name
 : any_name
 ;

table_name
 : any_name
 ;

table_or_index_name
 : any_name
 ;

new_table_name
 : any_name
 ;

column_name
 : any_name
 ;

collation_name
 : any_name
 ;

foreign_table
 : any_name
 ;

index_name
 : any_name
 ;

trigger_name
 : any_name
 ;

view_name
 : any_name
 ;

module_name
 : any_name
 ;

pragma_name
 : any_name
 ;

savepoint_name
 : any_name
 ;

table_alias
 : any_name
 ;

transaction_name
 : any_name
 ;

variable_name
 : any_name
 ;

any_name
 : IDENTIFIER
 | keyword
 | STRING_LITERAL
 | MACROVAR
 //| '(' any_name ')'
 //| ( IDENTIFIER | keyword )? '$' '{'( IDENTIFIER | keyword ) '}'
 //| BIND_PARAMETER
 //| IDENTIFIER BIND_PARAMETER
 ;

swallow_to_semi :
        ~( ';' )+
    ;


SCOL : ';';
DOT : '.';
OPEN_PAR : '(';
CLOSE_PAR : ')';
COMMA : ',';
ASSIGN : '=';
STAR : '*';
PLUS : '+';
MINUS : '-';
TILDE : '~';
PIPE2 : '||';
DIV : '/';
MOD : '%';
LT2 : '<<';
GT2 : '>>';
AMP : '&';
PIPE : '|';
LT : '<';
LT_EQ : '<=';
GT : '>';
GT_EQ : '>=';
EQ : '==';
NOT_EQ1 : '!=';
NOT_EQ2 : '<>';

// http://www.sqlite.org/lang_keywords.html
K_ABORT : A B O R T;
K_ACTION : A C T I O N;
K_ADD : A D D;
K_AFTER : A F T E R;
K_ALL : A L L;
K_ALTER : A L T E R;
K_ANALYZE : A N A L Y Z E;
K_AND : A N D;
K_AS : A S;
K_ASC : A S C;
K_ATTACH : A T T A C H;
K_AUTOINCREMENT : A U T O I N C R E M E N T;
K_BEFORE : B E F O R E;
K_BEGIN : B E G I N;
K_BETWEEN : B E T W E E N;
K_BY : B Y;
K_CASCADE : C A S C A D E;
K_CASE : C A S E;
K_CAST : C A S T;
K_CHECK : C H E C K;
K_COLLATE : C O L L A T E;
K_COLUMN : C O L U M N;
K_COMMIT : C O M M I T;
K_CONFLICT : C O N F L I C T;
K_CONSTRAINT : C O N S T R A I N T;
K_CREATE : C R E A T E;
K_CROSS : C R O S S;
K_CURRENT_DATE : C U R R E N T '_' D A T E;
K_CURRENT_TIME : C U R R E N T '_' T I M E;
K_CURRENT_TIMESTAMP : C U R R E N T '_' T I M E S T A M P;
K_DATABASE : D A T A B A S E;
K_DEFAULT : D E F A U L T;
K_DEFERRABLE : D E F E R R A B L E;
K_DEFERRED : D E F E R R E D;
K_DELETE : D E L E T E;
K_DESC : D E S C;
K_DETACH : D E T A C H;
K_DISTINCT : D I S T I N C T;
K_DROP : D R O P;
K_EACH : E A C H;
K_ELSE : E L S E;
K_END : E N D;
K_ESCAPE : E S C A P E;
K_EXCEPT : E X C E P T;
K_EXCLUSIVE : E X C L U S I V E;
K_EXISTS : E X I S T S;
K_EXPLAIN : E X P L A I N;
K_FAIL : F A I L;
K_FOR : F O R;
K_FOREIGN : F O R E I G N;
K_FROM : F R O M;
K_FULL : F U L L;
K_GLOB : G L O B;
K_GROUP : G R O U P;
K_HAVING : H A V I N G;
K_IF : I F;
K_IGNORE : I G N O R E;
K_IMMEDIATE : I M M E D I A T E;
K_IN : I N;
K_INDEX : I N D E X;
K_INDEXED : I N D E X E D;
K_INITIALLY : I N I T I A L L Y;
K_INNER : I N N E R;
K_INSERT : I N S E R T;
K_INSTEAD : I N S T E A D;
K_INTERSECT : I N T E R S E C T;
K_INTO : I N T O;
K_IS : I S;
K_ISNULL : I S N U L L;
K_JOIN : J O I N;
K_KEY : K E Y;
K_LEFT : L E F T;
K_LIKE : L I K E;
K_LIMIT : L I M I T;
K_MATCH : M A T C H;
K_NATURAL : N A T U R A L;
K_NO : N O;
K_NOT : N O T;
K_NOTNULL : N O T N U L L;
K_NULL : N U L L;
K_OF : O F;
//K_OFFSET : O F F S E T;
K_ON : O N;
K_OR : O R;
K_ORDER : O R D E R;
K_OUTER : O U T E R;
K_PLAN : P L A N;
K_PRAGMA : P R A G M A;
K_PRIMARY : P R I M A R Y;
K_QUERY : Q U E R Y;
K_RAISE : R A I S E;
K_RECURSIVE : R E C U R S I V E;
K_REFERENCES : R E F E R E N C E S;
K_REGEXP : R E G E X P;
K_REINDEX : R E I N D E X;
K_RELEASE : R E L E A S E;
K_RENAME : R E N A M E;
K_REPLACE : R E P L A C E;
K_RESTRICT : R E S T R I C T;
K_RIGHT : R I G H T;
K_ROLLBACK : R O L L B A C K;
K_ROW : R O W;
K_SAVEPOINT : S A V E P O I N T;
K_SELECT : S E L E C T;
K_SET : S E T;
K_TABLE : T A B L E;
K_TEMP : T E M P;
K_TEMPORARY : T E M P O R A R Y;
K_THEN : T H E N;
K_TO : T O;
K_TRANSACTION : T R A N S A C T I O N;
K_TRIGGER : T R I G G E R;
K_UNION : U N I O N;
K_UNIQUE : U N I Q U E;
K_UPDATE : U P D A T E;
K_USING : U S I N G;
K_VACUUM : V A C U U M;
K_VALUES : V A L U E S;
K_VIEW : V I E W;
K_VIRTUAL : V I R T U A L;
K_WHEN : W H E N;
K_WHERE : W H E R E;
K_WITH : W I T H;
K_WITHOUT : W I T H O U T;

K_MULTISET : M U L T I S E T;
K_VOLATILE : V O L A T I L E;
K_PRESERVE : P R E S E R V E;
K_ROWS : R O W S;
K_FORMAT : F O R M A T;
K_DATE : D A T E;
K_QUALIFY: Q U A L I F Y;
K_OVER : O V E R;
K_PARTITION: P A R T I T I O N;
K_GLOBAL: G L O B A L;
K_LOG: L O G;
K_CHARACTER: C H A R A C T E R;
K_DATA: D A T A;
K_CASESPECIFIC: C A S E S P E C I F I C;
K_CHECKSUM: C H E C K S U M;
K_TITLE: T I T L E;
K_COMMENT: C O M M E N T;
K_TIMESTAMP: T I M E S T A M P;
K_TIME: T I M E;
K_LOCKING: L O C K I N G;
K_LOCK: L O C K;
K_ACCESS: A C C E S S;
K_COMPRESS: C O M P R E S S;
K_COLLECT: C O L L E C T;
K_TRIM: T R I M;
K_JOURNAL: J O U R N A L;
K_TOP: T O P;
K_PERCENT: P E R C E N T;
K_TIES: T I E S;
K_SEL: S E L;
K_USER: U S E R;
K_EXTRACT: E X T R A C T;
K_FALLBACK: F A L L B A C K;
K_MOD: M O D;
K_MONTH: M O N T H;
K_RANK: R A N K;
K_DEL: D E L;
K_GENERATED: G E N E R A T E D;
K_GRANT: G R A N T;
K_SHOW: S H O W;
K_MINUS: M I N U S;
K_SUBSTRING: S U B S T R I N G;
K_CALL: C A L L;
K_ANY: A N Y;
K_TRANSLATE_CHK : T R A N S L A T E '_' C H K;
K_CHAR : C H A R;
K_BYTE : B Y T E;
K_INTEGER: I N T E G E R;
K_DECIMAL: D E C I M A L;
K_HOUR: H O U R;
K_SECOND: S E C O N D;
K_SAMPLE: S A M P L E;
K_MACRO: M A C R O;
K_TRUNCATE: T R U N C A T E;
K_INTERVAL: I N T E R V A L;

K_FUNCTION: F U N C T I O N;
K_PROCEDURE: P R O C E D U R E;
K_RETURN: R E T U R N;
K_OUT: O U T;
K_NOCOPY: N O C O P Y;
K_CURSOR: C U R S O R;
K_CONSTANT: C O N S T A N T;
K_RECORD: R E C O R D;
K_TYPE: T Y P E;
K_SUBTYPE: S U B T Y P E;
K_ROWTYPE: R O W T Y P E;
K_REF: R E F;
K_DETERMINISTIC: D E T E R M I N I S T I C;
K_PIPELINED: P I P E L I N E D;
K_PARALLEL_ENABLE: P A R A L L E L '_' E N A B L E;
K_RESULT_CACHE: R E S U L T '_' C A C H E;
K_EXCEPTION: E X C E P T I O N;
K_OTHERS: O T H E R S;
K_LOOP: L O O P;

K_CLOSE : C L O S E;
K_CONTINUE: C O N T I N U E;
K_EXECUTE: E X E C U T E;
K_FETCH: F E T C H;
K_BULK: B U L K;
K_RETURNING: R E T U R N I N G;
K_ELSIF: E L S I F;
K_DECLARE: D E C L A R E;
K_OPEN: O P E N;
K_GOTO: G O T O;
K_FORALL: F O R A L L;
K_SAVE: S A V E;
K_EXCEPTIONS: E X C E P T I O N S;
K_INDICES: I N D I C E S;
K_ISOPEN: I S O P E N;
K_FOUND: F O U N D;
K_NOTFOUND: N O T F O U N D;
K_ROWCOUNT: R O W C O U N T;
K_SEQUENCE: S E Q U E N C E;
K_EXIT: E X I T;
K_WHILE: W H I L E;
K_ARRAY: A R R A Y;
K_VARYING: V A R Y I N G;
K_VARRAY: V A R R A Y;

IDENTIFIER
 : '"' (~'"' | '""')* '"'
 | '`' (~'`' | '``')* '`'
 | '[' ~']'* ']'
 | [a-zA-Z_\$] ( '{' [a-zA-Z_] [a-zA-Z_0-9]* '}' | [a-zA-Z_0-9\$] )* // TODO check: needs more chars in set
 ;

MACROVAR
 : ':' [a-zA-Z_] [a-zA-Z_0-9]*
 ;

NUMERIC_LITERAL
 : DIGIT+ ( '.' DIGIT* )? ( E [-+]? DIGIT+ )?
 | '.' DIGIT+ ( E [-+]? DIGIT+ )?
 ;

/*
BIND_PARAMETER
 : '?' DIGIT*
 | [:@$] IDENTIFIER
 ;
*/
STRING_LITERAL
 : '\'' ( ~'\'' | '\'\'' )* '\''
 ;

BLOB_LITERAL
 : X STRING_LITERAL
 ;

SINGLE_LINE_COMMENT
 : '--' ~[\r\n]* -> channel(HIDDEN)
 ;

MULTILINE_COMMENT
 : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN)
 ;

SPACES
 : [ \u000B\t\r\n] -> channel(HIDDEN)
 ;

UNEXPECTED_CHAR
 : .
 ;

fragment DIGIT : [0-9];

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];