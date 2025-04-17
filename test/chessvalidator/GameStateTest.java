package chessvalidator;

import chessvalidator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class GameStateTest {

    private GameState gameState;

    @BeforeEach
    void setUp() {
        gameState = new GameState(); // Starts with initial position
    }

    @Test
    void testInitialPosition() {
        assertEquals(Color.WHITE, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getFullMoveNumber());
        assertTrue(gameState.canCastleKingSide(Color.WHITE));
        assertTrue(gameState.canCastleQueenSide(Color.WHITE));
        assertTrue(gameState.canCastleKingSide(Color.BLACK));
        assertTrue(gameState.canCastleQueenSide(Color.BLACK));
        assertNull(gameState.getEnPassantTargetSquare());
        assertEquals(0, gameState.getHalfMoveClock());
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e1")));
        assertEquals(PieceType.KING, gameState.getBoard().getPiece(Square.fromAlgebraic("e1")).type());
        assertFalse(gameState.isInCheck());
        assertFalse(gameState.isCheckmate());
        assertFalse(gameState.isStalemate());
    }

    @Test
    void testApplySimplePawnMove() {
        applySanMove("e4");

        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e2")));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e4")));
        assertEquals(PieceType.PAWN, gameState.getBoard().getPiece(Square.fromAlgebraic("e4")).type());
        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getFullMoveNumber()); // Still move 1
        assertNotNull(gameState.getEnPassantTargetSquare()); // EP possible after e4
        assertEquals(Square.fromAlgebraic("e3"), gameState.getEnPassantTargetSquare());
        assertEquals(0, gameState.getHalfMoveClock()); // Reset by pawn move
    }

    @Test
    void testApplyKnightMove() {
        applySanMove("Nf3");

        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("g1")));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f3")));
        assertEquals(PieceType.KNIGHT, gameState.getBoard().getPiece(Square.fromAlgebraic("f3")).type());
        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
        assertNull(gameState.getEnPassantTargetSquare()); // Nf3 doesn't set EP
        assertEquals(1, gameState.getHalfMoveClock()); // Incremented by non-pawn, non-capture move
    }

    @Test
    void testApplyKingsideCastlingWhite() {
        // Setup position for castling (needs pieces out of the way)
        gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);

        assertTrue(gameState.canCastleKingSide(Color.WHITE));
        applySanMove("O-O");

        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e1")));
        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("h1")));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("g1")));
        assertEquals(PieceType.KING, gameState.getBoard().getPiece(Square.fromAlgebraic("g1")).type());
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f1")));
        assertEquals(PieceType.ROOK, gameState.getBoard().getPiece(Square.fromAlgebraic("f1")).type());
        assertFalse(gameState.canCastleKingSide(Color.WHITE)); // Castling rights revoked
        assertFalse(gameState.canCastleQueenSide(Color.WHITE));
        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
    }

    @Test
    void testApplyQueensideCastlingBlack() {
        // Setup: White plays some move, then set up for black O-O-O
        applySanMove("e4"); // White's move 1
        gameState.getBoard().setPiece(Square.fromAlgebraic("b8"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("c8"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("d8"), null);

        assertTrue(gameState.canCastleQueenSide(Color.BLACK));
        applySanMove("O-O-O"); // Black's move 1

        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e8")));
        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("a8")));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("c8")));
        assertEquals(PieceType.KING, gameState.getBoard().getPiece(Square.fromAlgebraic("c8")).type());
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("d8")));
        assertEquals(PieceType.ROOK, gameState.getBoard().getPiece(Square.fromAlgebraic("d8")).type());
        assertFalse(gameState.canCastleKingSide(Color.BLACK)); // Castling rights revoked
        assertFalse(gameState.canCastleQueenSide(Color.BLACK));
        assertEquals(Color.WHITE, gameState.getCurrentPlayer());
        assertEquals(2, gameState.getFullMoveNumber()); // Incremented after Black's move
    }


    @Test
    void testApplyEnPassantCapture() {
        applySanMove("e4"); // 1. e4
        applySanMove("h6"); // 1... h6 (dummy move for black)
        applySanMove("e5"); // 2. e5
        applySanMove("f5"); // 2... f5 (Pawn moves two steps, creating EP target e6)

        assertEquals(Square.fromAlgebraic("f6"), gameState.getEnPassantTargetSquare());

        // 3. exf6 e.p.
        applySanMove("exf6"); // SAN might just be exf6

        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f6"))); // White pawn moved to f6
        assertEquals(Color.WHITE, gameState.getBoard().getPiece(Square.fromAlgebraic("f6")).color());
        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e5"))); // Original white pawn square empty
        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f5"))); // Captured black pawn square empty (this is key for EP)
        assertNull(gameState.getEnPassantTargetSquare()); // EP target cleared
        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
    }

    @Test
    void testApplyPromotion() {
        // Setup: White pawn on e7, Black King on a8 (to make it legal), White King on a1
        gameState = new GameState(); // Reset
        gameState.getBoard().setupInitialPosition(); // Start fresh for easier setup
        // Clear board except kings and the pawn
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) gameState.getBoard().setPiece(new Square(r, c), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.KING, Color.WHITE));
        gameState.getBoard().setPiece(Square.fromAlgebraic("a8"), new Piece(PieceType.KING, Color.BLACK));
        gameState.getBoard().setPiece(Square.fromAlgebraic("e7"), new Piece(PieceType.PAWN, Color.WHITE));
        gameState.loadFromFen("k7/4P3/8/8/8/8/8/K7 w - - 0 1"); // Use FEN for precise setup


        applySanMove("e8=Q");

        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e7")));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e8")));
        assertEquals(PieceType.QUEEN, gameState.getBoard().getPiece(Square.fromAlgebraic("e8")).type());
        assertEquals(Color.WHITE, gameState.getBoard().getPiece(Square.fromAlgebraic("e8")).color());
        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
    }

    @Test
    void testIsCheckmate_FoolsMate() {
        applySanMove("f3"); // 1. f3?
        applySanMove("e5"); // 1... e5
        applySanMove("g4"); // 2. g4??
        applySanMove("Qh4#"); // 2... Qh4#

        assertTrue(gameState.isCheckmate()); // White (current player after black's move) is checkmated
        assertEquals(Color.WHITE, gameState.getCurrentPlayer());
        assertTrue(gameState.isInCheck());
        assertTrue(gameState.generateLegalMoves().isEmpty());
    }

    @Test
    void testIsStalemate() {
        // Setup: White King h1, White Queen g5. Black King h3. Black to move.
        // Black has no legal moves but is not in check.
        gameState.loadFromFen("8/8/8/6Q1/8/7k/8/7K b - - 0 1");

        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
        assertFalse(gameState.isInCheck());
        assertTrue(gameState.generateLegalMoves().isEmpty());
        assertTrue(gameState.isStalemate());
        assertFalse(gameState.isCheckmate());
    }


    @Test
    void testGenerateLegalMoves_InitialPosition() {
        List<Move> moves = gameState.generateLegalMoves();
        // Standard opening position has 20 legal moves (16 pawn moves + 4 knight moves)
        assertEquals(20, moves.size());
        // Check a few examples are present
        assertTrue(findMove(moves, "e2", "e4", PieceType.PAWN), "e4 missing");
        assertTrue(findMove(moves, "g1", "f3", PieceType.KNIGHT), "Nf3 missing");
        // Check an illegal move isn't present
        assertFalse(findMove(moves, "e1", "e2", PieceType.KING), "Ke2 should be illegal");
    }

    @Test
    void testGenerateLegalMoves_PinnedPieceCannotMoveAlongPin() {
        // Setup: White King e1, White Rook e2, Black Rook e8. Rook on e2 is pinned.
        gameState.loadFromFen("4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1");
        List<Move> moves = gameState.generateLegalMoves();

        // Pinned Rook should not be able to move vertically along the pin line (e3, e4, etc.)
        assertFalse(findMove(moves, "e2", "e3", PieceType.ROOK), "Pinned Rook Re3 should be illegal");
        assertFalse(findMove(moves, "e2", "e4", PieceType.ROOK), "Pinned Rook Re4 should be illegal");
        // Pinned Rook *can* move horizontally if not blocked (doesn't expose king)
        assertTrue(findMove(moves, "e2", "d2", PieceType.ROOK), "Pinned Rook Rd2 should be legal");
        assertTrue(findMove(moves, "e2", "f2", PieceType.ROOK), "Pinned Rook Rf2 should be legal");
        // Pinned Rook *can* capture the pinning piece
        assertTrue(findMove(moves, "e2", "e8", PieceType.ROOK), "Pinned Rook Rxe8 should be legal");
        // King moves should be available
        assertTrue(findMove(moves, "e1", "d1", PieceType.KING), "Kd1 should be legal");
        assertTrue(findMove(moves, "e1", "f1", PieceType.KING), "Kf1 should be legal");
        // King cannot move into the pin line
        assertFalse(findMove(moves, "e1", "e2", PieceType.KING), "Ke2 should be illegal");
    }


    @Test
    void testGenerateLegalMoves_CannotCastleThroughCheck() {
        // Setup: White to castle kingside, but f1 is attacked by Black Bishop on c4
        gameState.loadFromFen("r3k2r/pppqppbp/2np1np1/8/2bPP3/2N2N2/PPP2PPP/R1BQR1K1 w kq - 0 8");
        // Black Bishop on c4 attacks f1. White has castling rights 'kq'.
        List<Move> moves = gameState.generateLegalMoves();

        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("e1"), Color.BLACK)); // King not in check
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("f1"), Color.BLACK)); // f1 attacked

        // Kingside castling should NOT be in the legal moves
        assertFalse(findCastleKingside(moves), "O-O should be illegal (moving through check on f1)");
        // Queenside castling might still be legal if path is clear/safe (check b1,c1,d1)
        // Based on FEN: b1 empty, c1 empty, d1 Queen. Queenside blocked.
        // Let's check manually:
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("d1"), Color.BLACK));
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("c1"), Color.BLACK));
        assertFalse(findCastleQueenside(moves), "O-O-O should be illegal (path not clear)"); // Actually illegal because d1 is occupied
    }


    @Test
    void testLoadFromFen_Valid() {
        String fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq e3 0 1";
        assertDoesNotThrow(() -> gameState.loadFromFen(fen));

        assertEquals(Color.BLACK, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getFullMoveNumber());
        assertEquals(0, gameState.getHalfMoveClock());
        assertEquals(Square.fromAlgebraic("e3"), gameState.getEnPassantTargetSquare());
        assertTrue(gameState.canCastleKingSide(Color.WHITE));
        assertTrue(gameState.canCastleQueenSide(Color.WHITE));
        assertTrue(gameState.canCastleKingSide(Color.BLACK));
        assertTrue(gameState.canCastleQueenSide(Color.BLACK));
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("c5")));
        assertEquals(PieceType.PAWN, gameState.getBoard().getPiece(Square.fromAlgebraic("c5")).type());
        assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e2"))); // Moved pawn
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f3"))); // Moved knight
    }

    @Test
    void testLoadFromFen_Invalid_Format() {
        String tooFewParts = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0";
        String tooManyParts = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 extra";
        String badRankCount = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1";
        String rankTooLong = "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        String rankInvalidChar = "rnbqkbnr/ppXppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        String badColor = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1";
        String badCastle = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQXq - 0 1";
        String badEP = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e9 0 1"; // Invalid square
        String badHalfMove = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1";
        String badFullMove = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0";

        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(tooFewParts));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(tooManyParts));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badRankCount));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(rankTooLong));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(rankInvalidChar));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badColor));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badCastle));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badEP));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badHalfMove));
        assertThrows(IllegalArgumentException.class, () -> gameState.loadFromFen(badFullMove));
    }


    // --- Helper Methods for Tests ---

    /** Applies a move specified by SAN, asserting it's found and applied without error */
    private void applySanMove(String san) {
        try {
            Move move = SanHelper.sanToMove(san, gameState);
            assertNotNull(move, "Move not found for SAN: " + san);
            gameState.applyMove(move);
        } catch (IllegalArgumentException e) {
            fail("Failed to parse or apply supposedly legal SAN move '" + san + "': " + e.getMessage(), e);
        }
    }

    /** Finds if a move with specific from/to/piece exists in a list */
    private boolean findMove(List<Move> moves, String fromAlg, String toAlg, PieceType type) {
        Square from = Square.fromAlgebraic(fromAlg);
        Square to = Square.fromAlgebraic(toAlg);
        return moves.stream().anyMatch(m ->
                !m.isCastling() && // Exclude castling unless specifically testing it
                        m.from().equals(from) &&
                        m.to().equals(to) &&
                        m.pieceMoved().type() == type
        );
    }

    private boolean findCastleKingside(List<Move> moves) {
        return moves.stream().anyMatch(Move::isCastleKingside);
    }

    private boolean findCastleQueenside(List<Move> moves) {
        return moves.stream().anyMatch(Move::isCastleQueenside);
    }
}