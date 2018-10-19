package com.army.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/20
 * @description
 */
open class MultiChannelTask : DefaultTask() {
    init {
        group = "多渠道打包"
    }

    @TaskAction
    fun action() {
        val channel: MultiChannelExtensions = project.extensions.findByName("channel") as MultiChannelExtensions
        println(channel.toString())
        val baseApk = File(channel.baseApk)
        val channelFile = File(channel.channelFile)
        val outDir = File(channel.outDir)
        if (!outDir.exists()) {
            outDir.mkdirs()
        }
        var name = baseApk.name
        name = name.substring(0, name.lastIndexOf("."))
        channelFile.readLines().forEach { channelInfo ->
            val apk = ApkParser.parse(baseApk)
            println("isV1 = ${apk.isV1}, isV2 = ${apk.isV2}")
            ApkBuilder.generateChannelApk(channelInfo, apk, File(outDir, "$name-$channelInfo.apk"))
        }
    }
}