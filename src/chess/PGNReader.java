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

class GameValidator {
    public static void validateGames(List<String> games) {
        int gameCounter = 1;
        for (String game : games) {
            System.out.println("Game " + gameCounter + ":");
            try {
                List<String> errors = SANParser.parse(game);
                if (!errors.isEmpty()) {
                    System.out.println("❌ Syntax errors:");
                    errors.forEach(System.out::println);
                } else {
                    ChessBoard board = new ChessBoard();
                    List<Move> moves = SANParser.toMoves(game, board);
                    for (Move move : moves) {
                        if (!board.applyMove(move)) {
                            System.out.println("❌ Invalid move: " + move);
                            throw new Exception();
                        }
                    }
                    System.out.println("✅ Valid");
                }
            } catch (Exception e) {
                System.out.println("❌ Invalid");
            }
            System.out.println();
            gameCounter++;
        }
    }
}