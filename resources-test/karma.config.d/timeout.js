// Increase timeouts for browser tests.
// In CI, synchronous XHR can block the event loop, preventing Karma heartbeats.
config.set({
    browserNoActivityTimeout: 60000,
    processKillTimeout: 5000
});
