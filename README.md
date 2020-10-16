# Vessel
todo: auto-gen github badge from release workflow (build status)
![Release](https://img.shields.io/github/v/release/textnow/vessel?include_prereleases)
![Coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fgithub.com%2Ftextnow%2Fvessel%2Ftree%2Fmaster%2F.github%2Fbadges%2Fcoverage.json&style=flat)
![License](https://img.shields.io/github/license/textnow/vessel)
![Android Build](https://github.com/textnow/vessel/workflows/Android%20Build/badge.svg?branch=master)
![Android Release](https://github.com/textnow/vessel/workflows/Android%20Release/badge.svg)


Vessel provides a Room (db) replacement for SharedPreferences.


## Jetpack DataStore

We were testing our implementation in-house when Google posted a blog, "[Prefer Storing Data with Jetpack DataStore](https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html)".

If an effort to avoid duplication, we reviewed the post to determine if we could use it instead of the solution we had done in house.

Their solution, in alpha at the time, proposed a similar idea to what we were doing.  There were a few key differences to the API however:

  * Their API does not allow blocking access. While we would prefer to not use blocking access, we are using it for transitional code that has not yet bet converted to coroutines.
  * Their API requires more effort to integrate. Specifically, it requires you to specify your own serializers.  Vessel does this automatically.
  * Their API does not allow incremental migration of SharedPreferences.  Vessel allows us to migrate our old code in smaller, more manageable chunks.
  
If those limitations do not apply to you, we encourage you to use the Jetpack solution.

If, on the other hand, our API provides the flexibility you need - welcome aboard.


## Usage

### Dependency

In order to use Vessel, you will want to include our repository.

```
repositories {
    maven { url = uri("https://maven.pkg.github.com/textnow/vessel") }
}
```

You can then use the latest dependency: ![Release](https://img.shields.io/github/v/release/textnow/vessel?include_prereleases)

```
implementation("com.textnow.android.vessel:vessel-runtime:<VERSION>")
```

### Initialization 

TBD

### API

TBD

### Testing

TBD

