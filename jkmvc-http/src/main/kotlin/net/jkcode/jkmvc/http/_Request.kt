package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.oreilly.servlet.MultipartRequest
import net.jkcode.jkmvc.orm.IRelationMeta
import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.RelationType
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Field
import java.util.*
import javax.servlet.http.HttpServletRequest

// http的日志
val httpLogger = LoggerFactory.getLogger("net.jkcode.jkmvc.db")

/****************************** HttpServletRequest扩展 *******************************/

/**
 * 是否post请求
 * @return
 */
public fun HttpServletRequest.isPost(): Boolean {
    return method.equals("POST", true);
}

/**
 * 是否option请求
 * @return
 */
public fun HttpServletRequest.isOptions(): Boolean {
    return method.equals("OPTIONS", true);
}

/**
 * 是否get请求
 * @return
 */
public fun HttpServletRequest.isGet(): Boolean {
    return method.equals("GET", true);
}

/**
 * 是否 multipart 请求
 * @return
 */
public fun HttpServletRequest.isMultipartContent(): Boolean{
    if(contentType.isNullOrEmpty())
        return false

    return contentType.startsWith("multipart/form-data", true)
}

/**
 * 是否上传文件的请求
 * @return
 */
public fun HttpServletRequest.isUpload(): Boolean{
    return isPost() && isMultipartContent()
}

/**
 * 是否ajax请求
 * @return
 */
public fun HttpServletRequest.isAjax(): Boolean {
    return "XMLHttpRequest".equals(getHeader("x-requested-with"), true) // // 通过XMLHttpRequest发送请求
            && "text/javascript, application/javascript, */*".equals(getHeader("Accept"), true); // 通过jsonp来发送请求
}

/**
 * 生成curl命令
 */
public fun HttpServletRequest.toCurlCommand(): String {
    // curl命令
    val cmd = StringBuilder("curl ")

    // 方法
    if (isGet())
        cmd.append("-G ")

    // get参数
    var qs = queryString
    qs = if(qs == null) "" else qs

    // 路径: '$url?$qs'
    cmd.append('\'').append(requestURL).append('?').append(qs).append('\'')

    //请求头： -H '$k:$v' -H '$k:$v'
    val hnames = headerNames
    while (hnames.hasMoreElements()) {
        val k = hnames.nextElement();
        val v = getHeader(k)
        // -H '$k:$v'
        cmd.append("-H '").append(k).append(':').append(v).append("' ");
    }

    // post参数： -d '$k=$v&$k=$v&'
    if (isPost()) {
        //-d '
        cmd.append("-d '")
        val pnames = parameterNames
        while (pnames.hasMoreElements()) {
            val k = pnames.nextElement();
            val v = getParameter(k)
            // $k=$v&
            cmd.append(k).append('=').append(v).append('&');
        }
        // '
        cmd.append("' ");
    }

    return cmd.toString()
}

/****************************** com.oreilly.servlet.MultipartRequest扩展 *******************************/
/**
 * 上传参数的属性
 */
val parametersField: Field = MultipartRequest::class.java.getDeclaredField("parameters").apply { this.isAccessible = true }

/**
 * 获得上传参数
 * @return
 */
public inline fun MultipartRequest.getParameterMap(): Map<String, Vector<String>> {
    return parametersField.get(this) as Map<String, Vector<String>>
}

/**
 * 上传文件的属性
 */
//val filesField: Field = MultipartRequest::class.java.getField("files").apply { isAccessible = true }

/**
 * 获得上传文件
 *     wrong - com.oreilly.servlet.UploadedFile 类不可访问
 * @return
 */
/*public inline fun MultipartRequest.getFileMap(): Map<String, UploadedFile> {
    return filesField.get(this) as Map<String, UploadedFile>
}*/

/**
 * 获得上传文件
 *    由于com.oreilly.servlet.UploadedFile 类不可访问，因此我们返回File，而不是com.oreilly.servlet.UploadedFile
 * @return
 */
public inline fun MultipartRequest.getFileMap(): Map<String, File> {
    val names = fileNames as Enumeration<String>
    if(!names.hasMoreElements())
        return emptyMap()

    val map = HashMap<String, File>()
    for (name in names){
        val file = this.getFile(name)
        if (isSafeUploadFile(file)) {
            map[name] = file
        }else{
            file.delete()
        }
    }
    return map
}

/**
 * 检查是否安全的上传文件
 *
 * @param uploadFile 上传文件
 * @return boolean
 */
public inline fun isSafeUploadFile(uploadFile: File): Boolean {
    val ext = uploadFile.extension
    return !ext.equals("jsp", true) && ext.equals("jspx", true)
}

/****************************** Orm扩展 *******************************/
/**
 * 从请求中解析并设置多个字段值
 *
 * @param values   字段值的数组：<字段名 to 字段值>
 * @param expected 要设置的字段名的数组
 */
public fun Orm.fromRequest(req: HttpRequest, expected: List<String> = emptyList()): Unit {
    // 默认为请求中的所有列
    val columns = if (expected.isEmpty()) req.parameterNames.iterator() else expected!!.iterator()

    // 取得请求中的指定参数
    for (column in columns)
        setFromRequest(column, req.getParameter(column))
}

/**
 * 设置单个字段值
 *
 * @param column 列
 * @param value 值
 */
private fun Orm.setFromRequest(column: String, value: Any?) {
    if(value == null)
        return;

    // 获得关联属性
    val relation = ormMeta.getRelation(column)

    // 1 普通属性, 智能设置string值
    if (relation == null) {
        setIntelligent(column, value as String)
        return
    }

    // 2 关联对象属性, 直接反json化
    val json = if(value is String) JSON.parse(value) else value

    // 2.1 有一个
    if (relation!!.type == RelationType.BELONGS_TO || relation.type == RelationType.HAS_ONE) {
        val related = buildRelatedFromRequest(column, relation, json)
        set(column, related)
        return
    }

    // 2.2 有多个
    if(json !is JSONArray)
        throw IllegalArgumentException("类[${javaClass}]的关联属性[${column}]赋值需要是JSONArray")
    val related = json.map {
        buildRelatedFromRequest(column, relation, it, "数组")
    }
    set(column, related)
}

/**
 * 从请求中构建关联对象
 * @param column 列
 * @param relation 列的关联关系
 * @param json 列的值
 * @return 关联对象
 */
private fun Orm.buildRelatedFromRequest(column: String, relation: IRelationMeta, json: Any, postfix: String = ""): Orm {
    if(json !is JSONObject)
        throw IllegalArgumentException("类[$javaClass]的关联属性[$column]赋值需要是JSONObject$postfix")

    val related = relation.newModelInstance() as Orm
    for ((k, v) in json)
        related.setFromRequest(k, v)
    return related
}