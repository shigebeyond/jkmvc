package com.jkmvc.common

import java.io.File

/**
 * 文件大小单位
 *   相邻单位相差1024倍
 */
val fileSizeUnits: String = "BKMGT";

/**
 * 其他大小单位换算为字节数
 * @return Int
 */
fun Char.convertBytes():Int{
    val i:Int = fileSizeUnits.indexOf(this);
    if(i == -1)
        throw Exception("无效文件大小单位: $this");

    return Math.pow(1024.toDouble(), i.toDouble()).toInt()
}

/**
 * 遍历文件
 * @param action 访问者函数
 */
public /*tailrec*/ fun File.travel(action:(file: File) -> Unit): Unit {
    val files = listFiles()
    if (files == null) {
        return
    }

    for (file in files) {
        if(file.isFile)
            action(file)
        else
            file.travel(action)
    }
}