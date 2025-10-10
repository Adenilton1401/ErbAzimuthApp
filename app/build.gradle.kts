// ATENÇÃO: Adicionado o plugin KSP e as configurações do Compose
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Adiciona o plugin KSP para processar as anotações do Room
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.androidx.room)
}

// Carrega as propriedades do arquivo local.properties
val localProperties = Properties() // A classe agora é reconhecida
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile)) // A classe agora é reconhecida
}

android {
    namespace = "devandroid.adenilton.erbazimuth"
    compileSdk = 36 // Recomendo usar 34 por enquanto, que é a versão mais estável.

    defaultConfig {
        applicationId = "devandroid.adenilton.erbazimuth"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Pega a chave do local.properties ou usa um valor padrão se não encontrar
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: "CHAVE_NAO_ENCONTRADA"

        // Cria um recurso de string chamado 'maps_api_key' com o valor da chave.
        resValue("string", "maps_api_key", mapsApiKey)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Habilita o Jetpack Compose
    buildFeatures {
        compose = true
    }
    // Configura o compilador do Compose
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
}

// Informa ao Room onde salvar os arquivos de esquema.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material) // ADICIONADO: Para o tema base em XML

    // ViewModel e Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Google Maps
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    //implementation(libs.maps.ktx)
    //implementation(libs.maps.utils.ktx)
    implementation(libs.android.maps.utils)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // NOVA DEPENDÊNCIA para gerenciar permissões no Compose
    implementation(libs.accompanist.permissions)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.webkit)
    //implementation(libs.gms.play.services.maps)
    ksp(libs.androidx.room.compiler)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

}
