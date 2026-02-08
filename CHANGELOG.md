# Changelog

Notable changes are documented in this file, whose format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Fixed

- Fix JVM resource loading when filenames contain spaces or other URL-special characters. ([#272](https://github.com/goncalossilva/kotlinx-resources/issues/272))
- Fix iOS task dependency conflict with Compose Multiplatform resource assembly. ([#272](https://github.com/goncalossilva/kotlinx-resources/issues/272))

## [0.14.4] - 2026-01-28

### Fixed

- Fix Android unit/host tests missing `commonTest` resources on the test classpath. ([#264](https://github.com/goncalossilva/kotlinx-resources/issues/264))

## [0.14.3] - 2026-01-27

### Fixed

- Fix Android instrumented/device test resources on AGP 8.2.2+.

## [0.14.2] - 2026-01-25

### Fixed

- Fix JS/wasmJS resource loading when filenames contain spaces or other URL-special characters
- Fix Android missing common resources in instrumented tests (including `com.android.kotlin.multiplatform.application`).

## [0.14.1] - 2026-01-03

### Fixed

- Proxy Karma urlRoot to `/base/` for stable browser resource loading

## [0.14.0] - 2025-12-18

### Added

- WASI support via `wasmWasi` target

## [0.13.0] - 2025-12-18

### Added

- Android instrumented tests support

## [0.12.0] - 2025-12-17

### Changed

- **BREAKING:** Rename `FileReadException` to `ResourceReadException` for naming consistency

### Added

- **BREAKING:** Add charset parameter to `readText()` with UTF-8 as default. Supported charsets: UTF-8, UTF-16, UTF-16BE, UTF-16LE, ISO-8859-1, and US-ASCII.
- Wasm support via `wasmJs` target
- Support for resources outside the project directory via `srcDir`

### Fixed

- Handle all native test binaries instead of just the first
- Compose Multiplatform compatibility on iOS ([#141](https://github.com/goncalossilva/kotlinx-resources/issues/141))
- Wrap XHR errors in `ResourceReadException` for JS/Wasm browser environments

## [0.11.0] - 2025-12-13

### Added

- Add watchosDeviceArm64 target

### Fixed

- JS resource loading on Windows hosts

### Changed

- **BREAKING:** Resource paths are now specified relative to the resources folder. ([#162](https://github.com/goncalossilva/kotlinx-resources/pull/162) - thanks [@egorikftp](https://github.com/egorikftp)!)
  
  For example, a file located at `src/commonTest/resources/a-folder/a-file.txt` is now accessed using `Resource("a-folder/a-file.txt")` without the `src/commonTest/resources/` prefix.
  
  See README for more details.

- **BREAKING:** Task names were changed to be more consistent.

  `copyResources<Target>` was renamed to `<target>CopyResources` for consistency with how `<target>ProcessResources` is named. Also, we ensure `<target>` ends with "Test" for test targets (JS was inconsistent in this regard).

  This is an internal implementation detail, but projects relying on the task names may have to adjust their build scripts.

## [0.10.1] - 2025-07-26

### Changed

- Update dependencies
- Migrate to `NodeJsEnvSpec` and `YarnRootEnvSpec`, which fixes Kotlin 2.2.0 compatibility

## [0.10.0] - 2024-12-19

> [!IMPORTANT]
> Version 0.10.0 and higher of kotlinx-resources target Kotlin 2.1 and are incompatible with Kotlin 2.0.
> Only version 0.9.0 target Kotlin 2.0, and versions below it target older versions.

### Added

- Support for Kotlin 2.1.0 ([#115](https://github.com/goncalossilva/kotlinx-resources/pull/115) - thanks [@chadselph](https://github.com/chadselph)!)

## [0.9.0] - 2024-07-08

> [!IMPORTANT]
> Version 0.9.0 of kotlinx-resources targets Kotlin 2.0 and is incompatible with Kotlin 1.9.
> Only lower versions (even if published in the future), alongside the `k1` branch, target Kotlin 1.9.

### Added

- Support for Kotlin 2.0.0 ([#115](https://github.com/goncalossilva/kotlinx-resources/pull/115) - thanks [@DRSchlaubi](https://github.com/DRSchlaubi)!)

## [0.4.2] - 2024-07-03

### Fixed

- Re-add watchosX64 target

## [0.4.1] - 2024-03-29

### Fixed

- Reading resource bytes on POSIX platforms ([#106](https://github.com/goncalossilva/kotlinx-resources/pull/106) - thanks [@CharlieTap](https://github.com/CharlieTap)!)

## [0.4.0] - 2023-08-01

### Added

- Support for Kotlin 1.9.0.

### Changed

- Made `BuildConfig` internal.

## [0.3.2] - 2023-04-03

### Changed

- Removed compilation name in task names to avoid colliding with [native.cocoapods](https://kotlinlang.org/docs/native-cocoapods.html), an official plugin. Unfortunately, this does mean that `kotlinx-resources` is currently incompatible with `moko-resources`. 

## [0.3.1] - 2023-03-29

### Changed

- Added compilation name to task names to avoid colliding with [moko-resources](https://github.com/icerockdev/moko-resources). E.g., `copyResourcesDebugTestEtc` becomes `copyTestResourcesDebugTestEtc`.

## [0.3.0] - 2023-03-23

### Added

- Add `Resource.readBytes()` for reading resources as byte arrays.

### Changed

- Throw `ResourceReadException` when failing to read resources, instead of `RuntimeException`.
- Throw `UnsupportedOperationException` when using unsupported JS runtimes, instead of `RuntimeException`.

## [0.2.5] - 2023-02-19

### Fixed

- Support for Kotlin 1.8 build tools.

## [0.2.4] - 2022-11-12

### Added

- Support for iOS, watchOS, and tvOS simulators.

## [0.2.3] - 2022-09-29

### Fixed

- Support for Kotlin 1.7 build tools.

## [0.2.2] - 2022-04-16

### Fixed

- Use invariant separators in path to better support Windows.

## [0.2.1] - 2021-12-09

### Changed

- Removed cross-dependency from the library to the plugin.

### Fixed

- Add explicit version to plugin artifact to prevent issues with resolution.  

## [0.2.0] - 2021-12-08

### Added

- Automatic cleanup of Karma configuration file for proxying resources.

### Changed

- Removed dependency on `org.jetbrains.kotlinx:kotlinx-nodejs`.

## [0.1.0] - 2021-12-07

### Added

- Initial release.
