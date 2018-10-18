package com.army.plugin

import java.nio.ByteBuffer

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/19
 * @description
 */
data class V2SignBlock(val signBuffer: ByteBuffer, val map: LinkedHashMap<Int, ByteBuffer>)