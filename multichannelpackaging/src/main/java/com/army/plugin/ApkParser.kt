package com.army.plugin

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.jar.JarFile

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/18
 * @description
 * 看官方文档https://source.android.com/security/apksigning/v2和
 * https://android.googlesource.com/platform/build/+/android-7.1.1_r46/tools/signapk/src/com/android/signapk/ApkSignerV2.java
 * EOCD的组成部分看end_of_central_directory.png
 */
object ApkParser {

    fun parse(file: File): Apk {
        val apk = Apk()
        var randomAccessFile: RandomAccessFile? = null
        try {
            randomAccessFile = RandomAccessFile(file, "r")
            //先找到end_of_central_directory这一块的内容
            var eocdBuffer = find_end_of_central_directory(randomAccessFile, EOCD_COMMENT_OFFSET)
            if (eocdBuffer == null) {
                eocdBuffer =
                        find_end_of_central_directory(randomAccessFile, EOCD_COMMENT_OFFSET + EOCD_COMMENT_MAX_LENGTH)
            }
            apk.eocd = EOCD(eocdBuffer)
            val centralDirectoryIndex = apk.eocd?.getCentralDirectoryIndex() ?: 0
            apk.v2SignBlock = findV2Sign(randomAccessFile, centralDirectoryIndex)
            if (apk.v2SignBlock == null) {
                apk.isV1 = isV1(file)
            } else {
                apk.isV2 = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            randomAccessFile?.close()
        }
        return apk
    }

    /**
     * 返回end_of_central_directory
     */
    private fun find_end_of_central_directory(randomAccessFile: RandomAccessFile, offset: Int): ByteBuffer? {
        //移动到注释的位置
        randomAccessFile.seek(randomAccessFile.length() - offset)
        val eocdBuffer = ByteBuffer.allocate(offset)
        //所有数字字段均采用小端字节序
        eocdBuffer.order(ByteOrder.LITTLE_ENDIAN)
        randomAccessFile.read(eocdBuffer.array())
        val start = offset - EOCD_COMMENT_OFFSET
        for (index in 0..start) {
            //EOCD第一块数据占四个字节，所以用int
            val endTag = eocdBuffer.getInt(index)
            //如果是核心目录结束标志
            if (endTag == EOCD_END_TAG) {
                val commentLengthIndex = index + EOCD_COMMENT_LENGTH_OFFSET
                //注释长度占两个字节。所以用short
                val commentLength = eocdBuffer.getShort(commentLengthIndex)
                //判断注释长度的值是否与剩余的偏移值一致，-2是注释长度占两个字节
                if (commentLength == (offset - commentLengthIndex - 2).toShort()) {
//                    val finalEocdBuffer = ByteBuffer.allocate(22 + commentLength)
                    val finalEocdBuffer = ByteBuffer.allocate(offset - index)
                    finalEocdBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    System.arraycopy(eocdBuffer.array(), index, finalEocdBuffer.array(), 0, finalEocdBuffer.capacity())
                    return finalEocdBuffer
                }
            }
        }
        return null
    }

    /**
     * “APK 签名分块”的格式如下（所有数字字段均采用小端字节序）：
     * size of block，以字节数（不含此字段）计 (uint64)
     * 带 uint64 长度前缀的“ID-值”对序列：
     *      ID (uint32)
     *      value（可变长度：“ID-值”对的长度 - 4 个字节）
     * size of block，以字节数计 - 与第一个字段相同 (uint64)
     * magic“APK 签名分块 42”（16 个字节）
     * 在解析 APK 时，首先要通过以下方法找到“ZIP 中央目录”的起始位置：在文件末尾找到“ZIP 中央目录结尾”记录，
     * 然后从该记录中读取“中央目录”的起始偏移量。通过 magic 值，可以快速确定“中央目录”前方可能是“APK 签名分块”。
     * 然后，通过 size of block 值，可以高效地找到该分块在文件中的起始位置。
     */
    private fun findV2Sign(randomAccessFile: RandomAccessFile, cdIndex: Int): V2SignBlock? {
        // FORMAT:
        // uint64:  size (excluding this field)
        // repeated ID-value pairs:
        //     uint64:           size (excluding this field)
        //     uint32:           ID
        //     (size - 4) bytes: value
        // uint64:  size (same as the one above)
        // uint128: magic
        randomAccessFile.seek((cdIndex - 16 - 8).toLong())
        val v2BlackMagicBuffer = ByteBuffer.allocate(16 + 8)
        v2BlackMagicBuffer.order(ByteOrder.LITTLE_ENDIAN)
        randomAccessFile.read(v2BlackMagicBuffer.array())
        //先取八个字节的size of block，下标会自动向后移8位
        //block2Size的值是另外3块的大小，不包含本身的大小
        val block2Size = v2BlackMagicBuffer.getLong()
        val byteArray = ByteArray(16)
        v2BlackMagicBuffer.get(byteArray)
        if (Arrays.equals(byteArray, APK_SIGNING_BLOCK_MAGIC)) {
            //多减了8是block2Size本身的大小
            randomAccessFile.seek(cdIndex - block2Size - 8)
            val v2SignBuffer = ByteBuffer.allocate((block2Size + 8).toInt())
            v2SignBuffer.order(ByteOrder.LITTLE_ENDIAN)
            randomAccessFile.read(v2SignBuffer.array())
            if (v2SignBuffer.getLong() == block2Size) {
                val map = linkedMapOf<Int, ByteBuffer>()
                //8和16是size和magic的大小
                while (v2SignBuffer.position() < v2SignBuffer.capacity() - 8 - 16) {
                    v2SignBuffer.getLong()
                    val id = v2SignBuffer.getInt()
                    val value = ByteBuffer.allocate(4)
                    value.order(ByteOrder.LITTLE_ENDIAN)
                    v2SignBuffer.get(value.array())
                    map[id] = value
                }
                if (map.containsKey(V2_ID)) {
                    //position还原
                    v2SignBuffer.flip()
                    return V2SignBlock(v2SignBuffer, map)
                }
            }
        }
        return null
    }

    /**
     * 如果是v1签名的apk，META-INF文件夹里面会有三个文件，多余的两个是.SF和.RSA结尾的文件
     */
    private fun isV1(file: File): Boolean {
        val jarFile = JarFile(file)
        jarFile.getJarEntry("META-INF/MANIFEST.MF") ?: return false
        for (entry in jarFile.entries()) {
            if (entry.name.matches("META-INF/\\w+\\.SF".toRegex())) {
                return true
            }
        }
        return false
    }
}