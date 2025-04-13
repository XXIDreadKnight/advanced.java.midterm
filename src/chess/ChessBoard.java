package chess;

import chess.pieces.*;
import java.util.*;

public class ChessBoard {
    private final Piece[][] board = new Piece[8][8];
    private Color currentTurn = Color.WHITE;
    private boolean whiteKingMoved = false, blackKingMoved = false;
    private boolean whiteKingsideRookMoved = false, whiteQueensideRookMoved = false;
    private boolean blackKingsideRookMoved = false, blackQueensideRookMoved = false;
    private int[] enPassantTarget = null;

    public ChessBoard() {
        setupInitialBoard();
    }

    private void setupInitialBoard() {
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(Color.BLACK);
            board[6][i] = new Pawn(Color.WHITE);
        }
        board[0][0] = board[0][7] = new Rook(Color.BLACK);
        board[0][1] = board[0][6] = new Knight(Color.BLACK);
        board[0][2] = board[0][5] = new Bishop(Color.BLACK);
        board[0][3] = new Queen(Color.BLACK);
        board[0][4] = new King(Color.BLACK);

        board[7][0] = board[7][7] = new Rook(Color.WHITE);
        board[7][1] = board[7][6] = new Knight(Color.WHITE);
        board[7][2] = board[7][5] = new Bishop(Color.WHITE);
        board[7][3] = new Queen(Color.WHITE);
        board[7][4] = new King(Color.WHITE);
    }

    public Piece getPiece(int row, int col) {
        return board[row][col];
    }

    public boolean applyMove(Move move) {
        if (move == null || move.movingPiece == null || move.movingPiece.color != currentTurn) return false;
        if (!move.movingPiece.isValidMove(this, move.fromRow, move.fromCol, move.toRow, move.toCol)) return false;

        // Handle castling
        if (move.isCastling) {
            if (!isCastlingLegal(currentTurn, move.toCol > move.fromCol)) return false;
            int row = currentTurn == Color.WHITE ? 7 : 0;
            if (move.toCol == 6) {
                board[row][5] = board[row][7];
                board[row][7] = null;
            } else {
                board[row][3] = board[row][0];
                board[row][0] = null;
            }
        }

        // Handle en passant
        if (move.isEnPassant && enPassantTarget != null) {
            board[move.fromRow][move.fromCol] = null;
            board[move.toRow][move.toCol] = move.movingPiece;
            board[move.fromRow][move.toCol] = null;
        } else {
            board[move.toRow][move.toCol] = move.movingPiece;
            board[move.fromRow][move.fromCol] = null;
        }

        // Handle promotion
        if (move.isPromotion) {
            switch (move.promotionType) {
                case 'Q': board[move.toRow][move.toCol] = new Queen(currentTurn); break;
                case 'R': board[move.toRow][move.toCol] = new Rook(currentTurn); break;
                case 'B': board[move.toRow][move.toCol] = new Bishop(currentTurn); break;
                case 'N': board[move.toRow][move.toCol] = new Knight(currentTurn); break;
            }
        }

        // Update en passant target
        if (move.movingPiece instanceof Pawn && Math.abs(move.toRow - move.fromRow) == 2) {
            enPassantTarget = new int[] {(move.fromRow + move.toRow) / 2, move.fromCol};
        } else {
            enPassantTarget = null;
        }

        // Update castling rights
        if (move.movingPiece instanceof King) {
            if (currentTurn == Color.WHITE) whiteKingMoved = true;
            else blackKingMoved = true;
        }
        if (move.movingPiece instanceof Rook) {
            if (move.fromRow == 7 && move.fromCol == 0) whiteQueensideRookMoved = true;
            if (move.fromRow == 7 && move.fromCol == 7) whiteKingsideRookMoved = true;
            if (move.fromRow == 0 && move.fromCol == 0) blackQueensideRookMoved = true;
            if (move.fromRow == 0 && move.fromCol == 7) blackKingsideRookMoved = true;
        }

        currentTurn = currentTurn.opposite();
        return true;
    }

    public boolean isEnPassantTarget(int row, int col) {
        return enPassantTarget != null && enPassantTarget[0] == row && enPassantTarget[1] == col;
    }

    public boolean isCastlingLegal(Color color, boolean kingside) {
        if (color == Color.WHITE) {
            if (whiteKingMoved) return false;
            if (kingside && whiteKingsideRookMoved) return false;
            if (!kingside && whiteQueensideRookMoved) return false;
            return board[7][5] == null && board[7][6] == null;
        } else {
            if (blackKingMoved) return false;
            if (kingside && blackKingsideRookMoved) return false;
            if (!kingside && blackQueensideRookMoved) return false;
            return board[0][5] == null && board[0][6] == null;
        }
    }

    public boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int dRow = Integer.compare(toRow, fromRow);
        int dCol = Integer.compare(toCol, fromCol);
        int r = fromRow + dRow, c = fromCol + dCol;
        while (r != toRow || c != toCol) {
            if (board[r][c] != null) return false;
            r += dRow;
            c += dCol;
        }
        return true;
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }
}