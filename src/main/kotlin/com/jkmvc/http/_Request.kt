package com.jkmvc.http

import com.jkmvc.common.findProperty
import java.util.Enumeration
import java.util.HashMap
import com.oreilly.servlet.MultipartRequest
import kotlin.reflect.KProperty1

/****************************** com.oreilly.servlet.MultipartRequest扩展 *******************************/
/**
 * 上传参数的属性
 */
val parametersProp: KProperty1<MultipartRequest, Map<String, Array<String>>> = (MultipartRequest::class.findProperty("parameters") as KProperty1<MultipartRequest, Map<String, Array<String>>>?)!!

/**
 * 获得上传参数
 */
public fun MultipartRequest.getParameterMap(): Map<String, Array<String>> {
    return parametersProp.get(this)
}

/**
 * 上传文件的属性
 */
val filesProp: KProperty1<MultipartRequest, Map<String, Array<String>>> = (MultipartRequest::class.findProperty("files") as KProperty1<MultipartRequest, Map<String, Array<String>>>?)!!

/**
 * 获得上传文件
 */
public fun MultipartRequest.getFileMap(): Map<String, Array<String>> {
    return filesProp.get(this)
}