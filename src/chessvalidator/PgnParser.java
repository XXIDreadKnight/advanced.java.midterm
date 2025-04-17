package chessvalidator;

import chessvalidator.reports.FileValidationResult;
import chessvalidator.reports.PgnErrorInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PgnParser {

    // Regex for standard PGN header tags: [Key "Value"]
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\[\\s*(\\w+)\\s*\"(.*?)\"\\s*\\]$");
    // Regex for move number indicators (e.g., "1.", "1...", "12.")
    private static final Pattern MOVE_NUMBER_PATTERN = Pattern.compile("^\\d+\\.{1,3}");
    // Regex for game termination markers
    private static final Pattern RESULT_PATTERN = Pattern.compile("^(1-0|0-1|1/2-1/2|\\*)$");
    // Regex for comments (simplistic: assumes comments don't contain braces)
    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\{[^}]*\\}");
    // Regex for variations (simplistic: assumes variations don't contain nested parens)
    private static final Pattern VARIATION_PATTERN = Pattern.compile("\\([^)]*\\)");
    // Regex for Numeric Annotation Glyphs (NAGs), e.g., $1, $10
    private static final Pattern NAG_PATTERN = Pattern.compile("\\$\\d+");


    /**
     * Parses PGN text from a Reader into games, reporting syntax errors.
     *
     * @param reader The reader providing the PGN text.
     * @param fileResult The FileValidationResult object to store results and errors.
     * @return A list of ParsedGame objects, each containing headers, move strings, and any syntax errors.
     */
    public List<ParsedGame> parse(Reader reader, FileValidationResult fileResult) throws IOException {
        List<ParsedGame> parsedGames = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        int lineNum = 0;

        Map<String, String> currentHeaders = new HashMap<>();
        String currentFen = null;
        StringBuilder currentMoveText = new StringBuilder();
        List<PgnErrorInfo> currentSyntaxErrors = new ArrayList<>();
        boolean inMoveSection = false;
        int gameIndex = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNum++;
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                continue; // Skip empty lines
            }

            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmedLine);
            if (headerMatcher.matches()) {
                // If we were in a move section and encounter a new header, it implies the previous game ended (maybe without result?)
                // Or it's the start of the first game's headers.
                if (inMoveSection && currentMoveText.length() > 0) {
                    // Finalize previous game if moves were recorded
                    parsedGames.add(createParsedGame(gameIndex++, currentHeaders, currentFen, currentMoveText.toString(), currentSyntaxErrors));
                    // Reset for the new game
                    currentHeaders = new HashMap<>();
                    currentFen = null;
                    currentMoveText = new StringBuilder();
                    currentSyntaxErrors = new ArrayList<>();
                    inMoveSection = false;
                }
                if (inMoveSection) {
                    // Header found *after* some moves but before a result - potentially malformed PGN
                    currentSyntaxErrors.add(new PgnErrorInfo(gameIndex, 0, trimmedLine, "Header tag found after moves started, expected move or result.", true));
                }

                String key = headerMatcher.group(1);
                String value = headerMatcher.group(2);
                currentHeaders.put(key, value);

                if ("FEN".equalsIgnoreCase(key)) {
                    // Basic check if a FEN was already found for this game - might indicate malformed PGN
                    if (currentFen != null && !currentSyntaxErrors.stream().anyMatch(e -> e.message().contains("Duplicate FEN"))) {
                        currentSyntaxErrors.add(new PgnErrorInfo(gameIndex, 0, trimmedLine, "Duplicate FEN tag found for the same game.", true));
                    }
                    currentFen = value;
                }

            } else if (trimmedLine.startsWith("[")) {
                // Likely a malformed header
                if (!inMoveSection) { // Only report if we expect headers
                    currentSyntaxErrors.add(new PgnErrorInfo(gameIndex, 0, trimmedLine, "Potentially malformed header tag.", true));
                } else { // If in move section, treat as unexpected text
                    currentSyntaxErrors.add(new PgnErrorInfo(gameIndex, 0, trimmedLine, "Unexpected text starting with '[' in move section.", true));
                    currentMoveText.append(trimmedLine).append(" "); // Still append it, might be part of weird move text
                }
            }
            else {
                // Not a header, likely part of the move text section
                if (!inMoveSection && currentHeaders.isEmpty() && currentFen == null) {
                    // Text before any headers - PGN standard violation
                    currentSyntaxErrors.add(new PgnErrorInfo(gameIndex, 0, trimmedLine, "Move text found before any header tags or FEN.", true));
                }
                inMoveSection = true;
                currentMoveText.append(trimmedLine).append(" "); // Append line to move text buffer

                // Check if this line contains a game result, signaling the end of the current game
                // Split the appended text and check the last token
                String[] tokens = currentMoveText.toString().trim().split("\\s+");
                if (tokens.length > 0) {
                    String lastToken = tokens[tokens.length - 1];
                    if (RESULT_PATTERN.matcher(lastToken).matches()) {
                        // Game ended, finalize it
                        parsedGames.add(createParsedGame(gameIndex++, currentHeaders, currentFen, currentMoveText.toString(), currentSyntaxErrors));
                        // Reset for a potential next game
                        currentHeaders = new HashMap<>();
                        currentFen = null;
                        currentMoveText = new StringBuilder();
                        currentSyntaxErrors = new ArrayList<>();
                        inMoveSection = false;
                    }
                }
            }
        }

        // Add the last game if it had moves but didn't end with a result line parsed above
        if (currentMoveText.length() > 0 || !currentHeaders.isEmpty() || currentFen != null) {
            if (currentMoveText.length() == 0 && !currentSyntaxErrors.isEmpty()) {
                // We had headers and syntax errors, but no moves - report as a game anyway
            }
            parsedGames.add(createParsedGame(gameIndex, currentHeaders, currentFen, currentMoveText.toString(), currentSyntaxErrors));
        }

        if (parsedGames.isEmpty() && fileResult.getFileLevelErrors().isEmpty()) {
            fileResult.addFileError("No valid PGN games found in the file.");
        }

        return parsedGames;
    }

    private ParsedGame createParsedGame(int gameIndex, Map<String, String> headers, String fenString, String rawMoveText, List<PgnErrorInfo> syntaxErrors) {
        List<String> moves = extractMoveList(rawMoveText, gameIndex, syntaxErrors);
        return new ParsedGame(gameIndex, headers, fenString, moves, syntaxErrors);
    }

    // Extracts SAN moves, stripping comments, variations, NAGs, and move numbers.
    // Reports syntax errors found during extraction.
    private List<String> extractMoveList(String rawMoveText, int gameIndex, List<PgnErrorInfo> syntaxErrors) {
        List<String> moves = new ArrayList<>();
        if (rawMoveText == null || rawMoveText.isBlank()) {
            return moves;
        }

        // 1. Remove comments recursively (handle nested comments crudely)
        String text = rawMoveText;
        while (text.contains("{")) {
            text = COMMENT_PATTERN.matcher(text).replaceAll(" ");
        }
        // 2. Remove variations recursively (handle nested variations crudely)
        while (text.contains("(")) {
            text = VARIATION_PATTERN.matcher(text).replaceAll(" ");
        }
        // 3. Remove NAGs
        text = NAG_PATTERN.matcher(text).replaceAll(" ");

        // 4. Split into tokens
        String[] tokens = text.trim().split("\\s+");

        int moveCounter = 0; // For error reporting if needed

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            // Skip move number indicators
            if (MOVE_NUMBER_PATTERN.matcher(token).matches()) {
                continue;
            }

            // Check for game result - stop processing moves after result
            if (RESULT_PATTERN.matcher(token).matches()) {
                // We can optionally store the result if needed.
                break; // Stop adding moves once result is reached
            }

            // Rudimentary check for valid SAN start (Piece, castle, or pawn move)
            // This is NOT a full SAN validation, just a basic sanity check.
            if (token.matches("^[NBRQKOa-h].*")) {
                moves.add(token);
                moveCounter++;
            } else {
                // Found a token that doesn't look like a move, a number, or a result
                syntaxErrors.add(new PgnErrorInfo(gameIndex, (moveCounter / 2) + 1, token, "Unexpected token in move text section.", true));
            }
        }

        return moves;
    }

    // Inner class to hold the parsed data for a single game
    public static class ParsedGame {
        private final int gameIndex;
        private final Map<String, String> headers;
        private final String fenString;
        private final List<String> sanMoves;
        private final List<PgnErrorInfo> syntaxErrors;

        public ParsedGame(int gameIndex, Map<String, String> headers, String fenString, List<String> sanMoves, List<PgnErrorInfo> syntaxErrors) {
            this.gameIndex = gameIndex;
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            this.fenString = fenString;
            this.sanMoves = sanMoves != null ? new ArrayList<>(sanMoves) : new ArrayList<>();
            this.syntaxErrors = syntaxErrors != null ? new ArrayList<>(syntaxErrors) : new ArrayList<>();
        }

        public int getGameIndex() { return gameIndex; }
        public Map<String, String> getHeaders() { return headers; }
        public String getFenString() { return fenString; }
        public List<String> getSanMoves() { return sanMoves; }
        public List<PgnErrorInfo> getSyntaxErrors() { return syntaxErrors; }
    }
}