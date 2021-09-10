# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- [VSL-2](https://github.com/textnow/vessel/issues/2) Upgrade Kotlin version from 1.3.72 ➞ 1.5.21
- [VSL-5](https://github.com/textnow/vessel/issues/5) Upgrade Android Gradle plugin version from 3.6.4 ➞ 7.0.1
- [VSL-6](https://github.com/textnow/vessel/issues/6) To match that, upgraded Gradle from 5.6.4 ➞ 7.0.2
- This in turn required changing our JDK version from 8 ➞ 11
- Upgrade the build tools from 30.0.2 ➞ 31.0.0
- Upgrade Room from 2.2.5 ➞ 2.4.0-alpha04 to be compatible with the Kotlin upgrade (2.3.0 failed to compile)
- Upgrade jacoco from 0.8.6 ➞ 0.8.7
- Upgrade Koin from 2.1.6 ➞ 3.1.2

### Fixed

- Previously the published dependencies (pom.xml) mapped `implementation` ➞ `compile` and `api` ➞ `runtime`. That has now been corrected to `implementation` ➞ `runtime` and `api` ➞ `compile`. 

## [0.1.0] - 2020-10-16

### Added

-   Initial release of Vessel. Documentation and Release Process still WIP.

### Changed

### Deprecated

### Removed

### Fixed

### Security

[Unreleased]: https://github.com/textnow/vessel/compare/0.1.0...HEAD

[0.1.0]: https://github.com/textnow/vessel/compare/b6cd8b1b18e8d98cf2f0401338420fe993ba9535...0.1.0
