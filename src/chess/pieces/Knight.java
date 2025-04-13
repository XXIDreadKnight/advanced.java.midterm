package chess.pieces;

import chess.*;

public class Knight extends Piece {
    public Knight(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'N'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow), dc = Math.abs(toCol - fromCol);
        Piece target = board.getPiece(toRow, toCol);
        return dr * dc == 2 && (target == null || isOpponent(target));
    }
}