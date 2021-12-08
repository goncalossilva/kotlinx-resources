rootProject.name = "resources"

include(":resources-library")

includeBuild("resources-plugin") {
    dependencySubstitution {
        substitute(module("com.goncalossilva:resources")).using(project(":"))
    }
}
