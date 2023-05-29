package calc;

import java.util.*;


enum TokenType {
    OPERATOR,
    CONSTANT,
    FUNCTION,
    LPAREN,
    RPAREN,
    ELSE
}


public class ShuntingYard {
    private final Map<String, Func> binaryFunctions;
    private final Map<String, Func> unaryFunctions;
    private final Map<String, Double> constants;

    private final Map<String, Double> variables;

    private ArrayList<String> getFunctionNames() {
        Set<String> unaryNames = unaryFunctions.keySet();
        Set<String> binaryNames = binaryFunctions.keySet();
        ArrayList<String> names = new ArrayList<>();
        names.addAll(unaryNames);
        names.addAll(binaryNames);
        return names;
    }

    private ArrayList<String> getConstantNames() {
        return new ArrayList<>(constants.keySet());
    }

    ShuntingYard(Map<String, Double> variables) {
        this.variables = variables;
        binaryFunctions = new HashMap<>();
        BinaryOperation divide = (double x, double y) -> x / y;
        BinaryOperation multiply = (double x, double y) -> x * y;
        BinaryOperation subtract = (double x, double y) -> x - y;
        binaryFunctions.put("+", new Func(Double::sum, TokenType.OPERATOR, 2));
        binaryFunctions.put("-", new Func(subtract, TokenType.OPERATOR, 2));
        binaryFunctions.put("*", new Func(multiply, TokenType.OPERATOR, 3));
        binaryFunctions.put("/", new Func(divide, TokenType.OPERATOR, 3));
        binaryFunctions.put("^", new Func(Math::pow, TokenType.OPERATOR, 4));

        unaryFunctions = new HashMap<>();
        unaryFunctions.put("sin", new Func(Math::sin));
        unaryFunctions.put("cos", new Func(Math::cos));

        constants = Map.of(
                "pi", Math.PI,
                "e", Math.E
        );
    }

    // parse infix notation into reverse polish notation (Shunting Yard)
    public ArrayList<String> reversePolishNotation(String equation) {
        ArrayList<String> queue = new ArrayList<>();
        LinkedList<String> stack = new LinkedList<>();
        String obj;
        TokenType type;
        TokenType prevType = TokenType.ELSE;
        boolean acceptDecimal = true;
        boolean acceptNegative = true;
        int eqLen = equation.length();
        for (int i = 0; i < eqLen; i++) {
            char currentChar = equation.charAt(i);
            // skip spaces and commas
            if (currentChar == ' ' || currentChar == ',') {
                //prevType = TokenTypes::ELSE;
                continue;
            }
            // classify token
            String leftBrackets = "({[";
            String operators = "+-*/^";
            if (this.isNumber(currentChar)) {
                type = TokenType.CONSTANT;
                if (currentChar == '.') {
                    acceptDecimal = false;
                } else if (currentChar == '-') {
                    acceptNegative = false;
                }
                int startI = i;
                if (i < eqLen - 1) {
                    while (isNumber(equation.charAt(i + 1), acceptDecimal, acceptNegative)) {
                        i++;
                        if (i >= eqLen - 1) {
                            break;
                        }
                    }
                }
                obj = equation.substring(startI, i + 1);
                // subtraction sign detection
                if (obj.equals("-")) {
                    type = TokenType.OPERATOR;
                }
            } else {
                obj = findElement(i, equation, this.getFunctionNames());
                if (!obj.equals("")) {
                    // found valid object
                    type = operators.contains(obj) ? TokenType.OPERATOR : TokenType.FUNCTION;
                } else {
                    obj = findElement(i, equation, this.getConstantNames());
                    if (!obj.equals("")) {
                        // found valid object
                        type = TokenType.CONSTANT;
                    } else {

                        obj = findElement(i, equation, new ArrayList<>(variables.keySet()));
                        String rightBrackets = ")}]";
                        if (!obj.equals("")) {
                            type = TokenType.CONSTANT;
                        } else if (leftBrackets.indexOf(currentChar) != -1) {
                            type = TokenType.LPAREN;
                            obj = "(";
                        } else if (rightBrackets.indexOf(currentChar) != -1) {
                            type = TokenType.RPAREN;
                            obj = ")";
                        } else {
                            type = TokenType.ELSE;
                        }
                    }
                }
                i += obj.length() - 1;
            }

            // do something with the token
            String last_stack = (stack.size() > 0) ? stack.getLast() : "";
            switch (type) {
                case CONSTANT: {
                    queue.add(obj);
                    break;
                }
                case FUNCTION, LPAREN: {
                    stack.push(obj);
                    break;
                }
                case OPERATOR: {
                    if (stack.size() != 0) {
                        while (
                                (getFunctionNames().contains(last_stack) &&
                                        operators.indexOf(last_stack.charAt(0)) == -1
                                        ||
                                        getPrecedence(last_stack) > getPrecedence(obj) ||
                                        ((getPrecedence(last_stack) == getPrecedence(obj)) &&
                                                isLeftAssociative(last_stack))
                                ) &&
                                        leftBrackets.indexOf(last_stack.charAt(0)) == -1
                        ) {
                            // pop from the stack to the queue
                            queue.add(stack.getLast());
                            stack.removeLast();
                            if (stack.size() == 0) {
                                break;
                            }
                            last_stack = stack.getLast();
                        }
                    }
                    stack.push(obj);
                    break;

                }
                case RPAREN: {
                    while (last_stack.charAt(0) != '(') {
                        // pop from the stack to the queue
                        queue.add(stack.removeLast());
                        last_stack = stack.getLast();
                    }
                    stack.pop();
                    break;
                }
                default:
                    return queue;
            }
            prevType = type;
        }
        while (stack.size() > 0) {
            queue.add(stack.removeLast());
        }

        return queue;
    }

    // parse RPN to tree
    Node parse(ArrayList<String> rpn) {
        LinkedList<Node> stack = new LinkedList<>();

        for (String item : rpn) {
            if (isStringNumeric(item)) {
                // push number node
                stack.add(new NumNode(item));
            } else {
                // function
                FuncNode f = new FuncNode(item, unaryFunctions.containsKey(item));
                if (binaryFunctions.containsKey(item)) {
                    // right child is second argument
                    f.right = stack.getLast();
                    stack.removeLast();

                    // left child is first argument
                    f.left = stack.getLast();
                    stack.removeLast();
                } else if (unaryFunctions.containsKey(item)) {
                    // set child of node
                    f.left = stack.getLast();
                    stack.removeLast();
                }
                stack.push(f);
            }
        }

        if (stack.size() == 0) {
            return null;
        }

        return stack.getLast();
    }

    double eval(Node tree) {
        if (tree.isFunc) {
            FuncNode funcTree = new FuncNode(tree.name, unaryFunctions.containsKey(tree.name));
            if (funcTree.isUnary) {
                // evaluate child recursively and then evaluate with return value
                return funcTree.eval(this.eval(tree.left));
            } else {
                // evaluate each child recursively and then evaluate with return value
                return funcTree.eval(this.eval(tree.left), this.eval(tree.right));
            }
        } else {
            // number node
            NumNode numTree = new NumNode(tree.name);
            return numTree.eval();
        }
    }

    // determine if character is number
    private boolean isNumber(char c, boolean acceptDecimal, boolean acceptNegative) {
        if (c >= '0' && c <= '9') {
            return true;
        }
        // decimal point
        else if (acceptDecimal && c == '.') {
            return true;
        }
        // negative sign
        else return acceptNegative && c == '-';
    }

    private boolean isNumber(char c) {
        return this.isNumber(c, false, false);
    }

    boolean isStringNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // determine if string only contains numerical characters
    boolean containsNumbers(String str) {
        // cannot be a single decimal point or negative sign
        if (str.equals(".") || str.equals("-")) {
            return false;
        }

        // try to prove wrong
        boolean acceptDecimal = true;
        if (isNumber(str.charAt(0), true, true)) {
            // check first character for negative sign
            if (str.charAt(0) == '.') {
                // cannot be any more decimal points
                acceptDecimal = false;
            }
        } else {
            return false;
        }

        for (int i = 1, len = str.length(); i < len; i++) {
            // do not accept anymore negative signs
            if (!isNumber(str.charAt(i), acceptDecimal, false)) {
                return false;
            }

            if (str.charAt(i) == '.') {
                // cannot be any more decimal points
                acceptDecimal = false;
            }
        }

        return true;
    }

    // determine if function is left associative
    private boolean isLeftAssociative(String str) {
        return binaryFunctions.get(str).left;
    }

    // get function precedence
    int getPrecedence(String str) {
        if (binaryFunctions.containsKey(str)) {
            return binaryFunctions.get(str).prec;
        }
        // only care about operators, which are binary functions, so otherwise we can return 0
        return 0;
    }

    private String findElement(int i, String eqn, ArrayList<String> list) {
        for (String item : list) {
            int n = item.length();
            if (eqn.length() > i + n) {
                if (eqn.substring(i, i + n).equals(item)) {
                    return item;
                }
            }

        }
        return "";
    }

    // number node class
    class NumNode extends Node {
        NumNode(String name) {
            super(name, false);
        }

        // get numerical value of string
        double getNumericalVal(String str) {
            if (getConstantNames().contains(str)) {
                // is a constant
                return constants.get(str);
            } else if (variables.containsKey(str)) {
                // is a variable
                return variables.get(str);
            } else {
                // is a number
                return Double.parseDouble(str);
            }
        }

        // return numerical value
        double eval() {
            return getNumericalVal(name);
        }
    }

    // function node class
    class FuncNode extends Node {
        boolean isUnary;
        Func func;

        FuncNode(String name, boolean isUnary) {
            super(name, true);
            this.isUnary = isUnary;
            this.func = isUnary ? unaryFunctions.get(name) : binaryFunctions.get(name);
        }

        double eval(double x) {
            return this.func.eval(x);
        }

        double eval(double x, double y) {
            return this.func.eval(x, y);
        }
    }

}

