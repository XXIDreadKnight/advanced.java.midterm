package chess.pieces;

import chess.*;

public class Queen extends Piece {
    public Queen(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'Q'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow == toRow || fromCol == toCol || Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol)) {
            if (!board.isPathClear(fromRow, fromCol, toRow, toCol)) return false;
            Piece target = board.getPiece(toRow, toCol);
            return target == null || isOpponent(target);
        }
        return false;
    }
}