# alsakt

alsakt is an ALSA JNI binding and Kotlin OO-wrapper library.
It is based on [JavaCPP](https://github.com/bytedeco/javacpp) technology.
The OO-wrapper part is based on [alsa-sharp](https://github.com/atsushieno/alsa-sharp) project.

alsakt is created mostly for use in [ktmidi](https://github.com/atsushieno/ktmidi) project.

## Building

Since alsakt 0.3.0, it bundles `libasound.so` on x86_64 Linux (maybe doable for other architectures, but needs native build setup). Before trying to build the Kotlin/JVM library, we have to build `libasound.so` first:

```
./build-native.sh
```

Then the resulting shared library will be packaged within the .jar by JavaCPP builder.

It is a Gradle Kotlin/JVM project and `./gradlew build` takes care of the Kotlin/JVM part.

## Licenses

alsakt is released under the MIT license.

The ALSA headers and `libasound.so` that are packaged in the resulting jar is built from [alsa-lib](https://github.com/alsa-project/alsa-lib) submodule, which is released under the LGPL v2.1 license.

[JavaCPP](https://github.com/bytedeco/javacpp/) is distributed under Apache V2 license.

