package chess.pieces;

import chess.*;

public abstract class Piece {
    public final Color color;

    public Piece(Color color) {
        this.color = color;
    }

    public abstract char getSymbol();

    public abstract boolean isValidMove(ChessBoard board, int fromRow, int fromCol, int toRow, int toCol);

    public String toString() {
        return color + " " + getClass().getSimpleName();
    }

    public boolean isOpponent(Piece other) {
        return other != null && this.color != other.color;
    }
}