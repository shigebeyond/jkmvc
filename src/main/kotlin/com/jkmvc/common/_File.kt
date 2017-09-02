package com.jkmvc.common

import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile

/****************************** 文件大小 *******************************/
/**
 * 文件大小单位
 *   相邻单位相差1024倍
 */
private val fileSizeUnits: String = "BKMGT";

/**
 * 其他大小单位换算为字节数
 * @return Int
 */
public fun Char.convertBytes():Int{
    val i:Int = fileSizeUnits.indexOf(this);
    if(i == -1)
        throw IllegalArgumentException("无效文件大小单位: $this");

    return Math.pow(1024.toDouble(), i.toDouble()).toInt()
}

/****************************** 文件路径 *******************************/
/**
 * 判断是否是绝对路径
 * @param path
 * @return
 */
public fun String.isAbsolutePath(): Boolean {
    return startsWith("/") || indexOf(":") > 0;
}

/****************************** 文件遍历 *******************************/
/**
 * 遍历文件
 *   使用栈来优化
 * @param action 访问者函数
 */
public fun File.travel(action:(file: File) -> Unit): Unit {
    val files: Stack<File> = Stack()
    files.push(this)
    travelFiles(files, action)
}

/**
 * 遍历文件
 * @param files 文件栈
 * @param action 访问者函数
 */
public fun travelFiles(files: Stack<File>, action:(file: File) -> Unit): Unit {
    while (!files.isEmpty()){
        val file = files.pop();
        if(file.isDirectory)
            files.addAll(file.listFiles())
        else
            action(file)
    }
}

/****************************** URL遍历 *******************************/
// jar url协议的正则
private val jarUrlProtocol = "jar|zip|wsjar|code-source".toRegex()

/**
 * url是否是jar包
 */
public fun URL.isJar(): Boolean {
    return jarUrlProtocol.matches(protocol)
}

/**
 * 遍历url中的资源
 * @param action 访问者函数
 */
public fun URL.travel(action:(relativePath:String, isDir:Boolean) -> Unit):Unit{
    if(isJar()){ // 遍历jar
        val conn = openConnection() as JarURLConnection
        val jarFile = conn.jarFile
        for (entry in jarFile.entries()){
            val isDir = entry.name.endsWith(File.separatorChar)
            action(entry.name, isDir);
        }
    }else{ // 遍历目录
        val root = this.javaClass.getResource("/").path
        File(path).travel {
            action(it.path.substring(root.length), it.isDirectory)
        }
    }
}