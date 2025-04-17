package chessvalidator; // In the 'test/chessvalidator' folder

import chessvalidator.model.*; // Import the test classes from the model sub-package

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManualTestRunner {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static final List<String> failures = new ArrayList<>(); // Use final

    // --- Assertion Helpers ---

    public static void assertTrue(boolean condition, String message) {
        testsRun++;
        if (condition) {
            testsPassed++;
            // System.out.println("  [PASS] " + message); // Optional: print pass messages
        } else {
            testsFailed++;
            String failureMsg = "[FAIL] " + message;
            failures.add(failureMsg);
            System.err.println("  " + failureMsg); // Print failures immediately
        }
    }

    public static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        testsRun++;
        // Use Objects.equals for null safety
        if (Objects.equals(expected, actual)) {
            testsPassed++;
            // System.out.println("  [PASS] " + message); // Optional: print pass messages
        } else {
            testsFailed++;
            String failureMsg = String.format("[FAIL] %s%n       Expected: <%s>%n       Actual:   <%s>",
                    message,
                    expected == null ? "null" : expected.toString(),
                    actual == null ? "null" : actual.toString());
            failures.add(failureMsg);
            System.err.println("  " + failureMsg); // Print failures immediately
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        testsRun++;
        if (!Objects.equals(unexpected, actual)) {
            testsPassed++;
        } else {
            testsFailed++;
            String failureMsg = String.format("[FAIL] %s%n       Did not expect: <%s> but got it.",
                    message,
                    unexpected == null ? "null" : unexpected.toString());
            failures.add(failureMsg);
            System.err.println("  " + failureMsg); // Print failures immediately
        }
    }

    public static void assertNotNull(Object object, String message) {
        assertTrue(object != null, message);
    }

    public static void assertNull(Object object, String message) {
        assertTrue(object == null, message);
    }

    // Helper for testing exceptions (basic version)
    public static void assertThrows(Class<? extends Throwable> expectedException, Runnable codeBlock, String message) {
        testsRun++;
        boolean exceptionThrown = false;
        boolean correctType = false;
        Throwable caught = null;
        String exceptionDetails = "";
        try {
            codeBlock.run();
        } catch (Throwable t) {
            caught = t;
            exceptionDetails = " (" + t.getMessage() + ")"; // Add exception message
            exceptionThrown = true;
            if (expectedException.isInstance(t)) {
                correctType = true;
            }
        }

        if (exceptionThrown && correctType) {
            testsPassed++;
            // System.out.println("  [PASS] " + message + " (Correctly threw " + expectedException.getSimpleName() + ")");
        } else if (exceptionThrown && !correctType) {
            testsFailed++;
            String failureMsg = String.format("[FAIL] %s%n       Expected exception: <%s>%n       Actual exception:   <%s>%s",
                    message,
                    expectedException.getName(),
                    caught.getClass().getName(),
                    exceptionDetails);
            failures.add(failureMsg);
            System.err.println("  " + failureMsg); // Print failures immediately
        } else { // No exception thrown
            testsFailed++;
            String failureMsg = String.format("[FAIL] %s%n       Expected exception: <%s> but none was thrown.",
                    message,
                    expectedException.getName());
            failures.add(failureMsg);
            System.err.println("  " + failureMsg); // Print failures immediately
        }
    }


    // --- Test Execution Logic ---

    public static void main(String[] args) {
        System.out.println("===== Starting Manual Unit Tests =====");
        long startTime = System.nanoTime();

        // --- Add calls to your test classes/methods here ---
        runSquareTests();
        runGameStateTests();
        runSanHelperTests();
        // Add runBoardTests(), runPieceTests() etc. if you create them
        // ---

        long endTime = System.nanoTime();
        double durationMillis = (endTime - startTime) / 1_000_000.0;

        System.out.println("\n===== Test Summary =====");
        System.out.printf("Total Tests Run: %d%n", testsRun);
        System.out.printf("Tests Passed:    %d%n", testsPassed);
        System.out.printf("Tests Failed:    %d%n", testsFailed);
        System.out.printf("Duration:        %.2f ms%n", durationMillis);

        if (testsFailed > 0) {
            System.err.println("\n--- Failures Detected ---");
            // Failures were already printed when they occurred
            System.exit(1); // Indicate failure
        } else {
            System.out.println("\nAll tests passed!");
            System.exit(0); // Indicate success
        }
    }

    // --- Methods to run tests for specific classes ---

    private static void runSquareTests() {
        System.out.println("\n--- Running Square Tests ---");
        SquareManualTest test = new SquareManualTest();
        test.testValidCreation();
        test.testInvalidCreation();
        test.testToAlgebraic();
        test.testIsValid();
        test.testEqualsAndHashCode();
        System.out.println("--- Square Tests Finished ---");
    }

    private static void runGameStateTests() {
        System.out.println("\n--- Running GameState Tests ---");
        GameStateManualTest test = new GameStateManualTest();
        test.setup(); test.testInitialPosition();
        test.setup(); test.testSimplePawnMove();
        test.setup(); test.testKnightMove();
        test.setup(); test.testKingsideCastlingWhiteValid();
        test.setup(); test.testKingsideCastlingWhiteInvalidBlocked();
        test.setup(); test.testKingsideCastlingWhiteInvalidInCheck();
        test.setup(); test.testKingsideCastlingWhiteInvalidThroughCheck();
        test.setup(); test.testEnPassantSetupAndTarget();
        test.setup(); test.testEnPassantCapture();
        test.setup(); test.testPromotion();
        test.setup(); test.testCheckmate();
        test.setup(); test.testStalemate();
        test.setup(); test.testIsSquareAttacked();
        test.setup(); test.testInvalidMove_BlockedPawn();
        test.setup(); test.testInvalidMove_MoveIntoCheck();
        test.setup(); test.testCastlingRightsUpdateKingMove();
        test.setup(); test.testCastlingRightsUpdateRookMove();
        System.out.println("--- GameState Tests Finished ---");
    }

    private static void runSanHelperTests() {
        System.out.println("\n--- Running SanHelper Tests ---");
        SanHelperManualTest test = new SanHelperManualTest();
        test.setup(); test.testSimplePawnSan();
        test.setup(); test.testSimpleKnightSan();
        test.setup(); test.testCaptureSan();
        test.setup(); test.testPawnCaptureSan();
        test.setup(); test.testCastlingSan();
        test.setup(); test.testPromotionSan();
        test.setup(); test.testDisambiguationFile();
        test.setup(); test.testDisambiguationRank();
        test.setup(); test.testDisambiguationSquare();
        test.setup(); test.testAmbiguousSan();
        test.setup(); test.testIllegalMoveSan();
        test.setup(); test.testInvalidSanFormat();
        test.setup(); test.testEnPassantSan();
        System.out.println("--- SanHelper Tests Finished ---");
    }

    // Helper for test classes to report unexpected exceptions during valid scenarios
    public static void failTest(String message) {
        testsRun++; // Still counts as a run attempt
        testsFailed++;
        String failureMsg = "[FAIL] UNEXPECTED EXCEPTION/FAILURE: " + message;
        failures.add(failureMsg);
        System.err.println("  " + failureMsg); // Print failures immediately
    }
}