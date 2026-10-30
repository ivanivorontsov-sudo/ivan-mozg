import java.util.Properties
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
val sp = Properties().apply {
    val f = file(System.getenv("IVMOZG_SIGNING") ?: "/home/box/keys/ivan-mozg/signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
android {
    namespace = "ru.vorontsov.ivanmozg"
    compileSdk = 36
    defaultConfig {
        applicationId = "ru.vorontsov.ivanmozg"
        minSdk = 26; targetSdk = 36
        versionCode = 1; versionName = "1.0.0"
        resourceConfigurations += listOf("ru")
    }
    signingConfigs { create("release") { if (sp.isNotEmpty()) {
        storeFile = file(sp.getProperty("storeFile")); storePassword = sp.getProperty("storePassword")
        keyAlias = sp.getProperty("keyAlias"); keyPassword = sp.getProperty("keyPassword"); storeType = "PKCS12" } } }
    buildTypes { release {
        if (sp.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true; isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/OSGI-INF/MANIFEST.MF") } }
    testOptions { unitTests { all { it.maxHeapSize = "512m" } } }
}
dependencies {
    implementation("io.github.open-spaced-repetition:fsrs:1.0.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
}
