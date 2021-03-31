# alsakt

alsakt is an ALSA JNI binding and Kotlin OO-wrapper library.
It is based on [JavaCPP](https://github.com/bytedeco/javacpp) technology.
The OO-wrapper part is based on [alsa-sharp](https://github.com/atsushieno/alsa-sharp) project.

## Building

It is a Gradle Kotlin/JVM project and `./gradlew build` would work, but
depending on the distribution you may have to edit [build.gradle](alsakt-javacpp/build.gradle#L33) and/or [Alsa.java](alsakt-javacpp/src/main/java/alsakt_presets/Alsa.java) configuration.

## Licenses

alsakt is released under the MIT license.

[JavaCPP](https://github.com/bytedeco/javacpp/) is distributed under Apache V2 license.

