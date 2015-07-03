# Mellex

Mellex is a simple tool to extract data flow information in SQL scripts.
It parses SQL scripts and outputs the data flow information of each script.

### Installation

Compile the antlr grammar file

> antlr4 -o .../src/main/java/io/github/jhg543/mellex/antlrparser -package io.github.jhg543.mellex.antlrparser -Dlanguage=Java -listener -no-visitor -lib .../src/main/resources/ .../src/main/resources/DefaultSQL.g4

Complie the java source code using maven

> mvn clean package

find the jar in "target" directory

### Usage

See code of io.github.jhg543.mellex.operations.ObjectList class

### Design

Use antlr parser to get the AST. Then  visit the AST to get data flow of each SQL statements. Then link the flow together.

### Notes

- This project is Currently in alpha.
- the grammar only supports a subset of SQL syntax, and is tested upon some Teradata SQL Scripts.
- the grammar will accept some invalid SQL statements.  Do not use it to check the correctness of SQL.
- no stored procedure and UDF support. It will be added later.