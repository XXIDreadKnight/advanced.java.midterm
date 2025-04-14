package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

public class Rook extends Piece {
    public Rook(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'R'; }

    @Override
    public boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol) {
        // Must move horizontally or vertically
        return fromRow == toRow || fromCol == toCol;
    }

    @Override
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        List<int[]> attacks = new ArrayList<>();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // Horizontal/Vertical
        for (int[] d : directions) {
            for (int i = 1; ; i++) {
                int r = fromRow + i * d[0];
                int c = fromCol + i * d[1];
                if (!board.isValidCoordinate(r, c)) break;
                attacks.add(new int[]{r, c});
                if (board.getPiece(r, c) != null) break;
            }
        }
        return attacks;
    }

    @Override
    public Piece copy() {
        Rook copy = new Rook(this.color);
        super.copyState(copy);
        return copy;
    }
}