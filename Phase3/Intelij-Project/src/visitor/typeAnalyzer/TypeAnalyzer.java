package visitor.typeAnalyzer;

import ast.node.Program;
import ast.node.declaration.ArgDeclaration;
import ast.node.declaration.Declaration;
import ast.node.declaration.FuncDeclaration;
import ast.node.declaration.MainDeclaration;
import ast.node.expression.*;
import ast.node.expression.operators.BinaryOperator;
import ast.node.expression.values.BooleanValue;
import ast.node.expression.values.FloatValue;
import ast.node.expression.values.IntValue;
import ast.node.statement.*;
import ast.type.NoType;
import ast.type.Type;
import ast.type.primitiveType.BooleanType;
import ast.type.primitiveType.FloatType;
import compileError.CompileError;
import compileError.Type.FunctionNotDeclared;
import compileError.Type.LeftSideNotLValue;
import compileError.Type.UnsupportedOperandType;
import compileError.Type.ConditionTypeNotBool;
import symbolTable.SymbolTable;
import symbolTable.itemException.ItemAlreadyExistsException;
import symbolTable.itemException.ItemNotFoundException;
import symbolTable.symbolTableItems.*;
import visitor.Visitor;

import java.util.ArrayList;

public class TypeAnalyzer extends Visitor<Void> {
    public ArrayList<CompileError> typeErrors = new ArrayList<>();
    ExpressionTypeChecker expressionTypeChecker = new ExpressionTypeChecker(typeErrors);

    @Override
    public Void visit(Program program) {
        for(var functionDec : program.getFuncs()) {
            functionDec.accept(this);
        }
        program.getMain().accept(this);

        return null;
    }

    @Override
    public Void visit(FuncDeclaration funcDeclaration) {
        try {
            FunctionItem functionItem = (FunctionItem)  SymbolTable.root.get(FunctionItem.STARTKEY + funcDeclaration.getName().getName());
            SymbolTable.push((functionItem.getFunctionSymbolTable()));
        } catch (ItemNotFoundException e) {
            //unreachable
        }

        for(var arg : funcDeclaration.getArgs()) {
            arg.accept(this);
        }

        for(var stmt : funcDeclaration.getStatements()) {
            stmt.accept(this);
        }

        SymbolTable.pop();

        return null;
    }

    @Override
    public Void visit(MainDeclaration mainDeclaration) {
        var mainItem = new MainItem(mainDeclaration);
        var mainSymbolTable = new SymbolTable(SymbolTable.top, "main");
        mainItem.setMainItemSymbolTable(mainSymbolTable);

        SymbolTable.push(mainItem.getMainItemSymbolTable());

        for (var stmt : mainDeclaration.getMainStatements()) {
            stmt.accept(this);
        }
        SymbolTable.pop();

        return null;
    }

    @Override
    public Void visit(ForloopStmt forloopStmt) {
        var arr_type = forloopStmt.getArrayName().accept(expressionTypeChecker);
        SymbolTable.push(SymbolTable.top);
        var loop_itr = new VariableItem(forloopStmt.getIterator().getName(), arr_type);

        boolean itr_pushed;
        try {
            SymbolTable.top.put(loop_itr);
            itr_pushed = true;
        } catch (ItemAlreadyExistsException e) {
            itr_pushed = false;
        }

        for(var stmt: forloopStmt.getStatements()){
            stmt.accept(this);
        }
//        if (itr_pushed) {
//            SymbolTable.pop(); //TODO: is it necessary?
//        }
        // TODO: check if stack state is OK!!!
        SymbolTable.pop();

        return null;
    }

    @Override
    public Void visit(AssignStmt assignStmt) {
        Type tl = assignStmt.getLValue().accept(expressionTypeChecker);
        Type tr = assignStmt.getRValue().accept(expressionTypeChecker);

        if(!expressionTypeChecker.sameType(tl, tr)) {
            typeErrors.add(new UnsupportedOperandType(assignStmt.getLine(), BinaryOperator.assign.name()));
        }
        return null;
    }

    @Override
    public Void visit(FunctionCall functionCall) {
        FunctionItem functionItem;
        try {
            functionItem = (FunctionItem)  SymbolTable.root.get(
                    FunctionItem.STARTKEY + functionCall.getUFuncName().getName());
        }
        catch (ItemNotFoundException ignored) {}
        functionCall.accept(expressionTypeChecker);

        return null;
    }

    @Override
    public Void visit(PrintStmt printStmt) {
        printStmt.getArg().accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        if(returnStmt.getExpression() != null) {
            returnStmt.getExpression().accept(expressionTypeChecker);
        }
        return null;
    }

    @Override
    public Void visit(ImplicationStmt implicationStmt) {
        Type cond = implicationStmt.getCondition().accept(expressionTypeChecker);
        if (!(cond instanceof BooleanType) && !(cond instanceof NoType)) {
            typeErrors.add(new ConditionTypeNotBool(implicationStmt.getLine()));
        }
        SymbolTable.push(new SymbolTable(SymbolTable.top, SymbolTable.top.name));
        for(var stmt: implicationStmt.getStatements()){
            stmt.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ArgDeclaration argDeclaration) {
        try {
            var item = SymbolTable.top.get(VariableItem.STARTKEY + argDeclaration.getIdentifier().getName());
            if(item instanceof VariableItem v_item) {
                try {
                    SymbolTable.top.put(v_item);
                } catch (ItemAlreadyExistsException e) {
                    return null;
                }
            }
            else if(item instanceof ArrayItem a_item) {
                try {
                    SymbolTable.top.put(a_item);
                } catch (ItemAlreadyExistsException e) {
                    return null;
                }
            }
        } catch(ItemNotFoundException e) {
            return null;
        }
        return null;
    }

    @Override
    public Void visit(ArrayDecStmt arrayDecStmt) {
        try {
            SymbolTable.top.put(new ArrayItem(arrayDecStmt.getIdentifier().getName(), arrayDecStmt.getType()));
        } catch (ItemAlreadyExistsException ignored) {}
        var arr_type = arrayDecStmt.getType();
        for (var init_val: arrayDecStmt.getInitialValues()) {
            Type init_val_type = init_val.accept(expressionTypeChecker);
            if(!init_val_type.toString().equals(arr_type.toString())) {
                typeErrors.add(new UnsupportedOperandType(arrayDecStmt.getLine(), BinaryOperator.assign.name()));
            }
        }
        return null;
    }

    @Override
    public Void visit(VarDecStmt varDecStmt) {
        var init_expr = varDecStmt.getInitialExpression();
        var type = varDecStmt.getType();
        var id = varDecStmt.getIdentifier();

        if(init_expr != null) {
            var init_expr_type = init_expr.accept(expressionTypeChecker);
            if(!expressionTypeChecker.sameType(init_expr_type, type)) {
                typeErrors.add(new UnsupportedOperandType(varDecStmt.getLine(), BinaryOperator.assign.name()));
            }
        }
        try {
            SymbolTable.top.put(new VariableItem(id.getName(), type));
        } catch (ItemAlreadyExistsException ignored) {}
        return null;
    }
}
