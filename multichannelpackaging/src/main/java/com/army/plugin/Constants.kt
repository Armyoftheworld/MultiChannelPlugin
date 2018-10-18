package com.army.plugin

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/18
 * @description
 */
/**
 * end_of_central_directory里注释长度的偏移量
 */
val EOCD_COMMENT_LENGTH_OFFSET = 20
/**
 * end_of_central_directory里注释的偏移量
 */
val EOCD_COMMENT_OFFSET = 22
/**
 * end_of_central_directory里注释的最大长度，因为注释的长度是2个字节即16bit，所以（2^16）-1=65535即0xFFFF
 */
val EOCD_COMMENT_MAX_LENGTH = 0xFFFF
/**
 * end_of_central_directory里的核心目录结束标志
 */
val EOCD_END_TAG = 0X06054B50
/**
 * https://android.googlesource.com/platform/build/+/android-7.1.1_r46/tools/signapk/src/com/android/signapk/ApkSignerV2.java
 * 这个类里面的变量
 */
val APK_SIGNING_BLOCK_MAGIC =
    byteArrayOf(0x41, 0x50, 0x4b, 0x20, 0x53, 0x69, 0x67, 0x20, 0x42, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x34, 0x32)
/**
 * APK 签名方案 v2 分块的id
 */
val V2_ID = 0x7109871A
