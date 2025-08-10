config.frameworks.push("detectBrowsers");
config.set({
    browsers: [],
    detectBrowsers: {
        enabled: true,
        usePhantomJS: false,
        preferHeadless: true,
        postDetection: function(browsers) {
            browsers = browsers.filter((browser) => browser.includes("Headless")) || browsers;
            browsers = browsers.filter((browser) => browser.includes("Chrom")) || browsers;
            browsers = browsers.slice(0, 1);
            return browsers;
        }
    }
});
config.plugins.push("karma-detect-browsers");
