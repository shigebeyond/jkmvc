package com.jkmvc.http

import com.jkmvc.common.Config
import com.jkmvc.common.convertBytes
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
class MultipartRequest(req: HttpServletRequest): Request(req) {

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
        protected val uploadPolicy: FileRenamePolicy = DefaultFileRenamePolicy()
    }

    /**
     * 上传文件的请求
     *    递延执行，以便能获得在 controller#action 动态设置的 uploadSubdir，用以构建上传目录
     *    第一次调用 this.mulReq 时，会解析请求中的字段与文件，并将文件保存到指定的目录 = 根目录/子目录
     */
    protected val mulReq:com.oreilly.servlet.MultipartRequest by lazy{
        com.oreilly.servlet.MultipartRequest(req, prepareUploadDirectory(), maxPostSize, uploadConfig["encoding"], uploadPolicy)
    }

    /**
     * 准备好上传目录 = 根目录/子目录
     *
     * @return
     */
    public fun prepareUploadDirectory(): String {
        // 上传目录 = 根目录/子目录
        var path:String = uploadConfig["uploadDirectory"]!!
        if(uploadSubdir != "")
            path = path + '/' + uploadSubdir + '/'
        val dir = File(path);
        // 如果目录不存在，则创建
        if(!dir.exists())
            dir.mkdirs();
        return path
    }

    /************************ 获得请求参数 *************************/
    /**
     * 检查是否有get/post的参数
     *
     * @param key
     * @return
     */
    public override fun containsParameter(key: String): Boolean {
        return mulReq.getParameterMap().containsKey(key)
    }

    /**
     * 获得get/post的参数名的枚举
     *
     * @return
     */
    public override fun getParameterNames():Enumeration<String>{
        return mulReq.parameterNames as Enumeration<String>;
    }

    /**
     * 获得get/post的参数值
     *
     * @param key
     * @return
     */
    public override fun getParameter(key: String): String? {
        return mulReq.getParameter(key);
    }

    /**
     * 获得get/post的参数值
     *
     * @param key
     * @return
     */
    public override fun getParameterValues(key: String): Array<String>? {
        return mulReq.getParameterValues(key)
    }

    /**
     * 获得上传参数
     * @return
     */
    public override fun getParameterMap(): Map<String, Array<String>>{
        return mulReq.getParameterMap()
    }

    /************************ 获得上传文件 *************************/
    /**
     * 检查是否有上传文件
     *
     * @param key
     * @return
     */
    public fun containsFile(key: String): Boolean {
        return mulReq.getFilesystemName(key) != null
    }

    /**
     * 获得文件名的枚举
     * @return
     */
    public fun getFileNames(): Enumeration<String>{
        return mulReq.fileNames as Enumeration<String>
    }

    /**
     * 获得某个上传文件
     *
     * @param name
     * @return
     */
    public fun getFile(name: String): File{
        return mulReq.getFile(name);
    }

    /**
     * 获得上传文件
     * @return
     */
    public fun getFileMap(): Map<String, File>{
        return mulReq.getFileMap()
    }

}