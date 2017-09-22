package com.jkmvc.http

import com.jkmvc.common.isNullOrEmpty
import com.jkmvc.orm.IOrm
import com.jkmvc.orm.Orm
import com.oreilly.servlet.MultipartRequest
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Field
import java.util.*
import javax.servlet.http.HttpServletRequest

// http的日志
val httpLogger = LoggerFactory.getLogger("com.jkmvc.db")

/****************************** HttpServletRequest扩展 *******************************/

/**
 * 是否post请求
 * @return
 */
public fun HttpServletRequest.isPost(): Boolean {
    return method === "POST";
}

/**
 * 是否get请求
 * @return
 */
public fun HttpServletRequest.isGet(): Boolean {
    return method === "GET";
}

/**
 * 是否 multipart 请求
 * @return
 */
public fun HttpServletRequest.isMultipartContent(): Boolean{
    return contentType.toLowerCase().startsWith("multipart/form-data")
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
    return "XMLHttpRequest".equals(getHeader("x-requested-with")) // // 通过XMLHttpRequest发送请求
            && "text/javascript, application/javascript, */*".equals(getHeader("Accept")); // 通过jsonp来发送请求
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
public inline fun MultipartRequest.getParameterMap(): Map<String, Array<String>> {
    return parametersField.get(this) as Map<String, Array<String>>
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
    val ext = uploadFile.extension.toLowerCase()
    return ext != "jsp" && ext != "jspx"
}

/****************************** Orm扩展 *******************************/
/**
 * 设置多个字段值
 *
 * @param values   字段值的数组：<字段名 to 字段值>
 * @param expected 要设置的字段名的数组
 * @return
 */
public fun Orm.requestValues(req: Request, expected: List<String>? = null): Orm {
    if (expected.isNullOrEmpty())
    {
        // 取得请求中的所有参数
        val columns : Enumeration<String> = req.parameterNames
        while(columns.hasMoreElements())
        {
            val column = columns.nextElement();
            setIntelligent(column, req.getParameter(column)!!)
        }
    }
    else
    {
        // 取得请求中的指定参数
        for (column in expected!!)
            setIntelligent(column, req.getParameter(column)!!)
    }
    return this;
}
