dependencies {
    api(project(":meta-language"))
    api(project(":meta-image"))
    api(project(":meta-voice"))
    api(project(":meta-vision"))
    api(project(":meta-segmentation"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
