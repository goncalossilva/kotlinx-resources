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

Kotlin Multiplatform (KMP) plugin and library for reading resources in tests.

It bridges the gap between different Kotlin Multiplatform targets, allowing you to access files from your `resources` folders in a single, consistent way.

## Setup

Apply the plugin and add the library as a dependency in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.goncalossilva.resources") version "<version>"
}

// ...

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

Replace `<version>` with the latest version shown in the badge above.

### Compatibility

Different Kotlin versions require different versions of the plugin/library:

| Kotlin        | kotlinx-resources                |
| ------------- | -------------------------------- |
| 2.1 and above | 0.10 and above                   |
| 2.0           | 0.9                              |
| 1.9 and below | 0.8 and below (plus `k1` branch) |

## Usage

To access a file in your tests:

1. Place it in a [`resources` folder](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.SourceSet.html#org.gradle.api.tasks.SourceSet:resources). For example, in `src/commonTest/resources/` to have it available in all targets, or `src/jsTest/resources/` to limit access to JS.
2. Instantiate a `Resource` class with the path relative to that folder.

### Basic Example

For a file located at `src/commonTest/resources/data/example.json`:

```kotlin
import com.goncalossilva.resources.Resource

class MyTest {
    @Test
    fun `example data exists`() {
        val resource = Resource("data/example.json")
        assertTrue(resource.exists())
    }

    @Test
    fun `example data ends in a newline`() {
        val content = Resource("data/example.json").readText()
        assertTrue(content.endsWith("\n"))
    }
}
```

### API Overview

The `Resource` class provides a clean and simple API:

```kotlin
class Resource(path: String) {
    // Checks if the resource exists at the given path.
    fun exists(): Boolean

    // Reads the entire resource content as a UTF-8 string.
    fun readText(): String

    // Reads the entire resource content as a byte array.
    fun readBytes(): ByteArray
}
```

### Collisions

As a rule of thumb, place test files in `src/commonTest/resources/`. This avoids collisions entirely.

But if you want to override a common file, you can have a platform-specific version of it in the platform-specific source set (e.g., `src/jvmTest/resources/`). By default, Gradle will throw a "Entry (...) is a duplicate" error during the build process, prompting you to set a `duplicateStrategy` in your `build.gradle.kts`.

To have platform-specific resources override common ones, set the strategy to `EXCLUDE`:

```kotlin
tasks.withType<Copy>().configureEach {
    if (name.contains("copyResources") || name.contains("TestProcessResources")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
```

[Other `DuplicatesStrategy` options are available](https://docs.gradle.org/current/javadoc/org/gradle/api/file/DuplicatesStrategy.html), but avoid `INCLUDE`, as the override behavior becomes inconsistent across platforms.

## Example Project

Library tests use the library itself, so they serve as a practical example.

See [`ResourceTest`](https://github.com/goncalossilva/kotlinx-resources/blob/main/resources-test/src/commonTest/kotlin/ResourceTest.kt) for example usage, and [`resources-test/src/commonTest/resources`](https://github.com/goncalossilva/kotlinx-resources/tree/main/resources-test/src/commonTest/resources) for the associated folder structure for resources.

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
