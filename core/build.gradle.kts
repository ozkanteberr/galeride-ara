plugins { kotlin("jvm") }
kotlin { jvmToolchain(17) }
// Gradle 8/JDK 17 Windows test worker cannot load classes from some Unicode paths.
// Optional ASCII output location keeps the checkout and source files in place.
providers.gradleProperty("coreBuildDir").orNull?.let { layout.buildDirectory.set(file(it)) }
tasks.withType<Test>().configureEach {
    defaultCharacterEncoding = "UTF-8"
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
