package chess;

import chess.pieces.Piece;

public class Move {
    public final int fromRow, fromCol;
    public final int toRow, toCol;
    public final Piece movingPiece;
    public final Piece capturedPiece;
    public final boolean isCastling;
    public final boolean isEnPassant;
    public final boolean isPromotion;
    public final char promotionType; // Q, R, B, N

    public Move(int fromRow, int fromCol, int toRow, int toCol,
                Piece movingPiece, Piece capturedPiece,
                boolean isCastling, boolean isEnPassant, boolean isPromotion, char promotionType) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movingPiece = movingPiece;
        this.capturedPiece = capturedPiece;
        this.isCastling = isCastling;
        this.isEnPassant = isEnPassant;
        this.isPromotion = isPromotion;
        this.promotionType = promotionType;
    }

    public String toString() {
        return movingPiece + " from (" + fromRow + "," + fromCol + ") to (" + toRow + "," + toCol + ")" +
                (isPromotion ? " promoting to " + promotionType : "") +
                (isCastling ? " (castling)" : "") +
                (isEnPassant ? " (en passant)" : "");
    }
}