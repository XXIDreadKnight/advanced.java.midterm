package chessvalidator;

import chessvalidator.model.Color;
import chessvalidator.model.GameState;
import chessvalidator.model.Move;
import chessvalidator.model.SanHelper;
import chessvalidator.reports.GameValidationResult;
import chessvalidator.reports.PgnErrorInfo;

public class GameValidator {

    /**
     * Validates a single parsed game by replaying moves.
     * Stops at the first logical error.
     * Assumes syntax validation has already happened.
     *
     * @param parsedGame The game data parsed by PgnParser.
     * @return A GameValidationResult containing the outcome and any errors.
     */
    public GameValidationResult validate(PgnParser.ParsedGame parsedGame) {
        GameValidationResult result = new GameValidationResult(parsedGame.getGameIndex(), parsedGame.getHeaders());

        // 1. Add any pre-existing syntax errors from the parsing phase
        parsedGame.getSyntaxErrors().forEach(result::addError);
        if (!parsedGame.getSyntaxErrors().isEmpty()) {
            // If syntax errors exist, we often don't proceed to logical validation,
            // but the requirement allows it. Let's try anyway unless it's too broken.
            // For simplicity now, stop if syntax errors were found.
            // Comment out below return to try logical validation even with syntax errors.
            // return result; // Stop validation here if syntax is bad
        }

        // 2. Initialize game state (Check for FEN first)
        GameState gameState;
        try {
            gameState = new GameState(); // Create instance
            String fen = parsedGame.getFenString();
            if (fen != null && !fen.isBlank()) {
                System.out.println("Game " + (parsedGame.getGameIndex()+1) + ": Starting from FEN: " + fen); // Debugging output
                gameState.loadFromFen(fen); // <<< LOAD FROM FEN
            } else {
                // No FEN provided, GameState default constructor already set up initial position
                System.out.println("Game " + (parsedGame.getGameIndex()+1) + ": Starting from initial position."); // Debugging output
            }
        } catch (IllegalArgumentException e) {
            // FEN parsing failed
            result.addError(new PgnErrorInfo(
                    parsedGame.getGameIndex(),
                    0, // Error before first move
                    "FEN Header", // Location hint
                    "Logical error: Invalid FEN string provided: " + e.getMessage(),
                    false // Logical error (prevents game replay)
            ));
            return result; // Cannot proceed if FEN is invalid
        } catch (Exception e) {
            // Catch unexpected errors during FEN loading
            result.addError(new PgnErrorInfo(
                    parsedGame.getGameIndex(),
                    0,
                    "FEN Header",
                    "Unexpected error loading FEN: " + e.getMessage(),
                    false
            ));
            e.printStackTrace();
            return result;
        }

        int halfMoveCount = 0; // Track moves for error reporting (1. e4 e5 is 2 half-moves)

        // 3. Replay moves one by one
        for (String sanMove : parsedGame.getSanMoves()) {
            halfMoveCount++;
            int fullMoveNum = gameState.getFullMoveNumber();


            try {
                // Attempt to parse SAN and find the corresponding legal move
                Move legalMove = SanHelper.sanToMove(sanMove, gameState);

                // Apply the validated legal move
                gameState.applyMove(legalMove);

                // Optional: Add checks for check/mate consistency with SAN '+' or '#'
                // boolean sanIndicatesCheck = sanMove.contains("+");
                // boolean sanIndicatesMate = sanMove.contains("#");
                // boolean actualCheck = gameState.isInCheck(); // Check AFTER move is made, for the NEXT player
                // boolean actualMate = gameState.isCheckmate(); // Check AFTER move is made

                // Report inconsistencies if desired (can be noisy)

            } catch (IllegalArgumentException | IllegalStateException e) {
                // This catches:
                // - Invalid SAN format
                // - Ambiguous SAN
                // - SAN corresponds to an illegal move
                // - Internal errors (e.g., king not found)
                String errorMessage = "Logical error: " + e.getMessage();
                result.addError(new PgnErrorInfo(
                        parsedGame.getGameIndex(),
                        fullMoveNum, // Use full move number for location
                        sanMove,
                        errorMessage,
                        false // Logical error
                ));
                // Stop processing this game on the first logical error
                return result;
            } catch (Exception e) {
                // Catch unexpected errors during validation
                result.addError(new PgnErrorInfo(
                        parsedGame.getGameIndex(),
                        fullMoveNum,
                        sanMove,
                        "Unexpected error during validation: " + e.getMessage(),
                        false
                ));
                e.printStackTrace(); // Log unexpected errors
                return result;
            }
        }

        // 4. Optional: Check final game state against Result header
        String resultHeader = parsedGame.getHeaders().get("Result");
        if (resultHeader != null && !resultHeader.equals("*")) {
            boolean mate = gameState.isCheckmate();
            boolean stale = gameState.isStalemate();
            // Basic check: If result is 1-0, black should be mated or resigned. If 0-1, white. If 1/2-1/2, stalemate or draw by rule.
            // This is complex to verify fully (resignations, 50-move, repetition aren't tracked here yet)
            // Add simple checks if needed. Example:
            // if (resultHeader.equals("1-0") && !(mate && gameState.getCurrentPlayer() == Color.BLACK)) { // White wins, black should be mated
            //     result.addError(new PgnErrorInfo(parsedGame.getGameIndex(), 0, "Result", "Header claims 1-0, but final position is not Black checkmated.", false));
            // } // Add similar checks for 0-1 and 1/2-1/2 (stalemate)
        }


        // If we reach here without logical errors, the game sequence is valid according to rules.
        // The overall game validity (`result.isValid()`) depends on whether syntax errors were also present.
        return result;
    }
}