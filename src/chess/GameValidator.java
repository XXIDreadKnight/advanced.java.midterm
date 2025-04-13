package chess;

import chess.util.*;
import java.io.File;
import java.util.*;

public class GameValidator {

    public static class ValidationResult {
        public final PGNParser.Game game;
        public final boolean isValid;
        public final List<String> errors;

        public ValidationResult(PGNParser.Game game, boolean isValid, List<String> errors) {
            this.game = game;
            this.isValid = isValid;
            this.errors = errors;
        }
    }

    public List<ValidationResult> validateGamesFromFileOrFolder(String path) {
        List<ValidationResult> results = new ArrayList<>();
        File f = new File(path);
        File[] files = f.isDirectory() ? Objects.requireNonNull(f.listFiles()) : new File[]{f};

        PGNParser parser = new PGNParser();

        for (File file : files) {
            try {
                List<PGNParser.Game> games = parser.parseFile(file);
                for (PGNParser.Game game : games) {
                    results.add(validateGame(game));
                }
            } catch (Exception e) {
                System.err.println("Failed to parse file: " + file.getName());
                e.printStackTrace();
            }
        }

        return results;
    }

    public ValidationResult validateGame(PGNParser.Game game) {
        List<String> errors = new ArrayList<>();
        ChessBoard board = new ChessBoard();
        SANParser sanParser = new SANParser(board);

        int moveIndex = 0;
        for (String moveText : game.moveTexts) {
            try {
                Move move = sanParser.parse(moveText);
                if (!board.applyMove(move)) {
                    errors.add("Illegal move at step " + (moveIndex + 1) + ": " + moveText);
                    return new ValidationResult(game, false, errors);
                }
            } catch (Exception e) {
                errors.add("Parsing error at step " + (moveIndex + 1) + ": " + moveText + " - " + e.getMessage());
            }
            moveIndex++;
        }

        return new ValidationResult(game, errors.isEmpty(), errors);
    }
}