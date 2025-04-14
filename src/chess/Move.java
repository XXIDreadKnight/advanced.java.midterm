package chess;

import chess.pieces.Piece;

/**
 * Represents a single move in a chess game, including special flags.
 */
public class Move {
    public final int fromRow, fromCol;
    public final int toRow, toCol;
    public final Piece movingPiece; // The piece instance that is moving
    public final Piece capturedPiece; // The piece instance captured (null if none)
    public final boolean isCastlingKingside;
    public final boolean isCastlingQueenside;
    public final boolean isEnPassant;
    public final boolean isPromotion;
    public final char promotionPieceSymbol; // Uppercase symbol: Q, R, B, N

    // Constructor for regular moves (including captures, promotion)
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece movingPiece, Piece capturedPiece, char promotionPieceSymbol) {
        this(fromRow, fromCol, toRow, toCol, movingPiece, capturedPiece, false, false, capturedPiece == null && movingPiece.getSymbol() == 'P' && fromCol != toCol, promotionPieceSymbol != 0, promotionPieceSymbol);
        if (isPromotion && promotionPieceSymbol == 0) {
            throw new IllegalArgumentException("Promotion move must specify a promotion piece symbol.");
        }
        if (!isPromotion && promotionPieceSymbol != 0) {
            throw new IllegalArgumentException("Non-promotion move cannot specify a promotion piece symbol.");
        }
    }

    // Constructor for castling
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece movingPiece, boolean kingside) {
        this(fromRow, fromCol, toRow, toCol, movingPiece, null, kingside, !kingside, false, false, (char) 0);
        if (!(movingPiece instanceof chess.pieces.King)) {
            throw new IllegalArgumentException("Castling move must involve the King.");
        }
    }

    // Private constructor handling all flags
    private Move(int fromRow, int fromCol, int toRow, int toCol,
                 Piece movingPiece, Piece capturedPiece,
                 boolean isCastlingKingside, boolean isCastlingQueenside, boolean isEnPassant,
                 boolean isPromotion, char promotionPieceSymbol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.movingPiece = movingPiece; // Store the actual piece instance
        this.capturedPiece = capturedPiece;
        this.isCastlingKingside = isCastlingKingside;
        this.isCastlingQueenside = isCastlingQueenside;
        this.isEnPassant = isEnPassant;
        this.isPromotion = isPromotion;
        this.promotionPieceSymbol = promotionPieceSymbol; // Store as char 'Q', 'R', etc.

        if (isCastlingKingside && isCastlingQueenside) {
            throw new IllegalArgumentException("Move cannot be both Kingside and Queenside castling.");
        }
        if ((isCastlingKingside || isCastlingQueenside) && isEnPassant) {
            throw new IllegalArgumentException("Move cannot be both Castling and En Passant.");
        }
        if ((isCastlingKingside || isCastlingQueenside) && isPromotion) {
            throw new IllegalArgumentException("Move cannot be both Castling and Promotion.");
        }
        if (isEnPassant && isPromotion) {
            throw new IllegalArgumentException("Move cannot be both En Passant and Promotion.");
        }
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(movingPiece != null ? movingPiece.getSymbol() : '?');
        sb.append(coordToString(fromRow, fromCol));
        sb.append(capturedPiece != null ? 'x' : '-');
        sb.append(coordToString(toRow, toCol));

        if (isPromotion) {
            sb.append("=").append(promotionPieceSymbol);
        }
        if (isCastlingKingside) {
            sb.append(" (O-O)");
        }
        if (isCastlingQueenside) {
            sb.append(" (O-O-O)");
        }
        if (isEnPassant) {
            sb.append(" (e.p.)");
        }
        return sb.toString();
    }

    // Helper to convert row/col to algebraic notation (e.g., "e4")
    public static String coordToString(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return "??";
        return "" + (char)('a' + col) + (char)('8' - row);
    }

    // Helper to get algebraic start square
    public String getFromSquare() {
        return coordToString(fromRow, fromCol);
    }

    // Helper to get algebraic end square
    public String getToSquare() {
        return coordToString(toRow, toCol);
    }
}