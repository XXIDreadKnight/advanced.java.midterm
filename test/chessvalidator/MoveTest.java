package chessvalidator;

import chessvalidator.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    private final Square e2 = Square.fromAlgebraic("e2");
    private final Square e4 = Square.fromAlgebraic("e4");
    private final Square d5 = Square.fromAlgebraic("d5");
    private final Square e8 = Square.fromAlgebraic("e8");
    private final Piece whitePawn = new Piece(PieceType.PAWN, Color.WHITE);
    private final Piece blackPawn = new Piece(PieceType.PAWN, Color.BLACK);
    private final Piece whiteKing = new Piece(PieceType.KING, Color.WHITE);

    @Test
    void testRegularMove() {
        Move move = new Move(e2, e4, whitePawn, null);
        assertFalse(move.isCapture());
        assertFalse(move.isPromotion());
        assertFalse(move.isCastling());
        assertFalse(move.isEnPassantCapture());
        assertEquals(e2, move.from());
        assertEquals(e4, move.to());
        assertEquals(whitePawn, move.pieceMoved());
        assertNull(move.pieceCaptured());
    }

    @Test
    void testCaptureMove() {
        Move move = new Move(e4, d5, whitePawn, blackPawn);
        assertTrue(move.isCapture());
        assertFalse(move.isPromotion());
        assertFalse(move.isCastling());
        assertFalse(move.isEnPassantCapture());
        assertEquals(blackPawn, move.pieceCaptured());
    }

    @Test
    void testPromotionMove() {
        Move move = new Move(e2, e8, whitePawn, null, PieceType.QUEEN);
        assertFalse(move.isCapture());
        assertTrue(move.isPromotion());
        assertFalse(move.isCastling());
        assertFalse(move.isEnPassantCapture());
        assertEquals(PieceType.QUEEN, move.promotionPieceType());
    }

    @Test
    void testPromotionCaptureMove() {
        Move move = new Move(e2, e8, whitePawn, blackPawn, PieceType.ROOK);
        assertTrue(move.isCapture());
        assertTrue(move.isPromotion());
        assertFalse(move.isCastling());
        assertFalse(move.isEnPassantCapture());
        assertEquals(blackPawn, move.pieceCaptured());
        assertEquals(PieceType.ROOK, move.promotionPieceType());
    }

    @Test
    void testKingsideCastle() {
        Square g1 = Square.fromAlgebraic("g1");
        Square e1 = Square.fromAlgebraic("e1");
        Move move = new Move(e1, g1, whiteKing, null, null, true, false, false);
        assertFalse(move.isCapture());
        assertFalse(move.isPromotion());
        assertTrue(move.isCastling());
        assertTrue(move.isCastleKingside());
        assertFalse(move.isCastleQueenside());
        assertFalse(move.isEnPassantCapture());
        assertEquals("O-O", move.toString());
    }

    @Test
    void testQueensideCastle() {
        Square c1 = Square.fromAlgebraic("c1");
        Square e1 = Square.fromAlgebraic("e1");
        Move move = new Move(e1, c1, whiteKing, null, null, false, true, false);
        assertFalse(move.isCapture());
        assertFalse(move.isPromotion());
        assertTrue(move.isCastling());
        assertFalse(move.isCastleKingside());
        assertTrue(move.isCastleQueenside());
        assertFalse(move.isEnPassantCapture());
        assertEquals("O-O-O", move.toString());
    }

    @Test
    void testEnPassant() {
        Square e5 = Square.fromAlgebraic("e5");
        Square f6 = Square.fromAlgebraic("f6");
        // Note: pieceCaptured in EP is the pawn *next* to the target square
        Piece blackPawnOnF5 = new Piece(PieceType.PAWN, Color.BLACK);
        Move move = new Move(e5, f6, whitePawn, blackPawnOnF5, null, false, false, true);
        assertTrue(move.isCapture()); // EP is a capture
        assertFalse(move.isPromotion());
        assertFalse(move.isCastling());
        assertTrue(move.isEnPassantCapture());
        assertEquals(blackPawnOnF5, move.pieceCaptured());
    }

    @Test
    void testEqualsAndHashCode() {
        Move e2e4_1 = new Move(e2, e4, whitePawn, null);
        Move e2e4_2 = new Move(e2, e4, whitePawn, null);
        Move e2e3 = new Move(e2, Square.fromAlgebraic("e3"), whitePawn, null);
        Move e4d5 = new Move(e4, d5, whitePawn, blackPawn);
        Move e4d5_ep = new Move(e4, d5, whitePawn, blackPawn, null, false, false, true);
        Move e7e8Q = new Move(Square.fromAlgebraic("e7"), e8, whitePawn, null, PieceType.QUEEN);

        assertEquals(e2e4_1, e2e4_2);
        assertEquals(e2e4_1.hashCode(), e2e4_2.hashCode());

        assertNotEquals(e2e4_1, e2e3);
        assertNotEquals(e2e4_1, e4d5);
        assertNotEquals(e4d5, e4d5_ep); // Different EP flag
        assertNotEquals(e2e4_1, e7e8Q);
        assertNotEquals(null, e2e4_1);
        assertNotEquals(new Object(), e2e4_1);

        // Castling moves
        Square g1 = Square.fromAlgebraic("g1");
        Square e1 = Square.fromAlgebraic("e1");
        Move castleK1 = new Move(e1, g1, whiteKing, null, null, true, false, false);
        Move castleK2 = new Move(e1, g1, whiteKing, null, null, true, false, false);
        Square c1 = Square.fromAlgebraic("c1");
        Move castleQ = new Move(e1, c1, whiteKing, null, null, false, true, false);

        assertEquals(castleK1, castleK2);
        assertEquals(castleK1.hashCode(), castleK2.hashCode());
        assertNotEquals(castleK1, castleQ);
    }
}