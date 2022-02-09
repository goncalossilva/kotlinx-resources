rootProject.name = "resources"

include("resources-test")
includeBuild("resources-plugin") {
    dependencySubstitution {
        substitute(module("com.goncalossilva:resources")).using(project(":"))
    }
}
includeBuild("resources-library")
