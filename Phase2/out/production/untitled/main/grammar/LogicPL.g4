grammar LogicPL;

@header{
import ast.node.*;
import ast.node.expression.*;
import ast.node.statement.*;
import ast.node.declaration.*;
import ast.node.expression.values.*;
import ast.node.expression.operators.*;
import ast.type.primitiveType.*;
import ast.type.*;
}

program returns[Program p]:
    {$p = new Program(); $p.setLine(0);}
    (f = functionDec {$p.addFunc($f.functionDeclaration);})*
    main = mainBlock {$p.setMain($main.main) ;}
    ;

functionDec returns[FuncDeclaration functionDeclaration]:
    {ArrayList<ArgDeclaration> args = new ArrayList<>();
     ArrayList<Statement> statements = new ArrayList<>();}
    FUNCTION name = identifier
    LPAR (arg1 = functionVarDec {args.add($arg1.argDeclaration);}
    (COMMA arg = functionVarDec {args.add($arg.argDeclaration) ;})*)?
    RPAR COLON returnType = type
    LBRACE ((stmt = statement   {statements.add($stmt.stmt)    ;})+) RBRACE
    {$functionDeclaration = new FuncDeclaration($name.id, $returnType.t, args, statements);
     $functionDeclaration.setLine($name.id.getLine())          ;}
    ;

functionVarDec returns [ArgDeclaration argDeclaration]:
    t = type arg_iden = identifier
    {$argDeclaration = new ArgDeclaration($arg_iden.id, $t.t);
     $argDeclaration.setLine($arg_iden.id.getLine());}
    ;

mainBlock returns [MainDeclaration main]:
    {ArrayList<Statement> mainStmts = new ArrayList<>();}
    m = MAIN LBRACE (s = statement {mainStmts.add($s.stmt);})+ RBRACE
    {$main = new MainDeclaration(mainStmts); $main.setLine($m.getLine());}
    ;

statement returns [Statement stmt]:
    a=assignSmt                 {$stmt = $a.assign     ;}
    | ( p=predicate SEMICOLON ) {$stmt = $p.pred       ;}
    | i=implication             {$stmt = $i.imp        ;}
    | r=returnSmt               {$stmt = $r.ret        ;}
    | pr=printSmt               {$stmt = $pr.print_stmt;}
    | l=forLoop                 {$stmt = $l.loop       ;}
    | v=localVarDeclaration     {$stmt = $v.var        ;}
    ;

assignSmt returns [AssignStmt assign]:
    v=variable ASSIGN nv=expression SEMICOLON
    {$assign = new AssignStmt($v.var,  $nv.expr);
     $assign.setLine($ASSIGN.getLine())         ;}
    ;

variable returns [Variable var]:
    id=identifier {$var = $id.id                                        ;}
    | name=identifier LBRACKET idx=expression RBRACKET
                  {$var = new ArrayAccess($name.id.getName(), $idx.expr);
                   $var.setLine($name.id.getLine())                     ;}
    ;

localVarDeclaration returns [Statement var]:
    v=varDeclaration       {$var = $v.var    ;}
    | arr=arrayDeclaration {$var = $arr.array;}
    ;

varDeclaration returns [VarDecStmt var]:
    t=type id=identifier    {$var = new VarDecStmt($id.id, $t.t)  ;
                             $var.setLine($id.id.getLine())       ;}
    (ASSIGN expr=expression {$var.setInitialExpression($expr.expr);})?
    SEMICOLON
    ;

arrayDeclaration returns [ArrayDecStmt array]:
    t=type LBRACKET l=INT_NUMBER RBRACKET id=identifier
                            {$array = new ArrayDecStmt($id.id, $t.t, $l.int);
                             $array.setLine($id.id.getLine())               ;}
    (init=arrayInitialValue {$array.setInitialValues($init.array)           ;}
    )? SEMICOLON
    ;

arrayInitialValue returns [ArrayList<Expression> array]:
    ASSIGN art=arrayList  {$array = $art.array;}
    ;

arrayList returns [ArrayList<Expression> array]:
    {$array = new ArrayList<Expression>()    ;}
    LBRACKET
        ( v1=value       {$array.add($v1.val);}
        | id1=identifier {$array.add($id1.id);}
        )
        ( COMMA
        ( v2=value       {$array.add($v2.val);}
        | id2=identifier {$array.add($id2.id);}
        )
        )*
    RBRACKET
    ;

printSmt returns [PrintStmt print_stmt]:
    PRINT LPAR arg=printExpr RPAR SEMICOLON
    {$print_stmt = new PrintStmt($arg.print_expr);
     $print_stmt.setLine($PRINT.getLine())       ;}
    ;

printExpr returns [Expression print_expr]:
    v=variable    {$print_expr = $v.var       ;}
    | q=query     {$print_expr = $q.query_expr;}
    ;

query returns [QueryExpression query_expr]:
    q1=queryType1   {$query_expr = $q1.query1;}
    | q2=queryType2 {$query_expr = $q2.query2;}
    ;

queryType1 returns [QueryExpression query1]:
    LBRACKET QUARYMARK pid=predicateIdentifier LPAR v=variable RPAR RBRACKET
    {$query1 = new QueryExpression($pid.id);
     $query1.setLine($QUARYMARK.getLine());
     $query1.setVar($v.var)               ;}
    ;

queryType2 returns [QueryExpression query2]:
    LBRACKET pid=predicateIdentifier LPAR QUARYMARK RPAR RBRACKET
    {$query2 = new QueryExpression($pid.id);
     $query2.setLine($QUARYMARK.getLine()) ;}
    ;

returnSmt returns [ReturnStmt ret]:
    RETURN          {$ret = new ReturnStmt(  null)  ;}
    ( v=value       {$ret = new ReturnStmt($v.val)  ;}
    | id=identifier {$ret = new ReturnStmt($id.id)  ;}
    )? SEMICOLON    {$ret.setLine($RETURN.getLine());}
    ;

forLoop returns [ForloopStmt loop]:
    {ArrayList<Statement> bodyStmts = new ArrayList<>()    ;}
    FOR LPAR iter=identifier COLON set=identifier RPAR
    LBRACE ((stmt=statement {bodyStmts.add($stmt.stmt);})*) RBRACE
    {$loop = new ForloopStmt($iter.id, $set.id, bodyStmts) ;
     $loop.setLine($FOR.getLine())                         ;}
    ;

predicate returns [PredicateStmt pred]:
    pid=predicateIdentifier LPAR v=variable RPAR
    {$pred = new PredicateStmt($pid.id, $v.var); $pred.setLine($pid.id.getLine());}
    ;

implication returns [ImplicationStmt imp]:
    {ArrayList<Statement> stmts = new ArrayList<>();}
    LPAR e=expression RPAR ARROW LPAR ((st=statement {stmts.add($st.stmt);})+) RPAR
    {$imp = new ImplicationStmt($e.expr, stmts)    ;
     $imp.setLine($ARROW.getLine())                ;}
    ;

expression returns [Expression expr]:
    a=andExpr r=expression2 {if($r.expr != null)
                            {$expr = new BinaryExpression($a.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                             $expr.setLine($r.expr.getLine());}
                             else
                            {$expr = $a.expr;}}
    ;

expression2 returns [BinaryExpression expr] locals [BinaryExpression bxpr]:
    OR a=andExpr r=expression2 {if($r.expr != null)
                               {$bxpr = new BinaryExpression($a.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                                $bxpr.setLine($r.expr.getLine());
                                $expr = new BinaryExpression(null, $bxpr, BinaryOperator.or);}
                                else
                               {$expr = new BinaryExpression(null, $a.expr, BinaryOperator.or);}}
                               {$expr.setLine($OR.getLine());}
    | /* epsilon */            {$expr = null;}
    ;

andExpr returns [Expression expr]:
    e=eqExpr r=andExpr2 {if($r.expr != null)
                        {$expr = new BinaryExpression($e.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                         $expr.setLine($r.expr.getLine());}
                         else
                        {$expr = $e.expr;}}
    ;

andExpr2 returns [BinaryExpression expr] locals [BinaryExpression bxpr]:
    AND e=eqExpr r=andExpr2 {if($r.expr != null)
                            {$bxpr = new BinaryExpression($e.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                             $bxpr.setLine($r.expr.getLine());
                             $expr = new BinaryExpression(null, $bxpr, BinaryOperator.and);}
                             else
                            {$expr = new BinaryExpression(null, $e.expr, BinaryOperator.and);}}
                            {$expr.setLine($AND.getLine());} // TODO: It may be wrong to pass null here()3 line upper [$expr = new BinaryExpression(null, $bxpr, BinaryOperator.and);}]
    | /* epsilon */         {$expr = null;}
    ;

eqExpr returns [Expression expr]:
    c=compExpr r=eqExpr2 {if($r.expr != null)
                         {$expr = new BinaryExpression($c.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                          $expr.setLine($r.expr.getLine());}
                          else
                         {$expr =       $c.expr;}}
    ;

eqExpr2 returns [BinaryExpression expr] locals [BinaryOperator op, BinaryExpression bxpr]:
    ( oo=EQ  {$op = BinaryOperator.eq;}
    | oo=NEQ {$op = BinaryOperator.neq;}
    ) c=compExpr r=eqExpr2 {if($r.expr != null)
                           {$bxpr = new BinaryExpression($c.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                            $bxpr.setLine($r.expr.getLine());     $expr = new BinaryExpression(null, $bxpr, $op);}
                            else
                           {$expr = new BinaryExpression(null,    $c.expr, $op);}}
                           {$expr.setLine($oo.getLine());}
    | /* epsilon */        {$expr = null;}
    ;

compExpr returns [Expression expr]:
    a=additive r=compExpr2   {if($r.expr != null)
                             {$expr = new BinaryExpression($a.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                              $expr.setLine($r.expr.getLine());}
                              else
                             {$expr = $a.expr;}}
    ;

compExpr2 returns [BinaryExpression expr] locals [BinaryOperator op, BinaryExpression bxpr]:
    ( oo=LT                  {$op = BinaryOperator.lt;}
    | oo=LTE                 {$op = BinaryOperator.lte;}
    | oo=GT                  {$op = BinaryOperator.gt;}
    | oo=GTE                 {$op = BinaryOperator.gte;}
    ) a=additive r=compExpr2 {if($r.expr != null)
                             {$bxpr = new BinaryExpression($a.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                              $bxpr.setLine($r.expr.getLine());   $expr=new BinaryExpression(null, $bxpr, $op);}
                              else
                             {$expr = new BinaryExpression(null,  $a.expr, $op);}}
                             {$expr.setLine($oo.getLine());}
    | /* epsilon */          {$expr=null;}
    ;

additive returns [Expression expr]:
    m=multicative r=additive2 {if($r.expr != null)
                              {$expr=new BinaryExpression($m.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                               $expr.setLine($r.expr.getLine());}
                               else
                              {$expr = $m.expr;}}
    ;

additive2 returns [BinaryExpression expr] locals [BinaryOperator op, BinaryExpression bxpr]:
    ( oo=PLUS                   {$op=BinaryOperator.add      ;}
    | oo=MINUS                  {$op=BinaryOperator.sub      ;}
    ) m=multicative r=additive2 {if($r.expr != null)
                                {$bxpr=new BinaryExpression($m.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                                 $bxpr.setLine($r.expr.getLine());   $expr=new BinaryExpression(null, $bxpr, $op);}
                                 else
                                {$expr=new BinaryExpression(null,    $m.expr, $op);}}
                                {$expr.setLine($oo.getLine());}
    | /* epsilon */             {$expr = null                ;}
    ;

multicative returns [Expression expr]:
    u=unary r=multicative2 {if($r.expr != null)
                           {$expr = new BinaryExpression($u.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                            $expr.setLine($r.expr.getLine());}
                            else
                           {$expr = $u.expr                 ;}}
    ;

multicative2 returns [BinaryExpression expr] locals [BinaryOperator op, BinaryExpression bxpr]:
    ( oo=MULT                {$op=BinaryOperator.mult      ;}
    | oo=MOD                 {$op=BinaryOperator.mod       ;}
    | oo=DIV                 {$op=BinaryOperator.div       ;}
    ) u=unary r=multicative2 {if($r.expr!=null)
                             {$bxpr = new BinaryExpression($u.expr, $r.expr.getRight(), $r.expr.getBinaryOperator());
                              $bxpr.setLine($r.expr.getLine());        $expr = new BinaryExpression(null, $bxpr, $op);}
                              else
                             {$expr = new BinaryExpression(null,    $u.expr, $op);}}
                             {$expr.setLine($oo.getLine()) ;}
    | /* epsilon */          {$expr = null                 ;}
    ;

unary returns [Expression expr] locals [UnaryOperator op]:
    o=other      {$expr=$o.expr                            ;}
    | ( oo=PLUS  {$op=UnaryOperator.plus                   ;}
      | oo=MINUS {$op=UnaryOperator.minus                  ;}
      | oo=NOT   {$op=UnaryOperator.not                    ;}
      ) e=other  {$expr = new UnaryExpression($op, $e.expr);
                  $expr.setLine($oo.getLine())             ;}
    ;

other returns [Expression expr]:
    LPAR e=expression RPAR {$expr=$e.expr          ;}
    | var=variable         {$expr=$var.var         ;}
    | val=value            {$expr=$val.val         ;}
    | q1=queryType1        {$expr=$q1.query1        ;}
    | fc=functionCall      {$expr=$fc.function_call;}
    ;

functionCall returns [FunctionCall function_call]:
    {ArrayList<Expression> exprs = new ArrayList<>()      ;}
    name=identifier LPAR (e=expression {exprs.add($e.expr);} (COMMA es=expression {exprs.add($es.expr);})*)? RPAR
    {$function_call = new FunctionCall(exprs,$name.id)    ;
     $function_call.setLine($name.id.getLine())           ;}
    ;

value returns [Value val]:
    p=numericValue         {$val = $p.val;}
    | t=TRUE               {$val = new BooleanValue(true);  $val.setLine($t.getLine());}
    | f=FALSE              {$val = new BooleanValue(false); $val.setLine($f.getLine());}
    | MINUS n=numericValue {$n.val.negateConstant();        $val =       $n.val       ;}
    ;

numericValue returns [Value val]:
    i=INT_NUMBER     {$val = new IntValue($i.int);                     $val.setLine($i.getLine());}
    | f=FLOAT_NUMBER {$val = new FloatValue(Float.parseFloat($f.text)); $val.setLine($f.getLine());}
    ;

identifier returns [Identifier id]:
    IDENTIFIER     {$id = new Identifier($IDENTIFIER.text);
                    $id.setLine($IDENTIFIER.getLine())    ;}
    ;

predicateIdentifier returns [Identifier id]:
    pid=PREDICATE_IDENTIFIER {$id = new Identifier($pid.text);
                              $id.setLine($pid.getLine())    ;}
    ;

type returns [Type t]:
    BOOLEAN {$t = new BooleanType();}
    | INT   {$t = new IntType();    }
    | FLOAT {$t = new FloatType();  }
    ;




FUNCTION : 'function';
BOOLEAN : 'boolean';
INT : 'int';
FLOAT: 'float';
MAIN: 'main';
PRINT: 'print';
RETURN: 'return';
FOR: 'for';
TRUE: 'true';
FALSE: 'false';

LPAR: '(';
RPAR: ')';
COLON: ':';
COMMA: ',';
LBRACE: '{';
RBRACE: '}';
SEMICOLON: ';';
ASSIGN: '=';
LBRACKET: '[';
RBRACKET: ']';
QUARYMARK: '?';
ARROW: '=>';
OR: '||';
AND: '&&';
EQ: '==';
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';
PLUS: '+';
MINUS: '-';
MULT: '*';
DIV: '/';
MOD: '%';
NEQ: '!=';
NOT: '!';


WS : [ \t\r\n]+ -> skip ;
COMMENT : '#' ~[\r\n]* -> skip ;

IDENTIFIER : [a-z][a-zA-Z0-9_]* ;
PREDICATE_IDENTIFIER : [A-Z][a-zA-Z0-9]* ;
INT_NUMBER : [0-9]+;
FLOAT_NUMBER: ([0-9]*[.])?[0-9]+;