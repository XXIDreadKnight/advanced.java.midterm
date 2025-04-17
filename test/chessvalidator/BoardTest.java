package chessvalidator;

import chessvalidator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @Test
    void testInitialSetup() {
        board.setupInitialPosition();

        // Check corners
        assertEquals(new Piece(PieceType.ROOK, Color.WHITE), board.getPiece(Square.fromAlgebraic("a1")));
        assertEquals(new Piece(PieceType.ROOK, Color.WHITE), board.getPiece(Square.fromAlgebraic("h1")));
        assertEquals(new Piece(PieceType.ROOK, Color.BLACK), board.getPiece(Square.fromAlgebraic("a8")));
        assertEquals(new Piece(PieceType.ROOK, Color.BLACK), board.getPiece(Square.fromAlgebraic("h8")));

        // Check kings
        assertEquals(new Piece(PieceType.KING, Color.WHITE), board.getPiece(Square.fromAlgebraic("e1")));
        assertEquals(new Piece(PieceType.KING, Color.BLACK), board.getPiece(Square.fromAlgebraic("e8")));

        // Check some pawns
        assertEquals(new Piece(PieceType.PAWN, Color.WHITE), board.getPiece(Square.fromAlgebraic("e2")));
        assertEquals(new Piece(PieceType.PAWN, Color.BLACK), board.getPiece(Square.fromAlgebraic("d7")));

        // Check empty square
        assertNull(board.getPiece(Square.fromAlgebraic("e4")));
    }

    @Test
    void testGetSetPiece() {
        Square e4 = Square.fromAlgebraic("e4");
        Piece whitePawn = new Piece(PieceType.PAWN, Color.WHITE);

        assertNull(board.getPiece(e4)); // Initially empty
        board.setPiece(e4, whitePawn);
        assertEquals(whitePawn, board.getPiece(e4));
        board.setPiece(e4, null); // Clear the piece
        assertNull(board.getPiece(e4));
    }

    @Test
    void testMovePiece() {
        board.setupInitialPosition();
        Square e2 = Square.fromAlgebraic("e2");
        Square e4 = Square.fromAlgebraic("e4");
        Piece whitePawn = board.getPiece(e2);

        assertNotNull(whitePawn);
        assertNull(board.getPiece(e4));

        board.movePiece(e2, e4);

        assertNull(board.getPiece(e2));
        assertEquals(whitePawn, board.getPiece(e4));
    }

    @Test
    void testFindKing() {
        board.setupInitialPosition();
        assertEquals(Square.fromAlgebraic("e1"), board.findKing(Color.WHITE));
        assertEquals(Square.fromAlgebraic("e8"), board.findKing(Color.BLACK));

        // Move white king
        board.movePiece(Square.fromAlgebraic("e1"), Square.fromAlgebraic("f1"));
        assertEquals(Square.fromAlgebraic("f1"), board.findKing(Color.WHITE));

        // Test on empty board (should be null)
        Board emptyBoard = new Board();
        assertNull(emptyBoard.findKing(Color.WHITE));
    }

    @Test
    void testCopyConstructor() {
        board.setupInitialPosition();
        Board copiedBoard = new Board(board);

        // Verify initial equality (visually via toString or by checking key pieces)
        assertEquals(board.toString(), copiedBoard.toString());

        // Modify original board
        board.movePiece(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4"));

        // Verify copy is unchanged
        assertNull(copiedBoard.getPiece(Square.fromAlgebraic("e4")));
        assertNotNull(copiedBoard.getPiece(Square.fromAlgebraic("e2")));
        assertNotEquals(board.toString(), copiedBoard.toString()); // Boards should differ now

        // Modify copy
        copiedBoard.movePiece(Square.fromAlgebraic("d7"), Square.fromAlgebraic("d5"));

        // Verify original is unchanged by the copy's modification
        assertNull(board.getPiece(Square.fromAlgebraic("d5")));
        assertNotNull(board.getPiece(Square.fromAlgebraic("d7")));
    }
}