package com.jkmvc.http

import com.jkmvc.common.Config
import com.jkmvc.common.convertBytes
import com.jkmvc.common.format
import com.oreilly.servlet.MultipartRequest
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy
import com.oreilly.servlet.multipart.FileRenamePolicy
import java.io.File
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * 上传文件的请求
 *
 * @author shijianhang<772910474@qq.com>
 * @date 6/23/17 7:58 PM
 */
class MultipartRequest(req: HttpServletRequest, val module:String /* 模块名，用作上传目录下的子目录 */): Request(req) {

    companion object{
        /**
         * 上传配置
         */
        protected val uploadConfig = Config.instance("upload")!!

        /**
         * 上传文件的最大size
         */
        protected val maxPostSize:Int
            get(){
                val sizeStr:String = uploadConfig["maxPostSize"]!!;
                val size:Int = sizeStr.substring(0, sizeStr.length - 1).toInt() // 大小
                val unit:Char = sizeStr[sizeStr.length - 1] // 单位
                return size * unit.convertBytes();
            }

        /**
         * 上传文件重命名的策略
         */
        protected val policy: FileRenamePolicy = DefaultFileRenamePolicy()

        /**
         * 准备好上传目录
         *
         * @param module 子目录
         * @return
         */
        public fun prepareUploadDirectory(module: String): String {
            // 目录：根目录+子目录+日期
            val path = uploadConfig["uploadDirectory"] + module + '/' + Date().format("yyyy/MM/dd") + '/';
            val dir = File(path);
            // 如果目录不存在，则创建
            if(!dir.exists())
                dir.mkdirs();
            return path;
        }
    }

    /**
     * 上传文件的请求
     */
    val mulReq:MultipartRequest = MultipartRequest(req, prepareUploadDirectory(module), maxPostSize, uploadConfig["encoding"], policy)

    /**
     * 检查是否有get/post/upload的参数
     *    兼容上传文件的情况
     * @param key
     * @return
     */
    public fun containsParameter(key: String): Boolean
    {
        return req.parameterMap.containsKey(key);
    }



    /**
     * 检查是否有get/post/upload的参数
     *    兼容上传文件的情况
     * @param key
     * @return
     */
    public fun containsParameter(key: String): Boolean
    {
        return mulReq.
    }

    /**
     * 获得get/post/upload的参数名的枚举
     *    兼容上传文件的情况
     * @return
     */
    public override fun getParameterNames():Enumeration<String>{
        return mulReq.parameterNames as Enumeration<String>;
    }

    /**
     * 获得get/post/upload的参数值
     *   兼容上传文件的情况
     * @param key
     * @return
     */
    public override fun getParameter(key: String): String? {
        return mulReq.getParameter(key);
    }


    /**
     * 获得get/post/upload的参数值
     *    兼容上传文件的情况
     * @param key
     * @return
     */
    public override fun getParameterValues(key: String): Array<String>? {
        return mulReq.getParameterValues(key)
    }

}