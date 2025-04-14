package chess.util;

import chess.*;
import chess.pieces.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Standard Algebraic Notation (SAN) moves within the context of a ChessBoard.
 */
public class SANParser {

    private final ChessBoard board; // Needs board state to resolve ambiguity and check legality

    // Regex patterns for SAN components
    private static final Pattern SAN_PATTERN = Pattern.compile(
            "([NBRQK])?" +      // Optional piece type (group 1)
                    "([a-h]?[1-8]?)?" + // Optional disambiguation file/rank (group 2)
                    "(x)?" +            // Optional capture indication 'x' (group 3)
                    "([a-h][1-8])" +    // Target square (group 4)
                    "(?:=([NBRQ]))?" +   // Optional promotion piece (group 5)
                    "([+#])?"           // Optional check/checkmate symbol (group 6) - ignored during parsing
    );
    private static final Pattern CASTLE_KINGSIDE = Pattern.compile("(O-O|0-0)\\+?#?");
    private static final Pattern CASTLE_QUEENSIDE = Pattern.compile("(O-O-O|0-0-0)\\+?#?");


    public SANParser(ChessBoard board) {
        this.board = board;
    }

    /**
     * Parses a SAN move string and returns the corresponding Move object.
     * @param san The SAN string (e.g., "Nf3", "exd5", "O-O", "e8=Q+").
     * @return The corresponding Move object.
     * @throws IllegalMoveException If the SAN is malformed, ambiguous, or represents an illegal move.
     */
    public Move parse(String san) throws IllegalMoveException {
        san = san.trim();
        Color currentTurn = board.getCurrentTurn();

        // Handle Castling
        if (CASTLE_KINGSIDE.matcher(san).matches()) {
            int row = (currentTurn == Color.WHITE) ? 7 : 0;
            // Create a potential move; ChessBoard will validate legality fully
            // Need the actual King piece from the board
            Piece king = board.getPiece(row, 4);
            if (!(king instanceof King) || king.color != currentTurn) {
                throw new IllegalMoveException("Invalid board state for castling: King not found at " + Move.coordToString(row, 4));
            }
            return new Move(row, 4, row, 6, king, true); // Kingside flag = true
        }
        if (CASTLE_QUEENSIDE.matcher(san).matches()) {
            int row = (currentTurn == Color.WHITE) ? 7 : 0;
            Piece king = board.getPiece(row, 4);
            if (!(king instanceof King) || king.color != currentTurn) {
                throw new IllegalMoveException("Invalid board state for castling: King not found at " + Move.coordToString(row, 4));
            }
            return new Move(row, 4, row, 2, king, false); // Kingside flag = false
        }

        // Handle standard moves using Regex
        Matcher matcher = SAN_PATTERN.matcher(san);
        if (!matcher.matches()) {
            throw new IllegalMoveException("Invalid SAN move format: '" + san + "'");
        }

        String pieceStr = matcher.group(1);
        String disambiguation = matcher.group(2);
        boolean isCapture = matcher.group(3) != null;
        String targetSquare = matcher.group(4);
        String promotionPieceStr = matcher.group(5);
        // Group 6 (check/mate) is ignored

        char pieceType = (pieceStr != null) ? pieceStr.charAt(0) : 'P'; // Default to Pawn
        char promotionPiece = (promotionPieceStr != null) ? promotionPieceStr.charAt(0) : 0;

        int toCol = targetSquare.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(targetSquare.charAt(1));

        // --- Find the piece that can make this move ---
        List<Move> candidateMoves = findCandidateMoves(pieceType, disambiguation, toRow, toCol, isCapture, promotionPiece);

        if (candidateMoves.isEmpty()) {
            throw new IllegalMoveException("No legal move found matching SAN '" + san + "' for " + currentTurn);
        }
        if (candidateMoves.size() > 1) {
            // If multiple pieces *could* make the move based on SAN, it's ambiguous
            StringBuilder ambiguityDetails = new StringBuilder();
            for(Move m : candidateMoves) ambiguityDetails.append(m.getFromSquare()).append(" ");
            throw new IllegalMoveException("Ambiguous SAN move '" + san + "'. Possible origins: " + ambiguityDetails.toString().trim());
        }

        // Found exactly one legal move matching the SAN
        return candidateMoves.get(0);
    }

    /**
     * Finds all *legal* moves on the current board that match the parsed SAN components.
     */
    private List<Move> findCandidateMoves(char pieceType, String disambiguation, int toRow, int toCol, boolean sanIndicatesCapture, char promotionPiece) {
        List<Move> matchingMoves = new ArrayList<>();
        Color currentTurn = board.getCurrentTurn();
        List<Move> allLegalMoves = board.generateLegalMoves(); // Get all fully legal moves

        for (Move legalMove : allLegalMoves) {
            Piece movingPiece = legalMove.movingPiece;

            // 1. Check piece type matches
            if (movingPiece.getSymbol() != pieceType) continue;

            // 2. Check target square matches
            if (legalMove.toRow != toRow || legalMove.toCol != toCol) continue;

            // 3. Check promotion piece matches (if applicable)
            if (promotionPiece != 0 && (!legalMove.isPromotion || legalMove.promotionPieceSymbol != promotionPiece)) continue;
            if (promotionPiece == 0 && legalMove.isPromotion) continue; // SAN didn't specify promotion but move is one

            // 4. Check capture indication consistency (optional but good)
            boolean actualCapture = legalMove.capturedPiece != null || legalMove.isEnPassant;
            // Basic check: SAN 'x' should mean actual capture, no 'x' should mean no capture.
            // Note: PGN standard is complex here, 'x' is optional sometimes. Be lenient?
            // Let's enforce: if SAN has 'x', move must be capture. If SAN lacks 'x', move must not be capture.
            // This might reject some valid PGN, but makes parsing deterministic here.
            if (sanIndicatesCapture != actualCapture) {
                // Allow pawn captures like "exd5" where 'x' is mandatory in SAN despite not being in group 3 of regex
                if (!(movingPiece instanceof Pawn && Math.abs(legalMove.fromCol - legalMove.toCol) == 1 && actualCapture)) {
                    // If not a pawn capture, enforce strict x match
                    continue;
                }
            }


            // 5. Check disambiguation matches
            if (disambiguation != null && !disambiguation.isEmpty()) {
                String fromSquare = legalMove.getFromSquare(); // e.g., "e2"
                boolean fileMatch = false;
                boolean rankMatch = false;

                if (disambiguation.length() == 1) {
                    char disChar = disambiguation.charAt(0);
                    if (disChar >= 'a' && disChar <= 'h') { // Disambiguation by file
                        if (fromSquare.charAt(0) != disChar) continue; // File doesn't match
                        fileMatch = true;
                    } else if (disChar >= '1' && disChar <= '8') { // Disambiguation by rank
                        if (fromSquare.charAt(1) != disChar) continue; // Rank doesn't match
                        rankMatch = true;
                    } else {
                        // Invalid disambiguation char - should have been caught earlier?
                        continue;
                    }
                } else if (disambiguation.length() == 2) { // Disambiguation by full square (e.g., "Nce2")
                    if (!fromSquare.equals(disambiguation)) continue;
                    fileMatch = true;
                    rankMatch = true;
                } else {
                    // Invalid disambiguation length
                    continue;
                }
                // If we reach here, the disambiguation rule was met
            }

            // If all checks passed, this legal move matches the SAN components
            matchingMoves.add(legalMove);
        }


        // --- Refine based on disambiguation rules ---
        // If multiple moves matched initially, we need to see if the disambiguation
        // provided (or lack thereof) uniquely identifies one. The loop above handles
        // the *filtering* part. If >1 remain, it's truly ambiguous.
        // If 0 remain, it's illegal.
        // If 1 remains, it's the correct move.

        // Re-check ambiguity if disambiguation was NOT provided but multiple moves match
        if ((disambiguation == null || disambiguation.isEmpty()) && matchingMoves.size() > 1) {
            // Need to determine if disambiguation *was required* according to PGN rules.
            // This is complex. For now, if we found multiple legal moves matching the basic SAN
            // without specific disambiguation chars, we declare it ambiguous.
            // A stricter parser would check if file/rank/square was needed.
            return new ArrayList<>(); // Treat as ambiguous/error if > 1 match without disambiguation
        }


        return matchingMoves; // Return the single match or empty list
    }

    // Helper (already in Move class, but potentially useful here too)
    private static String coordToString(int row, int col) {
        return "" + (char)('a' + col) + (char)('8' - row);
    }

}