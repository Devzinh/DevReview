package br.com.devplugins.utils;

/**
 * Utility class for validating commands before staging.
 * Implements Requirements 1.1, 1.2 - Command validation
 */
public class CommandValidator {

    /**
     * Validates that a command is not empty or whitespace-only.
     * 
     * @param commandLine The command line to validate
     * @return true if the command is valid, false otherwise
     */
    public static boolean isNotEmpty(String commandLine) {
        return commandLine != null && !commandLine.trim().isEmpty();
    }

    /**
     * Validates that a command has basic valid syntax.
     * A command is considered to have valid syntax if:
     * - It starts with "/" (after trimming)
     * - It has at least one character after the "/"
     * 
     * @param commandLine The command line to validate
     * @return true if the command has valid syntax, false otherwise
     */
    public static boolean hasValidSyntax(String commandLine) {
        if (commandLine == null) {
            return false;
        }
        
        String trimmed = commandLine.trim();
        
        // Must start with /
        if (!trimmed.startsWith("/")) {
            return false;
        }
        
        // Must have at least one character after /
        if (trimmed.length() <= 1) {
            return false;
        }
        
        // Extract the command part (first word after /)
        String commandPart = trimmed.substring(1).split(" ")[0];
        
        // Command part must not be empty
        return !commandPart.isEmpty();
    }

    /**
     * Validates a command completely.
     * 
     * @param commandLine The command line to validate
     * @return true if the command passes all validation checks, false otherwise
     */
    public static boolean isValid(String commandLine) {
        return isNotEmpty(commandLine) && hasValidSyntax(commandLine);
    }
}
