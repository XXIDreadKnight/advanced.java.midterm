package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Abstract base class for all chess pieces.
 */
public abstract class Piece {
    public final Color color;
    protected boolean hasMoved = false; // Added to track movement for castling/pawn first move

    public Piece(Color color) {
        this.color = color;
    }

    /**
     * Gets the standard algebraic notation character for the piece type.
     * ('P' for Pawn, 'N' for Knight, 'B' for Bishop, 'R' for Rook, 'Q' for Queen, 'K' for King).
     * Note: Pawn symbol 'P' is often omitted in SAN.
     * @return The character symbol.
     */
    public abstract char getSymbol();

    /**
     * Checks if a move is potentially valid according to the piece's movement rules,
     * without considering checks or board state legality (like path blocking for sliders).
     * This is a basic geometry check.
     * @param fromRow Starting row (0-7).
     * @param fromCol Starting column (0-7).
     * @param toRow   Target row (0-7).
     * @param toCol   Target column (0-7).
     * @return True if the move geometry is valid for this piece type, false otherwise.
     */
    public abstract boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol);

    /**
     * Generates a list of squares this piece attacks from a given position.
     * Does not consider blocking pieces except for pawns.
     * @param board The current board state.
     * @param fromRow Row of the piece.
     * @param fromCol Column of the piece.
     * @return A list of board coordinates [row, col] attacked by this piece.
     */
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        // Default implementation for pieces that don't attack (should be overridden)
        // Or provide specific implementations in subclasses
        return new ArrayList<>();
        // Example for Rook (simplified):
        // List<int[]> attacks = new ArrayList<>();
        // int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        // for (int[] d : directions) {
        //     for (int i = 1; ; i++) {
        //         int r = fromRow + i * d[0];
        //         int c = fromCol + i * d[1];
        //         if (!board.isValidCoordinate(r, c)) break;
        //         attacks.add(new int[]{r, c});
        //         if (board.getPiece(r, c) != null) break; // Stop after hitting any piece
        //     }
        // }
        // return attacks;
    }


    @Override
    public String toString() {
        // Use uppercase for white, lowercase for black (common debug representation)
        char sym = getSymbol();
        return color == Color.WHITE ? Character.toUpperCase(sym) : Character.toLowerCase(sym);
    }

    /**
     * Checks if the 'other' piece belongs to the opponent.
     * @param other The piece to compare against.
     * @return True if 'other' is not null and has the opposite color, false otherwise.
     */
    public boolean isOpponent(Piece other) {
        return other != null && this.color != other.color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    /**
     * Creates a deep copy of the piece.
     * @return A new Piece instance with the same color and moved status.
     */
    public abstract Piece copy();

    protected void copyState(Piece target) {
        target.hasMoved = this.hasMoved;
    }

}