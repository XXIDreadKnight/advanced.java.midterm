package chessvalidator.model; // In the 'test/chessvalidator/model' folder

// Static import for assertion methods from the runner
import static chessvalidator.ManualTestRunner.*;

// Import the class being tested
import chessvalidator.model.Square;

public class SquareManualTest {

    public void testValidCreation() {
        Square e4 = Square.fromAlgebraic("e4");
        assertNotNull(e4, "Square e4 should be created");
        assertEquals(3, e4.row(), "e4 row should be 3"); // Rank 4 is row 3 (0-based)
        assertEquals(4, e4.col(), "e4 col should be 4"); // File 'e' is col 4 (0-based)

        Square a1 = Square.fromAlgebraic("a1");
        assertEquals(0, a1.row(), "a1 row should be 0");
        assertEquals(0, a1.col(), "a1 col should be 0");

        Square h8 = Square.fromAlgebraic("h8");
        assertEquals(7, h8.row(), "h8 row should be 7");
        assertEquals(7, h8.col(), "h8 col should be 7");
    }

    public void testInvalidCreation() {
        assertNull(Square.fromAlgebraic("e9"), "e9 should be invalid");
        assertNull(Square.fromAlgebraic("i4"), "i4 should be invalid");
        assertNull(Square.fromAlgebraic("a0"), "a0 should be invalid");
        assertNull(Square.fromAlgebraic(""), "Empty string should be invalid");
        assertNull(Square.fromAlgebraic("e"), "Single char should be invalid");
        assertNull(Square.fromAlgebraic("4e"), "Wrong order should be invalid");
        assertNull(Square.fromAlgebraic(null), "Null input should be invalid");
    }

    public void testToAlgebraic() {
        assertEquals("e4", new Square(3, 4).toAlgebraic(), "Square(3,4) should be e4");
        assertEquals("a1", new Square(0, 0).toAlgebraic(), "Square(0,0) should be a1");
        assertEquals("h8", new Square(7, 7).toAlgebraic(), "Square(7,7) should be h8");
    }

    public void testIsValid() {
        assertTrue(new Square(0, 0).isValid(), "Square(0,0) should be valid");
        assertTrue(new Square(7, 7).isValid(), "Square(7,7) should be valid");
        assertFalse(new Square(-1, 0).isValid(), "Square(-1,0) should be invalid");
        assertFalse(new Square(0, 8).isValid(), "Square(0,8) should be invalid");
        assertFalse(new Square(8, 8).isValid(), "Square(8,8) should be invalid");
    }

    public void testEqualsAndHashCode() {
        Square e4a = Square.fromAlgebraic("e4");
        Square e4b = new Square(3, 4);
        Square f5 = Square.fromAlgebraic("f5");

        assertEquals(e4a, e4b, "e4 created different ways should be equal");
        assertNotEquals(e4a, f5, "e4 and f5 should not be equal");
        assertNotEquals(e4a, null, "Square should not equal null");
        assertNotEquals(e4a, "e4", "Square should not equal String");

        assertEquals(e4a.hashCode(), e4b.hashCode(), "Hash codes for equal squares should match");
        // Technically not required, but good practice:
        // assertNotEquals(e4a.hashCode(), f5.hashCode(), "Hash codes for unequal squares should ideally differ");
    }
}