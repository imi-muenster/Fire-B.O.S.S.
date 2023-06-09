import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.tinder.parser.ResourceGeneratorUsingModel
import java.io.FileOutputStream

plugins {
    kotlin("jvm") version "1.8.21"
    id("war")
}

group = "de.unimuenster.imi.medic"
version = "1.0"

repositories {
    mavenCentral()
}

buildscript {
    val hapi_version: String by project

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("ca.uhn.hapi.fhir:hapi-fhir:$hapi_version")
        classpath("ca.uhn.hapi.fhir:hapi-fhir-base:$hapi_version")
        classpath("ca.uhn.hapi.fhir:hapi-tinder-plugin:$hapi_version")
    }
}

val hapi_version: String by project

dependencies {
    // KOTLIN DEPENDENCIES //
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")

    // HAPI FHIR DEPENDENCIES //
    implementation("ca.uhn.hapi.fhir:hapi-fhir:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-client:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-server:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-jpaserver-base:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-tinder-plugin:$hapi_version")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-server-openapi:$hapi_version")

    // BASEX DEPENDENCIES //
    implementation("org.basex:basex:9.2.4")

    // SERVLET DEPENDENCIES //
    implementation("javax.servlet:javax.servlet-api:3.0.1")

    // LOGGING DEPENDENCIES //
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
    archiveFileName.set("fhirBoss.war")
    webAppDirectory.set(file("src/main/webapp"))
    webInf { from("./settings/").into("classes/settings")}
    webInf { from("./build/generated-sources/pathMap.properties").into("classes/resources")}
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

data class VersionSetting(val version: String, val generate: Boolean)

val config = listOf(
    VersionSetting("dstu2", false),
    VersionSetting("dstu3", false),
    VersionSetting("r4", true),
    VersionSetting("r5", false)
)

val generateResourceProviders : Task by tasks.creating() {
    doLast {
        for (conf in config) {
            if (conf.generate == true) {
                generateSources(conf.version)
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("${buildDir}/generated-sources")
        }
        resources {
            exclude("fhirResources")
        }
    }
}

val targetDirectory = "${buildDir}/generated-sources"
val baseDir = "${projectDir}"
val packageBase = "de.unimuenster.imi.medic.generated"

fun generateSources(version: String) {
    val (fhirContext, packageSuffix) = when (version) {
        "dstu2" -> FhirContext.forDstu2() to ""
        "dstu3" -> FhirContext.forDstu3() to ".dstu3"
        "r4" -> FhirContext.forR4() to ".r4"
        "45" -> FhirContext.forR5() to ".r5"
        else -> {
            logger.error("Could not create context")
            error("unknown fhir version '$version'")
        }
    }


    val p = Properties()
    try {
        p.load(fhirContext.version.fhirVersionPropertiesFile)
    } catch (e: java.io.IOException) {
        logger.error("Failed to load version property file", e)
    }

    logger.info("Property file contains: {}", p)

    val baseResourceNames: MutableList<String> = mutableListOf()
    for (next in p.keys) {
        val str = next as String

        if (str.startsWith("resource.")) {
            baseResourceNames.add(str.substring("resource.".length).toLowerCase())
        }
    }

    if (fhirContext.version.version == ca.uhn.fhir.context.FhirVersionEnum.DSTU3) {
        baseResourceNames.remove("conformance")
    }

    logger.info("Including the following resources: {}", baseResourceNames)

    val packagePath = "${packageBase}${packageSuffix}"

    val packageDirectoryBase = File(targetDirectory, packagePath
        .replace(".", "${File.separatorChar}"))

    packageDirectoryBase.mkdirs()

    val gen = ResourceGeneratorUsingModel(version, baseDir)
    gen.setBaseResourceNames(baseResourceNames)

    try {
        val template = "src/main/resources/vm/resource_provider_searchTemplate.vm"
        gen.parse()
        writePathMap(gen)
        gen.setFilenameSuffix("ResourceProvider")
        gen.setTemplate(template)
        gen.setTemplateFile(File(template))
        gen.writeAll(packageDirectoryBase, null, packagePath)
    } catch(e: Exception) {
        logger.error("Failed to generate sources", e)
    }
}

fun writePathMap(gen: ResourceGeneratorUsingModel) {
    val map: MutableMap<String, MutableMap<String, String>> = HashMap()
    for (res in gen.resources) {
        val searchMap: MutableMap<String, String> = HashMap()
        for (sp in res.searchParameters) {
            searchMap[sp.name] = sp.path
        }
        map[res.name] = searchMap
    }

    val properties = Properties()

    for (entry in map) {
        for (sm in entry.value)
            properties.put("${entry.key}.${sm.key}", sm.value)
    }

    properties.store(FileOutputStream("${targetDirectory}/pathMap.properties"), null)
}

tasks.build {
    dependsOn("generateResourceProviders")
}

tasks.compileKotlin {
    dependsOn("generateResourceProviders")
}


