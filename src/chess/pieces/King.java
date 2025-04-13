package chess.pieces;

import chess.*;

public class King extends Piece {
    public King(Color color) { super(color); }

    @Override
    public char getSymbol() { return 'K'; }

    @Override
    public boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow), dc = Math.abs(toCol - fromCol);
        if (dr <= 1 && dc <= 1) {
            Piece target = board.getPiece(toRow, toCol);
            return target == null || isOpponent(target);
        }
        // Castling
        if (fromRow == toRow && dr == 0 && (dc == 2)) {
            return board.isCastlingLegal(color, fromCol < toCol);
        }
        return false;
    }
}