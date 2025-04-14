package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

public class Bishop extends Piece {
    public Bishop(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'B'; }

    @Override
    public boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol) {
        // Must move diagonally
        return Math.abs(toRow - fromRow) == Math.abs(toCol - fromCol);
    }

    @Override
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        List<int[]> attacks = new ArrayList<>();
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}; // Diagonal directions
        for (int[] d : directions) {
            for (int i = 1; ; i++) {
                int r = fromRow + i * d[0];
                int c = fromCol + i * d[1];
                if (!board.isValidCoordinate(r, c)) break; // Off board
                attacks.add(new int[]{r, c});
                if (board.getPiece(r, c) != null) break; // Path blocked for further attacks in this direction
            }
        }
        return attacks;
    }

    @Override
    public Piece copy() {
        Bishop copy = new Bishop(this.color);
        super.copyState(copy);
        return copy;
    }
}