package com.jkmvc.common

import java.io.File
import java.io.FilenameFilter
import java.util.*

public tailrec fun File.travel(action:(file: File) -> Unit): Unit {
    val files = listFiles()
    if (files == null) {
        return
    }

    for (file in files) {
        if(file.isFile)
            action(file)
        else
            travel(action)
    }
}

fun main(args: Array<String>) {
    File("/oldhome/shi/code/java/jkmvc/src/main/kotlin/com/jkmvc/common").travel{
        println(it)
    }
}