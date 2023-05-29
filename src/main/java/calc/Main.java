package calc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {


    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();
        while (input.isBlank()) {
            System.out.print("No input, try again:");
            input = reader.readLine();
        }
        Map<String, Double> variables = new HashMap<>();
        variables.put("x", 15.0);
        ShuntingYard yard = new ShuntingYard(variables);
        String[] equations = input.split(";");

        for (String equation : equations) {
            System.out.printf("Equation: %s\n", equation);
            ArrayList<String> rpn = yard.reversePolishNotation(equation);
            if (rpn.isEmpty()) {
                System.out.println("Error: No nodes found in your equation");
                return;
            }
            if (rpn.size() == 1 && !yard.getConstantNames().contains(rpn.get(0))) {
                try {
                    Integer.parseInt(rpn.get(0));
                } catch (NumberFormatException ex) {
                    System.out.println("Error: Input is not a valid integer");
                    return;
                }
            }
            System.out.println("Array of nodes: " + Arrays.toString(rpn.toArray()));
            ArrayList<String> names = yard.getBinaryFuncNames();
            for (int i = 0; i < rpn.size() - 1; i++) {
                if (names.contains(rpn.get(i)) && names.contains(rpn.get(i + 1))) {
                    System.out.println("Error: multiple binary operators one after each other");
                    return;
                }
            }

            Node tree = yard.parse(rpn);
            double result = yard.eval(tree);
            System.out.printf("Result: %f\n", result);
            System.out.println("___________________________________________________");

        }
    }
}