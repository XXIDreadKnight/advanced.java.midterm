package chessvalidator.model;

import java.util.Objects;

// Represents a square using 0-based row and column indices
// Row 0 is rank 1, Row 7 is rank 8
// Col 0 is file 'a', Col 7 is file 'h'
public record Square(int row, int col) {

    public static final int MIN_ROW = 0;
    public static final int MAX_ROW = 7;
    public static final int MIN_COL = 0;
    public static final int MAX_COL = 7;

    public boolean isValid() {
        return row >= MIN_ROW && row <= MAX_ROW && col >= MIN_COL && col <= MAX_COL;
    }

    public static Square fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            return null; // Or throw exception
        }
        char fileChar = algebraic.charAt(0);
        char rankChar = algebraic.charAt(1);

        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            return null; // Or throw exception
        }

        int col = fileChar - 'a';
        int row = rankChar - '1';
        return new Square(row, col);
    }

    public String toAlgebraic() {
        if (!isValid()) {
            return "Invalid";
        }
        char fileChar = (char) ('a' + col);
        char rankChar = (char) ('1' + row);
        return "" + fileChar + rankChar;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square square = (Square) o;
        return row == square.row && col == square.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}