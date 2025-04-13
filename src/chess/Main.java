package chess;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar ChessValidator.jar <path-to-pgn-file-or-folder>");
            return;
        }

        String path = args[0];
        GameValidator validator = new GameValidator();
        List<GameValidator.ValidationResult> results = validator.validateGamesFromFileOrFolder(path);

        int index = 1;
        for (GameValidator.ValidationResult result : results) {
            System.out.println("Game " + index + ": " + (result.isValid ? "VALID ✅" : "INVALID ❌"));
            if (!result.isValid) {
                for (String err : result.errors) {
                    System.out.println("   - " + err);
                }
            }
            index++;
        }
    }
}