# Changelog

Notable changes are documented in this file, whose format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [0.10.1] - 2025-07-26

### Changed

- Update dependencies
- Migrate to `NodeJsEnvSpec` and `YarnRootEnvSpec`, which fixes Kotlin 2.2.0 compatibility

## [0.10.0] - 2024-12-19

> [!IMPORTANT]
> Version 0.10.0 and higher of kotlinx-resources target Kotlin 2.1 and are incompatible with Kotlin 2.0.
> Only version 0.9.0 target Kotlin 2.0, and versions below it target older versions.

### Added

- Support for Kotlin 2.1.0 (#115 - thanks @chadselph!)

## [0.9.0] - 2024-07-08

> [!IMPORTANT]
> Version 0.9.0 of kotlinx-resources targets Kotlin 2.0 and is incompatible with Kotlin 1.9.
> Only lower versions (even if published in the future), alongside the `k1` branch, target Kotlin 1.9.

### Added

- Support for Kotlin 2.0.0 (#115 - thanks @DRSchlaubi!)

## [0.4.2] - 2024-07-03

### Fixed

- Re-add watchosX64 target

## [0.4.1] - 2024-03-29

### Fixed

- Reading resource bytes on POSIX platforms (#106 - thanks @CharlieTap!)

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

- Throw `FileReadException` when failing to read resources, instead of `RuntimeException`.
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
