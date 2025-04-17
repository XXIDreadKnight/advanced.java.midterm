package chessvalidator.model; // In the 'test/chessvalidator/model' folder

// Static import for assertion methods from the runner
import static chessvalidator.ManualTestRunner.*;

// Import classes being tested or used in tests
import chessvalidator.model.*;

import java.util.List;
import java.util.Optional;

public class GameStateManualTest {

    private GameState gameState;

    // Call this before each logical test group in the runner if needed
    public void setup() {
        gameState = new GameState(); // Reset to initial position
    }

    // --- Test Methods (Previous tests are mostly okay, focus on the ones needing changes) ---

    public void testInitialPosition() {
        assertEquals(Color.WHITE, gameState.getCurrentPlayer(), "Initial player should be White");
        assertEquals(1, gameState.getFullMoveNumber(), "Initial move number should be 1");
        assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e2")), "e2 should have a piece initially");
        Piece e2Piece = gameState.getBoard().getPiece(Square.fromAlgebraic("e2"));
        assertEquals(PieceType.PAWN, e2Piece.type(), "e2 piece should be Pawn");
        assertEquals(Color.WHITE, e2Piece.color(), "e2 piece color should be White");
        Piece e1Piece = gameState.getBoard().getPiece(Square.fromAlgebraic("e1"));
        assertNotNull(e1Piece, "e1 should have a piece initially");
        assertEquals(Color.WHITE, e1Piece.color(), "e1 color should be White");
        assertEquals(PieceType.KING, e1Piece.type(), "e1 piece should be King");
        assertTrue(gameState.canCastleKingSide(Color.WHITE), "White initial K-side castle OK");
        assertTrue(gameState.canCastleQueenSide(Color.WHITE), "White initial Q-side castle OK");
        assertTrue(gameState.canCastleKingSide(Color.BLACK), "Black initial K-side castle OK");
        assertTrue(gameState.canCastleQueenSide(Color.BLACK), "Black initial Q-side castle OK");
        assertNull(gameState.getEnPassantTargetSquare(), "Initial EP target should be null");
    }

    public void testSimplePawnMove() {
        try {
            Move move = SanHelper.sanToMove("e4", gameState);
            assertNotNull(move, "Move e4 should be found");
            gameState.applyMove(move);

            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e2")), "e2 should be empty after e4");
            assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e4")), "e4 should have a piece after e4");
            Piece movedPawn = gameState.getBoard().getPiece(Square.fromAlgebraic("e4"));
            assertEquals(PieceType.PAWN, movedPawn.type(), "e4 piece type after e4");
            assertEquals(Color.WHITE, movedPawn.color(), "e4 piece color after e4");
            assertEquals(Color.BLACK, gameState.getCurrentPlayer(), "Player should be Black after White's move");
            assertEquals(1, gameState.getFullMoveNumber(), "Move number still 1 after White's first move");
            assertNotNull(gameState.getEnPassantTargetSquare(), "EP target should exist after e4");
            assertEquals(Square.fromAlgebraic("e3"), gameState.getEnPassantTargetSquare(), "EP target square should be e3");
        } catch (Exception e) {
            failTest("testSimplePawnMove failed unexpectedly: " + e.getMessage());
        }
    }

    public void testKnightMove() {
        try {
            Move move = SanHelper.sanToMove("Nf3", gameState);
            assertNotNull(move, "Move Nf3 should be found");
            gameState.applyMove(move);

            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("g1")), "g1 empty after Nf3");
            assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f3")), "f3 occupied after Nf3");
            Piece movedKnight = gameState.getBoard().getPiece(Square.fromAlgebraic("f3"));
            assertEquals(PieceType.KNIGHT, movedKnight.type(), "f3 piece type after Nf3");
            assertEquals(Color.WHITE, movedKnight.color(), "f3 piece color after Nf3");
            assertEquals(Color.BLACK, gameState.getCurrentPlayer(), "Player is Black after Nf3");
            assertNull(gameState.getEnPassantTargetSquare(), "EP target null after Nf3");
        } catch (Exception e) {
            failTest("testKnightMove failed unexpectedly: " + e.getMessage());
        }
    }

    public void testKingsideCastlingWhiteValid() {
        // Setup position for castling
        gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);

        try {
            Move move = SanHelper.sanToMove("O-O", gameState);
            assertNotNull(move, "Castling move O-O should be found");
            assertTrue(move.isCastleKingside(), "Move should be kingside castle");
            gameState.applyMove(move);

            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e1")), "e1 empty after O-O");
            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("h1")), "h1 empty after O-O");
            Piece king = gameState.getBoard().getPiece(Square.fromAlgebraic("g1"));
            Piece rook = gameState.getBoard().getPiece(Square.fromAlgebraic("f1"));
            assertNotNull(king, "g1 has piece after O-O");
            assertNotNull(rook, "f1 has piece after O-O");
            assertEquals(PieceType.KING, king.type(), "g1 is King after O-O");
            assertEquals(Color.WHITE, king.color(), "g1 King color");
            assertEquals(PieceType.ROOK, rook.type(), "f1 is Rook after O-O");
            assertEquals(Color.WHITE, rook.color(), "f1 Rook color");
            assertFalse(gameState.canCastleKingSide(Color.WHITE), "White K-side castle right removed");
            assertFalse(gameState.canCastleQueenSide(Color.WHITE), "White Q-side castle right removed (king moved)");
        } catch (Exception e) {
            failTest("testKingsideCastlingWhiteValid failed unexpectedly: " + e.getMessage());
        }
    }

    public void testKingsideCastlingWhiteInvalidBlocked() {
        // Default position - f1 is blocked by knight
        assertTrue(gameState.getBoard().getPiece(Square.fromAlgebraic("f1")) != null, "f1 should be blocked initially");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("O-O", gameState),
                "Should throw when trying to castle through blocked square (f1)"
        );
    }

    public void testKingsideCastlingWhiteInvalidInCheck() {
        // Setup: Black queen giving check from h4
        gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("h4"), new Piece(PieceType.QUEEN, Color.BLACK));
        // Need king for black too for valid state during check detection
        if (gameState.getBoard().findKing(Color.BLACK) == null)
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), new Piece(PieceType.KING, Color.BLACK));

        assertTrue(gameState.isInCheck(), "King should be in check from h4"); // Check current player's status
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("O-O", gameState),
                "Should throw when trying to castle while in check"
        );
    }

    public void testKingsideCastlingWhiteInvalidThroughCheck() {
        // Setup: Black rook attacking f1
        gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("f8"), new Piece(PieceType.ROOK, Color.BLACK));
        // Need king for black too
        if (gameState.getBoard().findKing(Color.BLACK) == null)
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), new Piece(PieceType.KING, Color.BLACK));

        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("f1"), Color.BLACK), "f1 should be attacked by Rook on f8");
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("e1"), Color.BLACK), "e1 should not be attacked initially");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("O-O", gameState),
                "Should throw when trying to castle through an attacked square (f1)"
        );
    }


    public void testEnPassantSetupAndTarget() {
        try {
            // 1. e4 (White)
            gameState.applyMove(SanHelper.sanToMove("e4", gameState));
            assertNotNull(gameState.getEnPassantTargetSquare(), "EP target should exist after e4");
            assertEquals(Square.fromAlgebraic("e3"), gameState.getEnPassantTargetSquare(), "EP target square should be e3 after e4");

            // 1... Nc6 (Black - non-pawn move clears EP)
            gameState.applyMove(SanHelper.sanToMove("Nc6", gameState));
            assertNull(gameState.getEnPassantTargetSquare(), "EP target should be null after non-pawn move");

            // 2. d4 (White)
            gameState.applyMove(SanHelper.sanToMove("d4", gameState));
            assertNotNull(gameState.getEnPassantTargetSquare(), "EP target should exist after d4");
            assertEquals(Square.fromAlgebraic("d3"), gameState.getEnPassantTargetSquare(), "EP target square should be d3 after d4");

            // 2... e5 (Black - pawn move creates EP target e6 for white)
            gameState.applyMove(SanHelper.sanToMove("e5", gameState));
            assertNotNull(gameState.getEnPassantTargetSquare(), "EP target should exist after e5");
            assertEquals(Square.fromAlgebraic("e6"), gameState.getEnPassantTargetSquare(), "EP target square should be e6 after e5");

        } catch (Exception e) {
            failTest("testEnPassantSetupAndTarget failed unexpectedly: " + e.getMessage());
        }
    }

    public void testEnPassantCapture() {
        try {
            // 1. e4 d5 2. e5 f5 (Setup EP for white on f6)
            gameState.applyMove(SanHelper.sanToMove("e4", gameState));
            gameState.applyMove(SanHelper.sanToMove("d5", gameState));
            gameState.applyMove(SanHelper.sanToMove("e5", gameState));
            gameState.applyMove(SanHelper.sanToMove("f5", gameState));

            assertEquals(Square.fromAlgebraic("f6"), gameState.getEnPassantTargetSquare(), "EP target f6 is set");
            assertTrue(gameState.getBoard().getPiece(Square.fromAlgebraic("f5")) != null, "Black pawn exists on f5");
            assertTrue(gameState.getBoard().getPiece(Square.fromAlgebraic("e5")) != null, "White pawn exists on e5");

            // 3. exf6 e.p.
            Move epMove = SanHelper.sanToMove("exf6", gameState); // Find the move
            assertNotNull(epMove, "EP move exf6 should be found");
            assertTrue(epMove.isEnPassantCapture(), "Move exf6 should be en passant");
            assertEquals(Square.fromAlgebraic("e5"), epMove.from(), "EP move from square");
            assertEquals(Square.fromAlgebraic("f6"), epMove.to(), "EP move to square");
            assertNotNull(epMove.pieceCaptured(), "EP move should register a capture");
            assertEquals(PieceType.PAWN, epMove.pieceCaptured().type(), "EP captured piece type");
            assertEquals(Color.BLACK, epMove.pieceCaptured().color(), "EP captured piece color");

            gameState.applyMove(epMove); // Apply the move

            assertNotNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f6")), "White pawn should be on f6 after EP capture");
            assertEquals(Color.WHITE, gameState.getBoard().getPiece(Square.fromAlgebraic("f6")).color(), "Piece on f6 is white");
            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e5")), "e5 should be empty after EP capture");
            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("f5")), "f5 (captured pawn original square) should be empty");
            assertNull(gameState.getEnPassantTargetSquare(), "EP target should be cleared after capture");
            assertEquals(Color.BLACK, gameState.getCurrentPlayer(), "Player should be Black after EP capture");

        } catch (Exception e) {
            failTest("testEnPassantCapture failed unexpectedly: " + e.getMessage());
        }
    }

    public void testPromotion() {
        try {
            // Setup: White pawn on e7, Black King on a8 (avoid check issues)
            gameState = new GameState(); // Reset
            gameState.getBoard().setupInitialPosition(); // Start fresh board
            gameState.getBoard().setPiece(Square.fromAlgebraic("e7"), new Piece(PieceType.PAWN, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("e2"), null); // remove original pawn
            gameState.getBoard().setPiece(Square.fromAlgebraic("a8"), new Piece(PieceType.KING, Color.BLACK)); // put black king away
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), null); // Clear target square and remove original king
            gameState.getBoard().setPiece(Square.fromAlgebraic("d8"), null); // Clear other pieces if needed
            gameState.getBoard().setPiece(Square.fromAlgebraic("f8"), null);

            Move move = SanHelper.sanToMove("e8=Q", gameState);
            assertNotNull(move, "Promotion move e8=Q found");
            assertTrue(move.isPromotion(), "Move should be promotion");
            assertEquals(PieceType.QUEEN, move.promotionPieceType(), "Promotion piece type is Queen");

            gameState.applyMove(move);

            assertNull(gameState.getBoard().getPiece(Square.fromAlgebraic("e7")), "e7 is empty after promotion");
            Piece promotedPiece = gameState.getBoard().getPiece(Square.fromAlgebraic("e8"));
            assertNotNull(promotedPiece, "e8 has piece after promotion");
            assertEquals(PieceType.QUEEN, promotedPiece.type(), "e8 piece is Queen");
            assertEquals(Color.WHITE, promotedPiece.color(), "e8 Queen color");

        } catch (Exception e) {
            failTest("testPromotion failed unexpectedly: " + e.getMessage());
        }
    }

    public void testCheckmate() {
        try {
            // Setup Fool's Mate
            gameState.applyMove(SanHelper.sanToMove("f3", gameState)); // 1. f3?
            gameState.applyMove(SanHelper.sanToMove("e5", gameState)); // 1... e5
            gameState.applyMove(SanHelper.sanToMove("g4", gameState)); // 2. g4??
            Move mateMove = SanHelper.sanToMove("Qh4#", gameState); // 2... Qh4# (SAN might just be Qh4)
            assertNotNull(mateMove, "Mate move Qh4# should be found");
            gameState.applyMove(mateMove);

            // After Black moves (Qh4#), it's White's turn, and White should be checkmated
            assertEquals(Color.WHITE, gameState.getCurrentPlayer(), "Current player (mated) should be White");
            assertTrue(gameState.isCheckmate(), "Game state should be checkmate");
            assertTrue(gameState.isInCheck(), "Mated player should be in check");
            assertTrue(gameState.generateLegalMoves().isEmpty(), "Mated player should have no legal moves");
        } catch (Exception e) {
            failTest("testCheckmate failed unexpectedly: " + e.getMessage());
        }
    }

    public void testStalemate() {
        // Setup King-vs-King and Pawn stalemate
        setup(); // Reset to initial position first

        try {
            // Make a few neutral moves to reach a state where it's Black's turn
            // For example: 1. a3 a6 2. b3 b6 3. c3 - Now it's Black's turn
            gameState.applyMove(SanHelper.sanToMove("a3", gameState)); // W1
            gameState.applyMove(SanHelper.sanToMove("a6", gameState)); // B1
            gameState.applyMove(SanHelper.sanToMove("b3", gameState)); // W2
            gameState.applyMove(SanHelper.sanToMove("b6", gameState)); // B2
            gameState.applyMove(SanHelper.sanToMove("c3", gameState)); // W3

            // Now clear the board and set up the specific stalemate position
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    gameState.getBoard().setPiece(new Square(r, c), null);
                }
            }
            gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.KING, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("a3"), new Piece(PieceType.KING, Color.BLACK));
            gameState.getBoard().setPiece(Square.fromAlgebraic("a2"), new Piece(PieceType.PAWN, Color.WHITE));

            // Now it should be Black's turn. King on a3 is blocked.
            assertEquals(Color.BLACK, gameState.getCurrentPlayer(), "Should be Black's turn after setup moves");
            assertFalse(gameState.isInCheck(), "Black should not be in check in stalemate position");

            List<Move> legalMoves = gameState.generateLegalMoves();
            assertTrue(legalMoves.isEmpty(), "Black should have no legal moves in stalemate. Found: " + legalMoves);

            // Check stalemate status
            assertTrue(gameState.isStalemate(), "Game state should be stalemate");
            assertFalse(gameState.isCheckmate(), "Game state should not be checkmate");

        } catch (Exception e) {
            failTest("testStalemate failed unexpectedly: " + e.getMessage());
        }
    }

    public void testIsSquareAttacked() {
        // Initial position checks
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("d3"), Color.WHITE), "d3 initially attacked by white c2/e2 pawn");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("f3"), Color.WHITE), "f3 initially attacked by white e2/g2 pawn");
        // assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("c3"), Color.WHITE), "c3 initially attacked by white b2/d2 pawn"); // This is actually false initially
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("c3"), Color.WHITE), "c3 NOT initially attacked by white pawns");
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("e4"), Color.WHITE), "e4 not attacked by white initially");
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("e4"), Color.BLACK), "e4 not attacked by black initially");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("f6"), Color.BLACK), "f6 initially attacked by black e7/g7 pawns");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("c6"), Color.BLACK), "c6 initially attacked by black b7/d7 pawns");

        // Place a white rook on d4, check attacks
        gameState.getBoard().setPiece(Square.fromAlgebraic("d4"), new Piece(PieceType.ROOK, Color.WHITE));
        gameState.getBoard().setPiece(Square.fromAlgebraic("d2"), null); // Clear pawn blocking d1
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("d7"), Color.WHITE), "d7 attacked by Rook on d4");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("d1"), Color.WHITE), "d1 attacked by Rook on d4");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("a4"), Color.WHITE), "a4 attacked by Rook on d4");
        assertTrue(gameState.isSquareAttacked(Square.fromAlgebraic("h4"), Color.WHITE), "h4 attacked by Rook on d4");
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("e5"), Color.WHITE), "e5 not attacked by Rook on d4");
        // Check if path is blocked
        gameState.getBoard().setPiece(Square.fromAlgebraic("d6"), new Piece(PieceType.PAWN, Color.BLACK)); // Block path to d7
        assertFalse(gameState.isSquareAttacked(Square.fromAlgebraic("d7"), Color.WHITE), "d7 NOT attacked by Rook on d4 when d6 blocked");
    }


    public void testInvalidMove_BlockedPawn() {
        try {
            // Test actual blocked scenario: 1. e4 e5 2. e5? (Illegal)
            setup(); // Reset
            gameState.applyMove(SanHelper.sanToMove("e4", gameState));
            gameState.applyMove(SanHelper.sanToMove("e5", gameState));
            assertThrows(IllegalArgumentException.class,
                    () -> SanHelper.sanToMove("e5", gameState), // White pawn on e4 cannot move to e5 (occupied by black pawn)
                    "Should throw when trying to move pawn e4 to e5 (occupied)"
            );

            // Test moving pawn one step forward when blocked
            setup(); // Reset
            gameState.getBoard().setPiece(Square.fromAlgebraic("e3"), new Piece(PieceType.PAWN, Color.BLACK)); // Put blocker
            assertThrows(IllegalArgumentException.class,
                    () -> SanHelper.sanToMove("e3", gameState), // White pawn on e2 cannot move to e3 (occupied)
                    "Should throw when trying to move pawn e2 to e3 (occupied)"
            );

        } catch (Exception e) {
            failTest("testInvalidMove_BlockedPawn failed unexpectedly: " + e.getMessage());
        }
    }

    public void testInvalidMove_MoveIntoCheck() {
        try {
            // Setup: White King e1, Black Rook a1. White Bishop on d2. White tries Bd3 (illegal pin)
            gameState = new GameState(); // Reset
            gameState.getBoard().setupInitialPosition(); // Use initial board
            gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.ROOK, Color.BLACK));
            gameState.getBoard().setPiece(Square.fromAlgebraic("b1"), null); // Clear path
            gameState.getBoard().setPiece(Square.fromAlgebraic("c1"), null); // Initial Bishop is on c1
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), null);
            // Need black king for valid state, put it away
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), new Piece(PieceType.KING, Color.BLACK));
            gameState.getBoard().setPiece(Square.fromAlgebraic("a8"), null); // remove black rook

            // Manually place the white bishop on d2 for the pin scenario
            gameState.getBoard().setPiece(Square.fromAlgebraic("d2"), new Piece(PieceType.BISHOP, Color.WHITE));

            assertThrows(IllegalArgumentException.class,
                    () -> SanHelper.sanToMove("Bd3", gameState), // Move would expose King to check
                    "Should throw when trying to move pinned Bishop d2 to d3"
            );

            // Verify Bd3 is not in the list of legal moves
            List<Move> legalMoves = gameState.generateLegalMoves();
            Optional<Move> bd3Move = legalMoves.stream().filter(m ->
                    m.pieceMoved().type() == PieceType.BISHOP &&
                            m.from().equals(Square.fromAlgebraic("d2")) &&
                            m.to().equals(Square.fromAlgebraic("d3"))
            ).findFirst();
            assertFalse(bd3Move.isPresent(), "Bd3 should not be present in legal moves list");

        } catch (Exception e) {
            failTest("testInvalidMove_MoveIntoCheck failed unexpectedly: " + e.getMessage());
        }
    }

    public void testCastlingRightsUpdateKingMove() {
        try {
            assertTrue(gameState.canCastleKingSide(Color.WHITE), "Initial W K-side OK");
            assertTrue(gameState.canCastleQueenSide(Color.WHITE), "Initial W Q-side OK");
            // Move King: 1. Ke2
            Move ke2 = SanHelper.sanToMove("Ke2", gameState);
            assertNotNull(ke2, "Move Ke2 should be valid");
            gameState.applyMove(ke2);
            assertFalse(gameState.canCastleKingSide(Color.WHITE), "W K-side BAD after Ke2");
            assertFalse(gameState.canCastleQueenSide(Color.WHITE), "W Q-side BAD after Ke2");
            // Black rights unaffected
            assertTrue(gameState.canCastleKingSide(Color.BLACK), "B K-side OK after Ke2");
            assertTrue(gameState.canCastleQueenSide(Color.BLACK), "B Q-side OK after Ke2");
        } catch (Exception e) {
            failTest("testCastlingRightsUpdateKingMove failed unexpectedly: " + e.getMessage());
        }
    }

    public void testCastlingRightsUpdateRookMove() {
        try {
            // Test King's Rook move effect
            setup(); // Start fresh
            assertTrue(gameState.canCastleKingSide(Color.WHITE), "Initial W K-side OK");
            assertTrue(gameState.canCastleQueenSide(Color.WHITE), "Initial W Q-side OK");
            // Move King's Rook: 1. Rh2
            Move rh2 = SanHelper.sanToMove("Rh2", gameState);
            assertNotNull(rh2, "Move Rh2 should be valid");
            gameState.applyMove(rh2);
            assertFalse(gameState.canCastleKingSide(Color.WHITE), "W K-side BAD after Rh2");
            assertTrue(gameState.canCastleQueenSide(Color.WHITE), "W Q-side OK after Rh2");

            // Test Queen's Rook move effect in isolation
            setup(); // Start fresh again
            assertTrue(gameState.canCastleKingSide(Color.WHITE), "Initial W K-side OK again");
            assertTrue(gameState.canCastleQueenSide(Color.WHITE), "Initial W Q-side OK again");
            // Move Queen's Rook: 1. Ra2
            Move ra2 = SanHelper.sanToMove("Ra2", gameState);
            assertNotNull(ra2, "Move Ra2 should be valid");
            gameState.applyMove(ra2);
            assertTrue(gameState.canCastleKingSide(Color.WHITE), "W K-side OK after Ra2");
            assertFalse(gameState.canCastleQueenSide(Color.WHITE), "W Q-side BAD after Ra2");

        } catch (Exception e) {
            failTest("testCastlingRightsUpdateRookMove failed unexpectedly: " + e.getMessage());
        }
    }
}