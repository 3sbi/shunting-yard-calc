package calc;

class Func {
    UnaryOperation unaryEval;       // unary function evaluation callback
    BinaryOperation binaryEval;     // binary function evaluation callback
    TokenType type;                 // type of function (ie function or operator)
    int prec;                       // precedence
    boolean left;                   // is left associative
    boolean unary;                  // is a unary function

    double eval(double x, double y) {
        return this.unary ? this.unaryEval.eval(x) : this.binaryEval.eval(x, y);
    }

    double eval(double x) {
        return this.eval(x, 0);
    }

    private void init(TokenType type, int prec, boolean left, boolean unary) {
        this.type = type;
        this.prec = prec;
        this.left = left;
        this.unary = unary;
    }

    Func(UnaryOperation operation) {
        this.init(TokenType.FUNCTION,0,true,true);
        this.unaryEval = operation;
    }
    Func(BinaryOperation operation, TokenType type, int prec) {
        this.init(type,prec,true,false);
        this.binaryEval = operation;
    }



}