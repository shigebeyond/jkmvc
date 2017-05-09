package com.jkmvc.common

import java.io.File

/**
 * 遍历文件
 * @param action 访问者函数
 */
public tailrec fun File.travel(action:(file: File) -> Unit): Unit {
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