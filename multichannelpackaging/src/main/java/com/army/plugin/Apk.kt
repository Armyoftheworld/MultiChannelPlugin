package com.army.plugin

import java.nio.ByteBuffer

/**
 * @author Army
 * @version V_1.0.0
 * @date 2018/10/18
 * @description
 */
class Apk {
    var eocd: EOCD? = null
    var v2SignBlock: V2SignBlock? = null
    var isV1: Boolean = false
    var isV2: Boolean = false
}