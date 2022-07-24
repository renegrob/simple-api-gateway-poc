plugins {
    java
    id("io.quarkus")
//    id ("org.kordamp.gradle.jandex").version("0.6.0")
}

repositories {
    mavenLocal() // only needed if you want to incude locally built maven projects
    mavenCentral()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-vertx")
    implementation("io.vertx:vertx-http-proxy")
//    implementation("io.quarkus:quarkus-vertx-web")
    implementation("io.smallrye.reactive:mutiny-reactor")
//    implementation("io.netty:netty-transport-native-epoll:*:linux-x86_64")
//    implementation("io.netty:netty-transport-native-kqueue:*:osx-x86_64")
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-web-client")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("org.eclipse.microprofile.openapi:microprofile-openapi-api")

    implementation("org.apache.commons:commons-lang3:3.12.0")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

group = "com.github.renegrob"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")  // include parameter names in byte code
}
