package com.jkmvc.http

import org.apache.commons.fileupload.FileItem

/**
 * 获得文件名
 * @return
 */
public fun FileItem.filename(): String
{
    return name.substringAfterLast("\\")
}