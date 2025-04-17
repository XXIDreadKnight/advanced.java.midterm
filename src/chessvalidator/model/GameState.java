package chessvalidator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameState {
    private Board board;
    private Color currentPlayer;
    boolean whiteCanCastleKingSide;
    private boolean whiteCanCastleQueenSide;
    private boolean blackCanCastleKingSide;
    private boolean blackCanCastleQueenSide;
    private Square enPassantTargetSquare; // Square *behind* the pawn that just moved two steps
    private int halfMoveClock; // For 50-move rule (optional for basic validation)
    private int fullMoveNumber;

    public GameState() {
        board = new Board();
        board.setupInitialPosition();
        currentPlayer = Color.WHITE;
        whiteCanCastleKingSide = true;
        whiteCanCastleQueenSide = true;
        blackCanCastleKingSide = true;
        blackCanCastleQueenSide = true;
        enPassantTargetSquare = null;
        halfMoveClock = 0;
        fullMoveNumber = 1;
    }

    /**
     * Clears the current state and loads a new state from a FEN string.
     * @param fenString The FEN string (e.g., "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").
     * @throws IllegalArgumentException if the FEN string is invalid.
     */
    public void loadFromFen(String fenString) throws IllegalArgumentException {
        if (fenString == null || fenString.isBlank()) {
            throw new IllegalArgumentException("FEN string cannot be null or empty.");
        }

        String[] parts = fenString.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid FEN string: Expected 6 parts, found " + parts.length + " in '" + fenString + "'");
        }

        // Reset board and state variables before loading
        this.board = new Board(); // Create a new empty board
        this.whiteCanCastleKingSide = false;
        this.whiteCanCastleQueenSide = false;
        this.blackCanCastleKingSide = false;
        this.blackCanCastleQueenSide = false;
        this.enPassantTargetSquare = null;
        this.halfMoveClock = 0;
        this.fullMoveNumber = 1; // Default, will be overridden

        try {
            // Part 1: Piece Placement
            parseFenPiecePlacement(parts[0]);

            // Part 2: Active Color
            parseFenActiveColor(parts[1]);

            // Part 3: Castling Availability
            parseFenCastling(parts[2]);

            // Part 4: En Passant Target Square
            parseFenEnPassant(parts[3]);

            // Part 5: Halfmove Clock
            this.halfMoveClock = Integer.parseInt(parts[4]);
            if (this.halfMoveClock < 0) throw new NumberFormatException("Halfmove clock cannot be negative.");

            // Part 6: Fullmove Number
            this.fullMoveNumber = Integer.parseInt(parts[5]);
            if (this.fullMoveNumber < 1) throw new NumberFormatException("Fullmove number must be >= 1.");

        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            // Catch specific errors during parsing (IllegalArgumentException covers NumberFormatException)
            throw new IllegalArgumentException("Invalid FEN string: Error parsing part. Details: " + e.getMessage() + " in '" + fenString + "'", e);
        } catch (Exception e) {
            // Catch any other unexpected errors
            throw new IllegalArgumentException("Unexpected error parsing FEN string: '" + fenString + "'", e);
        }
    }

    private void parseFenPiecePlacement(String placementPart) throws IllegalArgumentException {
        String[] ranks = placementPart.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN piece placement requires 8 ranks separated by '/', found " + ranks.length);
        }

        for (int rankIndex = 0; rankIndex < 8; rankIndex++) {
            int boardRow = 7 - rankIndex; // FEN ranks are 8 down to 1, board rows are 0 up to 7
            String rankStr = ranks[rankIndex];
            int boardCol = 0;
            for (char c : rankStr.toCharArray()) {
                if (boardCol >= 8) {
                    throw new IllegalArgumentException("FEN rank " + (8 - rankIndex) + " ('" + rankStr + "') has too many items.");
                }
                if (Character.isDigit(c)) {
                    int emptySquares = Character.getNumericValue(c);
                    if (emptySquares < 1 || emptySquares > 8) {
                        throw new IllegalArgumentException("Invalid FEN digit '" + c + "' in rank " + (8 - rankIndex));
                    }
                    // Place nulls for empty squares
                    for (int i = 0; i < emptySquares; i++) {
                        if (boardCol >= 8) throw new IllegalArgumentException("FEN rank " + (8 - rankIndex) + " adds up beyond 8 columns.");
                        board.setPiece(new Square(boardRow, boardCol), null);
                        boardCol++;
                    }
                } else {
                    Piece piece = fenCharToPiece(c);
                    if (piece == null) {
                        throw new IllegalArgumentException("Invalid character '" + c + "' in FEN piece placement.");
                    }
                    board.setPiece(new Square(boardRow, boardCol), piece);
                    boardCol++;
                }
            }
            if (boardCol != 8) {
                throw new IllegalArgumentException("FEN rank " + (8 - rankIndex) + " ('" + rankStr + "') does not add up to 8 columns (ended at " + boardCol + ").");
            }
        }
    }

    private Piece fenCharToPiece(char c) {
        Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
        PieceType type = switch (Character.toLowerCase(c)) {
            case 'p' -> PieceType.PAWN;
            case 'r' -> PieceType.ROOK;
            case 'n' -> PieceType.KNIGHT;
            case 'b' -> PieceType.BISHOP;
            case 'q' -> PieceType.QUEEN;
            case 'k' -> PieceType.KING;
            default -> null;
        };
        return (type == null) ? null : new Piece(type, color);
    }

    private void parseFenActiveColor(String colorPart) throws IllegalArgumentException {
        if ("w".equalsIgnoreCase(colorPart)) {
            this.currentPlayer = Color.WHITE;
        } else if ("b".equalsIgnoreCase(colorPart)) {
            this.currentPlayer = Color.BLACK;
        } else {
            throw new IllegalArgumentException("Invalid active color in FEN: '" + colorPart + "' (expected 'w' or 'b').");
        }
    }

    private void parseFenCastling(String castlingPart) throws IllegalArgumentException {
        if ("-".equals(castlingPart)) {
            return; // All flags remain false
        }
        for (char c : castlingPart.toCharArray()) {
            switch (c) {
                case 'K': this.whiteCanCastleKingSide = true; break;
                case 'Q': this.whiteCanCastleQueenSide = true; break;
                case 'k': this.blackCanCastleKingSide = true; break;
                case 'q': this.blackCanCastleQueenSide = true; break;
                default: throw new IllegalArgumentException("Invalid character '" + c + "' in FEN castling availability.");
            }
        }
    }

    private void parseFenEnPassant(String epPart) throws IllegalArgumentException {
        if (!"-".equals(epPart)) {
            Square epSquare = Square.fromAlgebraic(epPart);
            if (epSquare == null || !epSquare.isValid()) {
                throw new IllegalArgumentException("Invalid en passant target square in FEN: '" + epPart + "'.");
            }
            // Basic validation: EP square must be on rank 3 or 6
            if (epSquare.row() != 2 && epSquare.row() != 5) {
                throw new IllegalArgumentException("Invalid en passant target square rank in FEN: '" + epPart + "' (must be rank 3 or 6).");
            }
            // More validation could be added (e.g., is there actually an enemy pawn that could capture it?)
            this.enPassantTargetSquare = epSquare;
        } else {
            this.enPassantTargetSquare = null;
        }
    }

    // --- Getters ---
    public Board getBoard() { return board; }
    public Color getCurrentPlayer() { return currentPlayer; }
    public boolean canCastleKingSide(Color color) { return color == Color.WHITE ? whiteCanCastleKingSide : blackCanCastleKingSide; }
    public boolean canCastleQueenSide(Color color) { return color == Color.WHITE ? whiteCanCastleQueenSide : blackCanCastleQueenSide; }
    public Square getEnPassantTargetSquare() { return enPassantTargetSquare; }
    public int getFullMoveNumber() { return fullMoveNumber; }
    public int getHalfMoveClock() { return halfMoveClock; }


    // --- Core Logic ---

    /**
     * Applies a *legal* move to the board and updates game state.
     * Does minimal validation, assumes the move comes from generateLegalMoves or similar.
     * @param move The move to apply.
     */
    public void applyMove(Move move) {
        Board nextBoard = new Board(this.board); // Work on a copy
        Piece movingPiece = nextBoard.getPiece(move.from());

        // 1. Handle special moves first (Castling, En Passant, Promotion)
        if (move.isCastleKingside()) {
            int rank = (currentPlayer == Color.WHITE) ? 0 : 7;
            nextBoard.movePiece(new Square(rank, 4), new Square(rank, 6)); // King
            nextBoard.movePiece(new Square(rank, 7), new Square(rank, 5)); // Rook
        } else if (move.isCastleQueenside()) {
            int rank = (currentPlayer == Color.WHITE) ? 0 : 7;
            nextBoard.movePiece(new Square(rank, 4), new Square(rank, 2)); // King
            nextBoard.movePiece(new Square(rank, 0), new Square(rank, 3)); // Rook
        } else if (move.isEnPassantCapture()) {
            // Move the pawn
            nextBoard.movePiece(move.from(), move.to());
            // Remove the captured pawn (which is on the en passant target rank, same file as 'to')
            int capturedPawnRow = (currentPlayer == Color.WHITE) ? move.to().row() - 1 : move.to().row() + 1;
            Square capturedPawnSquare = new Square(capturedPawnRow, move.to().col());
            nextBoard.setPiece(capturedPawnSquare, null);
        } else {
            // 2. Regular move or promotion
            nextBoard.movePiece(move.from(), move.to());
            // Handle promotion
            if (move.isPromotion()) {
                nextBoard.setPiece(move.to(), new Piece(move.promotionPieceType(), currentPlayer));
            }
        }

        // --- Update State Variables ---
        this.board = nextBoard; // Commit the changes

        // Update castling rights (if king or rook moves)
        if (movingPiece.type() == PieceType.KING) {
            if (currentPlayer == Color.WHITE) {
                whiteCanCastleKingSide = false;
                whiteCanCastleQueenSide = false;
            } else {
                blackCanCastleKingSide = false;
                blackCanCastleQueenSide = false;
            }
        } else if (movingPiece.type() == PieceType.ROOK) {
            int homeRank = (currentPlayer == Color.WHITE) ? 0 : 7;
            if (move.from().row() == homeRank) {
                if (move.from().col() == 0) { // Queen side rook
                    if (currentPlayer == Color.WHITE) whiteCanCastleQueenSide = false; else blackCanCastleQueenSide = false;
                } else if (move.from().col() == 7) { // King side rook
                    if (currentPlayer == Color.WHITE) whiteCanCastleKingSide = false; else blackCanCastleKingSide = false;
                }
            }
        }
        // Also revoke rights if opponent's rook is captured on its starting square
        if (move.pieceCaptured() != null && move.pieceCaptured().type() == PieceType.ROOK) {
            int capturedHomeRank = (currentPlayer.opposite() == Color.WHITE) ? 0 : 7;
            if (move.to().row() == capturedHomeRank) {
                if (move.to().col() == 0) { // Queen side rook captured
                    if (currentPlayer.opposite() == Color.WHITE) whiteCanCastleQueenSide = false; else blackCanCastleQueenSide = false;
                } else if (move.to().col() == 7) { // King side rook captured
                    if (currentPlayer.opposite() == Color.WHITE) whiteCanCastleKingSide = false; else blackCanCastleKingSide = false;
                }
            }
        }


        // Update en passant target square
        // Set if a pawn moved two squares, clear otherwise
        if (movingPiece.type() == PieceType.PAWN && Math.abs(move.from().row() - move.to().row()) == 2) {
            int epRank = (currentPlayer == Color.WHITE) ? move.from().row() + 1 : move.from().row() - 1;
            enPassantTargetSquare = new Square(epRank, move.from().col());
        } else {
            enPassantTargetSquare = null;
        }

        // Update clocks (basic implementation)
        if (movingPiece.type() == PieceType.PAWN || move.isCapture()) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        if (currentPlayer == Color.BLACK) {
            fullMoveNumber++;
        }

        // Switch player
        currentPlayer = currentPlayer.opposite();
    }


    /**
     * Generates all pseudo-legal moves for the current player.
     * Pseudo-legal means they follow piece movement rules but might leave the king in check.
     * @return List of pseudo-legal moves.
     */
    public List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square from = new Square(r, c);
                Piece piece = board.getPiece(from);
                if (piece != null && piece.color() == currentPlayer) {
                    addPieceMoves(moves, piece, from);
                }
            }
        }
        // Add castling moves (pseudo-legal check for empty squares and not being in check *initially*)
        addCastlingMoves(moves);
        return moves;
    }

    /**
     * Generates all strictly legal moves for the current player.
     * Filters pseudo-legal moves to ensure the king is not left in check.
     * @return List of legal moves.
     */
    public List<Move> generateLegalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves();
        Square kingSquare = board.findKing(currentPlayer);

        if (kingSquare == null) {
            System.err.println("WARNING: King not found for " + currentPlayer + ". Cannot generate legal moves.");
            return legalMoves; // Or throw? Indicates an invalid state.
        }


        for (Move move : pseudoLegalMoves) {
            // Simulate the move on a temporary board
            GameState nextState = this.copy(); // Create a deep copy
            nextState.applyMove(move); // Apply the move (updates the temp state's board, turn etc.)

            // Check if the current player's king is in check *after* the move
            // Note: applyMove switches the player, so we check the *original* player's king
            Square potentiallyNewKingSquare = move.pieceMoved().type() == PieceType.KING ? move.to() : nextState.board.findKing(currentPlayer);
            if (potentiallyNewKingSquare == null) {
                System.err.println("WARNING: King disappeared after move " + move + ". Skipping.");
                continue;
            }

            if (!nextState.isSquareAttacked(potentiallyNewKingSquare, currentPlayer.opposite())) {
                legalMoves.add(move); // Move is legal if the king is not attacked
            }
        }
        return legalMoves;
    }

    // --- Helper methods for move generation ---

    private void addPieceMoves(List<Move> moves, Piece piece, Square from) {
        switch (piece.type()) {
            case PAWN: addPawnMoves(moves, from); break;
            case ROOK: addSlidingMoves(moves, from, piece, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}); break; // Vertical/Horizontal
            case KNIGHT: addKnightMoves(moves, from, piece); break;
            case BISHOP: addSlidingMoves(moves, from, piece, new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}); break; // Diagonal
            case QUEEN: addSlidingMoves(moves, from, piece, new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}); break; // Both
            case KING: addKingMoves(moves, from, piece); break;
        }
    }

    private void addPawnMoves(List<Move> moves, Square from) {
        Piece pawn = board.getPiece(from);
        Color color = pawn.color();
        int direction = (color == Color.WHITE) ? 1 : -1;
        int startRank = (color == Color.WHITE) ? 1 : 6;
        int promotionRank = (color == Color.WHITE) ? 7 : 0;

        // 1. Single step forward
        Square oneStep = new Square(from.row() + direction, from.col());
        if (oneStep.isValid() && board.getPiece(oneStep) == null) {
            if (oneStep.row() == promotionRank) {
                addPromotionMoves(moves, from, oneStep, pawn, null);
            } else {
                moves.add(new Move(from, oneStep, pawn, null));
            }

            // 2. Double step forward (only from starting rank and if one step is clear)
            if (from.row() == startRank) {
                Square twoSteps = new Square(from.row() + 2 * direction, from.col());
                if (twoSteps.isValid() && board.getPiece(twoSteps) == null) {
                    moves.add(new Move(from, twoSteps, pawn, null));
                }
            }
        }

        // 3. Captures (diagonal)
        int[] captureCols = {from.col() - 1, from.col() + 1};
        for (int captureCol : captureCols) {
            Square target = new Square(from.row() + direction, captureCol);
            if (target.isValid()) {
                Piece capturedPiece = board.getPiece(target);
                // Regular capture
                if (capturedPiece != null && capturedPiece.color() != color) {
                    if (target.row() == promotionRank) {
                        addPromotionMoves(moves, from, target, pawn, capturedPiece);
                    } else {
                        moves.add(new Move(from, target, pawn, capturedPiece));
                    }
                }
                // En Passant capture
                else if (target.equals(enPassantTargetSquare) && capturedPiece == null) {
                    int capturedPawnRow = from.row(); // Pawn being captured is beside the moving pawn
                    Square capturedPawnSquare = new Square(capturedPawnRow, target.col());
                    Piece enPassantCaptured = board.getPiece(capturedPawnSquare);
                    if (enPassantCaptured != null && enPassantCaptured.type() == PieceType.PAWN && enPassantCaptured.color() != color) {
                        moves.add(new Move(from, target, pawn, enPassantCaptured, null, false, false, true));
                    }
                }
            }
        }
    }

    private void addPromotionMoves(List<Move> moves, Square from, Square to, Piece pawn, Piece captured) {
        moves.add(new Move(from, to, pawn, captured, PieceType.QUEEN));
        moves.add(new Move(from, to, pawn, captured, PieceType.ROOK));
        moves.add(new Move(from, to, pawn, captured, PieceType.BISHOP));
        moves.add(new Move(from, to, pawn, captured, PieceType.KNIGHT));
    }


    private void addSlidingMoves(List<Move> moves, Square from, Piece piece, int[][] directions) {
        for (int[] d : directions) {
            int dr = d[0];
            int dc = d[1];
            for (int i = 1; ; i++) {
                Square to = new Square(from.row() + i * dr, from.col() + i * dc);
                if (!to.isValid()) break; // Off board

                Piece targetPiece = board.getPiece(to);
                if (targetPiece == null) {
                    moves.add(new Move(from, to, piece, null)); // Empty square
                } else {
                    if (targetPiece.color() != piece.color()) {
                        moves.add(new Move(from, to, piece, targetPiece)); // Capture opponent
                    }
                    break; // Blocked by own or opponent piece
                }
            }
        }
    }

    private void addKnightMoves(List<Move> moves, Square from, Piece knight) {
        int[] dr = {-2, -2, -1, -1, 1, 1, 2, 2};
        int[] dc = {-1, 1, -2, 2, -2, 2, -1, 1};

        for (int i = 0; i < 8; i++) {
            Square to = new Square(from.row() + dr[i], from.col() + dc[i]);
            if (to.isValid()) {
                Piece targetPiece = board.getPiece(to);
                if (targetPiece == null || targetPiece.color() != knight.color()) {
                    moves.add(new Move(from, to, knight, targetPiece));
                }
            }
        }
    }

    private void addKingMoves(List<Move> moves, Square from, Piece king) {
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            Square to = new Square(from.row() + dr[i], from.col() + dc[i]);
            if (to.isValid()) {
                Piece targetPiece = board.getPiece(to);
                if (targetPiece == null || targetPiece.color() != king.color()) {
                    // Note: Legality check (moving into check) is done in generateLegalMoves
                    moves.add(new Move(from, to, king, targetPiece));
                }
            }
        }
        // Castling moves are added separately in generatePseudoLegalMoves/addCastlingMoves
    }

    private void addCastlingMoves(List<Move> moves) {
        if (isSquareAttacked(board.findKing(currentPlayer), currentPlayer.opposite())) {
            return; // Cannot castle out of check
        }

        int rank = (currentPlayer == Color.WHITE) ? 0 : 7;
        Square kingPos = new Square(rank, 4);
        Piece king = board.getPiece(kingPos); // Should be the king

        // Kingside Castling (O-O)
        if (canCastleKingSide(currentPlayer)) {
            Square rookPos = new Square(rank, 7);
            Square fSquare = new Square(rank, 5); // f1 or f8
            Square gSquare = new Square(rank, 6); // g1 or g8
            if (board.getPiece(rookPos) != null && board.getPiece(rookPos).type() == PieceType.ROOK &&
                    board.getPiece(fSquare) == null && board.getPiece(gSquare) == null &&
                    !isSquareAttacked(fSquare, currentPlayer.opposite()) &&
                    !isSquareAttacked(gSquare, currentPlayer.opposite()))
            {
                moves.add(new Move(kingPos, gSquare, king, null, null, true, false, false));
            }
        }

        // Queenside Castling (O-O-O)
        if (canCastleQueenSide(currentPlayer)) {
            Square rookPos = new Square(rank, 0);
            Square bSquare = new Square(rank, 1); // b1 or b8
            Square cSquare = new Square(rank, 2); // c1 or c8
            Square dSquare = new Square(rank, 3); // d1 or d8
            if (board.getPiece(rookPos) != null && board.getPiece(rookPos).type() == PieceType.ROOK &&
                    board.getPiece(bSquare) == null && board.getPiece(cSquare) == null && board.getPiece(dSquare) == null &&
                    !isSquareAttacked(cSquare, currentPlayer.opposite()) &&
                    !isSquareAttacked(dSquare, currentPlayer.opposite()))
            {
                moves.add(new Move(kingPos, cSquare, king, null, null, false, true, false));
            }
        }
    }

    /**
     * Checks if a given square is attacked by the opponent.
     * @param targetSquare The square to check.
     * @param attackerColor The color of the pieces potentially attacking.
     * @return True if the square is attacked, false otherwise.
     */
    public boolean isSquareAttacked(Square targetSquare, Color attackerColor) {
        if (targetSquare == null) return false; // Or maybe throw?

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square attackerSquare = new Square(r, c);
                Piece attacker = board.getPiece(attackerSquare);
                if (attacker != null && attacker.color() == attackerColor) {
                    // Generate pseudo-legal moves for the attacker *as if* it's their turn
                    // This is slightly inefficient but conceptually simpler than reverse checks
                    List<Move> attackerMoves = new ArrayList<>();
                    // Temporarily switch player context for move generation? No, just check piece rules directly.
                    if (isAttacking(attacker, attackerSquare, targetSquare)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Helper for isSquareAttacked - checks if a piece at 'from' attacks 'to' based *only* on movement rules
    // Ignores whose turn it is, ignores pins, etc. Just raw attack vectors.
    private boolean isAttacking(Piece attacker, Square from, Square to) {
        if (attacker == null || from.equals(to)) return false;

        int dr = to.row() - from.row();
        int dc = to.col() - from.col();

        return switch (attacker.type()) {
            case PAWN -> {
                int direction = (attacker.color() == Color.WHITE) ? 1 : -1;
                yield dr == direction && Math.abs(dc) == 1;
            }
            case KNIGHT -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case BISHOP -> {
                if (Math.abs(dr) != Math.abs(dc)) yield false;
                yield isPathClear(from, to, dr, dc); // Not diagonal
            }
            case ROOK -> {
                if (dr != 0 && dc != 0) yield false;
                yield isPathClear(from, to, dr, dc); // Not horizontal/vertical
            }
            case QUEEN -> {
                if (Math.abs(dr) == Math.abs(dc) || dr == 0 || dc == 0) { // Diagonal or Straight
                    yield isPathClear(from, to, dr, dc);
                }
                yield false;
            }
            case KING -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1; // Adjacent square
        };
    }

    // Helper for sliding pieces (Rook, Bishop, Queen) attack check
    private boolean isPathClear(Square from, Square to, int dr, int dc) {
        int stepR = Integer.signum(dr);
        int stepC = Integer.signum(dc);
        int steps = Math.max(Math.abs(dr), Math.abs(dc));

        // Check squares between 'from' and 'to'
        for (int i = 1; i < steps; i++) {
            Square intermediate = new Square(from.row() + i * stepR, from.col() + i * stepC);
            if (board.getPiece(intermediate) != null) {
                return false; // Path is blocked
            }
        }
        return true; // Path is clear up to the target square
    }


    /** Checks if the current player is in check. */
    public boolean isInCheck() {
        Square kingSquare = board.findKing(currentPlayer);
        if (kingSquare == null) return false; // Should not happen
        return isSquareAttacked(kingSquare, currentPlayer.opposite());
    }

    /** Checks if the current player is checkmated. */
    public boolean isCheckmate() {
        return isInCheck() && generateLegalMoves().isEmpty();
    }

    /** Checks if the current player is stalemated. */
    public boolean isStalemate() {
        return !isInCheck() && generateLegalMoves().isEmpty();
    }

    /** Creates a deep copy of the game state. */
    public GameState copy() {
        GameState copy = new GameState(); // Creates new board etc.
        copy.board = new Board(this.board); // Deep copy the board
        copy.currentPlayer = this.currentPlayer;
        copy.whiteCanCastleKingSide = this.whiteCanCastleKingSide;
        copy.whiteCanCastleQueenSide = this.whiteCanCastleQueenSide;
        copy.blackCanCastleKingSide = this.blackCanCastleKingSide;
        copy.blackCanCastleQueenSide = this.blackCanCastleQueenSide;
        copy.enPassantTargetSquare = this.enPassantTargetSquare; // Square is immutable record
        copy.halfMoveClock = this.halfMoveClock;
        copy.fullMoveNumber = this.fullMoveNumber;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        // Basic equality check - useful for testing repetitions maybe?
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState gameState = (GameState) o;
        // Note: Board equality needs careful implementation if Piece/Square don't have good equals/hashCode
        // For now, assume Board.toString() comparison or manual check if needed.
        // Here we compare critical state components.
        return whiteCanCastleKingSide == gameState.whiteCanCastleKingSide &&
                whiteCanCastleQueenSide == gameState.whiteCanCastleQueenSide &&
                blackCanCastleKingSide == gameState.blackCanCastleKingSide &&
                blackCanCastleQueenSide == gameState.blackCanCastleQueenSide &&
                halfMoveClock == gameState.halfMoveClock &&
                fullMoveNumber == gameState.fullMoveNumber &&
                currentPlayer == gameState.currentPlayer &&
                Objects.equals(board.toString(), gameState.board.toString()) && // Simple board comparison
                Objects.equals(enPassantTargetSquare, gameState.enPassantTargetSquare);
    }

    @Override
    public int hashCode() {
        // Hash based on critical state components
        return Objects.hash(board.toString(), currentPlayer, whiteCanCastleKingSide, whiteCanCastleQueenSide, blackCanCastleKingSide, blackCanCastleQueenSide, enPassantTargetSquare, halfMoveClock, fullMoveNumber);
    }
}