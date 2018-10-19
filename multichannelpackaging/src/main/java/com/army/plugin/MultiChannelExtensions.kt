package com.army.plugin

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description
 */
open class MultiChannelExtensions {
    lateinit var baseApk: String
    lateinit var channelFile: String
    lateinit var outDir: String
    override fun toString(): String {
        return "MultiChannelExtensions(baseApk='$baseApk', channelFile='$channelFile', outDir='$outDir')"
    }

}