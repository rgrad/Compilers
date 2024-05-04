package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.LinkedList;
import java.util.List;

public class ForStatement extends Statement {
    private Expression expression;
    private String variableName;
    private List<Statement> body;

    public void setExpression(Expression expression) {
        this.expression = addChild(expression);
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setBody(List<Statement> statements) {
        this.body = new LinkedList<>();
        for (Statement statement : statements) {
            this.body.add(addChild(statement));
        }
    }

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<Statement> getBody() {
        return body;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        symbolTable.pushScope();
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            expression.validate(symbolTable);
            CatscriptType type = expression.getType();
            if (type instanceof CatscriptType.ListType) {
                symbolTable.registerSymbol(variableName, getComponentType());
            } else {
                addError(ErrorType.INCOMPATIBLE_TYPES, getStart());
                symbolTable.registerSymbol(variableName, CatscriptType.OBJECT);
            }
        }
        for (Statement statement : body) {
            statement.validate(symbolTable);
        }
        symbolTable.popScope();
    }

    private CatscriptType getComponentType() {
        return ((CatscriptType.ListType) expression.getType()).getComponentType();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        List evaluate = (List) expression.evaluate(runtime);

        for (Object o : evaluate){
            runtime.pushScope();
            runtime.setValue(variableName, o);
            for (Statement bodStates : body){

                bodStates.execute(runtime);
            }
            runtime.popScope();
        }

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Integer iteratorSlot = code.nextLocalStorageSlot();
        Integer loopVariableSlot = code.createLocalStorageSlotFor(variableName);
        Label startOfLoop = new Label();
        Label endOfLoop = new Label();

        // compile the expression - leaves a list on top of the op stack
        //invoke INTERFACE java/util/List.iterator ()Ljava/util/Iterator
        // store that iterator into the iterator slot

        // add startOfLoop Label (to jump back to at the end of the loop)
        // Aload the iterator slot
        //invoke INTERFACE hasNext
        // IFEQ jump to endOfLoop Label

        //ALOAD the iterater again
        //call next() on it
        // do a checkcast
        code.addTypeInstruction(Opcodes.CHECKCAST, ByteCodeGenerator.internalNameFor(getComponentType().getJavaType()));
        //save that into the loop var slot (might be a boolean/ int or a Reference)

        //compile loop body statement

        // goto start of loop

        // add endOfLoop Label


        super.compile(code);
    }

}
