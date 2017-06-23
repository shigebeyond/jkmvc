package com.jkmvc.http

import com.jkmvc.common.Config
import com.jkmvc.common.convertBytes
import com.jkmvc.common.format
import com.oreilly.servlet.MultipartRequest
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy
import java.io.File
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * 服务端对象，用于处理请求
 *
 * @author shijianhang
 * @date 2017-6-23 上午9:27:56
 *
 */
object Upload{

    /**
     * 上传配置
     */
    val config = Config.instance("upload")!!

    /**
     * 上传文件的最大size
     */
    val maxPostSize:Int
        get(){
            val sizeStr:String = config["maxPostSize"]!!;
            val size:Int = sizeStr.substring(0, sizeStr.length - 1).toInt() // 大小
            val unit:Char = sizeStr[sizeStr.length - 1] // 单位
            return size * unit.convertBytes();
        }


    public fun newMultipartRequest(module: String, req: HttpServletRequest): MultipartRequest {
        return MultipartRequest(req, prepareUploadDirectory(module), maxPostSize, config["encoding"], DefaultFileRenamePolicy())
    }

    /**
     * 准备好上传目录
     *
     * @param module 子目录
     * @return
     */
    public fun prepareUploadDirectory(module: String): String {
        // 目录：根目录+子目录+日期
        val path = config["uploadDirectory"] + module + '/' + Date().format("yyyy/MM/dd") + '/';
        val dir = File(path);
        // 如果目录不存在，则创建
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }
}