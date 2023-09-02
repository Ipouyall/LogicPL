import java.io.*;

import ast.node.Program;
import compileError.CompileError;
import main.grammar.LogicPLLexer;
import main.grammar.LogicPLParser;
import visitor.codeGenerator.CodeGenerator;
import visitor.nameAnalyzer.NameAnalyzer;
import visitor.astPrinter.ASTPrinter;
import org.antlr.v4.runtime.*;
import visitor.typeAnalyzer.TypeAnalyzer;

public class Main {
        public static void main(String[] args) throws java.io.IOException {

            CharStream reader = CharStreams.fromFileName(args[0]);
            LogicPLLexer lexer = new LogicPLLexer(reader);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LogicPLParser parser = new LogicPLParser(tokens);
            Program program = parser.program().p;


            NameAnalyzer nameAnalyzer = new NameAnalyzer();
            nameAnalyzer.visit(program);

            TypeAnalyzer typeAnalyzer = new TypeAnalyzer();
            typeAnalyzer.visit(program);
            if (typeAnalyzer.typeErrors.size() > 0){
                for(CompileError compileError: typeAnalyzer.typeErrors)
                    System.out.println(compileError.getMessage());
                return;
            }
            CodeGenerator codeGenerator = new CodeGenerator();
            String result = codeGenerator.visit(program);
            System.out.println(result);

            BufferedWriter writer = new BufferedWriter(new FileWriter("logicpl_jasmin_bytecode.j"));
            writer.write(result);
            writer.close();

            try {
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", "utilities/jasmin.jar", "logicpl_jasmin_bytecode.j");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                InputStream inputStream = p.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder output = new StringBuilder();

                while ((line = bufferedReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    System.out.println("Jasmin assembly completed successfully!");
                } else {
                    System.err.println("Jasmin assembly encountered errors:");
                    System.err.println(output.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Compilation was Successful!!");
        }

}