package visitor.nameAnalyzer;

import ast.node.Program;
import ast.node.declaration.*;
import ast.node.statement.*;
import compileError.*;
import compileError.Name.*;
import symbolTable.SymbolTable;
import symbolTable.symbolTableItems.*;
import symbolTable.itemException.ItemAlreadyExistsException;
import symbolTable.symbolTableItems.VariableItem;
import visitor.Visitor;

import java.util.ArrayList;
import java.util.Random;

public class NameAnalyzer extends Visitor<Void> {

    public ArrayList<CompileError> nameErrors = new ArrayList<>();

    protected static String generateRandomIdentifier(int length) {
        String candidateChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars
                    .length())));
        }
        return sb.toString();
    }

    @Override
    public Void visit(Program program) {
        SymbolTable.root = new SymbolTable();
        SymbolTable.push(SymbolTable.root);

        for (FuncDeclaration functionDeclaration : program.getFuncs()) {
            functionDeclaration.accept(this);
        }

        for (var stmt : program.getMain().getMainStatements()) {
            if(stmt instanceof VarDecStmt || stmt instanceof ForloopStmt || stmt instanceof ImplicationStmt || stmt instanceof ArrayDecStmt) {
                stmt.accept(this);
            }
        }

        return null;
    }


    @Override
    public Void visit(FuncDeclaration funcDeclaration) {
        var functionItem = new FunctionItem(funcDeclaration);
        var functionSymbolTable = new SymbolTable(SymbolTable.top, funcDeclaration.getName().getName());
        functionItem.setFunctionSymbolTable(functionSymbolTable);

        // ToDo
        FunctionRedefinition funcDecerror = null;
        boolean done = false;
        while(!done) {
            try {
                SymbolTable.top.put(functionItem);
                SymbolTable.push(functionSymbolTable);
                done = true;
            } catch (ItemAlreadyExistsException var5) {
                funcDecerror = new FunctionRedefinition(funcDeclaration.getLine(), funcDeclaration.getName().getName());
                functionItem.setName(funcDeclaration.getName().getName() + generateRandomIdentifier(15));
            }
        }
        if (funcDecerror != null)
            this.nameErrors.add(funcDecerror);


        for (ArgDeclaration varDeclaration : funcDeclaration.getArgs()) {
            varDeclaration.accept(this);
        }

        for (var stmt : funcDeclaration.getStatements()) {
            if(stmt instanceof VarDecStmt || stmt instanceof ForloopStmt || stmt instanceof ImplicationStmt || stmt instanceof ArrayDecStmt) {
                stmt.accept(this);
            }
        }

        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(VarDecStmt varDeclaration) {
        var variableItem = new VariableItem(varDeclaration.getIdentifier().getName(), varDeclaration.getType());

        // ToDo
        boolean done = false;
        VariableRedefinition varDecError = null;
        while(!done) {
            try {
                SymbolTable.top.put(variableItem);
                done = true;
            } catch (ItemAlreadyExistsException var5) {
                varDecError = new VariableRedefinition(varDeclaration.getLine(), varDeclaration.getIdentifier().getName());
                variableItem.setVarDeclaration(varDeclaration);
                variableItem.setName(varDeclaration.getIdentifier().getName() + generateRandomIdentifier(15));
            }
        }
        if (varDecError != null)
            this.nameErrors.add(varDecError);

        return null;
    }

    @Override
    public Void visit (ArgDeclaration argDeclaration) {
        var variableItem = new VariableItem(argDeclaration.getIdentifier().getName(), argDeclaration.getType());

        // ToDo
        boolean done = false;
        VariableRedefinition varDecError = null;
        while(!done) {
            try {
                SymbolTable.top.put(variableItem);
                done = true;
            } catch (ItemAlreadyExistsException var5) {
                varDecError = new VariableRedefinition(argDeclaration.getLine(), argDeclaration.getIdentifier().getName());
                variableItem.setVarDeclaration(new VarDecStmt(argDeclaration.getIdentifier(), argDeclaration.getType()));
                variableItem.setName(argDeclaration.getIdentifier().getName() + generateRandomIdentifier(15));
            }
        }
        if (varDecError != null)
            this.nameErrors.add(varDecError);
        return null;
    }

    @Override
    public Void visit(ArrayDecStmt arrayDecStmt) {
        var ArrayItem = new ArrayItem(arrayDecStmt.getIdentifier().getName(), arrayDecStmt.getType());

        // ToDo
        boolean done = false;
        VariableRedefinition arrDecError = null;
        while (!done) {
            try {
                SymbolTable.top.put(ArrayItem);
                done = true;
            } catch (ItemAlreadyExistsException var5) {
                arrDecError = new VariableRedefinition(arrayDecStmt.getLine(), arrayDecStmt.getIdentifier().getName());
                ArrayItem.setArrayDeclaration(arrayDecStmt);
                ArrayItem.setName(arrayDecStmt.getIdentifier().getName() + generateRandomIdentifier(15));
            }
        }
        if (arrDecError != null)
            this.nameErrors.add(arrDecError);
        return null;
    }

    @Override
    public Void visit(ForloopStmt forloopStmt) {
        var forSymbolTable = new SymbolTable(SymbolTable.top, "forLoopStmt");
        SymbolTable.push(forSymbolTable);
        try {
            forSymbolTable.put(new VariableItem(forloopStmt.getIterator().getName(), forloopStmt.getIterator().getType()));
        }
        catch (ItemAlreadyExistsException e) {
            throw new RuntimeException(e);
        }

        for (Statement stmt : forloopStmt.getStatements())
            if (stmt instanceof VarDecStmt || stmt instanceof ForloopStmt || stmt instanceof ImplicationStmt || stmt instanceof ArrayDecStmt)
                stmt.accept(this);

        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(ImplicationStmt implicationStmt) {
        var implicationSymbolTable = new SymbolTable(SymbolTable.top, "implicationStmt");
        SymbolTable.push(implicationSymbolTable);

        for (Statement stmt : implicationStmt.getStatements())
            if (stmt instanceof VarDecStmt || stmt instanceof ForloopStmt || stmt instanceof ImplicationStmt || stmt instanceof ArrayDecStmt)
                stmt.accept(this);


        SymbolTable.pop();
        return null;
    }
}

