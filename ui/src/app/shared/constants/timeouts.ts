/**
 * Application-wide timeout constants in milliseconds.
 * 
 * Centralized location for all timeout values to improve maintainability
 * and prevent magic numbers throughout the codebase.
 */
export class TimeoutConstants {
    /**
     * Standard delay for chart rendering after window resize or initialization
     */
    public static readonly CHART_REFRESH_DELAY_MS = 500;

    /**
     * Delay for debouncing energy query requests
     */
    public static readonly ENERGY_QUERY_DEBOUNCE_MS = 500;

    /**
     * Delay for debouncing channel subscription requests
     */
    public static readonly CHANNEL_SUBSCRIBE_DEBOUNCE_MS = 500;

    private constructor() {
        // Utility class - prevent instantiation
    }
}
