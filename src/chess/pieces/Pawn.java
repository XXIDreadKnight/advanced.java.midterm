package chess.pieces;

import chess.*;

public class Pawn extends Piece {
    public Pawn(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'P'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        int dir = color == Color.WHITE ? -1 : 1;
        int startRow = color == Color.WHITE ? 6 : 1;

        Piece target = board.getPiece(toRow, toCol);

        if (fromCol == toCol && target == null) {
            if (toRow - fromRow == dir) return true;
            if (fromRow == startRow && toRow - fromRow == 2 * dir && board.getPiece(fromRow + dir, fromCol) == null) return true;
        } else if (Math.abs(fromCol - toCol) == 1 && toRow - fromRow == dir) {
            if (target != null && isOpponent(target)) return true;
            if (board.isEnPassantTarget(toRow, toCol)) return true;
        }
        return false;
    }
}