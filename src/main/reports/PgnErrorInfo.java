package main.reports;

public record PgnErrorInfo(
        int gameIndex,    // 0-based index of the game within the file
        int moveNumber,   // Move number where error occurred (or 0 for header/syntax)
        String moveText,  // The problematic move text (or header line)
        String message,   // Description of the error
        boolean isSyntaxError // True if syntax, false if logical
) {
    @Override
    public String toString() {
        String type = isSyntaxError ? "Syntax" : "Logical";
        String location = (moveNumber > 0) ? "Move " + moveNumber + " ('" + moveText + "')" : (moveText != null ? "'" + moveText + "'" : "General");
        return String.format("[%s Error | Game %d | %s]: %s", type, gameIndex + 1, location, message);
    }
}