plugins {
    id("composeMultiplatformConvention")
}

kotlin {
    android.namespace = "koog.chat.core.llm"
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koog.agents)
            implementation(libs.koog.agents.additions)
            implementation(libs.ktor.client.engine.defaults)
            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(projects.core.coreCommon)
            implementation(projects.core.coreDatabaseApi)
        }
    }
}
