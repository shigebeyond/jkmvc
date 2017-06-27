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
abstract class MultipartRequest(protected val req:HttpServletRequest /* 请求对象 */): HttpServletRequest by req{

    companion object{
        /**
         * 上传配置
         */
        public val uploadConfig = Config.instance("upload")!!

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
     * 服务器的url
     */
    public val serverUrl:String
        get() = req.getScheme() + "://" + req.getServerName() + ':' + req.getServerPort()

    /**
     * 是否是上传文件的请求
     */
    protected val uploaded:Boolean = req.isUpload()

    /**
     *  上传子目录，用在子类 MultipartRequest 中
     *      如果你需要设置上传子目录，必须在第一次调用 this.mulReq 之前设置，否则无法生效
     */
    public var uploadSubdir:String = ""

    /**
     * 上传文件的请求
     *    递延执行，以便能获得在 controller#action 动态设置的 uploadSubdir，用以构建上传目录
     *    第一次调用 this.mulReq 时，会解析请求中的字段与文件，并将文件保存到指定的目录 = 根目录/子目录
     */
    protected val mulReq:com.oreilly.servlet.MultipartRequest by lazy(LazyThreadSafetyMode.NONE){
        if(!uploaded)
            throw Exception("当前请求不是上传文件的请求")

        com.oreilly.servlet.MultipartRequest(req, prepareUploadDirectory(), maxPostSize, uploadConfig["encoding"], uploadPolicy)
    }

    /**
     * 准备好上传目录 = 根目录/子目录
     *
     * @return
     */
    protected fun prepareUploadDirectory(): String {
        // 上传目录 = 根目录/子目录
        var path:String = uploadConfig["uploadDirectory"]!! + '/'
        if(uploadSubdir != "")
            path = path + uploadSubdir + '/'
        val dir = File(path);
        // 如果目录不存在，则创建
        if(!dir.exists())
            dir.mkdirs();
        return path
    }

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

    /**
     * 获得某个上传文件的相对路径
     *
     * @param name
     * @return
     */
    public fun uploadRelativePath(name: String): String{
        val file = mulReq.getFile(name);
        val uploadDirLen = uploadConfig["uploadDirectory"]!!.length + 1 // 如 upload/
        return file.path.substring(uploadDirLen)
    }

    /**
     * 获得上传文件的url
     * @param relativePath 相对路径
     * @return
     */
    public fun uploadUrl(relativePath:String):String {
        if(uploadConfig["uploadDomain"] == null)
            return serverUrl + contextPath + '/' + uploadConfig["uploadDirectory"] + '/' + relativePath;
        else
            return uploadConfig["uploadDomain"] + '/' + relativePath;
    }
}