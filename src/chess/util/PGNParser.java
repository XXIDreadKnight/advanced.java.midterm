package chess.util;

import java.io.*;
import java.util.*;

public class PGNParser {
    public static class Game {
        public Map<String, String> headers = new HashMap<>();
        public List<String> moveTexts = new ArrayList<>();
    }

    public List<Game> parseFile(File file) throws IOException {
        List<Game> games = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));

        Game currentGame = new Game();
        String line;
        StringBuilder moves = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[")) {
                int firstSpace = line.indexOf(" ");
                int lastQuote = line.lastIndexOf('"');
                String key = line.substring(1, firstSpace);
                String value = line.substring(firstSpace + 2, lastQuote);
                currentGame.headers.put(key, value);
            } else {
                moves.append(" ").append(line);
            }

            if ((line.startsWith("1.") || line.matches("\\d+\\.")) && !reader.ready()) {
                games.add(finalizeGame(currentGame, moves.toString()));
                currentGame = new Game();
                moves = new StringBuilder();
            }
        }

        if (!currentGame.headers.isEmpty() || moves.length() > 0) {
            games.add(finalizeGame(currentGame, moves.toString()));
        }

        reader.close();
        return games;
    }

    private Game finalizeGame(Game game, String movesStr) {
        movesStr = movesStr.replaceAll("\\d+\\.\\.\\.", ""); // remove ... notation
        String[] tokens = movesStr.trim().split("\\s+");
        for (String token : tokens) {
            if (!token.matches("\\d+\\.") && !token.matches("1-0|0-1|1/2-1/2|\\*") && !token.isEmpty()) {
                game.moveTexts.add(token);
            }
        }
        return game;
    }
}