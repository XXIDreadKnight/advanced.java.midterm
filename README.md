# PGN Validator

## Overview

PGN Validator is a command-line Java application designed to validate chess game recordings stored in the Portable Game Notation (PGN) format. It reads one or more PGN files (or all `.pgn` files within a specified directory) and evaluates each game included for:

1.  **Syntax Correctness:** Checks if the PGN file structure, headers, and move notation adhere to standard conventions. Detects errors like malformed tags or invalid tokens.
2.  **Logical Correctness:** Replays each game move-by-move from the starting position (either standard or specified via a FEN header) and validates each move against the official rules of chess.

The goal is to identify faulty PGN files or specific games within them that contain errors.

## Features

*   **PGN Parsing:** Reads standard PGN files, including headers and move text sections.
*   **Multi-Game Support:** Handles PGN files containing multiple consecutive games.
*   **Header Parsing:** Extracts key-value pairs from PGN headers.
*   **FEN Support:** Can start game validation from a custom position specified by a `[FEN "..."]` tag in the headers. Falls back to the standard starting position if no FEN tag is present.
*   **Comprehensive Syntax Validation:** Detects and reports *all* identifiable syntax errors within each game's PGN text (headers and moves) without stopping the parsing process for that file.
*   **Logical Validation (Game Replay):**
    *   Simulates each game move by move.
    *   Validates full Standard Algebraic Notation (SAN), including basic disambiguation.
    *   Validates standard piece movement, captures, pawn promotion (to Q, R, B, N).
    *   Validates castling (kingside/queenside), considering rights, check, and path clearance.
    *   Validates en passant captures.
    *   Detects illegal moves (e.g., moving through pieces, moving pinned pieces incorrectly, moving into check, impossible moves).
    *   Stops logical validation for a specific game upon encountering the *first* illegal/invalid move.
*   **Clear Error Reporting:** Provides console output indicating whether each game is `VALID` or `INVALID`, listing specific errors (syntax or logical) with their location (game index, approximate move number) and a descriptive message.
*   **Directory Processing:** Can automatically find and process all `.pgn` files within a given directory.
*   **Basic Annotation Skipping:** Skips over standard comments (`{}`), variations (`()`), and Numeric Annotation Glyphs (`$n`) during move extraction.
*   **Multithreading (Optional):** Includes basic support for processing multiple files in parallel using `java.util.concurrent`.

## Requirements

*   Java Development Kit (JDK) 17 or later installed and configured.

## Getting Started (Setup)

1.  **Clone the Repository:** Obtain the project code, typically via Git:
    ```bash
    git clone <your-repository-url>
    cd PgnValidator # Or your project's directory name
    ```
2.  **Open in IntelliJ IDEA:**
    *   Launch IntelliJ IDEA.
    *   Select "Open" or "Import Project" and navigate to the cloned `PgnValidator` directory.
3.  **Configure JDK:**
    *   Go to `File` -> `Project Structure` -> `Project`.
    *   Ensure an SDK of version 17 or higher is selected. If not, add your installed JDK.
4.  **(Optional) Add JUnit 5 for Tests:**
    *   If you want to run the included unit tests, you'll need the JUnit 5 libraries.
    *   Download the `junit-jupiter-api-*.jar` and `junit-jupiter-engine-*.jar` files (and potentially `junit-platform-commons-*.jar`) from the JUnit website or Maven Central.
    *   Go to `File` -> `Project Structure` -> `Libraries`.
    *   Click the `+` button -> `Java`.
    *   Navigate to and select the downloaded JUnit JAR files. Click `OK`.

## How to Compile and Run

**1. Using IntelliJ IDEA:**

*   **Build:** Build the project using `Build` -> `Build Project`. This will compile the `.java` files into an output directory (commonly `out/production/PgnValidator` or similar, check your project settings).
*   **Run:**
    *   Open the `src/chessvalidator/Main.java` file.
    *   Right-click inside the editor pane and select "Run 'Main.main()'".
    *   **First time:** You'll need to configure the run arguments:
        *   Go to `Run` -> `Edit Configurations...`.
        *   Select the `Main` configuration.
        *   In the `Program arguments:` field, enter the path to the PGN file or directory you want to validate (e.g., `Tbilisi2015.pgn` or `examples/` or the full path if needed).
        *   Click `Apply`, then `OK`.
    *   Run again using the play button (▶️) or `Run` -> `Run 'Main'`. The output will appear in the "Run" tool window at the bottom.

**2. Using Command Line / Terminal:**

*   **Build:** First, build the project within IntelliJ as described above to ensure the `.class` files are generated in the `out` directory.
*   **Navigate:** Open your system's terminal or command prompt and navigate to the root directory of your project (the one containing the `src` and `out` folders).
*   **Execute:** Run the `Main` class using the `java` command. You need to specify the classpath (`-cp`) to point to your compiled output directory and provide the PGN file/directory path as an argument.

    ```bash
    # SYNTAX: java -cp <path_to_output_directory> <fully_qualified_main_class> <path_to_pgn_file_or_folder>

    # EXAMPLE (assuming standard IntelliJ output directory):
    java -cp out/production/PgnValidator chessvalidator.Main Tbilisi2015.pgn

    # EXAMPLE validating a specific test file in an 'examples' subfolder:
    java -cp out/production/PgnValidator chessvalidator.Main examples/logical_error_illegal_move.pgn

    # EXAMPLE validating all PGN files in the 'examples' directory:
    java -cp out/production/PgnValidator chessvalidator.Main examples/
    ```
    *(Note: Adjust the classpath `out/production/PgnValidator` if your IntelliJ output directory differs. Use the correct package name `chessvalidator.Main`)*

## Input Format

The program expects input files with the `.pgn` extension containing chess games in Standard PGN format.

*   Files can contain one or multiple games separated by empty lines between the last move/result of one game and the first header of the next.
*   Standard 7-tag roster headers (`Event`, `Site`, `Date`, `Round`, `White`, `Black`, `Result`) are commonly expected but not strictly enforced beyond basic syntax.
*   The `[FEN "..."]` tag is supported for starting validation from a specific position.
*   Standard Algebraic Notation (SAN) is expected for moves.
*   Comments (`{}`), Variations (`()`), and NAGs (`$n`) are generally ignored/skipped.

## Output Format

The program prints validation results to the standard output (console).

1.  **Progress Indication:** Messages indicating which file is being processed (especially useful with multithreading).
2.  **Detailed Report Per File:**
    *   A header indicating the file being reported.
    *   If any file-level errors occurred (e.g., cannot read file), they are listed.
    *   For each game found in the file:
        *   A header identifying the game index (1-based) and basic info from headers (Site, Date, White, Black).
        *   The overall status: `VALID` or `INVALID`.
        *   If `INVALID`, a list of all detected errors for that game:
            *   `[Type Error | Game N | Location]: Message`
            *   *Type:* `Syntax` or `Logical`.
            *   *Location:* Can be a Move number (e.g., `Move 19 (Bg2)`), a Header reference (`FEN Header`, `Header Tag`), or `General`.
            *   *Message:* A description of the specific error.
3.  **Summary:** After processing all files, a summary section provides totals:
    *   Number of files processed.
    *   Total games validated across all files.
    *   Total invalid games found.
    *   Number of files that contained at least one error (file-level, syntax, or logical).
    *   Total processing time.

## Unit Tests

Unit tests using JUnit 5 are located in the test/ directory, primarily focusing on the core chess logic in the chessvalidator.model package. They cover:

*  Board setup and piece placement.

*  Piece movement rules (Pawn, Knight, Bishop, Rook, Queen, King).

*  Special moves: Castling, En Passant, Promotion.

*  Check, Checkmate, and Stalemate detection.

*  FEN parsing validity.

*  Move generation under constraints (e.g., pinned pieces).

You can run these tests within IntelliJ IDEA by right-clicking on the test folder or individual test files/methods and selecting "Run Tests".

## Sample Input (`examples/logical_error_illegal_move.pgn`)

```pgn
[Event "Logical Test Game - Illegal Move"]
[Site "Local"]
[Date "2023.10.27"]
[Round "1"]
[White "Test Player C"]
[Black "Test Player D"]
[Result "*"]

1. e4 c5
2. Nf3 d6
3. Bb5+ Bd7
4. Bxd7+ Qxd7
5. c4 Nc6
6. Nc3 Nf6
7. O-O g6
8. d4 cxd4
9. Nxd4 Bg7
10. f3 O-O
11. Be3 Rac8
12. b3 e6
13. Qd2 Rfd8
14. Rad1 Qe7
15. Ndb5 Ne8
16. Bg5 f6
17. Bh4 a6
18. Na3 Qf7
19. Bg2 { Logical Error: Bishop on h4 cannot move to g2. }
19... f5
20. exf5 gxf5
*

Sample Output (for the input above)
(Note: Exact error message might vary slightly based on implementation details, but the core information should be similar)

Found 1 PGN file(s). Starting validation...
Using single thread for validation.
Validating: examples/logical_error_illegal_move.pgn on thread: main
Game 1: Starting from initial position.
Validation Report for: examples/logical_error_illegal_move.pgn
--- Game 1 (Site Local Date 2023.10.27 Test Player C-Test Player D): INVALID ---
  [Logical Error | Game 1 | Move 19 ('Bg2')]: Logical error: Illegal move or invalid SAN: Bg2 (No legal move matches Piece Bishop[h4] -> g2) 

Processed 1 files containing 1 games in X ms. <-- Time will vary
Files with errors: 1
Total invalid games (syntax or logical): 1

Validation Report for: examples/logical_error_illegal_move.pgn
--- Game 1 (Site Local Date 2023.10.27 Test Player C-Test Player D): INVALID ---
  [Logical Error | Game 1 | Move 19 ('Bg2')]: Logical error: Illegal move or invalid SAN: Bg2 (No legal move matches Piece Bishop[h4] -> g2) 


Validation complete.

