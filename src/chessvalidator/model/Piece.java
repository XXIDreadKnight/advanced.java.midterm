package chessvalidator.model;

import java.util.Objects;

public record Piece(PieceType type, Color color) {
    @Override
    public String toString() {
        char c = switch (type) {
            case PAWN -> 'p';
            case ROOK -> 'r';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case QUEEN -> 'q';
            case KING -> 'k';
        };
        return color == Color.WHITE ? String.valueOf(Character.toUpperCase(c)) : String.valueOf(c);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return type == piece.type && color == piece.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, color);
    }
}