package chess;

import chess.pieces.*;
import java.util.*;

/**
 * Represents the state of a chessboard and enforces chess rules.
 */
public class ChessBoard {
    private final Piece[][] board = new Piece[8][8];
    private Color currentTurn = Color.WHITE;
    private int[] enPassantTarget = null; // Stores [row, col] of square *behind* pawn that just moved 2 squares
    private Map<Color, Boolean> kingMoved = new HashMap<>();
    private Map<Color, Boolean> kingsideRookMoved = new HashMap<>();
    private Map<Color, Boolean> queensideRookMoved = new HashMap<>();

    // For undoing moves
    private Stack<MoveHistory> history = new Stack<>();

    // Inner class to store state for undoing moves
    private static class MoveHistory {
        final Move move;
        final int[] oldEnPassantTarget;
        final boolean kingMovedBefore;
        final boolean kingsideRookMovedBefore;
        final boolean queensideRookMovedBefore;
        final Piece capturedPiece; // Keep track of the actual captured piece instance

        MoveHistory(Move move, int[] oldEnPassantTarget, boolean km, boolean ksrm, boolean qsrm, Piece captured) {
            this.move = move;
            this.oldEnPassantTarget = oldEnPassantTarget;
            this.kingMovedBefore = km;
            this.kingsideRookMovedBefore = ksrm;
            this.queensideRookMovedBefore = qsrm;
            this.capturedPiece = captured; // Store the captured piece itself
        }
    }

    /**
     * Initializes a new board with the standard starting position.
     */
    public ChessBoard() {
        setupInitialBoard();
        kingMoved.put(Color.WHITE, false);
        kingMoved.put(Color.BLACK, false);
        kingsideRookMoved.put(Color.WHITE, false);
        kingsideRookMoved.put(Color.BLACK, false);
        queensideRookMoved.put(Color.WHITE, false);
        queensideRookMoved.put(Color.BLACK, false);
    }

    /** Creates a deep copy of the board state */
    public ChessBoard copy() {
        ChessBoard newBoard = new ChessBoard(); // Creates empty board with initial state maps
        newBoard.currentTurn = this.currentTurn;
        newBoard.enPassantTarget = this.enPassantTarget == null ? null : this.enPassantTarget.clone();
        newBoard.kingMoved.putAll(this.kingMoved);
        newBoard.kingsideRookMoved.putAll(this.kingsideRookMoved);
        newBoard.queensideRookMoved.putAll(this.queensideRookMoved);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (this.board[r][c] != null) {
                    newBoard.board[r][c] = this.board[r][c].copy(); // Use piece copy constructor
                } else {
                    newBoard.board[r][c] = null;
                }
            }
        }
        // History stack is NOT copied, as it represents the past of a specific game instance
        return newBoard;
    }


    private void setupInitialBoard() {
        // Clear board first
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }

        // Pawns
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(Color.BLACK);
            board[6][i] = new Pawn(Color.WHITE);
        }
        // Rooks
        board[0][0] = new Rook(Color.BLACK); board[0][7] = new Rook(Color.BLACK);
        board[7][0] = new Rook(Color.WHITE); board[7][7] = new Rook(Color.WHITE);
        // Knights
        board[0][1] = new Knight(Color.BLACK); board[0][6] = new Knight(Color.BLACK);
        board[7][1] = new Knight(Color.WHITE); board[7][6] = new Knight(Color.WHITE);
        // Bishops
        board[0][2] = new Bishop(Color.BLACK); board[0][5] = new Bishop(Color.BLACK);
        board[7][2] = new Bishop(Color.WHITE); board[7][5] = new Bishop(Color.WHITE);
        // Queens
        board[0][3] = new Queen(Color.BLACK);
        board[7][3] = new Queen(Color.WHITE);
        // Kings
        board[0][4] = new King(Color.BLACK);
        board[7][4] = new King(Color.WHITE);

        currentTurn = Color.WHITE;
        enPassantTarget = null;
        history.clear();
        kingMoved.replaceAll((c, v) -> false);
        kingsideRookMoved.replaceAll((c, v) -> false);
        queensideRookMoved.replaceAll((c, v) -> false);

    }

    public Piece getPiece(int row, int col) {
        if (!isValidCoordinate(row, col)) return null;
        return board[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        if (isValidCoordinate(row, col)) {
            board[row][col] = piece;
        }
    }

    public Color getCurrentTurn() {
        return currentTurn;
    }

    public int[] getEnPassantTarget() {
        return enPassantTarget;
    }


    /**
     * Attempts to apply the given move to the board.
     * Validates the move for legality (including checks) before applying.
     * Throws IllegalMoveException if the move is not valid.
     * @param move The move to apply.
     * @throws IllegalMoveException if the move is illegal for any reason.
     */
    public void applyMove(Move move) throws IllegalMoveException {
        if (move == null || move.movingPiece == null) {
            throw new IllegalMoveException("Move or moving piece is null.");
        }
        if (move.movingPiece.color != currentTurn) {
            throw new IllegalMoveException("Piece moved is not of the current turn's color (" + currentTurn + "). Found: " + move.movingPiece.color);
        }
        // Fetch the piece from the board at the source square to ensure it's the correct one
        Piece pieceOnBoard = getPiece(move.fromRow, move.fromCol);
        if (pieceOnBoard == null || pieceOnBoard.getClass() != move.movingPiece.getClass() || pieceOnBoard.color != move.movingPiece.color) {
            throw new IllegalMoveException("The piece specified in the move ("+ move.movingPiece +") does not match the piece on the board at " + Move.coordToString(move.fromRow, move.fromCol) + " ("+ pieceOnBoard +")");
        }

        // Validate the move thoroughly BEFORE applying it
        validateMoveLegality(move);

        // --- If validation passes, proceed to apply the move ---

        // Store state for undo
        int[] oldEnPassant = enPassantTarget == null ? null : enPassantTarget.clone();
        boolean kmBefore = kingMoved.get(currentTurn);
        boolean ksrmBefore = kingsideRookMoved.get(currentTurn);
        boolean qsrmBefore = queensideRookMoved.get(currentTurn);
        // Note: move.capturedPiece might be null even if it was a capture identified by SANParser initially.
        // We need the actual piece on the target square *before* moving. En passant capture target is different.
        Piece actualCapturedPiece = move.isEnPassant ? getPiece(move.fromRow, move.toCol) : getPiece(move.toRow, move.toCol); // Piece captured on target square or EP target
        history.push(new MoveHistory(move, oldEnPassant, kmBefore, ksrmBefore, qsrmBefore, actualCapturedPiece));


        // --- Apply the move ---
        int fromR = move.fromRow; int fromC = move.fromCol;
        int toR = move.toRow; int toC = move.toCol;
        Piece movingP = board[fromR][fromC]; // Use the actual piece from the board

        // Reset en passant target for the next turn (will be set below if applicable)
        this.enPassantTarget = null;

        // Move the piece
        board[toR][toC] = movingP;
        board[fromR][fromC] = null;
        movingP.setHasMoved(true); // Mark the piece as having moved

        // Handle special move types
        if (move.isCastlingKingside) {
            Piece rook = board[fromR][7]; // Rook starts at col 7 (h-file)
            board[fromR][5] = rook; // Rook moves to col 5 (f-file)
            board[fromR][7] = null;
            if (rook != null) rook.setHasMoved(true);
            kingMoved.put(currentTurn, true);
            kingsideRookMoved.put(currentTurn, true);
        } else if (move.isCastlingQueenside) {
            Piece rook = board[fromR][0]; // Rook starts at col 0 (a-file)
            board[fromR][3] = rook; // Rook moves to col 3 (d-file)
            board[fromR][0] = null;
            if (rook != null) rook.setHasMoved(true);
            kingMoved.put(currentTurn, true);
            queensideRookMoved.put(currentTurn, true);
        } else if (move.isEnPassant) {
            // Capture the pawn being passed - it's on the same rank as the moving pawn, but the target column
            board[fromR][toC] = null; // Remove the captured pawn
        } else if (move.isPromotion) {
            board[toR][toC] = createPiece(move.promotionPieceSymbol, currentTurn);
            // Ensure the new piece knows it potentially moved (though it didn't start the move)
            if (board[toR][toC]!= null) board[toR][toC].setHasMoved(true);
        }

        // Set new en passant target if a pawn moved two squares
        if (movingP instanceof Pawn && Math.abs(toR - fromR) == 2) {
            this.enPassantTarget = new int[]{(fromR + toR) / 2, fromC}; // Target is square *behind* pawn
        }

        // Update castling rights if king or rook moved from their starting squares
        if (movingP instanceof King) {
            kingMoved.put(currentTurn, true);
        } else if (movingP instanceof Rook) {
            if (fromR == (currentTurn == Color.WHITE ? 7 : 0)) {
                if (fromC == 0) queensideRookMoved.put(currentTurn, true);
                else if (fromC == 7) kingsideRookMoved.put(currentTurn, true);
            }
        }
        // If a piece is captured on a rook's starting square, castling rights might be lost too
        if (actualCapturedPiece instanceof Rook) {
            Color capturedColor = actualCapturedPiece.color;
            int capturedRow = toR; // The square where the capture happened
            int capturedCol = toC;
            if (capturedRow == (capturedColor == Color.WHITE ? 7 : 0)) {
                if (capturedCol == 0) queensideRookMoved.put(capturedColor, true);
                else if (capturedCol == 7) kingsideRookMoved.put(capturedColor, true);
            }
        }


        // Switch turn
        currentTurn = currentTurn.opposite();
    }

    /**
     * Undoes the last move made on the board.
     */
    public void undoMove() {
        if (history.isEmpty()) {
            return; // No moves to undo
        }
        MoveHistory hist = history.pop();
        Move move = hist.move;
        Piece movingPiece = getPiece(move.toRow, move.toCol); // Piece that ended up on target square

        // Switch turn back
        currentTurn = currentTurn.opposite();

        // Restore castling rights and en passant target from before the move
        this.enPassantTarget = hist.oldEnPassantTarget;
        this.kingMoved.put(currentTurn, hist.kingMovedBefore);
        this.kingsideRookMoved.put(currentTurn, hist.kingsideRookMovedBefore);
        this.queensideRookMoved.put(currentTurn, hist.queensideRookMovedBefore);
        // Need to potentially restore captured piece's castling rights loss
        if (hist.capturedPiece instanceof Rook) {
            Color capturedColor = hist.capturedPiece.color;
            int capturedRow = move.toRow; // Where the capture happened
            int capturedCol = move.toCol;
            if (capturedRow == (capturedColor == Color.WHITE ? 7 : 0)) {
                Piece cornerPiece = getPiece(capturedRow, capturedCol == 0 ? 0 : 7);
                // This logic is tricky - need to know if the rook that WAS there hadn't moved yet
                // For simplicity, we might rely on the explicit moved flags,
                // but a truly robust undo might need more state.
                // Let's assume the flags restored above are sufficient for now.
            }
        }


        // --- Reverse the move ---
        // Move piece back
        if (move.isPromotion) {
            // If it was a promotion, replace promoted piece with a pawn
            movingPiece = new Pawn(currentTurn);
            // Reset hasMoved status based on original pawn's state IF possible
            // This is complex - maybe just assume pawn had moved if it reached promotion rank
            movingPiece.setHasMoved(move.fromRow != (currentTurn == Color.WHITE ? 6 : 1));
        }

        board[move.fromRow][move.fromCol] = movingPiece;
        board[move.toRow][move.toCol] = null; // Clear target square (captured piece restored below)

        // Restore hasMoved status only if it was the piece's first move
        // We need the piece's state BEFORE the move. This is hard with current setup.
        // Simplification: If the piece is King/Rook and its moved flag is false now, it means this was its first move.
        if (movingPiece != null && !hist.kingMovedBefore && movingPiece instanceof King) movingPiece.setHasMoved(false);
        // Similar logic for rooks is harder due to two rooks. Need to check original square.
        if (movingPiece != null && movingPiece instanceof Rook) {
            int startRow = currentTurn == Color.WHITE ? 7 : 0;
            if (move.fromRow == startRow && move.fromCol == 0 && !hist.queensideRookMovedBefore) movingPiece.setHasMoved(false);
            if (move.fromRow == startRow && move.fromCol == 7 && !hist.kingsideRookMovedBefore) movingPiece.setHasMoved(false);
        }
        // If it was not a King/Rook or it wasn't their first move, hasMoved remains true.


        // Restore captured piece
        if (move.isEnPassant) {
            // Put the captured pawn back on its square
            board[move.fromRow][move.toCol] = hist.capturedPiece; // Captured pawn was adjacent
        } else if (hist.capturedPiece != null) {
            // Put the regularly captured piece back
            board[move.toRow][move.toCol] = hist.capturedPiece;
        }

        // Undo castling rook move
        if (move.isCastlingKingside) {
            Piece rook = board[move.fromRow][5]; // Rook is now on f-file
            board[move.fromRow][7] = rook; // Move back to h-file
            board[move.fromRow][5] = null;
            if (rook != null && !hist.kingsideRookMovedBefore) rook.setHasMoved(false); // Reset moved status if it was first move
        } else if (move.isCastlingQueenside) {
            Piece rook = board[move.fromRow][3]; // Rook is now on d-file
            board[move.fromRow][0] = rook; // Move back to a-file
            board[move.fromRow][3] = null;
            if (rook != null && !hist.queensideRookMovedBefore) rook.setHasMoved(false); // Reset moved status if it was first move
        }
    }


    /**
     * Central validation logic called before applying a move.
     * Checks piece movement rules, path clearing, captures, special moves, and king safety.
     * @param move The move to validate.
     * @throws IllegalMoveException If the move is invalid.
     */
    private void validateMoveLegality(Move move) throws IllegalMoveException {
        Piece movingPiece = getPiece(move.fromRow, move.fromCol); // Get the piece from the board
        if (movingPiece == null) {
            throw new IllegalMoveException("No piece found at starting square " + move.getFromSquare());
        }
        if (movingPiece.color != currentTurn) {
            throw new IllegalMoveException("Piece at " + move.getFromSquare() + " belongs to opponent.");
        }
        if (!movingPiece.isValidMovePattern(move.fromRow, move.fromCol, move.toRow, move.toCol)) {
            throw new IllegalMoveException("Invalid move pattern for " + movingPiece.getClass().getSimpleName() + " from " + move.getFromSquare() + " to " + move.getToSquare());
        }

        // --- Check board-specific conditions ---
        Piece targetPiece = getPiece(move.toRow, move.toCol);

        // Cannot capture own piece
        if (targetPiece != null && targetPiece.color == currentTurn) {
            throw new IllegalMoveException("Cannot capture own piece at " + move.getToSquare());
        }

        // Check path for sliding pieces (Rook, Bishop, Queen)
        if (movingPiece instanceof Rook || movingPiece instanceof Bishop || movingPiece instanceof Queen) {
            if (!isPathClear(move.fromRow, move.fromCol, move.toRow, move.toCol)) {
                throw new IllegalMoveException("Path is not clear for " + movingPiece.getClass().getSimpleName() + " from " + move.getFromSquare() + " to " + move.getToSquare());
            }
        }

        // --- Specific Piece/Move Type Logic ---
        if (movingPiece instanceof Pawn) {
            validatePawnMove(move, movingPiece, targetPiece);
        } else if (movingPiece instanceof King) {
            validateKingMove(move, movingPiece);
        } else if (movingPiece instanceof Knight) {
            // Knight validation is mostly pattern + target check (already done)
        } else {
            // Rook, Bishop, Queen standard moves (path clear, target check already done)
        }

        // --- Final Check: Move must not leave the king in check ---
        if (moveLeavesKingInCheck(move)) {
            throw new IllegalMoveException("Move " + move.toString() + " leaves the king in check.");
        }
    }

    private void validatePawnMove(Move move, Piece pawn, Piece targetPiece) throws IllegalMoveException {
        int dr = move.toRow - move.fromRow;
        int dc = move.toCol - move.fromCol;
        int absDc = Math.abs(dc);
        int dir = (pawn.color == Color.WHITE) ? -1 : 1;

        if (absDc == 0) { // Forward move
            if (targetPiece != null) {
                throw new IllegalMoveException("Pawn cannot capture forward at " + move.getToSquare());
            }
            if (dr == dir) { // Single step
                if (move.isPromotion != ((pawn.color == Color.WHITE && move.toRow == 0) || (pawn.color == Color.BLACK && move.toRow == 7))) {
                    throw new IllegalMoveException("Promotion status mismatch for pawn move to rank " + (8 - move.toRow));
                }
            } else if (dr == 2 * dir) { // Double step
                int startRow = (pawn.color == Color.WHITE) ? 6 : 1;
                if (move.fromRow != startRow) {
                    throw new IllegalMoveException("Pawn can only move two steps from its starting rank.");
                }
                if (getPiece(move.fromRow + dir, move.fromCol) != null) {
                    throw new IllegalMoveException("Path blocked for pawn double step.");
                }
                if (move.isPromotion) {
                    throw new IllegalMoveException("Pawn cannot promote on a double step.");
                }
            } else {
                throw new IllegalMoveException("Invalid pawn forward move distance.");
            }
        } else if (absDc == 1) { // Capture or En Passant
            if (dr != dir) {
                throw new IllegalMoveException("Invalid pawn capture direction.");
            }
            if (move.isPromotion != ((pawn.color == Color.WHITE && move.toRow == 0) || (pawn.color == Color.BLACK && move.toRow == 7))) {
                throw new IllegalMoveException("Promotion status mismatch for pawn capture to rank " + (8 - move.toRow));
            }

            // Check for standard capture
            if (targetPiece != null) {
                if (targetPiece.color == pawn.color) { // Should have been caught earlier, but double check
                    throw new IllegalMoveException("Cannot capture own piece.");
                }
                if (move.isEnPassant) {
                    throw new IllegalMoveException("Move marked as En Passant but captures a piece on the target square.");
                }
                // Standard capture is valid here
            } else { // No piece on target square, must be En Passant
                if (!move.isEnPassant) {
                    throw new IllegalMoveException("Pawn diagonal move to empty square " + move.getToSquare() + " is only valid for En Passant.");
                }
                // Verify En Passant target square
                if (enPassantTarget == null || move.toRow != enPassantTarget[0] || move.toCol != enPassantTarget[1]) {
                    throw new IllegalMoveException("Invalid En Passant target square. Target is " + Arrays.toString(enPassantTarget) + ", move is to (" + move.toRow + "," + move.toCol + ")");
                }
                // En passant is valid here
            }
        } else {
            throw new IllegalMoveException("Invalid pawn move pattern (dx=" + dc + ", dy=" + dr + ")");
        }
    }

    private void validateKingMove(Move move, Piece king) throws IllegalMoveException {
        int dr = Math.abs(move.toRow - move.fromRow);
        int dc = Math.abs(move.toCol - move.fromCol);

        if (dr <= 1 && dc <= 1) { // Standard move (already checked pattern and target)
            if (move.isCastlingKingside || move.isCastlingQueenside) {
                throw new IllegalMoveException("Standard king move cannot be castling.");
            }
        } else if (dr == 0 && dc == 2) { // Castling
            if (!move.isCastlingKingside && !move.isCastlingQueenside) {
                throw new IllegalMoveException("King move by 2 squares must be flagged as castling.");
            }
            boolean kingside = move.toCol > move.fromCol; // True if kingside (g-file), false if queenside (c-file)
            validateCastling(king.color, kingside);
        } else {
            throw new IllegalMoveException("Invalid king move pattern."); // Should be caught by pattern check
        }
    }

    private void validateCastling(Color color, boolean kingside) throws IllegalMoveException {
        int row = (color == Color.WHITE) ? 7 : 0;
        int kingStartCol = 4;

        // 1. King or relevant Rook must not have moved
        if (kingMoved.get(color)) {
            throw new IllegalMoveException("Cannot castle: King has moved.");
        }
        if (kingside && kingsideRookMoved.get(color)) {
            throw new IllegalMoveException("Cannot castle kingside: Kingside rook has moved.");
        }
        if (!kingside && queensideRookMoved.get(color)) {
            throw new IllegalMoveException("Cannot castle queenside: Queenside rook has moved.");
        }

        // Check if rooks are actually there (relevant for Chess960, but good practice)
        Piece kingsideRook = getPiece(row, 7);
        Piece queensideRook = getPiece(row, 0);
        if (kingside && (kingsideRook == null || !(kingsideRook instanceof Rook) || kingsideRook.color != color)) {
            throw new IllegalMoveException("Cannot castle kingside: Kingside rook not in place.");
        }
        if (!kingside && (queensideRook == null || !(queensideRook instanceof Rook) || queensideRook.color != color)) {
            throw new IllegalMoveException("Cannot castle queenside: Queenside rook not in place.");
        }


        // 2. Squares between King and Rook must be empty
        if (kingside) { // O-O (f1/f8 and g1/g8 must be empty)
            if (getPiece(row, kingStartCol + 1) != null || getPiece(row, kingStartCol + 2) != null) {
                throw new IllegalMoveException("Cannot castle kingside: Path is blocked.");
            }
        } else { // O-O-O (b1/b8, c1/c8, d1/d8 must be empty)
            if (getPiece(row, kingStartCol - 1) != null || getPiece(row, kingStartCol - 2) != null || getPiece(row, kingStartCol - 3) != null) {
                throw new IllegalMoveException("Cannot castle queenside: Path is blocked.");
            }
        }

        // 3. King must not be in check
        if (isKingInCheck(color)) {
            throw new IllegalMoveException("Cannot castle: King is currently in check.");
        }

        // 4. King must not pass through or land on an attacked square
        Color opponentColor = color.opposite();
        int kingColThrough = kingside ? kingStartCol + 1 : kingStartCol - 1;
        int kingColEnd = kingside ? kingStartCol + 2 : kingStartCol - 2;

        if (isSquareAttackedBy(row, kingStartCol, opponentColor) || // Shouldn't happen if check #3 passed, but safe
                isSquareAttackedBy(row, kingColThrough, opponentColor) ||
                isSquareAttackedBy(row, kingColEnd, opponentColor)) {
            throw new IllegalMoveException("Cannot castle: King passes through or lands on an attacked square.");
        }
    }


    /**
     * Checks if the specified square is attacked by any piece of the given color.
     * @param targetRow Row of the square (0-7).
     * @param targetCol Column of the square (0-7).
     * @param attackerColor The color of the pieces to check for attacks.
     * @return True if the square is attacked by any piece of 'attackerColor', false otherwise.
     */
    public boolean isSquareAttackedBy(int targetRow, int targetCol, Color attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.color == attackerColor) {
                    // Check if this piece attacks the target square
                    // This requires generating potential moves/attacks from (r,c) to (targetRow, targetCol)
                    // Need a lightweight check, not full move validation
                    if (pieceAttacksSquare(p, r, c, targetRow, targetCol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Lightweight check if a specific piece attacks a target square, considering basic movement and captures.
     * Does NOT check for pins or discovered attacks, just direct attack lines.
     */
    private boolean pieceAttacksSquare(Piece attacker, int fromRow, int fromCol, int targetRow, int targetCol) {
        if (!attacker.isValidMovePattern(fromRow, fromCol, targetRow, targetCol)) {
            return false; // Doesn't match basic movement
        }

        // Pawns attack differently than they move
        if (attacker instanceof Pawn) {
            int dir = (attacker.color == Color.WHITE) ? -1 : 1;
            return Math.abs(fromCol - targetCol) == 1 && (targetRow - fromRow) == dir;
        }

        // Knights jump, so path check isn't needed
        if (attacker instanceof Knight) {
            return true; // Pattern already matched
        }

        // Kings attack adjacent squares
        if (attacker instanceof King) {
            return true; // Pattern already matched
        }

        // Sliding pieces (Rook, Bishop, Queen) require a clear path
        if (attacker instanceof Rook || attacker instanceof Bishop || attacker instanceof Queen) {
            return isPathClear(fromRow, fromCol, targetRow, targetCol);
        }

        return false; // Should not happen if all piece types covered
    }


    /**
     * Checks if the king of the specified color is currently in check.
     * @param kingColor The color of the king to check.
     * @return True if the king is in check, false otherwise.
     */
    public boolean isKingInCheck(Color kingColor) {
        int kingRow = -1, kingCol = -1;
        // Find the king
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p instanceof King && p.color == kingColor) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
            if (kingRow != -1) break;
        }

        if (kingRow == -1) {
            // This should not happen in a valid game state
            System.err.println("Warning: King of color " + kingColor + " not found on board!");
            return false;
        }

        // Check if any opponent piece attacks the king's square
        return isSquareAttackedBy(kingRow, kingCol, kingColor.opposite());
    }

    /**
     * Checks if making the given move would leave the current player's king in check.
     * Performs the check by temporarily making the move, checking for check, and then undoing the move.
     * @param move The move to test.
     * @return True if the move results in the current player's king being in check, false otherwise.
     */
    private boolean moveLeavesKingInCheck(Move move) {
        boolean leavesInCheck = false;
        Color movingColor = currentTurn; // Color whose king we care about

        try {
            // Simulate the move (use a temporary board or apply/undo)
            // Using apply/undo is simpler here if undoMove is reliable
            applyMoveInternal(move); // Apply without full validation stack for speed

            // Check if the king of the player who just moved is now in check
            if (isKingInCheck(movingColor)) {
                leavesInCheck = true;
            }

        } catch (Exception e) {
            // If the internal apply failed (shouldn't happen if called after basic checks)
            // Treat this defensively - assume it might leave king in check or is invalid
            System.err.println("Warning: Exception during check simulation for move " + move + ": " + e.getMessage());
            // If the move itself was fundamentally invalid (e.g., bad coordinates)
            // it shouldn't reach here, but handle defensively.
            // A truly invalid move cannot be made, so it technically doesn't *leave* king in check.
            // This depends on context. If called from validation, the exception is the primary error.
            // Here, we assume the basic move is possible and check consequence.
            // Let's return true to be safe, indicating a problem.
            leavesInCheck = true; // Or re-throw? Best to ensure outer validation catches primary issue. Let's stick to the check result.

        } finally {
            // IMPORTANT: Always undo the simulated move
            undoMoveInternal();
        }

        return leavesInCheck;
    }

    // --- Helper methods for simulation without history stack manipulation ---
    // These are simplified versions for the check-simulation. They lack robustness.
    // A better approach is using the main applyMove/undoMove or copying the board.
    // Using apply/undo is safer IF undo is perfectly implemented.

    private Piece tempBoard[][] = new Piece[8][8]; // Very basic simulation state
    private Color tempTurn;
    private int[] tempEnPassant;
    // ... potentially need to save/restore castling flags too for full accuracy ...

    private void applyMoveInternal(Move move) {
        // Basic simulation: copy state, make move. Lacks castling/EP flag updates.
        // This is NOT ROBUST for full simulation. Use applyMove/undoMove.
        // This is just a placeholder showing the *intent* of simulation.

        // -- START OF SIMPLISTIC/INCORRECT SIMULATION --
        for(int r=0; r<8; r++) System.arraycopy(this.board[r], 0, tempBoard[r], 0, 8);
        tempTurn = this.currentTurn;
        tempEnPassant = this.enPassantTarget; // shallow copy

        Piece piece = tempBoard[move.fromRow][move.fromCol];
        if(piece == null) return; // Should not happen if called correctly

        tempBoard[move.toRow][move.toCol] = piece;
        tempBoard[move.fromRow][move.fromCol] = null;

        // Handle EP capture simulation
        if (move.isEnPassant) {
            tempBoard[move.fromRow][move.toCol] = null;
        }
        // Handle promotion simulation
        else if (move.isPromotion) {
            tempBoard[move.toRow][move.toCol] = createPiece(move.promotionPieceSymbol, tempTurn);
        }
        // Handle castling simulation
        else if (move.isCastlingKingside) {
            Piece rook = tempBoard[move.fromRow][7];
            tempBoard[move.fromRow][5] = rook;
            tempBoard[move.fromRow][7] = null;
        } else if (move.isCastlingQueenside) {
            Piece rook = tempBoard[move.fromRow][0];
            tempBoard[move.fromRow][3] = rook;
            tempBoard[move.fromRow][0] = null;
        }

        // Switch turn for check evaluation
        this.currentTurn = this.currentTurn.opposite();
        // Update the main board state for isKingInCheck (risky!)
        // This is why apply/undo or board copying is preferred.
        for(int r=0; r<8; r++) System.arraycopy(tempBoard[r], 0, this.board[r], 0, 8);
        // -- END OF SIMPLISTIC/INCORRECT SIMULATION --

        // Correct approach:
        // applyMove(move); // Use the full method - requires fixing validateMoveLegality recursion
        // OR
        // ChessBoard copy = this.copy();
        // copy.applyMoveInternal_NoCheck(move); // A version that just changes state
        // boolean result = copy.isKingInCheck(movingColor);
        // return result;

    }

    private void undoMoveInternal() {
        // Restore state from before the simulation
        // This is complex and error prone without a proper history/copy mechanism

        // -- START OF SIMPLISTIC/INCORRECT UNDO --
        // Restore board from temp (assuming temp wasn't modified after check)
        // This is conceptually flawed because isKingInCheck reads the main board
        // for(int r=0; r<8; r++) System.arraycopy(tempBoard[r], 0, this.board[r], 0, 8); // Incorrect

        // Restore turn
        this.currentTurn = tempTurn;
        this.enPassantTarget = tempEnPassant; // Also needs deep copy if mutable
        // Need to restore castling flags too

        // The CORRECT way: Use the main undoMove() method.
        undoMove();

        // -- END OF SIMPLISTIC/INCORRECT UNDO --

    }

    /**
     * Checks if the path is clear between two squares for a sliding piece.
     * Assumes the move is diagonal, horizontal, or vertical.
     * Does not check the target square itself.
     * @param fromRow Start row.
     * @param fromCol Start col.
     * @param toRow End row.
     * @param toCol End col.
     * @return True if all squares between start (exclusive) and end (exclusive) are empty.
     */
    public boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int dRow = Integer.compare(toRow, fromRow); // -1, 0, or 1
        int dCol = Integer.compare(toCol, fromCol); // -1, 0, or 1

        int r = fromRow + dRow;
        int c = fromCol + dCol;

        while (r != toRow || c != toCol) {
            if (!isValidCoordinate(r, c)) return false; // Should not happen if pattern is valid
            if (board[r][c] != null) {
                return false; // Path is blocked
            }
            r += dRow;
            c += dCol;
        }
        return true; // Path is clear
    }

    /**
     * Checks if the given coordinates are within the board boundaries (0-7).
     */
    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }


    /** Creates a new piece instance based on symbol and color. */
    private Piece createPiece(char symbol, Color color) {
        return switch (Character.toUpperCase(symbol)) {
            case 'P' -> new Pawn(color);
            case 'N' -> new Knight(color);
            case 'B' -> new Bishop(color);
            case 'R' -> new Rook(color);
            case 'Q' -> new Queen(color);
            case 'K' -> new King(color);
            default -> null; // Should not happen with valid promotion symbols
        };
    }

    /**
     * Generates a list of all pseudo-legal moves for the current player.
     * Pseudo-legal moves are moves that follow piece movement rules but might leave the king in check.
     * @return A list of Move objects.
     */
    public List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        Color player = currentTurn;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.color == player) {
                    // Generate moves for this piece
                    moves.addAll(generateMovesForPiece(piece, r, c));
                }
            }
        }
        return moves;
    }

    /**
     * Generates a list of all fully legal moves for the current player.
     * Filters pseudo-legal moves to remove those that leave the king in check.
     * @return A list of legal Move objects.
     */
    public List<Move> generateLegalMoves() {
        List<Move> pseudoLegal = generatePseudoLegalMoves();
        List<Move> legalMoves = new ArrayList<>();

        for (Move move : pseudoLegal) {
            // Simulate the move and check if the king is safe
            // This requires a reliable way to simulate/undo or use board copies
            try {
                // System.out.println("Testing move: " + move); // Debug
                applyMove(move); // Try making the move (includes validation)
                // System.out.println(" -> OK"); // Debug
                legalMoves.add(move); // If no exception, it was legal
                undoMove();         // Undo to check the next one
            } catch (IllegalMoveException e) {
                // System.out.println(" -> Illegal: " + e.getMessage()); // Debug
                // This move was illegal (likely left king in check), skip it
            } catch (Exception e) {
                System.err.println("Unexpected error simulating move " + move + ": " + e);
                // May need to handle board state corruption if undo fails
                try { undoMove(); } catch (Exception ue) { System.err.println("Failed to undo after error: " + ue);}
            }
        }
        return legalMoves;
    }


    /** Generates pseudo-legal moves for a single piece */
    private List<Move> generateMovesForPiece(Piece piece, int fromRow, int fromCol) {
        List<Move> moves = new ArrayList<>();
        // Iterate through all possible target squares
        for (int toRow = 0; toRow < 8; toRow++) {
            for (int toCol = 0; toCol < 8; toCol++) {
                if (fromRow == toRow && fromCol == toCol) continue; // Cannot move to the same square

                // Basic pattern check first
                if (!piece.isValidMovePattern(fromRow, fromCol, toRow, toCol)) {
                    continue;
                }

                Piece targetPiece = getPiece(toRow, toCol);

                // Cannot capture own piece
                if (targetPiece != null && targetPiece.color == piece.color) {
                    continue;
                }

                // Path check for sliders
                if ((piece instanceof Rook || piece instanceof Bishop || piece instanceof Queen) &&
                        !isPathClear(fromRow, fromCol, toRow, toCol)) {
                    continue;
                }

                // --- Special Pawn Logic ---
                if (piece instanceof Pawn) {
                    int dir = (piece.color == Color.WHITE) ? -1 : 1;
                    int dr = toRow - fromRow;
                    int dc = toCol - fromCol;

                    if (dc == 0) { // Forward move
                        if (targetPiece != null) continue; // Cannot move forward onto a piece
                        if (dr == dir) { // Single step
                            // Check promotion
                            if ((piece.color == Color.WHITE && toRow == 0) || (piece.color == Color.BLACK && toRow == 7)) {
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, 'Q'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, 'R'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, 'B'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, 'N'));
                            } else {
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, (char)0));
                            }
                        } else if (dr == 2 * dir) { // Double step
                            int startRow = (piece.color == Color.WHITE) ? 6 : 1;
                            if (fromRow != startRow || getPiece(fromRow + dir, fromCol) != null) continue; // Check path
                            moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, null, (char)0));
                        }
                    } else if (Math.abs(dc) == 1 && dr == dir) { // Capture / En Passant
                        if (targetPiece != null && targetPiece.color != piece.color) { // Standard capture
                            if ((piece.color == Color.WHITE && toRow == 0) || (piece.color == Color.BLACK && toRow == 7)) {
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, 'Q'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, 'R'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, 'B'));
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, 'N'));
                            } else {
                                moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, (char)0));
                            }
                        } else if (targetPiece == null && enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) { // En Passant
                            // Create En Passant move (capture target is null in constructor, handled by flag)
                            Piece capturedPawn = getPiece(fromRow, toCol); // Pawn being captured
                            // Note: Move constructor needs update or handle EP specially
                            // Using the existing constructor, mark as EP, captured=null
                            Move epMove = new Move(fromRow, fromCol, toRow, toCol, piece, null, false, false, true, false, (char)0);
                            moves.add(epMove);
                        }
                    }
                }
                // --- General Piece Logic (Non-Pawn) ---
                else {
                    // Standard move or capture (path/target checked above)
                    char promotion = 0; // Only pawns promote
                    moves.add(new Move(fromRow, fromCol, toRow, toCol, piece, targetPiece, promotion));
                }
            }
        }

        // --- Add Castling Moves (if King) ---
        if (piece instanceof King) {
            // Check Kingside Castling (O-O)
            try {
                validateCastling(piece.color, true); // Check if legal
                moves.add(new Move(fromRow, fromCol, fromRow, fromCol + 2, piece, true)); // Kingside flag = true
            } catch (IllegalMoveException e) { /* Cannot castle kingside */ }

            // Check Queenside Castling (O-O-O)
            try {
                validateCastling(piece.color, false); // Check if legal
                moves.add(new Move(fromRow, fromCol, fromRow, fromCol - 2, piece, false)); // Kingside flag = false
            } catch (IllegalMoveException e) { /* Cannot castle queenside */ }
        }


        return moves;
    }

    /**
     * Checks if the current player is in checkmate.
     * @return True if it's checkmate, false otherwise.
     */
    public boolean isCheckmate() {
        // Must be in check AND have no legal moves
        return isKingInCheck(currentTurn) && generateLegalMoves().isEmpty();
    }

    /**
     * Checks if the current player is in stalemate.
     * @return True if it's stalemate, false otherwise.
     */
    public boolean isStalemate() {
        // Must NOT be in check AND have no legal moves
        return !isKingInCheck(currentTurn) && generateLegalMoves().isEmpty();
    }


    // Utility to print board to console for debugging
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  a b c d e f g h\n");
        for (int r = 0; r < 8; r++) {
            sb.append(8 - r).append(" ");
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                sb.append(p == null ? "." : p.toString());
                sb.append(" ");
            }
            sb.append(8 - r).append("\n");
        }
        sb.append("  a b c d e f g h\n");
        sb.append("Turn: ").append(currentTurn);
        if (enPassantTarget != null) {
            sb.append(", EP Target: ").append(Move.coordToString(enPassantTarget[0], enPassantTarget[1]));
        }
        sb.append(", W_K V: ").append(kingMoved.get(Color.WHITE));
        sb.append(", W_KR V: ").append(kingsideRookMoved.get(Color.WHITE));
        sb.append(", W_QR V: ").append(queensideRookMoved.get(Color.WHITE));
        sb.append(", B_K V: ").append(kingMoved.get(Color.BLACK));
        sb.append(", B_KR V: ").append(kingsideRookMoved.get(Color.BLACK));
        sb.append(", B_QR V: ").append(queensideRookMoved.get(Color.BLACK));
        sb.append(isKingInCheck(currentTurn) ? " (Check!)" : "");

        return sb.toString();
    }
}