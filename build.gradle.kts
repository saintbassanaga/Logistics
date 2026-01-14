plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "tech.bytesMind"
version = "0.0.1-SNAPSHOT"
description = "Logistics"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

extra["mapstructVersion"] = "1.6.3"
extra["archunitVersion"] = "1.3.0"
extra["testcontainersVersion"] = "1.20.4"

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Spring Security + OAuth2 Resource Server
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")

	// Database
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// MapStruct
	implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
	annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")

	// Lombok (optional but recommended)
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// OpenAPI / Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

	// Observability
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.tngtech.archunit:archunit-junit5:${property("archunitVersion")}")

	// TestContainers
	testImplementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
	testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
	testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
	systemProperty("spring.profiles.active", "local")
}
