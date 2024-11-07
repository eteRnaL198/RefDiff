package refdiff.parsers.universal;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Ast {
  public static void main(String[] args) {
    try {            
            ProcessBuilder builder = new ProcessBuilder("npx", "tree-sitter", "parse", "./main.java");
            builder.directory(new java.io.File("../tree-sitter-java"));
            
            Process process = builder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            
            process.waitFor();
            
            String commandResult = result.toString();
            System.out.println("result:\n" + commandResult);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
  }
}
