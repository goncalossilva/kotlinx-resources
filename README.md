# kotlinx-resources

[![badge-version]](https://search.maven.org/search?q=g:com.goncalossilva%20a:resources*)
![badge-jvm][badge-jvm]
![badge-js][badge-js]
![badge-android][badge-android]
![badge-ios][badge-ios]
![badge-watchos][badge-watchos]
![badge-tvos][badge-tvos]
![badge-macos][badge-macos]
![badge-windows][badge-windows]
![badge-linux][badge-linux]

Kotlin Multiplatform (KMP) plugin and library that add support for reading resources in tests.

The plugin and a library work in tandem to provide a unified API across platforms for reading resources from each source set's `resources` folder.

## Usage

List the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("com.goncalossilva.resources")
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

Once that's done, a `Resource` class becomes available in all test sources, with a simple API:

```kotlin
class Resource(path: String) {
    fun exists(): Boolean
    fun readText(): String
}
```

**Note that `path` should be relative to the project's directory**, such as `src/commonTest/resources/some/optional/folders/file.txt`. This is convenient from an implementation perspective, but also allows having resource files with the same name under difference resource folders.

## Example

Library tests use the library itself, so they serve as a practical example. See [`ResourceTest`](resources-library/src/commonTest/kotlin/ResourceTest.kt) for example usage, and [`resources-library/src/commonTest/resources`](resources-library/src/commonTest/resources) for the associated folder structure for resources.

## Acknowledgements

This library is inspired by [this gist](https://gist.github.com/dellisd/a1df42787d42b41cd3ce16f573984674) by [@dellisd](https://gist.github.com/dellisd).

## License

```
MIT License

Copyright (c) 2021 Gonçalo Silva

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

[badge-version]: https://img.shields.io/maven-central/v/com.goncalossilva/resources?style=flat
[badge-ios]: https://img.shields.io/badge/platform-ios-CDCDCD.svg?style=flat
[badge-js]: https://img.shields.io/badge/platform-js-F8DB5D.svg?style=flat
[badge-android]: https://img.shields.io/badge/platform-android-6EDB8D.svg?style=flat
[badge-jvm]: https://img.shields.io/badge/platform-jvm-DB413D.svg?style=flat
[badge-linux]: https://img.shields.io/badge/platform-linux-2D3F6C.svg?style=flat
[badge-windows]: https://img.shields.io/badge/platform-windows-4D76CD.svg?style=flat
[badge-macos]: https://img.shields.io/badge/platform-macos-111111.svg?style=flat
[badge-watchos]: https://img.shields.io/badge/platform-watchos-C0C0C0.svg?style=flat
[badge-tvos]: https://img.shields.io/badge/platform-tvos-808080.svg?style=flat
[badge-wasm]: httpss://img.shields.io/badge/platform-wasm-624FE8.svg?style=flat
