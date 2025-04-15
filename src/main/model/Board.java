package main.model;

import java.util.Arrays;

public class Board {
    private final Piece[][] squares = new Piece[8][8]; // [row][col]

    public Board() {
        // Initialize with nulls (empty squares)
        for (int r = 0; r < 8; r++) {
            Arrays.fill(squares[r], null);
        }
    }

    // Copy constructor
    public Board(Board other) {
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.squares[r], 0, this.squares[r], 0, 8);
        }
    }

    public void setupInitialPosition() {
        // Clear board first
        for (int r = 0; r < 8; r++) Arrays.fill(squares[r], null);

        // Pawns
        for (int c = 0; c < 8; c++) {
            setPiece(new Square(1, c), new Piece(PieceType.PAWN, Color.WHITE));
            setPiece(new Square(6, c), new Piece(PieceType.PAWN, Color.BLACK));
        }
        // Rooks
        setPiece(new Square(0, 0), new Piece(PieceType.ROOK, Color.WHITE));
        setPiece(new Square(0, 7), new Piece(PieceType.ROOK, Color.WHITE));
        setPiece(new Square(7, 0), new Piece(PieceType.ROOK, Color.BLACK));
        setPiece(new Square(7, 7), new Piece(PieceType.ROOK, Color.BLACK));
        // Knights
        setPiece(new Square(0, 1), new Piece(PieceType.KNIGHT, Color.WHITE));
        setPiece(new Square(0, 6), new Piece(PieceType.KNIGHT, Color.WHITE));
        setPiece(new Square(7, 1), new Piece(PieceType.KNIGHT, Color.BLACK));
        setPiece(new Square(7, 6), new Piece(PieceType.KNIGHT, Color.BLACK));
        // Bishops
        setPiece(new Square(0, 2), new Piece(PieceType.BISHOP, Color.WHITE));
        setPiece(new Square(0, 5), new Piece(PieceType.BISHOP, Color.WHITE));
        setPiece(new Square(7, 2), new Piece(PieceType.BISHOP, Color.BLACK));
        setPiece(new Square(7, 5), new Piece(PieceType.BISHOP, Color.BLACK));
        // Queens
        setPiece(new Square(0, 3), new Piece(PieceType.QUEEN, Color.WHITE));
        setPiece(new Square(7, 3), new Piece(PieceType.QUEEN, Color.BLACK));
        // Kings
        setPiece(new Square(0, 4), new Piece(PieceType.KING, Color.WHITE));
        setPiece(new Square(7, 4), new Piece(PieceType.KING, Color.BLACK));
    }

    public Piece getPiece(Square square) {
        if (!square.isValid()) return null;
        return squares[square.row()][square.col()];
    }

    public void setPiece(Square square, Piece piece) {
        if (!square.isValid()) return; // Or throw
        squares[square.row()][square.col()] = piece;
    }

    public void movePiece(Square from, Square to) {
        if (!from.isValid() || !to.isValid()) return; // Or throw
        Piece movingPiece = getPiece(from);
        setPiece(to, movingPiece);
        setPiece(from, null);
    }

    public Square findKing(Color color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square current = new Square(r, c);
                Piece p = getPiece(current);
                if (p != null && p.type() == PieceType.KING && p.color() == color) {
                    return current;
                }
            }
        }
        return null; // Should not happen in a valid game
    }

    // Basic print for debugging
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            sb.append(r + 1).append(" ");
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r][c];
                sb.append("[").append(p == null ? " " : p.toString()).append("]");
            }
            sb.append("\n");
        }
        sb.append("   a  b  c  d  e  f  g  h \n");
        return sb.toString();
    }
}