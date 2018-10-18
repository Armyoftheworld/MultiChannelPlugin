package com.army.plugin

import java.nio.ByteBuffer

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description
 */
class EOCD(val eocdbuffer: ByteBuffer?) {

    /**
     * 看end_of_central_directory.png，16存放的即central directory的起始偏移量
     */
    fun getCentralDirectoryIndex() = eocdbuffer?.getInt(16)

    fun getCentralDirectorySize() = eocdbuffer?.getInt(12)
}