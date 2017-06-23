package com.jkmvc.http

import com.oreilly.servlet.MultipartRequest
import java.io.File
import java.lang.reflect.Field
import java.util.Enumeration
import java.util.HashMap



/****************************** com.oreilly.servlet.MultipartRequest扩展 *******************************/
/**
 * 上传参数的属性
 */
val parametersField: Field = MultipartRequest::class.java.getField("parameters").apply { this.isAccessible = true }

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

