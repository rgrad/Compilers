package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class ListLiteralExpression extends Expression {
    List<Expression> values;
    private CatscriptType type;

    public ListLiteralExpression(List<Expression> values) {
        this.values = new LinkedList<>();
        for (Expression value : values) {
            this.values.add(addChild(value));
        }
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        for (Expression value : values) {
            value.validate(symbolTable);
        }
        if (values.size() > 0) {
            type = CatscriptType.getListType(values.get(0).getType());
            for (Expression value: values ){
                if(value.getType() != values.get(0).getType()){
                    type = CatscriptType.getListType(CatscriptType.OBJECT);
                }

            }
        } else {
            type = CatscriptType.getListType(CatscriptType.OBJECT);
        }
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        List<Object> valList = new ArrayList<>(); // Initialize valList

        for (int i = 0; i < values.size(); i++) {
            Object hold = values.get(i).evaluate(runtime);
            valList.add(hold);
        }
        return valList;

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        code.addTypeInstruction(Opcodes.NEW, internalNameFor(ArrayList.class));

        code.addInstruction(Opcodes.DUP);
        code.addMethodInstruction(Opcodes.INVOKESPECIAL, internalNameFor(ArrayList.class),
                "<init>","()V" );



        for(Expression value: values){
            code.addInstruction(Opcodes.DUP);
            value.compile(code);
            if (value.getType() == CatscriptType.INT || value.getType() == CatscriptType.BOOLEAN){
                box(code, value.getType());
            }
            code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, internalNameFor(ArrayList.class),
                    "add", "(Ljava/lang/Object;)Z");

            code.addInstruction(Opcodes.POP);

        }
    }


}
