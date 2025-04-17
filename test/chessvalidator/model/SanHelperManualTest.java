package chessvalidator.model; // In the 'test/chessvalidator/model' folder

// Static import for assertion methods from the runner
import static chessvalidator.ManualTestRunner.*;

// Import classes being tested or used in tests
import chessvalidator.model.*;

public class SanHelperManualTest {

    private GameState gameState;

    public void setup() {
        gameState = new GameState(); // Reset for each test
    }

    public void testSimplePawnSan() {
        try {
            Move move = SanHelper.sanToMove("e4", gameState);
            assertNotNull(move, "SAN 'e4' should parse");
            assertEquals(Square.fromAlgebraic("e2"), move.from(), "e4 should move from e2");
            assertEquals(Square.fromAlgebraic("e4"), move.to(), "e4 should move to e4");
            assertEquals(PieceType.PAWN, move.pieceMoved().type(), "e4 is a pawn move");
        } catch (Exception e) {
            failTest("testSimplePawnSan failed unexpectedly: " + e.getMessage());
        }
    }

    public void testSimpleKnightSan() {
        try {
            Move move = SanHelper.sanToMove("Nf3", gameState);
            assertNotNull(move, "SAN 'Nf3' should parse");
            assertEquals(Square.fromAlgebraic("g1"), move.from(), "Nf3 should move from g1");
            assertEquals(Square.fromAlgebraic("f3"), move.to(), "Nf3 should move to f3");
            assertEquals(PieceType.KNIGHT, move.pieceMoved().type(), "Nf3 is a knight move");
            assertNull(move.pieceCaptured(), "Nf3 is not a capture");
        } catch (Exception e) {
            failTest("testSimpleKnightSan failed unexpectedly: " + e.getMessage());
        }
    }

    public void testCaptureSan() {
        try {
            // Setup: 1.e4 d5
            gameState.applyMove(SanHelper.sanToMove("e4", gameState));
            gameState.applyMove(SanHelper.sanToMove("d5", gameState));
            // Now test 2. exd5
            Move move = SanHelper.sanToMove("exd5", gameState);
            assertNotNull(move, "SAN 'exd5' should parse");
            assertEquals(Square.fromAlgebraic("e4"), move.from(), "exd5 moves from e4");
            assertEquals(Square.fromAlgebraic("d5"), move.to(), "exd5 moves to d5");
            assertEquals(PieceType.PAWN, move.pieceMoved().type(), "exd5 is a pawn move");
            assertNotNull(move.pieceCaptured(), "exd5 is a capture");
            assertEquals(PieceType.PAWN, move.pieceCaptured().type(), "exd5 captures a pawn");
            assertEquals(Color.BLACK, move.pieceCaptured().color(), "exd5 captures black piece");

            // Test capture with piece: Place white Queen on d1, black rook on d8. Test Qxd8
            setup(); // Reset
            gameState.getBoard().setupInitialPosition(); // start fresh
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), new Piece(PieceType.QUEEN, Color.WHITE)); // replace pawn with queen
            gameState.getBoard().setPiece(Square.fromAlgebraic("d8"), new Piece(PieceType.ROOK, Color.BLACK)); // replace queen with rook
            // Clear pawns etc. if needed for legality
            gameState.getBoard().setPiece(Square.fromAlgebraic("d2"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("d7"), null);

            Move qxd8 = SanHelper.sanToMove("Qxd8", gameState);
            assertNotNull(qxd8, "SAN 'Qxd8' should parse");
            assertEquals(Square.fromAlgebraic("d1"), qxd8.from(), "Qxd8 moves from d1");
            assertEquals(Square.fromAlgebraic("d8"), qxd8.to(), "Qxd8 moves to d8");
            assertEquals(PieceType.QUEEN, qxd8.pieceMoved().type(), "Qxd8 is queen move");
            assertNotNull(qxd8.pieceCaptured(), "Qxd8 is capture");
            assertEquals(PieceType.ROOK, qxd8.pieceCaptured().type(), "Qxd8 captures rook");

        } catch (Exception e) {
            failTest("testCaptureSan failed unexpectedly: " + e.getMessage());
        }
    }

    public void testPawnCaptureSan() {
        // Test if pawn capture SAN without 'x' works (e.g., "ed" often used instead of exd5)
        // Our current SAN pattern REQUIRES 'x' for non-pawn captures, and allows optional 'x' overall.
        // Let's test if 'exd5' works (already done above) and if just 'e5' works for a capture if unambiguous
        try {
            // Setup: 1.d4 e5
            gameState.applyMove(SanHelper.sanToMove("d4", gameState));
            gameState.applyMove(SanHelper.sanToMove("e5", gameState));
            // Now test 2. dxe5 (explicit 'x')
            Move move = SanHelper.sanToMove("dxe5", gameState);
            assertNotNull(move, "SAN 'dxe5' should parse");
            assertEquals(Square.fromAlgebraic("d4"), move.from(), "dxe5 moves from d4");
            assertEquals(Square.fromAlgebraic("e5"), move.to(), "dxe5 moves to e5");
            assertNotNull(move.pieceCaptured(), "dxe5 is capture");

            // Test if 'de5' (omitted x) would parse.
            // PGN standard allows omitting 'x' for pawn captures when unambiguous (file initial + target square)
            Move moveNoX = SanHelper.sanToMove("de5", gameState); // Try without x
            assertNotNull(moveNoX, "SAN 'de5' (pawn capture no x) should parse if unambiguous");
            assertEquals(Square.fromAlgebraic("d4"), moveNoX.from(), "de5 moves from d4");
            assertEquals(Square.fromAlgebraic("e5"), moveNoX.to(), "de5 moves to e5");
            assertNotNull(moveNoX.pieceCaptured(), "de5 identified as capture");

        } catch (Exception e) {
            failTest("testPawnCaptureSan failed unexpectedly: " + e.getMessage());
        }
    }

    public void testCastlingSan() {
        try {
            // Setup for white kingside castle
            gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);
            Move o_o = SanHelper.sanToMove("O-O", gameState);
            assertNotNull(o_o, "SAN O-O should parse");
            assertTrue(o_o.isCastleKingside(), "O-O is kingside");

            setup(); // Reset for queenside
            // Setup for white queenside castle
            gameState.getBoard().setPiece(Square.fromAlgebraic("b1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("c1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), null);
            Move o_o_o = SanHelper.sanToMove("O-O-O", gameState);
            assertNotNull(o_o_o, "SAN O-O-O should parse");
            assertTrue(o_o_o.isCastleQueenside(), "O-O-O is queenside");

            // Test alternative notation 0-0
            setup();
            gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);
            Move zero_zero = SanHelper.sanToMove("0-0", gameState);
            assertNotNull(zero_zero, "SAN 0-0 should parse");
            assertTrue(zero_zero.isCastleKingside(), "0-0 is kingside");

            // Test alternative notation 0-0-0
            setup();
            gameState.getBoard().setPiece(Square.fromAlgebraic("b1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("c1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), null);
            Move zero_zero_zero = SanHelper.sanToMove("0-0-0", gameState);
            assertNotNull(zero_zero_zero, "SAN 0-0-0 should parse");
            assertTrue(zero_zero_zero.isCastleQueenside(), "0-0-0 is queenside");

        } catch (Exception e) {
            failTest("testCastlingSan failed unexpectedly: " + e.getMessage());
        }
    }

    public void testPromotionSan() {
        try {
            // Setup: White pawn on e7
            gameState = new GameState(); // Reset
            gameState.getBoard().setupInitialPosition();
            gameState.getBoard().setPiece(Square.fromAlgebraic("e7"), new Piece(PieceType.PAWN, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("e2"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("a8"), new Piece(PieceType.KING, Color.BLACK)); // Avoid check
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), null); // Clear target square and remove original king

            Move promQ = SanHelper.sanToMove("e8=Q", gameState);
            assertNotNull(promQ, "SAN e8=Q should parse");
            assertTrue(promQ.isPromotion(), "e8=Q is promotion");
            assertEquals(PieceType.QUEEN, promQ.promotionPieceType(), "e8=Q promotes to Queen");

            Move promN = SanHelper.sanToMove("e8=N", gameState);
            assertNotNull(promN, "SAN e8=N should parse");
            assertTrue(promN.isPromotion(), "e8=N is promotion");
            assertEquals(PieceType.KNIGHT, promN.promotionPieceType(), "e8=N promotes to Knight");

            // Test capture promotion
            gameState.getBoard().setPiece(Square.fromAlgebraic("d8"), new Piece(PieceType.ROOK, Color.BLACK));
            Move promCap = SanHelper.sanToMove("exd8=R", gameState);
            assertNotNull(promCap, "SAN exd8=R should parse");
            assertTrue(promCap.isPromotion(), "exd8=R is promotion");
            assertTrue(promCap.isCapture(), "exd8=R is capture");
            assertEquals(PieceType.ROOK, promCap.promotionPieceType(), "exd8=R promotes to Rook");

            // Test pawn promotion without = (older PGN standard allowed e8Q) - should fail with strict '=' requirement
            assertThrows(IllegalArgumentException.class, () -> SanHelper.sanToMove("e8Q", gameState), "SAN e8Q (no '=') should fail parsing");


        } catch (Exception e) {
            failTest("testPromotionSan failed unexpectedly: " + e.getMessage());
        }
    }


    public void testDisambiguationFile() {
        try {
            // Setup: White rooks on a1 and h1, target d1 empty
            gameState.getBoard().setupInitialPosition();
            gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.ROOK, Color.WHITE)); // ensure it's there
            gameState.getBoard().setPiece(Square.fromAlgebraic("h1"), new Piece(PieceType.ROOK, Color.WHITE)); // ensure it's there
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), null);
            // Clear blocking pieces
            gameState.getBoard().setPiece(Square.fromAlgebraic("b1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("c1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("e1"), null); // Clear king!
            gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);

            // Test Rad1
            Move rad1 = SanHelper.sanToMove("Rad1", gameState);
            assertNotNull(rad1, "SAN Rad1 should parse");
            assertEquals(Square.fromAlgebraic("a1"), rad1.from(), "Rad1 should be from a1");

            // Test Rhd1
            Move rhd1 = SanHelper.sanToMove("Rhd1", gameState);
            assertNotNull(rhd1, "SAN Rhd1 should parse");
            assertEquals(Square.fromAlgebraic("h1"), rhd1.from(), "Rhd1 should be from h1");

        } catch (Exception e) {
            failTest("testDisambiguationFile failed unexpectedly: " + e.getMessage());
        }
    }

    public void testDisambiguationRank() {
        try {
            // Setup: White rooks on d1 and d5, target d3 empty
            gameState.getBoard().setupInitialPosition();
            gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), new Piece(PieceType.ROOK, Color.WHITE)); // put rook on d1
            gameState.getBoard().setPiece(Square.fromAlgebraic("d5"), new Piece(PieceType.ROOK, Color.WHITE)); // put rook on d5
            gameState.getBoard().setPiece(Square.fromAlgebraic("d3"), null);
            // Clear blocking pieces
            gameState.getBoard().setPiece(Square.fromAlgebraic("d2"), null); // clear pawn
            gameState.getBoard().setPiece(Square.fromAlgebraic("d4"), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), null); // clear other rook

            // Test R1d3
            Move r1d3 = SanHelper.sanToMove("R1d3", gameState);
            assertNotNull(r1d3, "SAN R1d3 should parse");
            assertEquals(Square.fromAlgebraic("d1"), r1d3.from(), "R1d3 should be from d1");

            // Test R5d3
            Move r5d3 = SanHelper.sanToMove("R5d3", gameState);
            assertNotNull(r5d3, "SAN R5d3 should parse");
            assertEquals(Square.fromAlgebraic("d5"), r5d3.from(), "R5d3 should be from d5");

        } catch (Exception e) {
            failTest("testDisambiguationRank failed unexpectedly: " + e.getMessage());
        }
    }

    public void testDisambiguationSquare() {
        try {
            // Setup: White Queens on a1, h8, h4, target d4 empty
            gameState = new GameState(); // Reset clean
            // Clear board except kings for legality
            for (int r=0; r<8; r++) for (int c=0; c<8; c++) gameState.getBoard().setPiece(new Square(r,c), null);
            gameState.getBoard().setPiece(Square.fromAlgebraic("e1"), new Piece(PieceType.KING, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("e8"), new Piece(PieceType.KING, Color.BLACK));

            gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.QUEEN, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("h8"), new Piece(PieceType.QUEEN, Color.WHITE));
            gameState.getBoard().setPiece(Square.fromAlgebraic("h4"), new Piece(PieceType.QUEEN, Color.WHITE)); // Queen on h4 also attacks d4
            gameState.getBoard().setPiece(Square.fromAlgebraic("d4"), null);

            // Test Qa1d4 (requires full square disambiguation)
            Move qa1d4 = SanHelper.sanToMove("Qa1d4", gameState);
            assertNotNull(qa1d4, "SAN Qa1d4 should parse");
            assertEquals(Square.fromAlgebraic("a1"), qa1d4.from(), "Qa1d4 should be from a1");

            // Test Qh4d4
            Move qh4d4 = SanHelper.sanToMove("Qh4d4", gameState);
            assertNotNull(qh4d4, "SAN Qh4d4 should parse");
            assertEquals(Square.fromAlgebraic("h4"), qh4d4.from(), "Qh4d4 should be from h4");

            // Test Qh8d4
            Move qh8d4 = SanHelper.sanToMove("Qh8d4", gameState);
            assertNotNull(qh8d4, "SAN Qh8d4 should parse");
            assertEquals(Square.fromAlgebraic("h8"), qh8d4.from(), "Qh8d4 should be from h8");

        } catch (Exception e) {
            failTest("testDisambiguationSquare failed unexpectedly: " + e.getMessage());
        }
    }

    public void testAmbiguousSan() {
        // Use the file disambiguation setup
        gameState.getBoard().setupInitialPosition();
        gameState.getBoard().setPiece(Square.fromAlgebraic("a1"), new Piece(PieceType.ROOK, Color.WHITE)); // ensure it's there
        gameState.getBoard().setPiece(Square.fromAlgebraic("h1"), new Piece(PieceType.ROOK, Color.WHITE)); // ensure it's there
        gameState.getBoard().setPiece(Square.fromAlgebraic("d1"), null);
        // Clear blocking pieces
        gameState.getBoard().setPiece(Square.fromAlgebraic("b1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("c1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("e1"), null); // Clear king!
        gameState.getBoard().setPiece(Square.fromAlgebraic("f1"), null);
        gameState.getBoard().setPiece(Square.fromAlgebraic("g1"), null);

        // Try moving rook to d1 without disambiguation
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("Rd1", gameState),
                "Should throw for ambiguous SAN 'Rd1'"
        );
    }

    public void testIllegalMoveSan() {
        // Try moving pawn backwards
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("e1", gameState), // Pawn on e2 cannot move to e1
                "Should throw for illegal SAN 'e1' (pawn cannot move backwards)"
        );
        // Try moving knight illegally
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("Ng3", gameState), // Knight on g1 cannot move to g3
                "Should throw for illegal SAN 'Ng3'"
        );
        // Try moving King adjacent to other king
        gameState = new GameState(); // Reset clean
        gameState.getBoard().setPiece(Square.fromAlgebraic("e1"), new Piece(PieceType.KING, Color.WHITE));
        gameState.getBoard().setPiece(Square.fromAlgebraic("e3"), new Piece(PieceType.KING, Color.BLACK));
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("Ke2", gameState), // White king cannot move to e2 (adjacent to black king)
                "Should throw for illegal SAN 'Ke2' (kings adjacent)"
        );
    }

    public void testInvalidSanFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("N", gameState), "Invalid SAN format 'N'");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("e4e5", gameState), "Invalid SAN format 'e4e5'");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("Nxa9", gameState), "Invalid SAN format 'Nxa9' (invalid square)");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("O-O-O-O", gameState), "Invalid SAN format 'O-O-O-O'");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("e8=X", gameState), "Invalid SAN format 'e8=X' (invalid promotion piece)");
        assertThrows(IllegalArgumentException.class,
                () -> SanHelper.sanToMove("e4+", gameState), "SAN parser should ignore check/mate chars for finding move"); // SanHelper finds move *then* checks check/mate state
        try {
            Move m = SanHelper.sanToMove("e4+", gameState); // Should parse as e4
            assertNotNull(m, "SAN 'e4+' should parse as e4");
            assertEquals(Square.fromAlgebraic("e4"), m.to(), "e4+ parsed correctly");
        } catch (Exception e) {
            failTest("Parsing SAN with check indicator failed: " + e.getMessage());
        }
    }

    public void testEnPassantSan() {
        // Setup: 1. e4 d5 2. e5 f5 (White turn, EP target f6)
        try {
            gameState.applyMove(SanHelper.sanToMove("e4", gameState));
            gameState.applyMove(SanHelper.sanToMove("d5", gameState));
            gameState.applyMove(SanHelper.sanToMove("e5", gameState));
            gameState.applyMove(SanHelper.sanToMove("f5", gameState));

            // Test SAN exf6
            Move epMove = SanHelper.sanToMove("exf6", gameState);
            assertNotNull(epMove, "SAN 'exf6' should parse for EP");
            assertTrue(epMove.isEnPassantCapture(), "exf6 should be identified as EP capture");
            assertEquals(Square.fromAlgebraic("e5"), epMove.from(), "exf6 from e5");
            assertEquals(Square.fromAlgebraic("f6"), epMove.to(), "exf6 to f6");
            assertNotNull(epMove.pieceCaptured(), "exf6 captured piece");

        } catch (Exception e) {
            failTest("testEnPassantSan failed unexpectedly: " + e.getMessage());
        }
    }

}