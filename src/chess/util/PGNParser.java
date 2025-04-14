package chess.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses PGN (Portable Game Notation) files.
 * Focuses on extracting headers and move text for validation.
 * Includes basic handling for comments and NAGs.
 */
public class PGNParser {

    // Regular expressions for parsing elements
    private static final Pattern TAG_PAIR_PATTERN = Pattern.compile("\\[\\s*(\\w+)\\s*\"(.*?)\"\\s*\\]");
    private static final Pattern MOVE_NUMBER_PATTERN = Pattern.compile("\\d+\\.{1,3}"); // Matches 1. or 1... etc.
    private static final Pattern RESULT_PATTERN = Pattern.compile("(1-0|0-1|1/2-1/2|\\*)$"); // Game termination markers

    public static class Game {
        public Map<String, String> headers = new LinkedHashMap<>(); // Preserve order
        public List<String> moveTexts = new ArrayList<>(); // Stores SAN moves
        public String result = "*"; // Default result
        public List<String> syntaxErrors = new ArrayList<>(); // Store syntax errors found during parsing
        public int gameNumberInFile = 0; // Track which game it is
        public String fileName = ""; // Source file name
    }

    /**
     * Parses a single PGN file, potentially containing multiple games.
     * @param file The PGN file to parse.
     * @return A list of Game objects parsed from the file.
     * @throws IOException If an error occurs reading the file.
     */
    public List<Game> parseFile(File file) throws IOException {
        List<Game> games = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder currentGameText = new StringBuilder();
            int gameCounter = 0;
            boolean inMovetext = false;

            while ((line = reader.readLine()) != null) {
                // Check for start of a new game based on a tag after movetext/empty lines
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("[") && inMovetext && currentGameText.length() > 0) {
                    // Start of a new game detected after the previous game's moves
                    gameCounter++;
                    Game parsedGame = parseSingleGameText(currentGameText.toString(), gameCounter, file.getName());
                    games.add(parsedGame);
                    currentGameText = new StringBuilder(); // Reset for the new game
                    inMovetext = false;
                } else if (trimmedLine.isEmpty() && inMovetext) {
                    // Empty line often separates games, but could be within movetext.
                    // Let's append it but be wary. Relying on '[' is safer.
                }

                if (!trimmedLine.isEmpty()) {
                    currentGameText.append(line).append("\n");
                    if (!trimmedLine.startsWith("[")) {
                        inMovetext = true; // Once we see non-tag, non-empty line, assume movetext started
                    }
                } else if (currentGameText.length() > 0) {
                    // Append empty lines if they appear within a game block
                    currentGameText.append("\n");
                }
            }

            // Process the last game in the file
            if (currentGameText.length() > 0) {
                gameCounter++;
                Game parsedGame = parseSingleGameText(currentGameText.toString(), gameCounter, file.getName());
                games.add(parsedGame);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
            throw e;
        }
        return games;
    }

    /**
     * Parses the text content of a single game.
     * @param gameText The raw text of one game (headers and movetext).
     * @param gameNumber Sequence number in the file.
     * @param fileName Source file name.
     * @return A parsed Game object.
     */
    private Game parseSingleGameText(String gameText, int gameNumber, String fileName) {
        Game game = new Game();
        game.gameNumberInFile = gameNumber;
        game.fileName = fileName;
        StringBuilder movetextBuilder = new StringBuilder();
        boolean inHeader = true;
        int lineNumber = 0;

        String[] lines = gameText.split("\\r?\\n"); // Split into lines

        for (String line : lines) {
            lineNumber++;
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                if (inHeader) inHeader = false; // First empty line often marks end of headers
                continue; // Skip empty lines for processing
            }

            if (inHeader && trimmedLine.startsWith("[")) {
                Matcher matcher = TAG_PAIR_PATTERN.matcher(trimmedLine);
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    if (game.headers.containsKey(key)) {
                        game.syntaxErrors.add(String.format("Line %d: Duplicate header tag '%s'", lineNumber, key));
                    }
                    game.headers.put(key, value);
                } else {
                    game.syntaxErrors.add(String.format("Line %d: Malformed header tag: %s", lineNumber, trimmedLine));
                    inHeader = false; // Assume header section ended due to malformed line
                    movetextBuilder.append(line).append(" "); // Treat rest as movetext
                }
            } else {
                if (inHeader) {
                    // We encountered non-empty, non-tag line while expecting headers
                    inHeader = false;
                }
                // Append to movetext section
                movetextBuilder.append(line).append(" ");
            }
        }

        // Now process the collected movetext
        processMovetext(movetextBuilder.toString(), game);

        // Check for mandatory tags (optional based on strictness)
        // String[] requiredTags = {"Event", "Site", "Date", "Round", "White", "Black", "Result"};
        // for (String tag : requiredTags) {
        //     if (!game.headers.containsKey(tag)) {
        //         game.syntaxErrors.add("Missing required header tag: " + tag);
        //     }
        // }

        return game;
    }

    /**
     * Processes the raw movetext string, removing comments, NAGs, variations,
     * and extracting SAN move tokens and the game result.
     * @param rawMovetext The movetext section including comments, etc.
     * @param game The Game object to populate.
     */
    private void processMovetext(String rawMovetext, Game game) {
        // 1. Remove comments
        String processed = removeComments(rawMovetext, game);

        // 2. Remove Variations (simplistic removal - nested variations can be tricky)
        processed = removeVariations(processed, game);

        // 3. Remove NAGs (Numeric Annotation Glyphs like $1, $10)
        processed = processed.replaceAll("\\$\\d+", "");

        // 4. Tokenize remaining text (split by space, handle move numbers, result)
        String[] potentialTokens = processed.trim().split("\\s+");

        for (String token : potentialTokens) {
            if (token.isEmpty()) continue;

            // Skip move numbers (e.g., "1.", "2...", "10.")
            if (MOVE_NUMBER_PATTERN.matcher(token).matches()) {
                continue;
            }

            // Check for game result marker
            Matcher resultMatcher = RESULT_PATTERN.matcher(token);
            if (resultMatcher.matches()) {
                // Found a result, store it and stop processing further tokens as moves
                game.result = token;
                break; // Assume result is the last element
            }

            // Assume anything left is a SAN move token (validation happens later)
            // Further validation could check SAN characters here [a-h1-8NBRQKx+=O-]{2,7} etc.
            if (isValidSanFormat(token)) {
                game.moveTexts.add(token);
            } else if (!token.isEmpty()){
                // Report unexpected token if it's not empty, move number, or result
                game.syntaxErrors.add("Unexpected token or invalid SAN format in movetext: '" + token + "'");
            }
        }

        // Validate final result consistency if possible
        String headerResult = game.headers.get("Result");
        if (headerResult != null && !headerResult.equals(game.result)) {
            game.syntaxErrors.add("Result in header (" + headerResult + ") does not match result in movetext (" + game.result + ")");
        } else if (headerResult == null && !game.result.equals("*")) {
            // If movetext has a result but header doesn't, maybe update header? Or flag inconsistency.
            // game.headers.put("Result", game.result); // Option: Update header
            game.syntaxErrors.add("Result found in movetext (" + game.result + ") but missing or '*' in header.");
        }
    }

    // Basic check for plausible SAN characters. Doesn't guarantee validity.
    private boolean isValidSanFormat(String token) {
        // Allows letters, numbers, O, -, +, #, =, x
        // This is a very loose check. Real SAN validation is context-dependent.
        return token.matches("[a-h1-8NBRQKxO\\-\\+=#]+");
    }


    // Removes PGN comments: { ... } and ; ... to end of line
    private String removeComments(String text, Game game) {
        // Remove ; comments first (line based)
        text = text.replaceAll(";.*$", ""); // Remove ; to end of line

        // Remove { ... } comments (can span lines, handle nesting basic)
        StringBuilder sb = new StringBuilder();
        int braceDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                braceDepth++;
            } else if (c == '}') {
                if (braceDepth > 0) {
                    braceDepth--;
                } else {
                    game.syntaxErrors.add("Extraneous closing brace '}' found outside comment.");
                }
            } else if (braceDepth == 0) {
                sb.append(c);
            }
        }
        if (braceDepth > 0) {
            game.syntaxErrors.add("Unterminated comment brace '{' found.");
        }
        return sb.toString();
    }

    // Removes PGN variations: ( ... ) - Handles basic nesting
    private String removeVariations(String text, Game game) {
        StringBuilder sb = new StringBuilder();
        int parenDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                if (parenDepth > 0) {
                    parenDepth--;
                } else {
                    game.syntaxErrors.add("Extraneous closing parenthesis ')' found outside variation.");
                }
            } else if (parenDepth == 0) {
                sb.append(c);
            }
        }
        if (parenDepth > 0) {
            game.syntaxErrors.add("Unterminated variation parenthesis '(' found.");
        }
        return sb.toString();
    }
}