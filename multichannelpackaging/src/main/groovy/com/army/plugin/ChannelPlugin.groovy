package com.army.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/18
 * @description 多渠道打包插件的入口
 */
class ChannelPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        project.extensions.create("channel", ChannelExtensions)
        project.afterEvaluate{
            project.tasks.create("assembleChannel", ChannelTask)
        }
    }
}
