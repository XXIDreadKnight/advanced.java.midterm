# PGN Chess Game Validator

This Java program validates chess games recorded in the Portable Game Notation (PGN) format. It reads one or more PGN files (or all PGN files in a directory), parses each game, and verifies its correctness according to standard chess rules.

The validation process involves two phases for each game:

1.  **Syntax Validation:** Checks the PGN file structure, header tags, and move notation format. Reports *all* detected syntax errors for a game.
2.  **Logical Validation:** Replays the game move by move on an internal chessboard. Verifies that each move is legal based on the current position, including rules for piece movement, captures, castling, en passant, and promotion. The replay for a specific game stops immediately upon encountering the *first* illegal move (logical error).

## Features

*   Parses standard PGN files containing one or multiple games.
*   Detects and reports PGN syntax errors (malformed headers, invalid characters).
*   Full Standard Algebraic Notation (SAN) parsing and validation.
*   Complete chess rule enforcement:
    *   Per-piece legal move generation (Pawn, Knight, Bishop, Rook, Queen, King).
    *   Special moves: Castling (King-side & Queen-side), En Passant, Pawn Promotion.
    *   Check and Checkmate detection.
    *   Verification of check (`+`) and checkmate (`#`) annotations in SAN.
*   Handles standard starting position. (FEN support via `[FEN]` tag is currently *not* implemented).
*   Reports validation status (Valid/Invalid) for each game.
*   Provides detailed error messages indicating the location (line number for syntax, move number/player for logic) and reason for failure.
*   Robust parsing that attempts to continue validation even with some syntax errors.
*   Modular design using standard Java practices.
*   Unit tests for core chess logic (move generation, special moves).

## Prerequisites

*   Java Development Kit (JDK) 11 or higher.
*   Apache Maven (for building and managing dependencies).

## Building the Project

1.  Clone the repository:
    ```bash
    git clone <your-repository-url>
    cd pgn-validator
    ```
2.  Build the project using Maven:
    ```bash
    mvn clean package
    ```
    This will compile the code, run tests, and create an executable JAR file with dependencies in the `target/` directory (e.g., `pgn-validator-1.0-SNAPSHOT-jar-with-dependencies.jar`).

## Running the Validator

Execute the JAR file from your terminal, providing the path to a PGN file or a directory containing PGN files as a command-line argument:

**Validate a single file:**

```bash
java -jar target/pgn-validator-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/your/game.pgn