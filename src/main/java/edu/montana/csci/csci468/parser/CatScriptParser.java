package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;
//this is a test for gitub
import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement statement = parseFunctionDefinitionStatement();
        if (statement != null) {
            return statement;
        }
        return parseStatement();
    }

    private Statement parseStatement(){

        Statement stmt = parsePrintStatement();
        if(stmt != null){
            return stmt;
        }

        stmt = parseIfStatement();
        if (stmt != null){
            return stmt;
        }

        stmt = parseForStatement();
        if (stmt != null){
            return stmt;
        }

        stmt = parseVariableStatement();
        if (stmt != null){
            return stmt;
        }

        stmt = parseAssignmentOrFunctionCallStatement();
        if (stmt != null){
            return stmt;
        }



        if (currentFunctionDefinition != null){
            stmt = parseReturnStatement();
            if (stmt != null){
                return stmt;
            }

        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }



    private FunctionDefinitionStatement parseFunctionDefinitionStatement(){
        if(tokens.match(FUNCTION)){
            FunctionDefinitionStatement functionDefinitionStatement = new FunctionDefinitionStatement();
            currentFunctionDefinition = functionDefinitionStatement;
            List<Statement> functionBody = new ArrayList<>();
            TypeLiteral functionType = new TypeLiteral();
            functionDefinitionStatement.setStart(tokens.consumeToken());





            Token name = require(IDENTIFIER, functionDefinitionStatement);

            require(LEFT_PAREN, functionDefinitionStatement);
            //------This next part is for parsing the arguments in the function deffinition
            while(!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)){
               Token paramToken = tokens.consumeToken();
               TypeLiteral passer = new TypeLiteral();
               CatscriptType holder = parseTypeDeclaration(functionDefinitionStatement); //////--------------------------------method for parsing the type of a value.
               if (holder == CatscriptType.NULL){
                   passer = null;
               }else{
                   passer.setType(holder);
               }
               if (tokens.match(COMMA)){//-------skipping over commas in the list of arguments.
                   tokens.consumeToken();
               }
               functionDefinitionStatement.addParameter(paramToken.getStringValue(), passer);
            }
            require(RIGHT_PAREN, functionDefinitionStatement);
            //--------end of parsing arguments---------------------

            //Check for Function type declaration
            CatscriptType catFunType = parseTypeDeclaration(functionDefinitionStatement);
            if (catFunType == CatscriptType.NULL){
                functionType = null;
            }else {
                functionType.setType(catFunType);
            }
            functionDefinitionStatement.setType(functionType);
            //end type check



            //--------start of parsing functiuon body----------------
            require(LEFT_BRACE, functionDefinitionStatement);
            while(!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)){
                functionBody.add(parseStatement());
            }
            require(RIGHT_BRACE, functionDefinitionStatement);

            functionDefinitionStatement.setName(name.getStringValue());
            functionDefinitionStatement.setBody(functionBody);
            functionDefinitionStatement.setType(functionType);

            currentFunctionDefinition = null;

            return functionDefinitionStatement;
        }else{
            return null;
        }
    }

    private Statement parseIfStatement(){
        if(tokens.match(IF)){
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);
            List<Statement> mainStates = new ArrayList<>();
            while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)){
                Statement currentStatement = parseStatement();
                mainStates.add(currentStatement);
            }
            ifStatement.setTrueStatements(mainStates);
            require(RIGHT_BRACE, ifStatement);

            if (tokens.match(ELSE)){
                List<Statement> elseStatements = new ArrayList<>();
                tokens.consumeToken();
                if (tokens.match(IF)){
                    elseStatements.add(parseIfStatement());
                }else{
                    require(LEFT_BRACE, ifStatement);
                    while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)){
                        elseStatements.add(parseStatement());
                    }
                    require(RIGHT_BRACE, ifStatement);
                }
                ifStatement.setElseStatements(elseStatements);
            }
            return ifStatement;
        }
        return null;
    }

    private Statement parseForStatement() {
        if (tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, forStatement);
            Token varName = require(IDENTIFIER, forStatement);
            require(IN, forStatement);
            Expression inner = parseExpression();
            require(RIGHT_PAREN, forStatement);
            require(LEFT_BRACE, forStatement);
            List<Statement> body = new ArrayList<>();
            while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                Statement currentStatement = parseStatement();
                body.add(currentStatement);

            }
            require(RIGHT_BRACE, forStatement);
            forStatement.setBody(body);
            forStatement.setVariableName(varName.getStringValue());
            forStatement.setExpression(inner);
            return forStatement;


        }
        return null;
    }

    private Statement parseVariableStatement(){
        if(tokens.match(VAR)){
            VariableStatement varStat = new VariableStatement();
            varStat.setStart(tokens.consumeToken());
            CatscriptType type = CatscriptType.NULL;
            Token name = require(IDENTIFIER, varStat);
            if (tokens.match(COLON)){
                tokens.consumeToken();
                Token typeHold = tokens.consumeToken();
                Boolean listCheck = false;


                if (typeHold.getStringValue().equals("list")){
                    listCheck = true;
                    require(LESS, varStat);
                    typeHold = tokens.consumeToken();
                    require(GREATER, varStat);
                }


                if (typeHold.getStringValue().equals("int")){
                    type = CatscriptType.INT;
                    //varStat.setExplicitType(CatscriptType.INT);
                } else if (typeHold.getStringValue().equals("string")) {
                    type = CatscriptType.STRING;
                    //varStat.setExplicitType(CatscriptType.STRING);
                } else if (typeHold.getStringValue().equals("object")) {
                    type = CatscriptType.OBJECT;
                    //varStat.setExplicitType(CatscriptType.OBJECT);
                } else if (typeHold.getStringValue().equals("bool")) {
                    type = CatscriptType.BOOLEAN;
                    //varStat.setExplicitType(CatscriptType.BOOLEAN);
                } else if (typeHold.getStringValue().equals("null")) {
                    type = CatscriptType.NULL;
                    //varStat.setExplicitType(CatscriptType.NULL);
                } else if (typeHold.getStringValue().equals("void")) {
                    type = CatscriptType.VOID;
                    //varStat.setExplicitType(CatscriptType.VOID);
                }

                if (listCheck == true){
                    varStat.setExplicitType(CatscriptType.getListType(type));
                }else{
                    varStat.setExplicitType(type);
                }
            }


            require(EQUAL, varStat);
            Expression expression = parseExpression();


            varStat.setExpression(expression);
            varStat.setVariableName(name.getStringValue());

            return varStat;
        }

        return null;
    }



    private Statement parseAssignmentOrFunctionCallStatement(){
        if(tokens.match(IDENTIFIER)){
            Token ident = tokens.consumeToken();

            if (tokens.match(EQUAL)){
                tokens.consumeToken();

                Expression asignExp = parseExpression();
                AssignmentStatement fin = new AssignmentStatement();
                fin.setExpression(asignExp);
                fin.setVariableName(ident.getStringValue());
                return fin;
            } else if (tokens.match(LEFT_PAREN)) {
                FunctionCallExpression functionCallExpression = parseFunctionCallExpression(ident);
                return new FunctionCallStatement(functionCallExpression);
            }
        }

        return null;
    }

    private Statement parseReturnStatement(){
        if (tokens.match(RETURN)){
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setFunctionDefinition(currentFunctionDefinition);
            Token keyword = tokens.consumeToken();

            if (!tokens.match(RIGHT_BRACE)){
                Expression expression = parseExpression();
                returnStatement.setExpression(expression);
            }
            return  returnStatement;

        } else {
            return null;
        }
    }

    private CatscriptType parseTypeDeclaration(Statement currentStatement){
        CatscriptType passer = CatscriptType.NULL;
        if (tokens.match(COLON)){
            tokens.consumeToken();
            Token typeHold = tokens.consumeToken();
            CatscriptType type = CatscriptType.NULL;
            Boolean listCheck = false;


            if (typeHold.getStringValue().equals("list")){
                listCheck = true;
                if (tokens.matchAndConsume(LESS)){
                    typeHold = tokens.consumeToken();
                    require(GREATER, currentStatement);

                }


            }
            if (typeHold.getStringValue().equals("int")){
                type = CatscriptType.INT;
                //varStat.setExplicitType(CatscriptType.INT);
            } else if (typeHold.getStringValue().equals("string")) {
                type = CatscriptType.STRING;
                //varStat.setExplicitType(CatscriptType.STRING);
            } else if (typeHold.getStringValue().equals("object")) {
                type = CatscriptType.OBJECT;
                //varStat.setExplicitType(CatscriptType.OBJECT);
            } else if (typeHold.getStringValue().equals("bool")) {
                type = CatscriptType.BOOLEAN;
                //varStat.setExplicitType(CatscriptType.BOOLEAN);
            } else if (typeHold.getStringValue().equals("null")) {
                type = CatscriptType.NULL;
                //varStat.setExplicitType(CatscriptType.NULL);
            } else if (typeHold.getStringValue().equals("void")) {
                type = CatscriptType.VOID;
                //varStat.setExplicitType(CatscriptType.VOID);
            }

            if (listCheck == true){
                passer = CatscriptType.getListType(type);
            }else{
                passer = type;

            }
        }else {
            return CatscriptType.NULL;
        }
        return passer;
    }



    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rightHandSide.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rightHandSide.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression(){
        Expression expression = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)){
            Token operator = tokens.consumeToken();
            final Expression rhs = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rhs);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rhs.getEnd());
            expression = factorExpression;
        }

        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;

        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return  stringExpression;

        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;

        } else if (tokens.match(FALSE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression boolExpression = new BooleanLiteralExpression(false);
            boolExpression.setToken(boolToken);
            return boolExpression;

        } else if (tokens.match(TRUE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression boolExpresion = new BooleanLiteralExpression(true);
            boolExpresion.setToken(boolToken);
            return boolExpresion;

        } else if (tokens.match(IDENTIFIER)) {
            Token identToken = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)){
               FunctionCallExpression funExpress = parseFunctionCallExpression(identToken);
               return funExpress;
            }


            IdentifierExpression identExpression = new IdentifierExpression(identToken.getStringValue());
            identExpression.setToken(identToken);
            return identExpression;

        } else if (tokens.match(LEFT_PAREN)){
            Token parenToken = tokens.consumeToken();
            Expression inner = parseExpression();
            ParenthesizedExpression parenExpression = new ParenthesizedExpression(inner);
            tokens.consumeToken();
            return parenExpression;
        } else if (tokens.match(LEFT_BRACKET)) {
            ArrayList<Expression> elements = new ArrayList<Expression>();
            tokens.consumeToken();
            while (!tokens.match(RIGHT_BRACKET) && !tokens.match(EOF)){
               Expression element = parseExpression();
               elements.add(element);
               if (tokens.match(COMMA)){
                   tokens.consumeToken();
               }
            }
            ListLiteralExpression listLit = new ListLiteralExpression(elements);
            if (tokens.match(EOF)){
                listLit.addError(ErrorType.UNTERMINATED_LIST);
                return listLit;
            }
            tokens.consumeToken();
            return listLit;

        } else {

            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private FunctionCallExpression parseFunctionCallExpression(Token identifier){//-------------------------------------------------
        Token identToken = identifier;

        ArrayList<Expression> argumentsList = new ArrayList<Expression>();
        tokens.consumeToken();
        while (!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)){
            Expression argument = parseExpression();
            argumentsList.add(argument);
            if (tokens.match(COMMA)){
                tokens.consumeToken();
            }
        }
        FunctionCallExpression funCall = new FunctionCallExpression(identToken.getStringValue(), argumentsList);
        if (tokens.match(EOF)){
            funCall.addError(ErrorType.UNTERMINATED_ARG_LIST);
        }
        require(RIGHT_PAREN, funCall);
        return funCall;


    }


    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
