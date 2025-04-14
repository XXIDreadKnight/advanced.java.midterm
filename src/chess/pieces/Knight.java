package chess.pieces;

import chess.*;
import java.util.List;
import java.util.ArrayList;

public class Knight extends Piece {
    public Knight(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'N'; }

    @Override
    public boolean isValidMovePattern(int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow);
        int dc = Math.abs(toCol - fromCol);
        // L-shape: 2 squares in one cardinal direction, 1 in the other
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }

    @Override
    public List<int[]> getAttackedSquares(ChessBoard board, int fromRow, int fromCol) {
        List<int[]> attacks = new ArrayList<>();
        int[] dr = {-2, -2, -1, -1, 1, 1, 2, 2};
        int[] dc = {-1, 1, -2, 2, -2, 2, -1, 1};

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
        Knight copy = new Knight(this.color);
        super.copyState(copy);
        return copy;
    }
}