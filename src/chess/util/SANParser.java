package chess.util;

import chess.*;
import chess.pieces.*;

public class SANParser {
    private final ChessBoard board;

    public SANParser(ChessBoard board) {
        this.board = board;
    }

    public Move parse(String san) throws Exception {
        san = san.trim().replace("+", "").replace("#", "").replace("x", "");

        if (san.equals("O-O") || san.equals("0-0")) {
            int row = board.getCurrentTurn() == Color.WHITE ? 7 : 0;
            return new Move(row, 4, row, 6, new King(board.getCurrentTurn()), null, true, false, false, null);
        }
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            int row = board.getCurrentTurn() == Color.WHITE ? 7 : 0;
            return new Move(row, 4, row, 2, new King(board.getCurrentTurn()), null, true, false, false, null);
        }

        char promotion = 0;
        if (san.contains("=")) {
            int idx = san.indexOf("=");
            promotion = san.charAt(idx + 1);
            san = san.substring(0, idx);
        }

        char pieceChar = 'P';
        int idx = 0;
        char c = san.charAt(0);
        if (Character.isUpperCase(c) && c != 'O') {
            pieceChar = c;
            idx++;
        }

        String rest = san.substring(idx);
        String destStr = rest.substring(rest.length() - 2);
        int toCol = destStr.charAt(0) - 'a';
        int toRow = 8 - Character.getNumericValue(destStr.charAt(1));

        String disambiguator = rest.length() > 2 ? rest.substring(0, rest.length() - 2) : "";

        Piece movingPiece = pieceFromSymbol(pieceChar);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPiece(row, col);
                if (p != null && p.getClass() == movingPiece.getClass() && p.color == board.getCurrentTurn()) {
                    if (!disambiguator.isEmpty()) {
                        if (disambiguator.length() == 1) {
                            char d = disambiguator.charAt(0);
                            if (Character.isDigit(d) && row != 8 - Character.getNumericValue(d)) continue;
                            if (Character.isLetter(d) && col != d - 'a') continue;
                        }
                    }
                    if (p.isValidMove(board, row, col, toRow, toCol)) {
                        boolean isPromotion = (promotion != 0);
                        boolean isEnPassant = p instanceof Pawn && col != toCol && board.getPiece(toRow, toCol) == null;
                        return new Move(row, col, toRow, toCol, p, null, false, isPromotion, isEnPassant, isPromotion ? promotion : null);
                    }
                }
            }
        }

        throw new Exception("Illegal move: " + san);
    }

    private Piece pieceFromSymbol(char c) {
        Color color = board.getCurrentTurn();
        return switch (c) {
            case 'N' -> new Knight(color);
            case 'B' -> new Bishop(color);
            case 'R' -> new Rook(color);
            case 'Q' -> new Queen(color);
            case 'K' -> new King(color);
            default -> new Pawn(color);
        };
    }
}