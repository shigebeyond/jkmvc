package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy
import com.oreilly.servlet.multipart.FileRenamePolicy
import net.jkcode.jkutil.common.*
import sun.misc.IOUtils
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*
import javax.servlet.ServletRequestWrapper
import javax.servlet.http.HttpServletRequest

/**
 * 多部分参数(包含文本/文件类型)的请求, 即上传请求
 *   一个参数的值有2种类型: 1 多个文本值 2 单个文件的二进制数据
 *
 * @author shijianhang<772910474@qq.com>
 * @date 6/23/17 7:58 PM
 */
abstract class MultipartRequest(req: HttpServletRequest /* 请求对象 */): ServletRequestWrapper(req), HttpServletRequest by req{

    companion object{
        /**
         * 上传配置
         */
        public val uploadConfig: IConfig = Config.instance("upload")

        /**
         * 上传目录
         */
        public val uploadDirectory: String = uploadConfig.getString("uploadDirectory")!!.trim("", File.separator) // 去掉最后的路径分隔符

        /**
         * 上传文件的最大size
         */
        protected val maxPostSize: Int = fileSize2Bytes(uploadConfig["maxPostSize"]!!).toInt()

        /**
         * 禁止上传的文件扩展名
         */
        protected val forbiddenExt: List<String> = uploadConfig.getString("forbiddenExt")!!.split(',')

        /**
         * 上传文件重命名的策略
         */
        protected val uploadPolicy: FileRenamePolicy = DefaultFileRenamePolicy()

        /**
         * 服务器的url
         */
        public var serverUrl:String? = null
    }

    init {
        // fix bug: 不能实时调用 org.eclipse.jetty.server.Request.getServerName(), 因为jetty在异步servlet下会丢失 _metadata 数据 => 缓存起来
        if(serverUrl == null)
            serverUrl = req.getScheme() + "://" + req.getServerName() + ':' + req.getServerPort()
    }

    /**
     * 请求对象
     */
    protected val req: HttpServletRequest
        get() = request as HttpServletRequest

    /**
     *  上传子目录
     *      如果你需要设置上传子目录，必须在第一次调用 this.mulReq 之前设置，否则无法生效
     */
    public var uploadSubdir:String = ""

    /**
     * 多部分参数值, 一次性解析所有参数
     *    一个参数的值有2种类型
     *    1 多个文本值, 类型为 Array<String>
     *    2 单个文件的二进制数据, 类型为 File
     */
    protected val partMap: Hashtable<String, Any> by lazy{
        if(!isUpload())
            throw UnsupportedOperationException("当前请求不是上传文件的请求")

        val table = Hashtable<String, Any>()
        // 遍历每个部分, 一次性解析所有参数
        for(part in parts){
            val name = part.name
            if(table.containsKey(name))
                continue

            if(part.isFile()) // 文件域: 一个文件
                table[name] = parsePartFile(name)
            else// 文本域: 多个值
                table[name] = parsePartTexts(name)
        }

        table
    }

    /**
     * 获得多部分表单的参数名
     * @return
     */
    public fun getPartNames(): Enumeration<String>{
        return partMap.keys()
    }

    /**
     * 解析文本域
     *
     * @param name
     * @return
     */
    protected fun parsePartTexts(name: String): Array<String?> {
        return parts.mapToArray { part ->
            if(part.isText() && part.name == name) // 逐个 part 匹配 name, 可能会匹配多个 part
                part.inputStream.readBytes().toString(Charset.forName("UTF-8"))
            else
                null
        }
    }

    /**
     * 解析文件域
     *
     * @param name
     * @return
     */
    protected fun parsePartFile(name: String): File? {
        val part = getPart(name)
        if(part.isFile())
            return null

        if(isForbiddenUploadFile(part.submittedFileName))
            throw UnsupportedOperationException("文件域[$name]的文件为[${part.submittedFileName}], 属于禁止上传的文件类型")

        val fileName = URLDecoder.decode(part.submittedFileName, "UTF-8")
        val file = prepareUploadFile(fileName)
        part.write(file.absolutePath)
        return file
    }

    /**
     * 检查文件是否禁止上传
     *
     * @param fileName 上传文件名
     * @return boolean
     */
    public fun isForbiddenUploadFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.')
        return forbiddenExt.any {
            it.equals(ext, true)
        }
    }

    /**
     * 准备好上传目录 = 根目录/子目录
     *
     * @return
     */
    protected fun prepareUploadDirectory(): String {
        // 上传目录 = 根目录/子目录
        var path:String = uploadDirectory + File.separatorChar
        if(uploadSubdir != "")
            path = path + uploadSubdir + File.separatorChar
        // 如果目录不存在，则创建
        path.prepareDirectory()
        return path
    }

    /**
     * 准备好上传文件路径
     *
     * @param fileName 文件名
     * @return
     */
    protected fun prepareUploadFile(fileName: String): File {
        //准备好上传目录, 并构建文件
        val file = File(prepareUploadDirectory(), fileName)
        // 文件创建或重命名
        return file.createOrRename()
    }

    /**
     * 获得文本域的值
     *
     * @param name
     * @return
     */
    public fun getPartTexts(name: String): Array<String>? {
        val v = partMap[name]
        if(v == null)
            return null

        if(v is File)
            throw IllegalArgumentException("表单域[$name]是不是文本域")

        return v as Array<String>
    }

    /**
     * 获得上传的文件, 已保存到上传子目录
     *
     * @param name
     * @return
     */
    public fun getPartFile(name: String): File? {
        val v = partMap[name]
        if(v == null)
            return null

        if(v !is File)
            throw IllegalArgumentException("表单域[$name]是不是文件域")

        return v
    }

    /**
     * 获得上传文件的相对路径
     *
     * @param name
     * @return
     */
    public fun getPartFileRelativePath(name: String): String? {
        val file = getPartFile(name)
        if(file == null)
            return null

        return getFileRelativePath(file)
    }

    /**
     * 获得指定文件的相对路径
     *
     * @param file
     * @return
     */
    public fun getFileRelativePath(file: File): String {
        return file.path.substring(uploadDirectory.length + 1)
    }

    /**
     * 获得上传文件的url
     * @param relativePath 上传文件的相对路径
     * @return
     */
    public fun getUploadUrl(relativePath: String): String {
        if(uploadConfig.containsKey("uploadDomain"))
            return uploadConfig.getString("uploadDomain") + '/' + relativePath;
        else
            return serverUrl + contextPath + '/' + uploadConfig["uploadDirectory"] + '/' + relativePath;
    }

    /**
     * 从输入流中解析json
     * @return
     */
    public fun parseJson(): Any {
        val bytes = IOUtils.readFully(inputStream, -1, false)
        return JSON.parse(bytes)
    }

    /**
     * 从输入流中解析json对象
     * @return
     */
    public fun parseJsonObject(): JSONObject {
        return parseJson() as JSONObject
    }

    /**
     * 从输入流中解析json数组
     * @return
     */
    public fun parseJsonArray(): JSONArray {
        return parseJson() as JSONArray
    }
}