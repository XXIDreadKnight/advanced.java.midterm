package chessvalidator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SanHelper {

    // Regex needs refinement, especially for disambiguation and checks/mates
    // Basic structure: (Piece?)(Disambiguation?)(x?)(Target Square)(Promotion?)(Check/Mate?)
    // Example: Nf3, exd5, O-O, O-O-O, e8=Q, Raxd1+, Ngf3#
    // This is a simplified pattern - robust SAN parsing is very complex.
    private static final Pattern SAN_PATTERN = Pattern.compile(
            "^(?<piece>[NBRQK])?" +                     // Optional Piece (Pawn is implied)
                    "(?<disambiguation>[a-h]?[1-8]?)?" +       // Optional disambiguation (file, rank, or both - needs context)
                    "(?<capture>x)?" +                         // Optional capture 'x'
                    "(?<target>[a-h][1-8])" +                  // Target square (e.g., e4)
                    "(?:=(?<promotion>[NBRQ]))?" +             // Optional promotion (e.g., =Q)
                    "(?<checkormate>[+#])?" +                   // Optional check (+) or mate (#) - informational only for parsing
                    "$"); // End of string

    private static final Pattern CASTLE_KINGSIDE_PATTERN = Pattern.compile("^(O-O|0-0)[+#]?$");
    private static final Pattern CASTLE_QUEENSIDE_PATTERN = Pattern.compile("^(O-O-O|0-0-0)[+#]?$");


    /**
     * Parses a Standard Algebraic Notation (SAN) move string in the context of a given game state.
     *
     * @param san       The move string (e.g., "Nf3", "exd5", "O-O").
     * @param gameState The current state of the game.
     * @return The corresponding legal Move object.
     * @throws IllegalArgumentException if the SAN is invalid, ambiguous, or represents an illegal move.
     */
    public static Move sanToMove(String san, GameState gameState) throws IllegalArgumentException {
        List<Move> legalMoves = gameState.generateLegalMoves();
        Color currentPlayer = gameState.getCurrentPlayer();

        // 1. Handle Castling
        if (CASTLE_KINGSIDE_PATTERN.matcher(san).matches()) {
            return findMatchingMove(legalMoves, Move::isCastleKingside, san, "Kingside Castling");
        }
        if (CASTLE_QUEENSIDE_PATTERN.matcher(san).matches()) {
            return findMatchingMove(legalMoves, Move::isCastleQueenside, san, "Queenside Castling");
        }

        // 2. Handle Regular Moves
        Matcher matcher = SAN_PATTERN.matcher(san);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid SAN format: " + san);
        }

        String pieceStr = matcher.group("piece");
        String disambiguationStr = matcher.group("disambiguation");
        boolean isCapture = matcher.group("capture") != null;
        String targetSquareStr = matcher.group("target");
        String promotionStr = matcher.group("promotion");
        // String checkOrMateStr = matcher.group("checkormate"); // Ignored for finding the move

        PieceType movingPieceType = (pieceStr == null) ? PieceType.PAWN : PieceType.fromSanChar(pieceStr.charAt(0));
        Square targetSquare = Square.fromAlgebraic(targetSquareStr);
        PieceType promotionPieceType = (promotionStr == null) ? null : PieceType.fromSanChar(promotionStr.charAt(0));

        if (targetSquare == null) {
            throw new IllegalArgumentException("Invalid target square in SAN: " + targetSquareStr);
        }

        // Filter legal moves to find candidates matching the SAN components
        List<Move> candidates = new ArrayList<>();
        for (Move move : legalMoves) {
            if (move.pieceMoved().type() == movingPieceType &&
                    move.to().equals(targetSquare) &&
                    (promotionPieceType == null || move.promotionPieceType() == promotionPieceType))
            {
                // Check capture flag consistency (optional but good practice)
                // PGN 'x' is sometimes omitted for pawn captures, so primarily rely on target square occupation
                boolean moveIsCapture = move.isCapture(); // This includes en passant
                Piece targetOccupant = gameState.getBoard().getPiece(targetSquare);
                boolean targetOccupiedByOpponent = targetOccupant != null && targetOccupant.color() != currentPlayer;

                // Basic capture check: if SAN has 'x', move must be capture. If SAN !has 'x', move must not be capture (except EP maybe).
                // More robust: If target square is occupied by opponent OR it's an EP move, it's a capture context.
                boolean isCaptureContext = targetOccupiedByOpponent || move.isEnPassantCapture();

                // Allow SAN 'x' if it's a capture context. Allow no 'x' if not capture context.
                // Tolerate missing 'x' for captures (common) but flag explicit 'x' on non-capture as error later?
                if (isCapture && !isCaptureContext) {
                    continue; // SAN says capture, but move isn't - invalid SAN/move mismatch
                }
                // PGN standard technically requires 'x' for captures except pawns.
                // We might be more lenient here just to find the move.

                // Check disambiguation if present
                if (disambiguationStr != null && !disambiguationStr.isEmpty()) {
                    if (!matchesDisambiguation(move.from(), disambiguationStr)) {
                        continue; // Doesn't match the required disambiguation
                    }
                }
                candidates.add(move);
            }
        }

        // Evaluate candidates
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Illegal move or invalid SAN: " + san + " (No legal move matches)");
        }

        if (candidates.size() > 1) {
            // Ambiguity check: If multiple moves match BUT disambiguation was NOT provided,
            // or the provided disambiguation was insufficient.
            if (disambiguationStr == null || disambiguationStr.isEmpty()) {
                throw new IllegalArgumentException("Ambiguous move: " + san + " (Matches: " + candidates.stream().map(Move::toString).collect(Collectors.joining(", ")) + ")");
            } else {
                // If disambiguation *was* provided but still multiple matches, it means the SAN
                // might be syntactically okay but still ambiguous in *this specific position*,
                // OR our disambiguation logic needs refinement.
                throw new IllegalArgumentException("Ambiguous move despite disambiguation '" + disambiguationStr + "': " + san + " (Matches: " + candidates.stream().map(Move::toString).collect(Collectors.joining(", ")) + ")");
            }
        }

        // We should have exactly one match
        return candidates.get(0);
    }

    /** Helper to check if a 'from' square matches a disambiguation string */
    private static boolean matchesDisambiguation(Square from, String disambiguation) {
        if (disambiguation == null || disambiguation.isEmpty()) {
            return true; // No disambiguation needed
        }
        String fromAlg = from.toAlgebraic(); // e.g., "d2"
        if (disambiguation.length() == 1) {
            char dChar = disambiguation.charAt(0);
            if (dChar >= 'a' && dChar <= 'h') { // File disambiguation (e.g., "d")
                return fromAlg.charAt(0) == dChar;
            } else if (dChar >= '1' && dChar <= '8') { // Rank disambiguation (e.g., "2")
                return fromAlg.charAt(1) == dChar;
            }
        } else if (disambiguation.length() == 2) { // Full square disambiguation (e.g., "d2")
            return fromAlg.equals(disambiguation);
        }
        return false; // Invalid disambiguation format or doesn't match
    }


    /** Helper to find a unique move matching a predicate */
    private static <T> T findMatchingMove(List<T> moves, java.util.function.Predicate<T> predicate, String san, String moveDescription) throws IllegalArgumentException {
        List<T> matches = new ArrayList<>();
        for (T move : moves) {
            if (predicate.test(move)) {
                matches.add(move);
            }
        }
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Illegal move: " + san + " (" + moveDescription + " not possible)");
        }
        if (matches.size() > 1) {
            // This shouldn't happen for unique actions like castling if generation is correct
            throw new IllegalStateException("Internal error: Multiple legal moves found for unique action: " + san);
        }
        return matches.get(0);
    }

}