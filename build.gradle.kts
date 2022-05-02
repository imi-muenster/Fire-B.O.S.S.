import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("war")
}

group = "de.uni_muenster.imi.fhirFacade"
version = "0.1"

repositories {
    mavenCentral()
}

val hapi_version: String by project
val ktor_version: String by project



dependencies {
    // KOTLIN DEPENDENCIES //
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")

    // HAPI FHIR DEPENDENCIES //
    implementation("ca.uhn.hapi.fhir:hapi-fhir:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-client:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-server:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base:$hapi_version")

    // BASEX DEPENDENCIES //
    implementation("org.basex:basex:9.2.4")

    // SERVLET DEPENDENCIES //
    implementation("javax.servlet:javax.servlet-api:3.0.1")
    implementation("org.eclipse.jetty:jetty-server:11.0.9")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.9")
    implementation("org.eclipse.jetty:jetty-webapp:11.0.9")

    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha14") {
        exclude("ch.qos.logback", "logback-core")
    }
    compileOnly("ch.qos.logback:logback-core:1.3.0-alpha14")
    implementation("org.jlib:jlib-awslambda-logback:1.0.0")

    //PROCESSING DEPENDENCIES
    implementation("org.apache.commons:commons-lang3:3.12.0")

}

tasks.war {
    archiveFileName.set("fhirFacade.war")
    webAppDirectory.set(file("src/main/webapp"))
    webInf { from("./settings/").into("classes/settings")}
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

