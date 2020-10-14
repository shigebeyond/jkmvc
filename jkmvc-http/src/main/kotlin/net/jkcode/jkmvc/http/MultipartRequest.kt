package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import net.jkcode.jkutil.common.*
import sun.misc.IOUtils
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.HashMap

/**
 * 处理多部分参数(包含文本/文件类型)的请求, 即上传请求
 *   对上传请求中文本字段, 在 servlet3 中跟在普通请求中一样, 直接使用 parameterMap / getParameter() 来获取, 因此不用改写 parameterMap / getParameter()
 *   对上传请求中文件字段, 设计单独的api来获取: partFileMap/partFileNames/getPartFile()/getPartFileValues()
 *
 *   Servlet3.0中提供了对文件上传的原生支持，我们不需要借助任何第三方上传组件(如Apache的commons-fileupload组件)，直接使用Servlet3.0提供的API就行。
 *   1. 一个参数封装为类型 Part, 他的值有2种类型: 1 文本值 2 文件的二进制数据
 *   2. 限制文件大小是在 web.xml 的 <servlet> 内部配置, 单位是byte
 *   <multipart-config>
 *       <max-file-size>500</max-file-size>
 *       <max-request-size>700</max-request-size>
 *       <file-size-threshold>0</file-size-threshold>
 *   </multipart-config>
 *   3. jkmvc使用的是 filter, 因此限制文件大小暂时不支持, 会导致上传文件报错: No multipart config for servlet, 在 JkFilter 中临时处理
 *
 *
 * @author shijianhang<772910474@qq.com>
 * @date 6/23/17 7:58 PM
 */
abstract class MultipartRequest(req: HttpServletRequest /* 请求对象 */): IHttpRequest(req){

    companion object{
        /**
         * 上传配置
         */
        public val uploadConfig: IConfig = Config.instance("upload")

        /**
         * 全局服务器的url, 只需要初始化一次
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
     * 内部请求对象
     *   如果不是内部请求, 则返回null
     */
    public val innerRequest: InnerHttpRequest?
        get(){
            if(isInner)
                return request as InnerHttpRequest

            return null
        }

    /**
     * 上传请求中文件参数
     */
    public val partFileMap: Map<String, List<PartFile>> by lazy{
        if(!isUpload){
            emptyMap<String, List<PartFile>>()
        }else {
            val map = HashMap<String, MutableList<PartFile>>()
            // 遍历每个部分, 一次性解析所有参数
            for (part in parts) {
                if(part.isFile) {
                    val files = map.getOrPut(part.name) {
                        LinkedList()
                    }
                    files.add(PartFile(part))
                }
            }

            map
        }
    }

    /**
     * 上传请求中文件参数名
     */
    public val partFileNames: Set<String>
        get() = partFileMap.keys

    /**
     * 获得上传的文件
     *
     * @param name
     * @return
     */
    public fun getPartFile(name: String): PartFile? {
        return partFileMap[name]?.firstOrNull()
    }

    /**
     * 获得上传的文件
     *
     * @param name
     * @return
     */
    public fun getPartFileValues(name: String): List<PartFile>? {
        return partFileMap[name]
    }

    /**
     * 保存上传文件, 并返回相对路径
     *
     * @param name
     * @return
     */
    public fun storePartFileAndGetRelativePath(name: String): String? {
        return getPartFile(name)?.storeAndGetRelativePath()
    }

    /**
     * 获得指定文件的相对路径
     *
     * @param file
     * @return
     */
    public fun getFileRelativePath(file: String): String {
        return UploadFileUtil.getFileRelativePath(file)
    }

    /**
     * 获得上传文件的url
     * @param relativePath 上传文件的相对路径
     * @return
     */
    public fun getUploadUrl(relativePath: String): String {
        return uploadConfig.getString("uploadDomain") + '/' + relativePath;
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