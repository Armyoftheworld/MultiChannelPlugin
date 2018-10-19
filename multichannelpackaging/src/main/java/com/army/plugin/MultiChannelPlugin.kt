package com.army.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description
 */
open class MultiChannelPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("channel", MultiChannelExtensions::class.java)
        project.afterEvaluate {
            project.tasks.create("assembleChannel", MultiChannelTask::class.java)
        }
    }
}