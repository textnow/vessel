# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.2.1] - 2024-12-10

### Changed

-   Update JDK for GHA jobs

## [1.2.0] - 2024-12-10

### Changed

-   Update Gradle - This requires JDK 17
-   Update kotlin, coroutines, room, gson and test libraries
-   Update room now compiles kotlin instead of java
-   Android compile sdk is now 34 and targets 33

### Added

-   Caching interface signatures have changed to allow caching to happen in parallel

## [1.1.1] - 2024-12-04

### Changed

-   Update dokka and github actions to compile correctly
-   Forcing `get`, `set`, `replace` and `delete` functionality to be on `Dispatchers.IO`

## [1.1.0] - 2023-08-23

-   `preload`/`preloadBlocking` now return a status object `PreloadReport` which contains info about any errors encountered while preloading
-   `preload`/`preloadBlocking` now catch errors during deserialization of individual values - any errors here are reported in the `PreloadReport` (as above)
-   For completeness similar info has been added to the profiling data (see `Vessel.snapshot`)
-   Fixed issue #36

## [1.0.0] - 2023-06-16

### Added

-   New Vesel functions `preload` and `preloadBlocking`, to load all data into the cache - this is a breaking change to the Vessel interface
-   New Vessel getter `profileData`, which returns profiling data - time spent in the database by each thread/coroutine, cache hits
-   New constructor parameter `profile`, to enable/disable above profiling

### Changed

-   `get/getBlocking` now caches the value read from the database
-   `set/setBlocking` now checks the cache before writing to avoid redundant writes to the database
-   `delete/deleteBlocking` now caches that the value was deleted
-   `replace` similarly caches the results of the replace operation
-   all of the above APIs now check the cache before going to the database, to avoid unnecessary I/O
-   Update Robolectric to 4.7 - this allows testing on Arm Macs

## [0.1.3] - 2022-07-15

### Changed

-   [VSL-24](https://github.com/textnow/vessel/issues/24) Change code coverage engine to Kover + IntelliJ

### Removed

-   Remove `manifest =` lines from Robolectric `@Config` as per <http://robolectric.org/migrating/#migrating-to-40>
-   Remove `@ExperimentalCoroutinesApi` from `flow` accessor due to IDE recommendation

## [0.1.2] - 2022-03-23

### Added

-   [VSL-25](https://github.com/textnow/vessel/issues/25) Added new `replace` method overload to support passing in the old data model class type
-   [VSL-3](https://github.com/textnow/vessel/issues/3) Added the ability to enable caching for Vessel

### Deprecated

-   [VSL-25](https://github.com/textnow/vessel/issues/25) Deprecated `replace` method overload that required the old data model object to be passed in

## [0.1.1] - 2021-09-10

### Changed

-   [VSL-2](https://github.com/textnow/vessel/issues/2) Upgrade Kotlin version from 1.3.72 ➞ 1.5.21
-   [VSL-5](https://github.com/textnow/vessel/issues/5) Upgrade Android Gradle plugin version from 3.6.4 ➞ 7.0.1
-   [VSL-6](https://github.com/textnow/vessel/issues/6) To match that, upgraded Gradle from 5.6.4 ➞ 7.0.2
-   This in turn required changing our JDK version from 8 ➞ 11
-   Upgrade the build tools from 30.0.2 ➞ 31.0.0
-   Upgrade Room from 2.2.5 ➞ 2.4.0-alpha04 to be compatible with the Kotlin upgrade (2.3.0 failed to compile)
-   Upgrade jacoco from 0.8.6 ➞ 0.8.7
-   Upgrade Koin from 2.1.6 ➞ 3.1.2
-   Updated build tools from 30.0.2 ➞ 31.0.0
-   Updated compile SDK from 30 ➞ 31

### Fixed

-   Previously the published dependencies (pom.xml) mapped `implementation` ➞ `compile` and `api` ➞ `runtime`. That has now been corrected to `implementation` ➞ `runtime` and `api` ➞ `compile`.

## [0.1.0] - 2020-10-16

### Added

-   Initial release of Vessel. Documentation and Release Process still WIP.

### Changed

### Deprecated

### Removed

### Fixed

### Security

[Unreleased]: https://github.com/textnow/vessel/compare/1.2.1...HEAD

[1.2.1]: https://github.com/textnow/vessel/compare/1.0.0...1.2.1

[1.0.0]: https://github.com/textnow/vessel/compare/0.1.3...1.0.0

[0.1.3]: https://github.com/textnow/vessel/compare/0.1.2...0.1.3

[0.1.2]: https://github.com/textnow/vessel/compare/0.1.1...0.1.2

[0.1.1]: https://github.com/textnow/vessel/compare/0.1.0...0.1.1

[0.1.0]: https://github.com/textnow/vessel/compare/b6cd8b1b18e8d98cf2f0401338420fe993ba9535...0.1.0
