package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

public class King extends Piece {
    public King(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'K'; }

    @Override
    public boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow);
        int dc = Math.abs(toCol - fromCol);

        // Standard move (1 square any direction)
        if (dr <= 1 && dc <= 1 && (dr != 0 || dc != 0)) {
            return true;
        }
        // Castling move pattern (handled specially in ChessBoard due to complex rules)
        if (dr == 0 && dc == 2) {
            return true; // Pattern is valid, legality checked elsewhere
        }
        return false;
    }

    @Override
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        List<int[]> attacks = new ArrayList<>();
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int r = fromRow + dr[i];
            int c = fromCol + dc[i];
            if (board.isValidCoordinate(r, c)) {
                attacks.add(new int[]{r, c});
            }
        }
        return attacks;
    }

    @Override
    public Piece copy() {
        King copy = new King(this.color);
        super.copyState(copy);
        return copy;
    }
}