package chessvalidator.reports;

import java.util.ArrayList;
import java.util.List;

public class FileValidationResult {
    private final String filePath;
    private final List<GameValidationResult> gameResults = new ArrayList<>();
    private final List<String> fileLevelErrors = new ArrayList<>(); // Errors not tied to a specific game (e.g., read errors)
    private boolean hasErrors = false;

    public FileValidationResult(String filePath) {
        this.filePath = filePath;
    }

    public void addGameResult(GameValidationResult result) {
        gameResults.add(result);
        if (!result.isValid()) {
            hasErrors = true;
        }
    }

    public void addFileError(String errorMessage) {
        fileLevelErrors.add(errorMessage);
        hasErrors = true;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<GameValidationResult> getGameResults() {
        return gameResults;
    }

    public List<String> getFileLevelErrors() { return fileLevelErrors; }

    public boolean hasErrors() { return hasErrors; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("Validation Report for: ").append(filePath).append("\n");
        sb.append("========================================\n");

        if (!fileLevelErrors.isEmpty()) {
            sb.append("File-Level Errors:\n");
            fileLevelErrors.forEach(e -> sb.append("  - ").append(e).append("\n"));
            sb.append("----------------------------------------\n");
        }

        if (gameResults.isEmpty() && fileLevelErrors.isEmpty()) {
            sb.append("No games found or processed in this file.\n");
        } else {
            gameResults.forEach(gr -> sb.append(gr)); // Uses GameValidationResult.toString()
        }
        sb.append("========================================\n");
        return sb.toString();
    }
}