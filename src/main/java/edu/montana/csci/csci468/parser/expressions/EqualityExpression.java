package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class EqualityExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public EqualityExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    public boolean isEqual() {
        return operator.getType().equals(TokenType.EQUAL_EQUAL);
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
    }

    @Override
    public CatscriptType getType() {
        return CatscriptType.BOOLEAN;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Boolean evaluate(CatscriptRuntime runtime) {

        Object left = leftHandSide.evaluate(runtime);
        Object right = rightHandSide.evaluate(runtime);

        if (isEqual()){
            return left == right;
        }else{
            return left != right;
        }


    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        getLeftHandSide().compile(code);
        box(code, getLeftHandSide().getType());


        getRightHandSide().compile(code);
        box(code, getRightHandSide().getType());


        if (isEqual()){
            code.addJumpInstruction(Opcodes.IF_ACMPEQ, trueLabel);

        } else if (!isEqual()) {
            code.addJumpInstruction(Opcodes.IF_ACMPNE, trueLabel);

        }

        code.pushConstantOntoStack(false);
        code.addJumpInstruction(Opcodes.GOTO, endLabel);

        code.addLabel(trueLabel);
        code.pushConstantOntoStack(true);

        code.addLabel(endLabel);


    }


}
