# Vessel
![Release](https://img.shields.io/github/v/release/textnow/vessel?include_prereleases)
![Coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Ftextnow%2Fvessel%2Fmaster%2F.github%2Fbadges%2Fcoverage.json&style=flat)
![License](https://img.shields.io/github/license/textnow/vessel)
![Android Build](https://github.com/textnow/vessel/workflows/Android%20Build/badge.svg?branch=master)
![Android Release](https://github.com/textnow/vessel/workflows/Android%20Release/badge.svg)


Vessel provides a Room (db) replacement for SharedPreferences.

### Design Goals

* Keep the interface minimal
* Use a Kotlin data class for each "preference" set we want to save
  - If we did a 1:1 mapping from SharedPreferences, we would be limited to types supported by Room; and we would need a column for each type
* Each data class should contain things that would normally be used together
  - keep data classes small
  - single read/write for the group
* Access from both coroutines and legacy code
* The database (primary) key is the canonical name of the data class.
* The database value is the json serialized version of the data

Originally, we had designed the interface using `inline reified`, but we removed that in favor of the ability to support interfaces and mocking.

### Jetpack DataStore

We were testing our implementation in-house when Google posted a blog, "[Prefer Storing Data with Jetpack DataStore](https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html)".

If an effort to avoid duplication, we reviewed the post to determine if we could use it instead of the solution we had done in house.

Their solution, in alpha at the time, proposed a similar idea to what we were doing.  There were a few key differences to the API however:

  * Their API does not allow blocking access. While we would prefer to not use blocking access, we are using it for transitional code that has not yet bet converted to coroutines.
  * Their API requires more effort to integrate. Specifically, it requires you to specify your own serializers.  Vessel does this automatically.
  * Their API does not allow incremental migration of SharedPreferences.  Vessel allows us to migrate our old code in smaller, more manageable chunks.
  
If those limitations do not apply to you, we encourage you to use the Jetpack solution.

If, on the other hand, our API provides the flexibility you need - welcome aboard.


## Usage

### Personal Access Token

We're currently publishing via [Github Packages](https://github.com/features/packages).  

Unlike Maven Central, Github requires you to authenticate to pull dependencies. 

We are considering publishing to alternative repositories as well.

In the meantime, you will need to setup a [Personal Access Token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token).

We recommend checking:
* `repo:status`
* `repo_deployment`
* `public_repo`
* `read:packages`

Once you have your token, you will use it instead of your password; along with your Github username.

NOTE:  If you are using Nexus, these credentials can be supplied there. That will eliminate the need to supply them in the repositories configuration below.

### Repository

In order to use Vessel, you will want to include our repository.  The following instructions are based on the [Github Documentation](https://docs.github.com/en/free-pro-team@latest/packages/using-github-packages-with-your-projects-ecosystem/configuring-gradle-for-use-with-github-packages#authenticating-to-github-packages).

```
repositories {
    maven {
        name = "GithubPackages-Vessel" 
        url = uri("https://maven.pkg.github.com/textnow/vessel")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USER")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### Dependency

You can then use the latest dependency: ![Release](https://img.shields.io/github/v/release/textnow/vessel?include_prereleases)

```
implementation("com.textnow.android.vessel:vessel-runtime:<VERSION>")
```

### Initialization 

The minimum initialization for Vessel would be:

```
val vessel = VesselImpl(context)
```

It is highly recommended that you provide it through a Dependency Injection framework.

Some examples include:
* [Koin](https://insert-koin.io/)
* [Kodein](https://kodein.org/di/)
* [Dagger](https://developer.android.com/training/dependency-injection/dagger-basics)
* [Hilt](https://dagger.dev/hilt/)

At the very least you should use a single instance per `name` (see below).

There are additional parameters that you can set.  It's recommended to use the Kotlin named parameters.

For example,

```
val vessel = VesselImpl(
    appContext = context,
    inMemory = false,
    allowMainThread = true,
    callback = VesselCallback(
        onCreate = { Log.d(TAG, "Database created") },
        onOpen = { Log.d(TAG, "Database opened") },
        onClosed = { Log.d(TAG, "Database closed") },
        onDestructiveMigration = { Log.d(TAG, "Destructive migration") }
    ),
    cache = DefaultCache()
)
```

| Parameter | Description |
| :--- | :--- |
| appContext | The application context. *This is the only required parameter.* |
| name | Unique name of your vessel. This allows you to have more than one. |
| inMemory | When false (default) it will use a SQL database.  When true (for example, in tests) it will use an in-memory database |
| allowMainThread | If you have legacy code that temporarily needs to make calls from the main thread, this can be your friend |
| callback | A callback for database state changes |
| cache | When null (default), no caching is used. Otherwise, you can specify an implementation of `VesselCache` to enable caching: `DefaultCache` or `LruCache` |

Let's look at the callback a little closer.

| Optional Parameter | Lambda Description |
| :--- | :--- |
| onCreate | Called when the database has been created |
| onOpen | Called when the database has been opened |
| onClosed | Called when the database has been closed |
| onDestructiveMigration | Called when the database has been migrated destructively | 

In the above example, we are simply calling `Log.d` from the callbacks. If you are calling it from Robolectric, you might consider using `println` instead.

### API

The API can be broken into five key areas:
* Blocking Accessors
* Suspend Accessors
* Utilities
* Observers
* Helpers (for Testing)

The source of the API can be found [here](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/main/java/com/textnow/android/vessel/Vessel.kt).

For the following explanations, we will use this sample data class:
```
data class SimpleData(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val number: Int?
)
```

When defining your own data classes to be stored in Vessel, consideration should be given to excluding them from ProGuard or other code obfuscation and shrinkage tools that your project uses. Because we use the canonical name of data classes as database keys, there is a risk that obfuscation tools could change the compiled class name and render existing data unretrievable during runtime.

#### Blocking Accessors

The blocking accessors are useful if you are calling from Java or non-coroutine Kotlin code.

Getting a value from Java:
```
SimpleData data = vessel.getBlocking(SimpleData.class);
```

Getting a value from Kotlin:
```
val data = vessel.getBlocking(SimpleData::class)
```

Setting a value is the same for both platforms (other than the trailing semi-colon):
```
vessel.setBlocking(data)
```

Deleting has two forms, like get.

Java:
```
vessel.deleteBlocking(SimpleData.class);
```

Kotlin:
```
vessel.deleteBlocking(SimpleData::class)
```

#### Suspend Accessors

The suspend accessors are only designed for use by Kotlin coroutines.

Getting a value:
```
suspend fun doWork() {
  val data = vessel.get(SimpleData::class)
}
```

Setting a value:
```
suspend fun doWork() {
  vessel.set(data)
}
```

And, deleting a value.
```
suspend fun doWork() {
  vessel.delete(SimpleData::class)
}
```

#### Utilities

The utilities are just a couple features we thought people would find useful.

The first one allows you to clear your database.
```
vessel.clear()
```

And the second one allows you to replace an old data class with a new type of data class, in a single transaction... IE:
```
suspend fun doWork() {
  val oldData = SimpleDataV1(...)
  val newData = SimpleDataV2(...)
  vessel.replace(old = oldData, new = newData)
}
```

#### Observers

The observer accessors allow you to observe changes over time.

A little verbose for clarity:

```
val simpleFlow: Flow<SimpleData?> = vessel.flow(SimpleData::class)
val simpleLive: LiveData<SimpleData?> = vessel.livedata(SimpleData::class)
```

In both of these cases, you are observing a single row in the database for changes.

#### Helpers (for Testing)

These are identified as helpers for testing, because you would rarely (if ever) need them in production code.

Close the database.
```
vessel.close()
```

And check what the data type (or primary key) is of a specified data class.
```
val data = SimpleData(...)
val type = vessel.typeNameOf(data)
```

### Testing

We provide a couple mechanisms to simplify testing.

#### Robolectric

The recommended approach when testing against Robolectric is to use the in-memory database:
* Create a test instance of `VesselImpl` in your `@Before` method. (see above)
  - This is easier to manage with Dependency Injection
* Set `inMemory = true` on the test instance.
* In your `@After` call both `clear()` and `close()`

We have an example of that, utilizing Koin, here:
* [TestModule](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/test/java/com/textnow/android/vessel/di/TestModule.kt)
* [BaseVesselTest](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/test/java/com/textnow/android/vessel/BaseVesselTest.kt)
* [VesselImplTest](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/test/java/com/textnow/android/vessel/VesselImplTest.kt)

#### Junit

If you want to write tests with strict Junit, that can be accomplished using our [NoOpVessel](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/main/java/com/textnow/android/vessel/NoOpVessel.kt).

You can use something like [MockK](https://mockk.io/) to override the no-op methods.

We have an example of that in [NoOpTest](https://github.com/textnow/vessel/blob/master/vessel-runtime/src/test/java/com/textnow/android/vessel/NoOpTest.kt).


