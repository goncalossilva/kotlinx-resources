// Increase timeouts for browser tests in CI environments.
// Synchronous XHR can block the event loop, preventing Karma heartbeats.
config.set({
    browserNoActivityTimeout: 60000,
    processKillTimeout: 5000
});
