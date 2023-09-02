package visitor.codeGenerator;

import ast.node.Program;
import ast.node.declaration.ArgDeclaration;
import ast.node.declaration.FuncDeclaration;
import ast.node.declaration.MainDeclaration;
import ast.node.expression.BinaryExpression;
import ast.node.expression.FunctionCall;
import ast.node.expression.Identifier;
import ast.node.expression.UnaryExpression;
import ast.node.expression.operators.BinaryOperator;
import ast.node.expression.operators.UnaryOperator;
import ast.node.expression.values.IntValue;
import ast.node.statement.AssignStmt;
import ast.node.statement.ReturnStmt;
import ast.node.statement.Statement;
import ast.node.statement.VarDecStmt;
import visitor.Visitor;

import java.util.ArrayList;
import java.util.HashMap;

public class CodeGenerator extends Visitor<String> {

    private ArrayList<String> slots = new ArrayList<>();

    int max_stack = 2;
    int current_stack = 1;
    int max_local = 0;

    private int slotsOf(String variable){
        int index = slots.indexOf(variable);
        if (index == -1){
            slots.add(variable);
            max_local += 1;
            return slots.size() - 1;
        }
        return index;
    }
    private String getIntInst(int num){
        current_stack += 1;
        if (current_stack > max_stack)
            max_stack = current_stack;
        if (num <= 5 && num >=0)
            return "iconst_" + Integer.toString(num);
        if (num <= (1 << 7) - 1 && num >= -(1 << 7))
            return "bipush " + Integer.toString(num);
        if (num <= (1 << 15) - 1 && num >= -(1 << 15))
            return "sipush " + Integer.toString(num);
        return "ldc " + Integer.toString(num);
    }

    private String getOprInst(BinaryOperator binaryOperator){
        current_stack -= 1;

        if(binaryOperator.equals(BinaryOperator.add))
            return "iadd";
        if (binaryOperator.equals(BinaryOperator.sub))
            return "isub";
        if (binaryOperator.equals(BinaryOperator.mult))
            return "imul";
        if (binaryOperator.equals(BinaryOperator.div))
            return "idiv";

        return "irem";
    }

    @Override
    public String visit(Program program) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("""
                .class public Main
                .super java/lang/Object
                
                .method public <init>()V
                .limit stack 1
                .limit locals 1
                    aload_0
                    invokenonvirtual java/lang/Object/<init>()V
                    return
                .end method
                
                """);
        buffer.append(program.getFuncs().get(0).accept(this));
        buffer.append('\n');
        buffer.append(program.getMain().accept(this));
        return buffer.toString();
    }

    @Override
    public String visit(MainDeclaration mainDeclaration) {
        return """
                .method public static main([Ljava/lang/String;)V
                    .limit stack 1
                    .limit locals 1
                         
                    invokestatic Main/calculate()I
                    pop
                    return
                .end method
                """;
    }

    @Override
    public String visit(FuncDeclaration funcDeclaration) {
        String func_name = funcDeclaration.getName().getName();
        StringBuilder buffer = new StringBuilder();
        StringBuilder stmts = new StringBuilder();
        buffer.append(".method public static ").append(func_name).append("()I").append('\n');

        for (Statement statement : funcDeclaration.getStatements()){
            if (statement instanceof VarDecStmt || statement instanceof AssignStmt || statement instanceof ReturnStmt)
                stmts.append(statement.accept(this)).append('\n');
        }
        buffer.append(".limit stack ").append(max_stack).append('\n');
        buffer.append(".limit locals ").append(max_local).append("\n\n");
        buffer.append(stmts);
        buffer.append(".end method").append('\n');
        return buffer.toString();
    }

    @Override
    public String visit(AssignStmt assignStmt) {
        StringBuilder buffer = new StringBuilder();
        if (assignStmt.getRValue() != null)
            buffer.append(assignStmt.getRValue().accept(this));
        Identifier identifier = (Identifier)assignStmt.getLValue();
        int slot = slotsOf(identifier.getName());
        if (slot <= 3)
            buffer.append('\t').append("istore_").append(slot).append('\n');
        else
            buffer.append('\t').append("istore ").append(slot).append('\n');
        current_stack -= 1;

        return buffer.toString();
    }
    @Override
    public String visit(ReturnStmt returnStmt) {
        StringBuilder buffer = new StringBuilder();
        if (returnStmt.getExpression() != null)
            buffer.append(returnStmt.getExpression().accept(this));
        buffer.append('\t').append("ireturn").append('\n');
        return buffer.toString();
    }

    @Override
    public String visit(VarDecStmt varDecStmt) {
        StringBuilder buffer = new StringBuilder();
        if (varDecStmt.getInitialExpression() != null) {
            current_stack -= 1;
            buffer.append(varDecStmt.getInitialExpression().accept(this));
            int slot = slotsOf(varDecStmt.getIdentifier().getName());
            if (slot <= 3)
                buffer.append('\t').append("istore_").append(slot).append('\n');
            else
                buffer.append('\t').append("istore ").append(slot).append('\n');
        }
        return buffer.toString();
    }
    @Override
    public String visit(BinaryExpression binaryExpression) {
        StringBuilder buffer = new StringBuilder();

        if (binaryExpression.getLeft() != null)
            buffer.append(binaryExpression.getLeft().accept(this));

        if (binaryExpression.getRight() != null)
            buffer.append(binaryExpression.getRight().accept(this));

        buffer.append('\t').append(getOprInst(binaryExpression.getBinaryOperator())).append('\n');
        return buffer.toString();
    }
    @Override
    public String visit(UnaryExpression unaryExpression) {
        StringBuilder buffer = new StringBuilder();
        if (unaryExpression.getOperand() != null)
            buffer.append(unaryExpression.getOperand().accept(this));
        if (unaryExpression.getUnaryOperator().equals(UnaryOperator.minus))
            buffer.append('\t').append("ineg").append('\n');
        return buffer.toString();
    }
    @Override
    public String visit(Identifier identifier) {
        StringBuilder buffer = new StringBuilder();
        int slot = slotsOf(identifier.getName());
        current_stack += 1;
        if (max_stack < current_stack)
            max_stack = current_stack;

        if (slot <= 3)
            buffer.append('\t').append("iload_").append(slot).append('\n');
        else
            buffer.append('\t').append("iload ").append(slot).append('\n');
        return buffer.toString();
    }

    @Override
    public String visit(IntValue value) {
        return "\t" + getIntInst(value.getConstant()) + "\n";
    }
}
