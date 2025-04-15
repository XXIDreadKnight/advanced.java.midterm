package main.model;

public enum PieceType {
    PAWN('P'), ROOK('R'), KNIGHT('N'), BISHOP('B'), QUEEN('Q'), KING('K');

    private final char sanChar;

    PieceType(char sanChar) {
        this.sanChar = sanChar;
    }

    public char getSanChar() {
        return sanChar;
    }

    public static PieceType fromSanChar(char c) {
        for (PieceType type : values()) {
            if (type.sanChar == Character.toUpperCase(c)) {
                return type;
            }
        }
        // Pawn has no char in basic SAN, handle implicitly or check for lowercase file char
        return null;
    }
}