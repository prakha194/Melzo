// `myspoti-ext`: build, check and try Orpheon extensions on a computer.
//   ./gradlew :extensions-cli:run --args="run path/to/extension lyrics ..."
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("app.myspoti.extensions.cli.RunKt")
    applicationName = "myspoti-ext"
}

dependencies {
    implementation(project(":extensions"))
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    testImplementation("junit:junit:4.13.2")
}
