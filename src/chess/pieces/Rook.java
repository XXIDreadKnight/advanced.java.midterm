package chess.pieces;

import chess.*;

public class Rook extends Piece {
    public Rook(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'R'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) return false;
        if (!board.isPathClear(fromRow, fromCol, toRow, toCol)) return false;
        Piece target = board.getPiece(toRow, toCol);
        return target == null || isOpponent(target);
    }
}