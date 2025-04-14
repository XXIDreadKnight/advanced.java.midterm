// --- START OF FILE Pawn.java ---
package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

public class Pawn extends Piece {
    public Pawn(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'P'; }

    @Override
    public boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol) {
        int dir = (color == Color.WHITE) ? -1 : 1;
        int dr = toRow - fromRow;
        int dc = Math.abs(toCol - fromCol);

        // Forward move (1 square)
        if (dc == 0 && dr == dir) return true;
        // Initial double move
        int startRow = (color == Color.WHITE) ? 6 : 1;
        if (dc == 0 && dr == 2 * dir && fromRow == startRow) return true;
        // Capture
        if (dc == 1 && dr == dir) return true;

        return false;
    }

    // Note: isValidMove logic is complex and better handled within ChessBoard
    // considering board state (blocking pieces, en passant targets, captures).
    // The pattern check is a basic filter.

    @Override
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        List<int[]> attacks = new ArrayList<>();
        int dir = (color == Color.WHITE) ? -1 : 1;
        int targetRow = fromRow + dir;

        // Diagonal captures
        int leftCol = fromCol - 1;
        int rightCol = fromCol + 1;

        if (board.isValidCoordinate(targetRow, leftCol)) {
            attacks.add(new int[]{targetRow, leftCol});
        }
        if (board.isValidCoordinate(targetRow, rightCol)) {
            attacks.add(new int[]{targetRow, rightCol});
        }
        return attacks;
    }

    @Override
    public Piece copy() {
        Pawn copy = new Pawn(this.color);
        super.copyState(copy);
        return copy;
    }
}