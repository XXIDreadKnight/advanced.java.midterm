package chessvalidator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import chessvalidator.model.Square;

class SquareTest {

    @Test
    void testValidSquareCreation() {
        Square a1 = new Square(0, 0);
        Square h8 = new Square(7, 7);
        Square e4 = new Square(3, 4);

        assertTrue(a1.isValid());
        assertTrue(h8.isValid());
        assertTrue(e4.isValid());
    }

    @Test
    void testInvalidSquareCreation() {
        Square invalidRowLow = new Square(-1, 4);
        Square invalidRowHigh = new Square(8, 4);
        Square invalidColLow = new Square(3, -1);
        Square invalidColHigh = new Square(3, 8);

        assertFalse(invalidRowLow.isValid());
        assertFalse(invalidRowHigh.isValid());
        assertFalse(invalidColLow.isValid());
        assertFalse(invalidColHigh.isValid());
    }

    @Test
    void testFromAlgebraicValid() {
        assertEquals(new Square(0, 0), Square.fromAlgebraic("a1"));
        assertEquals(new Square(7, 7), Square.fromAlgebraic("h8"));
        assertEquals(new Square(3, 4), Square.fromAlgebraic("e4"));
        assertEquals(new Square(1, 2), Square.fromAlgebraic("c2"));
    }

    @Test
    void testFromAlgebraicInvalid() {
        assertNull(Square.fromAlgebraic(null));
        assertNull(Square.fromAlgebraic(""));
        assertNull(Square.fromAlgebraic("a"));
        assertNull(Square.fromAlgebraic("1"));
        assertNull(Square.fromAlgebraic("a9")); // Invalid rank
        assertNull(Square.fromAlgebraic("i1")); // Invalid file
        assertNull(Square.fromAlgebraic("e0")); // Invalid rank
        assertNull(Square.fromAlgebraic("E4")); // Case sensitive? Let's assume lowercase file
        assertNull(Square.fromAlgebraic("aa"));
        assertNull(Square.fromAlgebraic("11"));
    }

    @Test
    void testToAlgebraicValid() {
        assertEquals("a1", new Square(0, 0).toAlgebraic());
        assertEquals("h8", new Square(7, 7).toAlgebraic());
        assertEquals("e4", new Square(3, 4).toAlgebraic());
        assertEquals("c2", new Square(1, 2).toAlgebraic());
    }

    @Test
    void testEqualsAndHashCode() {
        Square e4_1 = new Square(3, 4);
        Square e4_2 = Square.fromAlgebraic("e4");
        Square d4 = new Square(3, 3);
        Square e5 = new Square(4, 4);

        assertEquals(e4_1, e4_2);
        assertEquals(e4_1.hashCode(), e4_2.hashCode());

        assertNotEquals(e4_1, d4);
        assertNotEquals(e4_1.hashCode(), d4.hashCode()); // Hashcodes *could* collide, but unlikely for these simple ones
        assertNotEquals(e4_1, e5);
        assertNotEquals(null, e4_1);
        assertNotEquals(new Object(), e4_1);
    }
}