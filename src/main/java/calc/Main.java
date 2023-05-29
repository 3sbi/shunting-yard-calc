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
            System.out.println("Array of nodes: " + Arrays.toString(rpn.toArray()));
            Node tree = yard.parse(rpn);
            double result = yard.eval(tree);
            System.out.printf("Result: %f\n", result);
            System.out.println("___________________________________________________");
        }
    }
}