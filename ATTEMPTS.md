# Flaky test attempts log

Last updated: 2026-01-01

## Rules in effect
- Flake considered solved only after 10 consecutive CI passes (later raised to 20).
- Only CI runs count. Local runs are for spot-checks only.
- If a run fails, reset to the base commit and form a new hypothesis.

## Current hypothesis (active)
- Proxy only the Karma urlRoot to `/base/` (drop the `/ -> /base/` proxy),
  to avoid incorrect rewrites while keeping urlRoot requests mapped.

## CI streak for current hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20647389731, 2026-01-01)
- Attempt 2: PASS (run 20647508735, 2026-01-01)
- Attempt 3: PASS (run 20647684906, 2026-01-02)
- Attempt 4: PASS (run 20647796627, 2026-01-02)
- Attempt 5: PASS (run 20647990979, 2026-01-02)
- Attempt 6: PASS (run 20648135973, 2026-01-02)
- Attempt 7: INFRA (run 20648322491, 2026-01-02)
  - Windows build: Maven Central 403 downloading dependencies
    (kotlin-stdlib-wasm-wasi, kotlin-test-junit5, junit-jupiter, etc). Not
    counted toward the streak; rerun.

## Previous hypothesis (ended): filename prefix to force last-load
- Ensure the Karma resources config fragment runs last (filename prefix),
  so other fragments don't override the resource404 middleware/proxies.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: FAIL (run 20646414814, 2026-01-01)
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): root-relative rewrite in middleware
- Treat root-relative resource requests as resources in the Karma middleware,
  rewriting them to `/base/` before proxying so missing files return a real 404.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20548705580, 2025-12-28)
- Attempt 2: PASS (run 20548973381, 2025-12-28)
- Attempt 3: PASS (run 20549884014, 2025-12-28)
- Attempt 4: PASS (run 20550384826, 2025-12-28)
- Attempt 5: PASS (run 20551152885, 2025-12-28)
- Attempt 6: PASS (run 20551797594, 2025-12-28)
- Attempt 7: PASS (run 20551989423, 2025-12-28)
- Attempt 8: PASS (run 20552793183, 2025-12-28)
- Attempt 9: PASS (run 20644764036, 2026-01-01)
- Attempt 10: FAIL (run 20646129960, 2026-01-01)
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): Inspect response bodies for text-like extensions
- Inspect response bodies for text-like resource extensions (.json, .txt, .xml,
  .csv) even when Content-Type isn't text, so missing resources mislabelled by
  Karma still surface as not found.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20546905369, 2025-12-28)
- Attempt 2: PASS (run 20547016281, 2025-12-28)
- Attempt 3: PASS (run 20547152515, 2025-12-28)
- Attempt 4: PASS (run 20547264813, 2025-12-28)
- Attempt 5: PASS (run 20547383097, 2025-12-28)
- Attempt 6: PASS (run 20547497870, 2025-12-28)
- Attempt 7: PASS (run 20547648958, 2025-12-28)
- Attempt 8: PASS (run 20547756156, 2025-12-28)
- Attempt 9: PASS (run 20547887844, 2025-12-28)
- Attempt 10: PASS (run 20548033495, 2025-12-28)
- Attempt 11: PASS (run 20548165535, 2025-12-28)
- Attempt 12: FAIL (run 20548283401, 2025-12-28)
  - macOS JS browser: ResourceTest.doesNotExistRoot FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7).
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): Content-Type gated body inspection
- Gate HTML/error body inspection on Content-Type (text/*, json, xml) and skip
  body checks for binary types, to avoid false positives on readBytes.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: FAIL (run 20517065260, 2025-12-26)
  - Ubuntu build: :resources-library:compileKotlinWasmJs failed
    (unresolved reference getResponseHeader in wasm Resource). Fix and rerun.
- Attempt 2: FAIL (run 20545789024, 2025-12-27)
  - Ubuntu build: :resources-library:compileKotlinWasmJs failed
    (lowercase() on JsString; convert to String before lowercasing).
- Attempt 3: PASS (run 20545863444, 2025-12-27)
- Attempt 4: PASS (run 20545993784, 2025-12-28)
- Attempt 5: PASS (run 20546128527, 2025-12-28)
- Attempt 6: PASS (run 20546247475, 2025-12-28)
- Attempt 7: PASS (run 20546558448, 2025-12-28)
- Attempt 8: FAIL (run 20546695082, 2025-12-28)
  - macOS JS browser: ResourceTest.doesNotExistRoot FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7).
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): printable-ratio guard for body detection
- Keep GET-based `exists()` but only apply HTML/error body detection when the
  response looks like text (printable ratio), skipping binary payloads so
  readBytes for real resources doesn't false-positive.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20514377487, 2025-12-26)
- Attempt 2: PASS (run 20514534500, 2025-12-26)
- Attempt 3: PASS (run 20514754949, 2025-12-26)
- Attempt 4: PASS (run 20515101897, 2025-12-26)
- Attempt 5: PASS (run 20515313597, 2025-12-26)
- Attempt 6: PASS (run 20515974623, 2025-12-26)
- Attempt 7: FAIL (run 20516380769, 2025-12-26)
  - Windows wasmJs browser: ResourceTest.readBytesNested FAILED
    (ChromeHeadless143.0.0.0, Windows10).
  - Windows wasmJs browser: ResourceTest.readBytesRootThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10).
  - Windows wasmJs browser: ResourceTest.readBytesNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): GET exists + raw body fallback detection
- Use GET for browser `exists()` and enhance fallback detection by inspecting
  the start of the response body for HTML/error prefixes in JS + wasm, so
  missing resources don't report as present.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20513982005, 2025-12-26)
- Attempt 2: FAIL (run 20514176761, 2025-12-26)
  - Windows wasmJs browser: ResourceTest.readBytesNested FAILED
    (ChromeHeadless143.0.0.0, Windows10).
  - Windows wasmJs browser: ResourceTest.readBytesRootThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

## Previous hypothesis (ended): urlRoot rewrite for all non-Karma assets
- Treat any request under urlRoot as a resource (rewrite to `/base/`) unless it
  targets known Karma assets (context/debug/karma/adapter/favicon), removing
  header-based heuristics so HEAD/GET behave consistently.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20510992409, 2025-12-25)
- Attempt 2: PASS (run 20511453149, 2025-12-25)
- Attempt 3: PASS (run 20512338466, 2025-12-25)
- Attempt 4: PASS (run 20512506240, 2025-12-26)
- Attempt 5: PASS (run 20512862762, 2025-12-26)
- Attempt 6: PASS (run 20513026043, 2025-12-26)
- Attempt 7: PASS (run 20513223955, 2025-12-26)
- Attempt 8: FAIL (run 20513832634, 2025-12-26)
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): urlRoot XHR rewrite with header heuristics
- Treat urlRoot-relative XHR as resources even when `sec-fetch-dest` is missing,
  as long as the path is not an HTML/script asset. This ensures the resource404
  middleware consistently rewrites to `/base/` and returns 404 for missing files.
- Revert JS/wasm browser fallback detection to the baseline status-only check,
  relying on Karma middleware for missing resources instead.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20507872870, 2025-12-25)
- Attempt 2: PASS (run 20508008088, 2025-12-25)
- Attempt 3: PASS (run 20508290350, 2025-12-25)
- Attempt 4: PASS (run 20508668163, 2025-12-25)
- Attempt 5: PASS (run 20508849811, 2025-12-25)
- Attempt 6: PASS (run 20509249798, 2025-12-25)
- Attempt 7: PASS (run 20509792330, 2025-12-25)
- Attempt 8: FAIL (run 20509910475, 2025-12-25)
  - macOS JS browser: ResourceTest.doesNotExistNested FAILED
    (ChromeHeadless143.0.0.0, MacOS10.15.7). Streak reset.

## Previous hypothesis (ended): HTML/plain-text fallback detection (JS + wasm)
- Treat HTML/plain-text fallback bodies as missing resources in browser
  Resource implementations (JS + wasm), while keeping current Karma config.

## CI streak for previous hypothesis (target: 20 consecutive passes)
- Attempt 1: PASS (run 20502477408, 2025-12-25)
- Attempt 2: PASS (run 20502873070, 2025-12-25)
- Attempt 3: PASS (run 20503301834, 2025-12-25)
- Attempt 4: PASS (run 20503664276, 2025-12-25)
- Attempt 5: PASS (run 20504061889, 2025-12-25)
- Attempt 6: PASS (run 20504294326, 2025-12-25)
- Attempt 7: PASS (run 20505204329, 2025-12-25)
- Attempt 8: PASS (run 20505685978, 2025-12-25)
- Attempt 9: PASS (run 20505886617, 2025-12-25)
- Attempt 10: PASS (run 20506037446, 2025-12-25)
- Attempt 11: PASS (run 20506183346, 2025-12-25)
- Attempt 12: FAIL (run 20506360115, 2025-12-25)
  - Windows JS browser: ResourceTest.readBytesNestedThrowsWhenNotFound FAILED
    (ChromeHeadless143.0.0.0, Windows10). Streak reset.

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
