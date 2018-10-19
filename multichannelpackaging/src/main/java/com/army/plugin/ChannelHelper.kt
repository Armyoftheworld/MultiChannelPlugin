package com.army.plugin

import java.io.File

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description
 */
object ChannelHelper {

    fun getChannelInfo(apkFile: File): String {
        val apk = ApkParser.parse(apkFile)
        return if (apk.isV1) {
            getV1SignChannel(apk)
        } else if (apk.isV2) {
            getV2SignChannel(apk)
        } else {
            ""
        }
    }

    private fun getV1SignChannel(apk: Apk): String {
        val eocdbuffer = apk.eocd?.eocdbuffer
        if (eocdbuffer != null) {
            val commentLength = eocdbuffer.getShort(EOCD_COMMENT_LENGTH_OFFSET)
            if (commentLength > 0) {
                val commentByteArray = ByteArray(commentLength.toInt())
                eocdbuffer.position(EOCD_COMMENT_OFFSET)
                eocdbuffer.get(commentByteArray)
                return String(commentByteArray)
            }
        }
        return ""
    }

    private fun getV2SignChannel(apk: Apk): String {
        val map = apk.v2SignBlock?.map
        if (map != null && map.containsKey(CHANNEL_ID)) {
            val byteBuffer = map[CHANNEL_ID]
            if (byteBuffer != null) {
                return String(byteBuffer.array())
            }
        }
        return ""
    }
}