package chess;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for the Chess PGN Validator application.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp <classpath> chess.Main <path-to-pgn-file-or-folder>");
            System.out.println("Example (if compiled to classes folder): java -cp . chess.Main path/to/games");
            System.out.println("Example (if packaged to JAR): java -jar ChessValidator.jar path/to/games");
            return;
        }

        String path = args[0];
        System.out.println("Starting Chess PGN Validation for path: " + path);
        System.out.println("=============================================");

        GameValidator validator = new GameValidator();
        List<GameValidator.ValidationResult> results = validator.validateGamesFromFileOrFolder(path);

        System.out.println("\n=============================================");
        System.out.println("Validation Summary:");
        System.out.println("=============================================");

        if (results.isEmpty()) {
            System.out.println("No games processed or validated.");
            return;
        }

        int validCount = 0;
        int invalidCount = 0;

        for (GameValidator.ValidationResult result : results) {
            String status = result.isValid ? "VALID ✅" : "INVALID ❌";
            System.out.printf("%s: %s\n", result.gameIdentifier, status);

            // Print headers for context (optional)
            System.out.println("  Headers:");
            for (Map.Entry<String, String> entry : result.game.headers.entrySet()) {
                System.out.printf("    [%s \"%s\"]\n", entry.getKey(), entry.getValue());
            }


            if (!result.isValid) {
                invalidCount++;
                System.out.println("  Errors:");
                for (String err : result.errors) {
                    System.out.println("    - " + err);
                }
            } else {
                validCount++;
                System.out.println("  Result: " + result.game.result); // Show result for valid games
            }
            System.out.println("---------------------------------------------"); // Separator
        }

        System.out.println("\n=============================================");
        System.out.println("Total Games Processed: " + results.size());
        System.out.println("Valid Games:           " + validCount);
        System.out.println("Invalid Games:         " + invalidCount);
        System.out.println("=============================================");
    }
}