package chess;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import chess.util.SANParser;
import chess.pieces.*;

public class PGNReader {
    public static List<String> readGamesFromPath(String pathStr) throws IOException {
        List<String> games = new ArrayList<>();
        Path path = Paths.get(pathStr);
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.pgn")) {
                for (Path entry : stream) {
                    games.addAll(readGamesFromFile(entry));
                }
            }
        } else {
            games.addAll(readGamesFromFile(path));
        }
        return games;
    }

    private static List<String> readGamesFromFile(Path filePath) throws IOException {
        List<String> games = new ArrayList<>();
        StringBuilder currentGame = new StringBuilder();
        List<String> lines = Files.readAllLines(filePath);
        for (String line : lines) {
            if (line.trim().isEmpty() && currentGame.length() > 0) {
                games.add(currentGame.toString());
                currentGame.setLength(0);
            } else {
                currentGame.append(line).append("\n");
            }
        }
        if (currentGame.length() > 0) {
            games.add(currentGame.toString());
        }
        return games;
    }
}