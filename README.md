# kotlinx-resources

[![badge-library-version]](https://search.maven.org/search?q=g:com.goncalossilva%20a:resources*)
[![badge-plugin-version]](https://plugins.gradle.org/plugin/com.goncalossilva.resources)
![badge-jvm][badge-jvm]
![badge-js][badge-js]
![badge-nodejs][badge-nodejs]
![badge-android][badge-android]
![badge-ios][badge-ios]
![badge-watchos][badge-watchos]
![badge-tvos][badge-tvos]
![badge-macos][badge-macos]
![badge-windows][badge-windows]
![badge-linux][badge-linux]

Kotlin Multiplatform (KMP) plugin and library that add support for reading resources in tests.

The plugin and a library work in tandem to provide a unified API across platforms for reading resources from each source set's `resources` folder.

## Setup

List the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.goncalossilva.resources") version "<version>"
}
```

And add the dependency to your `commonTest` source set:

```kotlin
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation("com.goncalossilva:resources:<version>")
            }
        }
    }
}
```

### Compatibility

Different Kotlin versions require different versions of the plugin/library:

| Kotlin        | kotlinx-resources                |
|---------------|----------------------------------|
| 2.1 and above | 0.10 and above                   |
| 2.0           | 0.9                              |
| 1.9 and below | 0.8 and below (plus `k1` branch) |

## Usage

Once setup is done, a `Resource` class becomes available in all test sources, with a simple API:

```kotlin
class Resource(path: String) {
    fun exists(): Boolean
    fun readText(): String
    fun readBytes(): ByteArray
}
```

To set up resources:

1. **Put them in the [resources folder](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html#org.gradle.api.tasks.SourceSet:resources) of a source set.** For example, `src/commonTest/resources/` (accessible to all) or `src/jsTest/resources/` (accessible to JS).
2. **Specify the path relative to the resources folder.** For example, if your file is at
   `src/commonTest/resources/a-folder/a-file.txt`, use `Resource("a-folder/a-file.txt")`.

### Collisions

As a rule of thumb, `src/commonTest/resources/` is the ideal place for shared resources. It avoids conflicts entirely.

But in cases where the same resource is present in different resources folders, you will see the following error if you don't set a `duplicateStrategy`:

```
> Entry test-file.txt is a duplicate but no duplicate handling strategy has been set.
```

In these cases, specify a `duplicateStrategy` to disambiguate. Platform-specific resources take precedence when `DuplicatesStrategy.EXCLUDE` is used.

So if you have:

- `src/commonTest/resources/test-file.txt`
- `src/jvmTest/resources/test-file.txt`

And if you configure your project's `duplicateStrategy` like so:

```gradle
tasks.withType<Copy>().configureEach {
    if (name.contains("copyResources") || name.contains("TestProcessResources")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
```

The file on JVM's resources folder will take precedence over the one in the common resources folder when running JVM tests.

Other `DuplicatesStrategy` options are available, but they are untested.

## Example

Library tests use the library itself, so they serve as a practical example.

See [`ResourceTest`](resources-test/src/commonTest/kotlin/ResourceTest.kt) for example usage, and [`resources-test/src/commonTest/resources`](resources-library/src/commonTest/resources) for the associated folder structure for resources.

## Acknowledgements

This library is inspired by [this gist](https://gist.github.com/dellisd/a1df42787d42b41cd3ce16f573984674) by [@dellisd](https://gist.github.com/dellisd).

## License

Released under the [MIT License](https://opensource.org/licenses/MIT).

[badge-library-version]: https://img.shields.io/maven-central/v/com.goncalossilva/resources?style=flat
[badge-plugin-version]: https://img.shields.io/gradle-plugin-portal/v/com.goncalossilva.resources?style=flat
[badge-ios]: https://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat
[badge-js]: https://img.shields.io/badge/platform-js-F8DB5D.svg?style=flat
[badge-nodejs]: https://img.shields.io/badge/platform-nodejs-68a063.svg?style=flat
[badge-android]: https://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat
[badge-jvm]: https://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat
[badge-linux]: https://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat
[badge-windows]: https://img.shields.io/badge/platform-windows-4D76CD.svg?style=flat
[badge-macos]: https://img.shields.io/badge/platform-macos-111111.svg?style=flat
[badge-watchos]: https://img.shields.io/badge/platform-watchos-C0C0C0.svg?style=flat
[badge-tvos]: https://img.shields.io/badge/platform-tvos-808080.svg?style=flat
[badge-wasm]: httpss://img.shields.io/badge/platform-wasm-624FE8.svg?style=flat
