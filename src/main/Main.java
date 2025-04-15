package main;

import main.reports.FileValidationResult;
import main.reports.GameValidationResult;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class Main {

    private static final boolean USE_MULTITHREADING = true; // Set to true to enable parallel processing

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java com.yourusername.pgnvalidator.Main <path/to/pgn/file_or_folder>");
            System.exit(1);
        }

        String inputPath = args[0];
        Path path = Paths.get(inputPath);

        if (!Files.exists(path)) {
            System.err.println("Error: Input path not found: " + inputPath);
            System.exit(1);
        }

        List<Path> pgnFiles = findPgnFiles(path);

        if (pgnFiles.isEmpty()) {
            System.out.println("No .pgn files found in the specified path.");
            return;
        }

        System.out.println("Found " + pgnFiles.size() + " PGN file(s). Starting validation...");

        List<FileValidationResult> allResults = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        if (USE_MULTITHREADING && pgnFiles.size() > 1) {
            int numThreads = Runtime.getRuntime().availableProcessors();
            System.out.println("Using " + numThreads + " threads for validation.");
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            List<java.util.concurrent.Future<FileValidationResult>> futures = new ArrayList<>();

            for (Path pgnFile : pgnFiles) {
                futures.add(executor.submit(() -> validateFile(pgnFile)));
            }

            executor.shutdown(); // Disable new tasks from being submitted

            try {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(60, TimeUnit.MINUTES)) { // Adjust timeout as needed
                    System.err.println("Executor did not terminate in the specified time.");
                    List<Runnable> droppedTasks = executor.shutdownNow(); // Cancel currently executing tasks
                    System.err.println("Executor was shut down forcefully. " + droppedTasks.size() + " tasks were aborted.");
                }

                // Collect results from completed futures
                for (java.util.concurrent.Future<FileValidationResult> future : futures) {
                    try {
                        allResults.add(future.get());
                    } catch (Exception e) {
                        System.err.println("Error retrieving result from thread: " + e.getMessage());
                        // Optionally create a FileValidationResult indicating the failure for that file
                    }
                }

            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.err.println("Validation was interrupted.");
            }

        } else {
            // Single-threaded execution
            System.out.println("Using single thread for validation.");
            for (Path pgnFile : pgnFiles) {
                allResults.add(validateFile(pgnFile));
            }
        }


        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Print results
        System.out.println("\n--- Validation Summary ---");
        int totalFiles = allResults.size();
        long totalGames = allResults.stream().mapToLong(r -> r.getGameResults().size()).sum();
        long invalidGames = allResults.stream().flatMap(r -> r.getGameResults().stream()).filter(gr -> !gr.isValid()).count();
        long filesWithErrors = allResults.stream().filter(FileValidationResult::hasErrors).count();

        System.out.printf("Processed %d files containing %d games in %d ms.%n", totalFiles, totalGames, duration);
        System.out.printf("Files with errors: %d%n", filesWithErrors);
        System.out.printf("Total invalid games (syntax or logical): %d%n", invalidGames);


        System.out.println("\n--- Detailed Report ---");
        allResults.forEach(System.out::println); // Uses FileValidationResult.toString()

        System.out.println("Validation complete.");
    }

    private static List<Path> findPgnFiles(Path startPath) {
        List<Path> pgnFiles = new ArrayList<>();
        try {
            if (Files.isDirectory(startPath)) {
                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().toLowerCase().endsWith(".pgn")) {
                            pgnFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        System.err.println("Warning: Cannot access file " + file + ": " + exc.getMessage());
                        return FileVisitResult.CONTINUE; // Skip problematic files/dirs
                    }
                });
            } else if (Files.isRegularFile(startPath) && startPath.toString().toLowerCase().endsWith(".pgn")) {
                pgnFiles.add(startPath);
            }
        } catch (IOException e) {
            System.err.println("Error walking file tree: " + e.getMessage());
        }
        return pgnFiles;
    }

    private static FileValidationResult validateFile(Path pgnFile) {
        System.out.println("Validating: " + pgnFile + " on thread: " + Thread.currentThread().getName());
        FileValidationResult fileResult = new FileValidationResult(pgnFile.toString());
        PgnParser parser = new PgnParser();
        GameValidator validator = new GameValidator();

        try (Reader reader = new FileReader(pgnFile.toFile())) {
            List<PgnParser.ParsedGame> parsedGames = parser.parse(reader, fileResult);

            for (PgnParser.ParsedGame parsedGame : parsedGames) {
                GameValidationResult gameResult = validator.validate(parsedGame);
                fileResult.addGameResult(gameResult);
            }

        } catch (IOException e) {
            System.err.println("Error reading file " + pgnFile + ": " + e.getMessage());
            fileResult.addFileError("Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during validation of " + pgnFile + ": " + e.getMessage());
            fileResult.addFileError("Unexpected validation error: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for unexpected errors
        }
        return fileResult;
    }
}