package br.com.devplugins.staging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Wrapper repository that adds retry logic with exponential backoff and circuit breaker pattern.
 * 
 * <p>This class wraps any StagedCommandRepository implementation and provides resilience against
 * transient failures by automatically retrying failed operations with exponential backoff.</p>
 * 
 * <h2>Retry Logic:</h2>
 * <ul>
 *   <li>Configurable maximum retry attempts (default: 3)</li>
 *   <li>Exponential backoff with configurable base delay (default: 100ms)</li>
 *   <li>Maximum backoff delay cap (default: 5000ms)</li>
 * </ul>
 * 
 * <h2>Circuit Breaker:</h2>
 * <ul>
 *   <li>Opens after consecutive failures exceed threshold (default: 5)</li>
 *   <li>Half-open state after cooldown period (default: 30 seconds)</li>
 *   <li>Closes after successful operation in half-open state</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe. Circuit breaker state is managed using atomic operations.</p>
 * 
 * @see StagedCommandRepository
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class RetryableRepository implements StagedCommandRepository {
    
    private final StagedCommandRepository delegate;
    private final JavaPlugin plugin;
    
    // Retry configuration
    private final int maxRetries;
    private final long baseDelayMs;
    private final long maxDelayMs;
    
    // Circuit breaker state
    private final AtomicInteger consecutiveFailures;
    private final int failureThreshold;
    private final long cooldownMs;
    private final AtomicLong circuitOpenedAt;
    
    private enum CircuitState {
        CLOSED,    // Normal operation
        OPEN,      // Failing, reject all requests
        HALF_OPEN  // Testing if service recovered
    }
    
    /**
     * Creates a retryable repository with default configuration.
     * 
     * @param delegate the underlying repository to wrap
     * @param plugin the plugin instance for logging
     */
    public RetryableRepository(StagedCommandRepository delegate, JavaPlugin plugin) {
        this(delegate, plugin, 3, 100, 5000, 5, 30000);
    }
    
    /**
     * Creates a retryable repository with custom configuration.
     * 
     * @param delegate the underlying repository to wrap
     * @param plugin the plugin instance for logging
     * @param maxRetries maximum number of retry attempts
     * @param baseDelayMs base delay in milliseconds for exponential backoff
     * @param maxDelayMs maximum delay cap in milliseconds
     * @param failureThreshold number of consecutive failures before opening circuit
     * @param cooldownMs cooldown period in milliseconds before attempting half-open
     */
    public RetryableRepository(StagedCommandRepository delegate, JavaPlugin plugin,
                               int maxRetries, long baseDelayMs, long maxDelayMs,
                               int failureThreshold, long cooldownMs) {
        this.delegate = delegate;
        this.plugin = plugin;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.maxDelayMs = maxDelayMs;
        this.failureThreshold = failureThreshold;
        this.cooldownMs = cooldownMs;
        this.consecutiveFailures = new AtomicInteger(0);
        this.circuitOpenedAt = new AtomicLong(0);
    }
    
    /**
     * Gets the current circuit breaker state.
     * 
     * @return the current circuit state
     */
    private CircuitState getCircuitState() {
        int failures = consecutiveFailures.get();
        
        if (failures < failureThreshold) {
            return CircuitState.CLOSED;
        }
        
        long openedAt = circuitOpenedAt.get();
        if (openedAt == 0) {
            // Just exceeded threshold, open the circuit
            circuitOpenedAt.compareAndSet(0, System.currentTimeMillis());
            plugin.getLogger().warning("Circuit breaker OPENED after " + failures + " consecutive failures");
            return CircuitState.OPEN;
        }
        
        long elapsed = System.currentTimeMillis() - openedAt;
        if (elapsed >= cooldownMs) {
            plugin.getLogger().info("Circuit breaker entering HALF_OPEN state after " + elapsed + "ms cooldown");
            return CircuitState.HALF_OPEN;
        }
        
        return CircuitState.OPEN;
    }
    
    /**
     * Records a successful operation, resetting circuit breaker state.
     */
    private void recordSuccess() {
        int previousFailures = consecutiveFailures.getAndSet(0);
        if (previousFailures >= failureThreshold) {
            circuitOpenedAt.set(0);
            plugin.getLogger().info("Circuit breaker CLOSED after successful operation");
        }
    }
    
    /**
     * Records a failed operation, incrementing failure counter.
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            plugin.getLogger().warning("Repository failure count: " + failures + " (threshold: " + failureThreshold + ")");
        }
    }
    
    /**
     * Executes an operation with retry logic and circuit breaker protection.
     * 
     * @param operation the operation to execute
     * @param operationName the name of the operation for logging
     */
    private void executeWithRetry(Runnable operation, String operationName) {
        CircuitState state = getCircuitState();
        
        if (state == CircuitState.OPEN) {
            plugin.getLogger().warning("Circuit breaker is OPEN, rejecting " + operationName + " operation");
            return;
        }
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                operation.run();
                recordSuccess();
                return;
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delay = calculateBackoff(attempt);
                    plugin.getLogger().warning("Repository " + operationName + " failed (attempt " + 
                                             (attempt + 1) + "/" + (maxRetries + 1) + "), retrying in " + delay + "ms: " + 
                                             e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, 
                                         "Repository " + operationName + " failed after " + (maxRetries + 1) + " attempts", e);
                }
            }
        }
        
        recordFailure();
    }
    
    /**
     * Calculates exponential backoff delay for a given attempt.
     * 
     * @param attempt the attempt number (0-based)
     * @return the delay in milliseconds
     */
    private long calculateBackoff(int attempt) {
        long delay = baseDelayMs * (1L << attempt); // 2^attempt
        return Math.min(delay, maxDelayMs);
    }
    
    @Override
    public void save(StagedCommand command) {
        executeWithRetry(() -> delegate.save(command), "save");
    }
    
    @Override
    public void delete(StagedCommand command) {
        executeWithRetry(() -> delegate.delete(command), "delete");
    }
    
    @Override
    public List<StagedCommand> loadAll() {
        CircuitState state = getCircuitState();
        
        if (state == CircuitState.OPEN) {
            plugin.getLogger().warning("Circuit breaker is OPEN, returning empty list for loadAll");
            return List.of();
        }
        
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                List<StagedCommand> result = delegate.loadAll();
                recordSuccess();
                return result;
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delay = calculateBackoff(attempt);
                    plugin.getLogger().warning("Repository loadAll failed (attempt " + 
                                             (attempt + 1) + "/" + (maxRetries + 1) + "), retrying in " + delay + "ms: " + 
                                             e.getMessage());
                    
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, 
                                         "Repository loadAll failed after " + (maxRetries + 1) + " attempts", e);
                }
            }
        }
        
        recordFailure();
        return List.of();
    }
    
    @Override
    public void saveAll(List<StagedCommand> commands) {
        executeWithRetry(() -> delegate.saveAll(commands), "saveAll");
    }
    
    @Override
    public void deleteAll(List<StagedCommand> commands) {
        executeWithRetry(() -> delegate.deleteAll(commands), "deleteAll");
    }
    
    /**
     * Gets the current number of consecutive failures.
     * 
     * @return the failure count
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
    
    /**
     * Checks if the circuit breaker is currently open.
     * 
     * @return true if circuit is open, false otherwise
     */
    public boolean isCircuitOpen() {
        return getCircuitState() == CircuitState.OPEN;
    }
    
    /**
     * Manually resets the circuit breaker state.
     * This should only be used for testing or administrative purposes.
     */
    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        circuitOpenedAt.set(0);
        plugin.getLogger().info("Circuit breaker manually reset");
    }
    
    /**
     * Gets a human-readable status report of the retry mechanism.
     * 
     * @return status report string
     */
    public String getStatusReport() {
        CircuitState state = getCircuitState();
        StringBuilder sb = new StringBuilder();
        sb.append("Retry Configuration:\n");
        sb.append("  Max Retries: ").append(maxRetries).append("\n");
        sb.append("  Base Delay: ").append(baseDelayMs).append("ms\n");
        sb.append("  Max Delay: ").append(maxDelayMs).append("ms\n");
        sb.append("\nCircuit Breaker Status:\n");
        sb.append("  State: ").append(state).append("\n");
        sb.append("  Consecutive Failures: ").append(consecutiveFailures.get()).append("/").append(failureThreshold).append("\n");
        
        if (state == CircuitState.OPEN || state == CircuitState.HALF_OPEN) {
            long openedAt = circuitOpenedAt.get();
            long elapsed = System.currentTimeMillis() - openedAt;
            sb.append("  Time Since Opened: ").append(elapsed).append("ms\n");
            sb.append("  Cooldown Period: ").append(cooldownMs).append("ms\n");
        }
        
        return sb.toString();
    }
}
