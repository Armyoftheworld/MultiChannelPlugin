package com.army.plugin;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction;

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/18
 * @description
 */
class ChannelTask extends DefaultTask {
    ChannelTask() {
        group = "多渠道打包"
    }

    @TaskAction
    def action() {
        def baseApk = new File(project.channel.baseApk)
        def channelFile = new File(project.channel.channelFile)
        def outDir = new File(project.channel.outDir)
        if (!outDir.exists()) {
            outDir.mkdirs()
        }
        def name = baseApk.name
        name = name.substring(0, name.lastIndexOf("."))
        channelFile.readLines().forEach {
            def apk = ApkParser.INSTANCE.parse(baseApk)
            println("apk.v1 = $apk.v1, apk.v2 = $apk.v2")
        }
    }
}
