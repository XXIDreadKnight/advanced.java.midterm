package chess;

/**
 * Custom exception for signaling illegal chess moves during validation.
 */
public class IllegalMoveException extends Exception {
    public IllegalMoveException(String message) {
        super(message);
    }
}