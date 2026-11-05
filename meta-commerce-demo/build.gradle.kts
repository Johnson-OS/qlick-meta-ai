plugins { id("org.springframework.boot") }
dependencies {
    implementation(project(":meta-language"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
