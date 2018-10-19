package com.army.plugin

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description 生成新的apk文件
 */
object ApkBuilder {

    fun generateChannelApk(channel: String, apk: Apk, outFile: File) {
        if (apk.isV1) {
            generateV1ChannelApk(channel, apk, outFile)
        } else if (apk.isV2) {
            generateV2ChannelApk(channel, apk, outFile)
        } else {
            throw Exception("签名方式不正确")
        }
    }

    private fun generateV1ChannelApk(channel: String, apk: Apk, outFile: File) {
        val randomAccessFile = RandomAccessFile(apk.file, "r")

        //核心目录的索引即第一块的大小
        val centralDirectoryIndex = apk.eocd?.getCentralDirectoryIndex() ?: 0
        //读取第一块的内容，没有修改，直接读取然后写入
        val centralOfZip = ByteArray(centralDirectoryIndex)
        randomAccessFile.read(centralOfZip)
        outFile.writeBytes(centralOfZip)
        println("first success")

        //读取核心目录的内容，没有修改，直接读取然后写入
        randomAccessFile.seek(centralDirectoryIndex.toLong())
        val centralDirectorySize = apk.eocd?.getCentralDirectorySize() ?: 0
        val centralDirectory = ByteArray(centralDirectorySize)
        randomAccessFile.read(centralDirectory)
        outFile.appendBytes(centralDirectory)
        println("second success")

        //读取end_of_central_directory的注释长度之前的内容
        randomAccessFile.seek((centralDirectoryIndex + centralDirectorySize).toLong())
        val eocdForword = ByteArray(EOCD_COMMENT_LENGTH_OFFSET)
        randomAccessFile.read(eocdForword)
        outFile.appendBytes(eocdForword)
        println("third forword success")

        //向注释块写入渠道信息
        val channelByteArray = channel.toByteArray()
        val commentByteBuffer = ByteBuffer.allocate(2 + channelByteArray.size)
        commentByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        commentByteBuffer.putShort(channelByteArray.size.toShort())
        commentByteBuffer.put(channelByteArray)
        outFile.appendBytes(commentByteBuffer.array())
        println("third backword success")
    }

    private fun generateV2ChannelApk(channel: String, apk: Apk, outFile: File) {
        val randomAccessFile = RandomAccessFile(apk.file, "r")

        val centralDirectoryIndex = apk.eocd!!.getCentralDirectoryIndex()
        //原签名块的大小
        val preV2SignSize = apk.v2SignBlock!!.signBuffer.capacity()
        //读取第一块的内容，没有修改，直接读取然后写入
        val centralOfZip = ByteArray(centralDirectoryIndex!! - preV2SignSize)
        randomAccessFile.read(centralOfZip)
        outFile.writeBytes(centralOfZip)
        println("first success")

        // FORMAT:
        // uint64:  size (excluding this field)
        // repeated ID-value pairs:
        //     uint64:           size (excluding this field)
        //     uint32:           ID
        //     (size - 4) bytes: value
        // uint64:  size (same as the one above)
        // uint128: magic
        randomAccessFile.seek(centralOfZip.size.toLong())
        val channelByteArray = channel.toByteArray()
        var blockV2SignSize = preV2SignSize + 8 + 4 + channelByteArray.size
        if (apk.v2SignBlock!!.map.containsKey(CHANNEL_ID)) {
            //如果已经包含渠道信息了，大小需要重新计算
            blockV2SignSize -= 8 + 4 + (apk.v2SignBlock!!.map[CHANNEL_ID]?.capacity() ?: 0)
        }
        val newV2SignBlock = ByteBuffer.allocate(blockV2SignSize)
        newV2SignBlock.order(ByteOrder.LITTLE_ENDIAN)
        //uint64:  size (excluding this field)
        newV2SignBlock.putLong((blockV2SignSize - 8).toLong())
        //重新加入ID-value pairs
        apk.v2SignBlock!!.map.forEach { id, value ->
            if (id != CHANNEL_ID) {
                newV2SignBlock.putLong((4 + value.capacity()).toLong())
                newV2SignBlock.putInt(id)
                newV2SignBlock.put(value)
            }
        }
        //添加签名信息
        newV2SignBlock.putLong((4 + channelByteArray.size).toLong())
        newV2SignBlock.putInt(CHANNEL_ID)
        newV2SignBlock.put(channelByteArray)
        // uint64:  size (same as the one above)
        // uint128: magic
        newV2SignBlock.putLong((blockV2SignSize - 8).toLong())
        newV2SignBlock.put(APK_SIGNING_BLOCK_MAGIC)
        outFile.appendBytes(newV2SignBlock.array())
        println("second success")

        //读取核心目录的内容，没有修改，直接读取然后写入
        randomAccessFile.seek(centralDirectoryIndex.toLong())
        val centralDirectorySize = apk.eocd?.getCentralDirectorySize() ?: 0
        val centralDirectory = ByteArray(centralDirectorySize)
        randomAccessFile.read(centralDirectory)
        outFile.appendBytes(centralDirectory)
        println("third success")

        //读取eocd前16位，没有修改，直接读取然后写入
        val eocdbuffer = apk.eocd?.eocdbuffer
        val eocdForwork = ByteArray(EOCD_CD_INDEX_OFFSET)
        eocdbuffer?.get(eocdForwork)
        //跳过核心位置的位移块，先获取注释（这里貌似用randomAccessFile也可以）
        eocdbuffer?.getInt()
        val commentSize = (eocdbuffer?.capacity() ?: 0) - (eocdbuffer?.position() ?: 0)
        val commentByteArray = ByteArray(commentSize)
        eocdbuffer?.get(commentByteArray)

        val eocd = ByteBuffer.allocate(eocdbuffer?.capacity() ?: 0)
        eocd.order(ByteOrder.LITTLE_ENDIAN)
        eocd.put(eocdForwork)
        eocd.putInt(centralOfZip.size + newV2SignBlock.capacity())
        eocd.put(commentByteArray)
        outFile.appendBytes(eocd.array())
        println("fourth success")
        //还原
        eocdbuffer?.flip()
    }
}