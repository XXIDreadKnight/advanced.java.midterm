package chess;

import chess.util.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Validates chess games provided in PGN format, checking both PGN syntax and chess rule legality.
 */
public class GameValidator {

    /**
     * Holds the result of validating a single game.
     */
    public static class ValidationResult {
        public final PGNParser.Game game; // Parsed game data
        public final boolean isValid;     // Overall validity (syntax and logic)
        public final List<String> errors; // List of errors found (syntax or logical)
        public final String gameIdentifier; // e.g., "File 'x.pgn', Game 3"

        public ValidationResult(PGNParser.Game game, boolean isValid, List<String> errors) {
            this.game = game;
            this.isValid = isValid;
            this.errors = errors;
            this.gameIdentifier = String.format("File '%s', Game %d", game.fileName, game.gameNumberInFile);
        }
    }

    /**
     * Validates all PGN games found in the specified file or folder.
     * @param path Path to a PGN file or a folder containing PGN files.
     * @return A list of ValidationResult objects, one for each game found.
     */
    public List<ValidationResult> validateGamesFromFileOrFolder(String path) {
        List<ValidationResult> results = new ArrayList<>();
        File f = new File(path);
        PGNParser parser = new PGNParser();

        List<File> filesToProcess = new ArrayList<>();
        if (f.isFile() && f.getName().toLowerCase().endsWith(".pgn")) {
            filesToProcess.add(f);
        } else if (f.isDirectory()) {
            File[] listing = f.listFiles((dir, name) -> name.toLowerCase().endsWith(".pgn"));
            if (listing != null) {
                filesToProcess.addAll(Arrays.asList(listing));
            }
        } else {
            System.err.println("Error: Path is not a valid file or directory: " + path);
            return results; // Return empty list
        }

        if (filesToProcess.isEmpty()) {
            System.err.println("No PGN files found at path: " + path);
            return results;
        }


        for (File file : filesToProcess) {
            try {
                System.out.println("Processing file: " + file.getName() + "...");
                List<PGNParser.Game> games = parser.parseFile(file);
                System.out.println("  Found " + games.size() + " game(s). Validating...");

                for (PGNParser.Game game : games) {
                    // Validate each game individually
                    results.add(validateSingleGame(game));
                }
            } catch (IOException e) {
                System.err.println("!! Failed to read or parse file: " + file.getName() + " - " + e.getMessage());
                // Optionally create a 'failed file' result entry
            } catch (Exception e) {
                System.err.println("!! Unexpected error processing file: " + file.getName());
                e.printStackTrace(); // Log unexpected errors
            }
        }

        return results;
    }

    /**
     * Validates a single parsed PGN game for both syntax and logical errors.
     * @param game The parsed PGN game data.
     * @return A ValidationResult object.
     */
    public ValidationResult validateSingleGame(PGNParser.Game game) {
        List<String> errors = new ArrayList<>();
        errors.addAll(game.syntaxErrors); // Start with syntax errors from the parser

        ChessBoard board = new ChessBoard();
        SANParser sanParser = new SANParser(board); // Needs the board for context
        boolean logicErrorFound = false;

        // Only proceed with move validation if basic syntax seems okay (or configurable)
        // For now, always try, but report syntax errors first.

        int ply = 0; // Half-move counter
        for (int i = 0; i < game.moveTexts.size(); i++) {
            String moveText = game.moveTexts.get(i);
            ply++;
            int moveNumber = (ply + 1) / 2;
            Color turn = board.getCurrentTurn();
            String movePrefix = String.format("Move %d (%s): '%s'", moveNumber, turn, moveText);

            try {
                // 1. Parse SAN to get a potential move
                Move move = sanParser.parse(moveText);

                // 2. Apply the move (this now includes full legality check)
                // applyMove will throw IllegalMoveException if invalid
                board.applyMove(move);

                // Optional: Check for checkmate/stalemate after the move
                // if (board.isCheckmate()) { ... game ended ... }
                // if (board.isStalemate()) { ... game ended ... }

            } catch (IllegalMoveException e) {
                // Catch illegal moves (from SAN parsing or board application)
                errors.add(String.format("%s - Illegal: %s", movePrefix, e.getMessage()));
                logicErrorFound = true;
                break; // Stop processing moves for this game on first logical error
            } catch (Exception e) {
                // Catch other unexpected errors during parsing/validation
                errors.add(String.format("%s - Unexpected Error: %s", movePrefix, e.getMessage()));
                // Log stack trace for debugging unexpected issues
                // e.printStackTrace();
                logicErrorFound = true;
                break; // Stop processing on unexpected errors too
            }
        }

        // Final check: Game result consistency (if no prior errors stopped processing)
        if (!logicErrorFound && game.moveTexts.size() > 0) {
            String expectedResult = "*"; // Ongoing
            if (board.isCheckmate()) {
                expectedResult = (board.getCurrentTurn() == Color.BLACK) ? "1-0" : "0-1"; // Winner is the one whose turn it *was*
            } else if (board.isStalemate()) {
                expectedResult = "1/2-1/2";
            }
            // Could add checks for other draw conditions (50-move, repetition) if implemented

            if (!game.result.equals("*") && !expectedResult.equals("*") && !game.result.equals(expectedResult)) {
                errors.add(String.format("Game End Mismatch: PGN result is '%s', but board state implies '%s'.", game.result, expectedResult));
            } else if (game.result.equals("*") && !expectedResult.equals("*")) {
                errors.add(String.format("Game End Mismatch: PGN result is '*', but board state implies game over ('%s').", expectedResult));
            } else if (!game.result.equals("*") && expectedResult.equals("*")) {
                // PGN says game over, but board state doesn't confirm (could be resignation, draw offer, etc.)
                // This is harder to automatically verify without more rules. Optionally flag it.
                // errors.add(String.format("Game End Info: PGN result is '%s', but board state is ongoing.", game.result));
            }
        }


        boolean isValid = errors.isEmpty();
        return new ValidationResult(game, isValid, errors);
    }
}