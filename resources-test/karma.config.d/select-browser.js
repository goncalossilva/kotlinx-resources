// Pick a single installed headless browser, and apply a Linux `--no-sandbox` workaround.
config.frameworks.push("detectBrowsers");
config.plugins.push("karma-detect-browsers");

const IS_LINUX = process.platform === "linux";

config.set({
    browsers: [],
    customLaunchers: IS_LINUX ? {
        ChromeHeadlessNoSandbox: { base: "ChromeHeadless", flags: ["--no-sandbox"] },
        ChromeCanaryHeadlessNoSandbox: { base: "ChromeCanaryHeadless", flags: ["--no-sandbox"] },
        ChromiumHeadlessNoSandbox: { base: "ChromiumHeadless", flags: ["--no-sandbox"] },
    } : {},
    detectBrowsers: {
        enabled: true,
        usePhantomJS: false,
        preferHeadless: true,
        postDetection: function (browsers) {
            if (!Array.isArray(browsers)) return browsers;

            browsers = browsers.filter((browser) => browser.includes("Headless"));
            if (!browsers.length) return browsers;

            const preferred = ["ChromeHeadless", "ChromiumHeadless", "FirefoxHeadless"];
            let browser = preferred.find((b) => browsers.includes(b)) || browsers[0];

            if (IS_LINUX) {
                if (browser === "ChromeHeadless") browser = "ChromeHeadlessNoSandbox";
                else if (browser === "ChromeCanaryHeadless") browser = "ChromeCanaryHeadlessNoSandbox";
                else if (browser === "ChromiumHeadless") browser = "ChromiumHeadlessNoSandbox";
            }

            return [browser];
        },
    },
});
