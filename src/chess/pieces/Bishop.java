package chess.pieces;

import chess.*;

public class Bishop extends Piece {
    public Bishop(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'B'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) return false;
        if (!board.isPathClear(fromRow, fromCol, toRow, toCol)) return false;
        Piece target = board.getPiece(toRow, toCol);
        return target == null || isOpponent(target);
    }
}