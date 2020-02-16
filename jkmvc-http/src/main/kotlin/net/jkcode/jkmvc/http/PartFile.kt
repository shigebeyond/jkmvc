package net.jkcode.jkmvc.http

import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.servlet.http.Part

/**
 * 上传的文件
 */
class PartFile(protected val part: Part): Part by part {

    init {
        if(part.isText())
            throw IllegalArgumentException("非文件域");
    }
}