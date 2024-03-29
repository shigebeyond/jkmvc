package net.jkcode.jkmvc.http

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import io.netty.handler.codec.http.cookie.DefaultCookie
import net.jkcode.jkmvc.orm.Orm
import net.jkcode.jkmvc.orm.relation.IRelation
import java.net.URI
import javax.servlet.DispatcherType
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession
import javax.servlet.http.Part

/****************************** HttpSession扩展 *******************************/

/**
 * 获得并删除属性
 * @param key
 * @return
 */
fun HttpSession.getAndRemoveAttribute(key: String): Any? {
    val result = getAttribute(key)
    if (result != null)
        removeAttribute(key)
    return result
}

/****************************** HttpServletRequest扩展 *******************************/

/**
 * 是否内部请求: INCLUDE/FORWARD
 * @return
 */
public val HttpServletRequest.isInner: Boolean
    get() = dispatcherType == DispatcherType.INCLUDE || dispatcherType == DispatcherType.FORWARD

/**
 * 是否post请求
 * @return
 */
public val HttpServletRequest.isPost: Boolean
    get() {
        return method.equals("POST", true);
    }

/**
 * 是否option请求
 * @return
 */
public val HttpServletRequest.isOptions: Boolean
    get() {
        return method.equals("OPTIONS", true);
    }

/**
 * 是否get请求
 * @return
 */
public val HttpServletRequest.isGet: Boolean
    get() {
        return method.equals("GET", true);
    }

/**
 * 是否 multipart 请求
 * @return
 */
public val HttpServletRequest.isMultipartContent: Boolean
    get() {
        if (contentType.isNullOrEmpty())
            return false

        return contentType.startsWith("multipart/form-data", true)
    }

/**
 * 是否上传文件的请求
 * @return
 */
public val HttpServletRequest.isUpload: Boolean
    get() {
        return isPost && isMultipartContent
    }

/**
 * 是否ajax请求
 * @return
 */
public val HttpServletRequest.isAjax: Boolean
    get() {
        return "XMLHttpRequest".equals(getHeader("x-requested-with"), true) // // 通过XMLHttpRequest发送请求
                && "text/javascript, application/javascript, */*".equals(getHeader("Accept"), true); // 通过jsonp来发送请求
    }

/**
 * 来源url
 */
public val HttpServletRequest.referer: String?
    get() {
        return getHeader("referer")
    }

/**
 * 来源主机
 */
public val HttpServletRequest.refererHost: String?
    get() {
        val referer = this.referer
        if (referer.isNullOrBlank())
            return null

        return URI(referer).host
    }

/**
 * 设置多个属性
 */
public fun HttpServletRequest.setAttributes(data: Map<String, Any?>) {
    for ((k, v) in data)
        setAttribute(k, v);
}

/**
 * 读取某个属性, 如果没有则设置
 */
public fun HttpServletRequest.getOrPutAttribute(key: String, default: () -> Any): Any {
    var value = getAttribute(key)
    // 如果没有则设置
    if (value == null) {
        value = default()
        setAttribute(key, value)
    }
    return value
}

/**
 * 读取某个属性, 如果没有则设置
 */
public fun HttpSession.getOrPutAttribute(key: String, default: () -> Any): Any {
    var value = getAttribute(key)
    // 如果没有则设置
    if (value == null) {
        value = default()
        setAttribute(key, value)
    }
    return value
}

/**
 * 读取某个会过期的属性, 如果没有则设置, 如果过期则重新生成
 */
public fun HttpSession.getOrPutExpiringAttribute(key: String, expireSencond: Long = 6000, default: () -> Any): Any? {
    // 读取
    var result = getExpiringAttribute(key)
    // 如果没有/过期, 则设置
    if (result == null) {
        result = default()
        putExpiringAttribute(key, result, expireSencond)
    }

    return result
}

/**
 * 设置某个会过期的属性
 */
public fun HttpSession.putExpiringAttribute(key: String, value: Any, expireSencond: Long = 6000){
    // 过期时间
    val expire = System.currentTimeMillis() + expireSencond * 1000
    // 值 + 过期时间
    val pair = value to expire
    setAttribute(key, pair)
}

/**
 * 读取某个会过期的属性, 过期则返回null
 */
public fun HttpSession.getExpiringAttribute(key: String): Any? {
    var pair = getAttribute(key) as Pair<Any, Long>?
    if (pair == null)
        return null

    // 检查是否过期
    if (pair.second < System.currentTimeMillis()) {
        removeAttribute(key)
        return null
    }

    return pair.first
}

/**
 * 生成curl命令
 */
public fun HttpServletRequest.toCurlCommand(): String {
    // curl命令
    val cmd = StringBuilder("curl ")

    // 方法
    if (isGet)
        cmd.append("-G ")

    // get参数
    var qs = queryString
    qs = if (qs == null) "" else qs

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
    if (isPost) {
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

/****************************** Cookie扩展 *******************************/

/**
 * 转为netty的cookie
 */
public fun Cookie.toNettyCookie(): DefaultCookie {
    val result = DefaultCookie(this.name, this.value)
    result.isHttpOnly = this.isHttpOnly
    result.isSecure = this.secure
    result.setDomain(this.domain)
    result.setPath(this.path)
    result.setMaxAge(this.maxAge.toLong())
    return result
}

/****************************** Part扩展 *******************************/
/**
 * 是否文本域
 */
public val Part.isText: Boolean
    get() {
        return submittedFileName == null // this is what Apache FileUpload does ...
    }

/**
 * 是否文件域
 */
public val Part.isFile: Boolean
    get() {
        return !isText
    }

/****************************** Orm扩展 *******************************/
/**
 * 从请求中解析并设置多个字段值
 *    只处理当前对象属性, 不处理关联对象属性
 *
 * @param values   字段值的数组：<字段名 to 字段值>
 * @param include 要设置的字段名的数组
 * @param exclude 要排除的字段名的列表
 */
public fun <T : Orm> T.fromRequest(req: HttpRequest, include: List<String> = emptyList(), exclude: List<String> = emptyList()): T {
    // 1 文本参数
    val columns = if (include.isEmpty())
                    if(this.loaded) // 已加载: 取主键外的所有列, 对应update()
                        this.ormMeta.propsExcludePk
                    else // 未加载: 所有列, 对应create()
                        this.ormMeta.props
                else
                    include

    // 取得请求中的指定参数
    for (column in columns) {
        if (exclude.contains(column))
            continue

        // 1 文本参数
        val value = req.getParameter(column)
        if (value != null) {
            setFromRequest(column, value)
            continue
        }

        // 2 文件参数
        val file = req.getPartFile(column)
        if (file != null) {
            // 先保存文件
            val path = file!!.storeAndGetRelativePath()
            // 属性值为文件相对路径
            set(column, path)
        }
    }
    return this
}

/**
 * 设置单个字段值
 *
 * @param column 列
 * @param value 值
 */
private fun Orm.setFromRequest(column: String, value: Any?) {
    if (value == null)
        return;

    // 获得关联属性
    val relation = ormMeta.getRelation(column)

    // 1 普通属性, 智能设置string值
    if (relation == null) {
        setIntelligent(column, value as String)
        return
    }

    // 2 关联对象属性, 直接反json化
    val json = if (value is String) JSON.parse(value) else value

    // 2.1 有一个
    if (relation.isBelongsTo || relation.isHasOne) {
        val related = buildRelatedFromRequest(column, relation, json)
        set(column, related)
        return
    }

    // 2.2 有多个
    if (json !is JSONArray)
        throw IllegalArgumentException("Class [${javaClass}]'s related property [${column}] only accept JSONArray")
    val related = json.map {
        buildRelatedFromRequest(column, relation, it, "Array")
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
private fun Orm.buildRelatedFromRequest(column: String, relation: IRelation, json: Any, postfix: String = ""): Orm {
    if (json !is JSONObject)
        throw IllegalArgumentException("class [$javaClass]'s related property [$column] only accept JSONObject$postfix")

    val related = relation.newModelInstance() as Orm
    for ((k, v) in json)
        related.setFromRequest(k, v)
    return related
}