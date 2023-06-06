package visitor.typeAnalyzer;

import ast.node.expression.*;
import ast.node.expression.operators.BinaryOperator;
import ast.node.expression.operators.UnaryOperator;
import ast.node.expression.values.IntValue;
import ast.node.expression.values.FloatValue;
import ast.node.expression.values.BooleanValue;
import ast.node.statement.AssignStmt;
import ast.node.statement.ReturnStmt;
import ast.type.NoType;
import ast.type.Type;
import ast.type.primitiveType.BooleanType;
import ast.type.primitiveType.FloatType;
import ast.type.primitiveType.IntType;
import compileError.CompileError;
import compileError.Type.FunctionNotDeclared;
import compileError.Type.UnsupportedOperandType;
import compileError.Type.VarNotDeclared;
import symbolTable.SymbolTable;
import symbolTable.itemException.ItemNotFoundException;
import symbolTable.symbolTableItems.ArrayItem;
import symbolTable.symbolTableItems.FunctionItem;
import symbolTable.symbolTableItems.VariableItem;
import visitor.Visitor;

import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    public ArrayList<CompileError> typeErrors;
    public ExpressionTypeChecker(ArrayList<CompileError> typeErrors){
        this.typeErrors = typeErrors;
    }

    public boolean sameType(Type el1, Type el2){
        if (el1 instanceof NoType || el2 instanceof NoType) {
            return true;
        }
        if (el1 instanceof BooleanType && el2 instanceof BooleanType) {
            return true;
        }
        if (el1 instanceof FloatType && el2 instanceof FloatType) {
            return true;
        }
        if (el1 instanceof IntType && el2 instanceof IntType) {
            return true;
        }
        return false;
    }

    public boolean isLvalue(Expression expr){
        return (expr instanceof Identifier) || (expr instanceof ArrayAccess);
    }


    @Override
    public Type visit(UnaryExpression unaryExpression) {

        Expression uExpr = unaryExpression.getOperand();
        Type expType = uExpr.accept(this);
        UnaryOperator operator = unaryExpression.getUnaryOperator();

        if (operator.equals(UnaryOperator.minus) && (expType instanceof IntType)) {
            return new IntType();
        }
        if (operator.equals(UnaryOperator.minus) && (expType instanceof FloatType)) {
            return new FloatType();
        }
        if (operator.equals(UnaryOperator.plus) && (expType instanceof IntType)) {
                return new IntType();
        }
        if (operator.equals(UnaryOperator.plus) && (expType instanceof FloatType)) {
            return new FloatType();
        }
        if (operator.equals(UnaryOperator.not) && (expType instanceof BooleanType)) {
            return new BooleanType();
        }

        if (!(expType instanceof NoType)) {
            typeErrors.add(new UnsupportedOperandType(unaryExpression.getLine(), operator.name()));
        }

        return new NoType();
    }

    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Expression left_expr = binaryExpression.getLeft();
        Expression right_expr = binaryExpression.getRight();

        Type left_expr_type = left_expr.accept(this);
        Type right_expr_type = right_expr.accept(this);
        BinaryOperator operator = binaryExpression.getBinaryOperator();

        if (left_expr_type instanceof NoType
                || right_expr_type instanceof NoType) {
            return new NoType();
        }

        if (operator.equals(BinaryOperator.and)
                || operator.equals(BinaryOperator.or)) {
            if (sameType(left_expr_type, right_expr_type)
                    && (left_expr_type instanceof BooleanType
                        || right_expr_type instanceof BooleanType)) {
                return new BooleanType();
            }
            typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
            return new NoType();
        }

        if (operator.equals(BinaryOperator.lt)
                || operator.equals(BinaryOperator.gt)
                || operator.equals(BinaryOperator.eq)
                || operator.equals(BinaryOperator.neq)
                || operator.equals(BinaryOperator.gte)
                || operator.equals(BinaryOperator.lte)) {
            if (sameType(left_expr_type, right_expr_type)) {
                return new BooleanType();
            }
            return new NoType();
        }

        if (operator.equals(BinaryOperator.add)
                || operator.equals(BinaryOperator.sub)
                || operator.equals(BinaryOperator.mult)
                || operator.equals(BinaryOperator.div)
                || operator.equals(BinaryOperator.mod)) {
            if (!sameType(left_expr_type, right_expr_type)
                    ||  left_expr_type instanceof BooleanType
                    || right_expr_type instanceof BooleanType) {
                typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if (left_expr_type instanceof IntType
                    || right_expr_type instanceof IntType) {
                return new IntType();
            }
            if (left_expr_type instanceof FloatType
                    || right_expr_type instanceof FloatType) {
                return new FloatType();
            }
            return new NoType();
        }

        if (operator.equals(BinaryOperator.assign)) {
            if (!sameType(left_expr_type, right_expr_type)) {
                typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
                return new NoType();
            }
            if (left_expr_type instanceof BooleanType &&  right_expr_type instanceof BooleanType) {
                return new BooleanType();
            }
            if (left_expr_type instanceof IntType && right_expr_type instanceof IntType) {
                return new IntType();
            }
            if (left_expr_type instanceof FloatType && right_expr_type instanceof FloatType) {
                return new FloatType();
            }
            return new NoType();
        }

        typeErrors.add(new UnsupportedOperandType(binaryExpression.getLine(), operator.name()));
        return new NoType();
    }

    @Override
    public Type visit(Identifier identifier) {
        try {
            var elem = SymbolTable.top.get(VariableItem.STARTKEY + identifier.getName());
            if (elem instanceof VariableItem varItem) {
                return varItem.getType();
            }
            if (elem instanceof ArrayItem arrayItem) {
                return arrayItem.getType();
            }
        } catch (ItemNotFoundException itemNotFoundException) {
            typeErrors.add(new VarNotDeclared(identifier.getLine(), identifier.getName()));
            return new NoType();
        }
        return new NoType();
    }

    @Override
    public Type visit(FunctionCall functionCall) {
        FunctionItem functionItem;
        try {
            functionItem = (FunctionItem) SymbolTable.root.get(FunctionItem.STARTKEY +
                    functionCall.getUFuncName().getName());
        } catch (ItemNotFoundException itemNotFoundException) {
            typeErrors.add(new FunctionNotDeclared(functionCall.getLine(), functionCall.getUFuncName().getName()));
            return new NoType();
        }
        for (var arg : functionCall.getArgs()) {
            arg.accept(this);
        }

        return functionItem.getHandlerDeclaration().getType();
    }

    @Override
    public Type visit(IntValue value) {
        return new IntType();
    }

    @Override
    public Type visit(FloatValue value) {
        return new FloatType();
    }

    @Override
    public Type visit(BooleanValue value) {
        return new BooleanType();
    }

    @Override
    public Type visit(ArrayAccess arrayAccess) {
        try{
            if(SymbolTable.top.get(ArrayItem.STARTKEY + arrayAccess.getName()) instanceof VariableItem arrayItem) {
                return arrayItem.getType();
            }
            else if(SymbolTable.top.get(ArrayItem.STARTKEY + arrayAccess.getName()) instanceof ArrayItem arrayItem) {
                return arrayItem.getType();
            }
        } catch (ItemNotFoundException itemNotFoundException) {}

        typeErrors.add(new VarNotDeclared(arrayAccess.getLine(), arrayAccess.getName()));
        return new NoType();
    }

    @Override
    public Type visit(QueryExpression queryExpression) {
        var expr_var = queryExpression.getVar();
        if(expr_var != null) {
            var varType = expr_var.accept(this);
            if (varType instanceof NoType) {
                return new BooleanType();
            }
        }
        // for query2, we won't set the q.var
        return new NoType();
    }
}
