plugins {
	java
	application
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "csw"
version = "0.0.1-SNAPSHOT"


application {
	mainClass.set("csw.fcfs.FcfsApplication")
	applicationDefaultJvmArgs = listOf(
		"-Xms8G", "-Xmx8G",                // fixed heap
		"-XX:+UseZGC",                     // production-ready ZGC
		// If you *really* want generational ZGC and accept preview risk:
		// "-XX:+UnlockExperimentalVMOptions", "-XX:+ZGenerational", "--enable-preview",

		// Container friendliness (override if you pin CPU/memory differently)
		"-XX:+UseContainerSupport",
		"-XX:MaxRAMPercentage=75",
		"-XX:ActiveProcessorCount=4",      // example for 4 vCPUs

		// Reliability & observability
		"-XX:+HeapDumpOnOutOfMemoryError",
		"-XX:+ExitOnOutOfMemoryError",
		"-Xlog:gc*,safepoint*:time,level,tags"
	)

}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(23)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.1.1")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")

	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-blackbird:2.18.2")
	implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.18.2")


	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.8.2")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:8.0.1")
	implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:8.0.1")
	implementation("org.springframework.boot:spring-boot-starter-mail")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
