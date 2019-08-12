package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import net.jkcode.jkmvc.orm.IRelationMeta
import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.RelationType
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part

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

/****************************** Part扩展 *******************************/
/**
 * 是否文本域
 */
public fun Part.isText(): Boolean {
    return submittedFileName == null // this is what Apache FileUpload does ...
}

/**
 * 是否文件域
 */
public fun Part.isFile(): Boolean {
    return !isText()
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