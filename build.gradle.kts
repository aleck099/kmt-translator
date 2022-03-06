plugins {
    id("net.mamoe.mirai-console") version "2.10.0"
    val kotlinVersion = "1.6.20-M1"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

}

group = "net.accel.kmt"
version = "1.5"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.tencentcloudapi:tencentcloud-sdk-java:4.0.11")
}
