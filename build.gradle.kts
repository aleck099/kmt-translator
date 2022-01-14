plugins {
    id("net.mamoe.mirai-console") version "2.9.2"
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

}

group = "net.accel.kmt"
version = "1.3"

repositories {
    mavenLocal()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
}
