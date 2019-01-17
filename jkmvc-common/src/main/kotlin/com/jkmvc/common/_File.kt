package com.jkmvc.common

import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.util.*

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

/****************************** 文本处理 *******************************/
/**
 * 整个文件替换文本内容
 *
 * @param transform 文本转换lambda
 */
public fun File.replaceText(transform:(txt: String) -> String){
    val txt = this.readText()
    val newTxt = transform(txt)
    this.writeText(newTxt)
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
 * 获得根资源
 */
public fun ClassLoader.getRootResource(): URL {
    val res = getResource("/") // web环境
    if(res != null)
        return res
    return getResource(".") // cli环境
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
        val root = Thread.currentThread().contextClassLoader.getRootResource().path
         // println("classLoader根目录：" + root)
         // println("当前目录：" + path)
        var rootLen = root.length

        /**
         * fix bug: window下路径对不上
         * classLoader根目录：/C:/Webclient/tomcat0/webapps/ROOT/WEB-INF/classes/
         * 文件绝对路径：      C:\Webclient\tomcat0\webapps\ROOT\WEB-INF\classes\com\jkmvc\szpower\controller\WorkInstructionController.class
         * 文件相对路径=文件绝对路径-跟路径：om\jkmvc\szpower\controller\WorkInstructionController.class
         * => com变为om，少了一个c，导致根据文件相对路径来加载对应的class错误
         */
        val os = System.getProperty("os.name")
        // println("操作系统：" + os)
        if(os.startsWith("Windows", true))
            rootLen--

        /**
         * fix bug: 测试环境下路径对不上
         * classLoader根目录: /home/shi/code/java/java/jksoa/jksoa-client/out/test/classes/
         * 文件绝对路径:       /home/shi/code/java/java/jksoa/jksoa-client/out/production/classes/com/jksoa/example/EchoService.class
         * 参考: ClientTests.testScanClass()
         */
        //println("是否单元测试环境: " + Application.isJunitTest)
        if(Application.isJunitTest)
            rootLen = rootLen + 6

        File(path).travel {
            // println("文件绝对路径：" + it.path)
            val relativePath = if(os.startsWith("Windows", true))
                                    it.path.substring(rootLen)
                                else
                                    it.path.substring(rootLen)
            // println("文件相对路径：" + relativePath)
            action(relativePath, it.isDirectory)
        }
    }
}