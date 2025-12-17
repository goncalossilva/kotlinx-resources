package com.goncalossilva.resources

import org.gradle.api.Project

internal interface AndroidAssetsConfigurer {
    fun configure(project: Project)
}
