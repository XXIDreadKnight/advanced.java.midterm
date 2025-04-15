package main.model;

import java.util.Objects;

public record Move(
        Square from,
        Square to,
        Piece pieceMoved, // Piece being moved
        Piece pieceCaptured, // Piece captured (can be null)
        PieceType promotionPieceType, // Type to promote to (null if not promotion)
        boolean isCastleKingside,
        boolean isCastleQueenside,
        boolean isEnPassantCapture // Flag if this move IS an en passant capture
) {
    // Simplified constructor for regular moves
    public Move(Square from, Square to, Piece pieceMoved, Piece pieceCaptured) {
        this(from, to, pieceMoved, pieceCaptured, null, false, false, false);
    }

    // Constructor for promotion
    public Move(Square from, Square to, Piece pieceMoved, Piece pieceCaptured, PieceType promotionPieceType) {
        this(from, to, pieceMoved, pieceCaptured, promotionPieceType, false, false, false);
    }

    public boolean isCapture() {
        return pieceCaptured != null;
    }

    public boolean isPromotion() { return promotionPieceType != null; }

    public boolean isCastling() { return isCastleKingside || isCastleQueenside; }

    @Override
    public String toString() { // Basic representation, not full SAN
        StringBuilder sb = new StringBuilder();
        if (isCastleKingside) return "O-O";
        if (isCastleQueenside) return "O-O-O";

        sb.append(pieceMoved.type() != PieceType.PAWN ? pieceMoved.type().getSanChar() : "");
        sb.append(from.toAlgebraic());
        sb.append(isCapture() ? "x" : "-");
        sb.append(to.toAlgebraic());
        if (isPromotion()) sb.append("=").append(promotionPieceType.getSanChar());
        if (isEnPassantCapture()) sb.append(" e.p.");

        return sb.toString();
    }

    // Need proper equals/hashCode if used in Sets/Maps
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return isCastleKingside == move.isCastleKingside &&
                isCastleQueenside == move.isCastleQueenside &&
                isEnPassantCapture == move.isEnPassantCapture &&
                Objects.equals(from, move.from) &&
                Objects.equals(to, move.to) &&
                Objects.equals(pieceMoved, move.pieceMoved) &&
                Objects.equals(pieceCaptured, move.pieceCaptured) &&
                promotionPieceType == move.promotionPieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, pieceMoved, pieceCaptured, promotionPieceType, isCastleKingside, isCastleQueenside, isEnPassantCapture);
    }
}