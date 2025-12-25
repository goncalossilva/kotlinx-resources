# Flaky test attempts log

Last updated: 2025-12-25

## Rules in effect
- Flake considered solved only after 10 consecutive CI passes (later raised to 20).
- Only CI runs count. Local runs are for spot-checks only.
- If a run fails, reset to the base commit and form a new hypothesis.

## Current hypothesis (active)
- Treat HTML/plain-text fallback bodies as missing resources in browser
  Resource implementations (JS + wasm), while keeping current Karma config.

## CI streak for current hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20502477408, 2025-12-25)
- Attempt 2: PASS (run 20502873070, 2025-12-25)
- Attempt 3: PASS (run 20503301834, 2025-12-25)
- Attempt 4: PASS (run 20503664276, 2025-12-25)
- Attempt 5: PASS (run 20504061889, 2025-12-25)

## Previous hypothesis (ended): rewrite urlRoot XHR to /base
- Rewrite XHR/fetch requests under urlRoot to `/base/` before the resource
  middleware runs (use `sec-fetch-dest: empty` as a signal).
- Keep `HEAD` handling, urlRoot-aware `/base/` checks, and the middleware
  re-apply hook.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20500319907, 2025-12-25)
- Attempt 2: FAIL (run 20500639968, 2025-12-25)
  - Windows JS browser: ResourceTest.readTextNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): handle HEAD in resource middleware
- Handle `HEAD` requests inside the Karma resource middleware:
  return 200 with Content-Length when the file exists, 404 otherwise.
- Keep urlRoot-aware `/base/` handling and the middleware re-apply hook.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20498780840, 2025-12-25)
- Attempt 2: PASS (run 20498951037, 2025-12-25)
- Attempt 3: PASS (run 20499194126, 2025-12-25)
- Attempt 4: PASS (run 20499402139, 2025-12-25)
- Attempt 5: PASS (run 20499630919, 2025-12-25)
- Attempt 6: FAIL (run 20499835906, 2025-12-25)
  - Windows wasmJs browser: ResourceTest.readTextNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): re-apply resource404 middleware after config.set
- Wrap `config.set` in the Karma fragment so the resource404 middleware is
  re-applied after every config update (protects against fragment ordering).
- Keep urlRoot-aware `/base/` handling; no library changes.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20492547071, 2025-12-24)
- Attempt 2: PASS (rerun 20492547071, 2025-12-24)
- Attempt 3: PASS (rerun 20492547071, 2025-12-24)
- Attempt 4: CANCELED (run 20498380550, 2025-12-25)
  - GitHub Actions concurrency canceled this run. Streak reset; rerun.
- Attempt 5: PASS (run 20498399369, 2025-12-25)
- Attempt 6: FAIL (run 20498570641, 2025-12-25)
  - Windows wasmJs browser: WasmJsResourceTest.platformResourceOverride FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.
  - macOS job canceled after Windows failure (fail-fast).

## Previous hypothesis (ended): urlRoot-prefixed /base handling
- Attempt 1: PASS (run 20490718242, 2025-12-24)
- Attempt 2: FAIL (rerun 20490718242, 2025-12-24)
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): Karma 404 middleware for /base
- Attempt 1: FAIL (run 20481341626, 2025-12-24)
  - macOS wasmJs browser: Karma config error
    (SyntaxError: Identifier 'path' has already been declared). Streak reset.
- Attempt 2: FAIL (run 20484919027, 2025-12-24)
  - Windows build: Gradle wrapper download timeout (infra). Rerun.
- Attempt 3: PASS (run 20485372937, 2025-12-24)
- Attempt 4: PASS (run 20485986327, 2025-12-24)
- Attempt 5: PASS (run 20486676133, 2025-12-24)
- Attempt 6: PASS (rerun 20486676133, 2025-12-24)
- Attempt 7: PASS (rerun 20486676133, 2025-12-24)
- Attempt 8: PASS (rerun 20486676133, 2025-12-24)
- Attempt 9: PASS (rerun 20486676133, 2025-12-24)
- Attempt 10: FAIL (rerun 20486676133, 2025-12-24)
  - Windows JS browser: ResourceTest.readBytesNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): JSON/plain 404 payload detection
- Treat JSON/plain-text 404 fallback payloads (e.g., "Cannot GET", "Not Found",
  "404") as missing resources when the body looks like an error response, even
  if Content-Type is application/json or missing.
- Keep HTML/Karma detection and only apply fallback checks for non-.html/.htm
  and non-.js/.mjs/.cjs paths to avoid false positives.
- Use GET for exists() so we can evaluate headers and body together.
- Apply the same logic in JS and wasm ResourceBrowser code paths.
- Abandoned before CI runs; too much harness coupling and complexity.

## Previous hypothesis (ended): HTML/Karma + plain-text fallback detection
- Attempt 1: PASS (run 20457716042, 2025-12-23)
- Attempt 2: PASS (rerun 20457716042, 2025-12-23)
- Attempt 3: PASS (rerun 20457716042, 2025-12-23)
- Attempt 4: PASS (rerun 20457716042, 2025-12-23)
- Attempt 5: PASS (rerun 20457716042, 2025-12-23)
- Attempt 6: PASS (rerun 20457716042, 2025-12-23)
- Attempt 7: PASS (rerun 20457716042, 2025-12-23)
- Attempt 8: PASS (rerun 20457716042, 2025-12-23)
- Attempt 9: PASS (rerun 20457716042, 2025-12-23)
- Attempt 10: PASS (rerun 20457716042, 2025-12-23)
- Attempt 11: PASS (rerun 20457716042, 2025-12-23)
- Attempt 12: PASS (rerun 20457716042, 2025-12-23)
- Attempt 13: PASS (rerun 20457716042, 2025-12-23)
- Attempt 14: PASS (rerun 20457716042, 2025-12-23)
- Attempt 15: PASS (rerun 20457716042, 2025-12-23)
- Attempt 16: PASS (rerun 20457716042, 2025-12-23)
- Attempt 17: PASS (rerun 20457716042, 2025-12-23)
- Attempt 18: PASS (rerun 20457716042, 2025-12-23)
- Attempt 19: PASS (rerun 20457716042, 2025-12-23)
- Attempt 20: FAIL (rerun 20457716042, 2025-12-23)
  - macOS JS browser: ResourceTest.doesNotExistRoot FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): HTML + Karma fallback detection
- Attempt 1: FAIL (run 20445704091, 2025-12-22)
  - macOS build: Gradle distribution download timed out (infra). Rerun.
- Attempt 2: PASS (rerun 20445704091, 2025-12-22)
- Attempt 3: PASS (rerun 20445704091, 2025-12-22)
- Attempt 4: PASS (rerun 20445704091, 2025-12-22)
- Attempt 5: PASS (rerun 20445704091, 2025-12-23)
- Attempt 6: PASS (rerun 20445704091, 2025-12-23)
- Attempt 7: PASS (rerun 20445704091, 2025-12-23)
- Attempt 8: PASS (rerun 20445704091, 2025-12-23)
- Attempt 9: PASS (rerun 20445704091, 2025-12-23)
- Attempt 10: PASS (rerun 20445704091, 2025-12-23)
- Attempt 11: PASS (rerun 20445704091, 2025-12-23)
- Attempt 12: PASS (rerun 20445704091, 2025-12-23)
- Attempt 13: PASS (rerun 20445704091, 2025-12-23)
- Attempt 14: PASS (rerun 20445704091, 2025-12-23)
- Attempt 15: PASS (rerun 20445704091, 2025-12-23)
- Attempt 16: PASS (rerun 20445704091, 2025-12-23)
- Attempt 17: PASS (rerun 20445704091, 2025-12-23)
- Attempt 18: FAIL (rerun 20445704091, 2025-12-23)
  - Windows JS browser: ResourceTest.readBytesNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): HTML fallback via headers/body
- Attempt 1: PASS (run 20434777902, 2025-12-22)
- Attempt 2: PASS (rerun 20434777902, 2025-12-22)
- Attempt 3: PASS (rerun 20434777902, 2025-12-22)
- Attempt 4: PASS (rerun 20434777902, 2025-12-22)
- Attempt 5: PASS (rerun 20434777902, 2025-12-22)
- Attempt 6: PASS (rerun 20434777902, 2025-12-22)
- Attempt 7: PASS (rerun 20434777902, 2025-12-22)
- Attempt 8: PASS (rerun 20434777902, 2025-12-22)
- Attempt 9: FAIL (rerun 20434777902, 2025-12-22)
  - Windows JS browser: ResourceTest.readBytesNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): Body HTML fallback
- Attempt 1: FAIL (run 20434269388, 2025-12-22)
  - macOS JS browser: ResourceTest.doesNotExistRoot FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): Content-Type HTML fallback
- Attempt 1: PASS (run 20432628516, 2025-12-22)
- Attempt 2: PASS (rerun 20432628516, 2025-12-22)
- Attempt 3: PASS (rerun 20432628516, 2025-12-22)
- Attempt 4: PASS (rerun 20432628516, 2025-12-22)
- Attempt 5: FAIL (rerun 20432628516, 2025-12-22)
  - Windows JS browser: ResourceTest.readTextRootThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Notable failures observed previously (before current hypothesis)
- macOS JS: ResourceBrowserTest
  - doesNotExistRoot (assertFalse failed)
  - doesNotExistNested (assertFalse failed)
- Windows JS:
  - readBytesRootThrowsWhenNotFound
  - readTextWithAsciiCharset
- Windows wasmJs:
  - readBytesNestedThrowsWhenNotFound
- Infra/plugin failure observed:
  - com.goncalossilva.useanybrowser not found

## Previous hypotheses tried (all reset after failure)
- JS-only try/catch changes for browser resource access (exists/readBytes).
  - Reached 10 consecutive CI passes but failed when rerun for 20.
- wasm request wrapper refactors (open/send helpers, safe status/response access).
  - Did not eliminate flakes; reset.
- wasm status==0 retry guard.
  - Required a fix for status type (Short -> Int), but still failed; reset.
- wasm try/catch simplification variants.
  - Did not stabilize CI; reset.
- HTML fallback detection via Content-Type header (JS + wasm).
  - Failed with Windows JS readTextRootThrowsWhenNotFound.
- HTML fallback detection via response body (JS + wasm).
  - Failed with macOS JS doesNotExistRoot.

## Build/compile issues encountered while iterating
- wasm compile error: outer class receiver not allowed inside ResourceBrowser
  (fixed by capturing path as a property).
- wasm compile error: status type mismatch (Short vs Int) when comparing to 0.
