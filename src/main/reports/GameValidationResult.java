package main.reports;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameValidationResult {
    private final int gameIndex;
    private final Map<String, String> headers;
    private final List<PgnErrorInfo> errors = new ArrayList<>();
    private boolean isValid = true; // Assume valid until an error is found

    public GameValidationResult(int gameIndex, Map<String, String> headers) {
        this.gameIndex = gameIndex;
        this.headers = headers;
    }

    public void addError(PgnErrorInfo error) {
        this.errors.add(error);
        this.isValid = false; // Any error makes the game invalid
    }

    public boolean isValid() {
        // A game is only truly valid if it has NO errors (syntax or logical)
        return isValid && errors.isEmpty();
    }

    public List<PgnErrorInfo> getErrors() {
        return errors;
    }

    public Map<String, String> getHeaders() { return headers; }
    public int getGameIndex() { return gameIndex; }

    @Override
    public String toString() {
        String gameId = headers.getOrDefault("Site", "?") + " " + headers.getOrDefault("Date", "?") +
                " " + headers.getOrDefault("White", "?") + "-" + headers.getOrDefault("Black", "?");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- Game %d (%s): %s ---%n",
                gameIndex + 1, gameId, isValid() ? "VALID" : "INVALID"));
        if (!isValid()) {
            errors.forEach(e -> sb.append("  ").append(e).append("\n"));
        }
        return sb.toString();
    }
}