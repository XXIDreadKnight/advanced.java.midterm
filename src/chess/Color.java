package chess;

public enum Color {
    WHITE, BLACK;

    /**
     * Returns the opposing color.
     * @return BLACK if this is WHITE, WHITE if this is BLACK.
     */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}