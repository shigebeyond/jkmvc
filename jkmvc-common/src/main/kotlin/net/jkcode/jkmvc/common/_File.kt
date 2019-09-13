package net.jkcode.jkmvc.common

import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*


/****************************** 文件大小 *******************************/
/**
 * 文件大小单位
 *   相邻单位相差1024倍
 */
private val fileSizeUnits: String = "BKMGT";

/**
 * 文件大小单位换算为字节数
 * @param unit
 * @return Int
 */
public fun fileSizeUnit2Bytes(unit: Char): Long {
    val i:Int = fileSizeUnits.indexOf(unit);
    if(i == -1)
        throw IllegalArgumentException("无效文件大小单位: $unit");

    return Math.pow(1024.0, i.toDouble()).toLong()
}

/**
 * 文件大小字符串换算为字节数
 * @param sizeStr
 * @return Int
 */
public fun fileSize2Bytes(sizeStr: String): Long {
    val size: Int = sizeStr.substring(0, sizeStr.length - 1).toInt() // 大小
    val unit: Char = sizeStr[sizeStr.length - 1] // 单位
    return size * fileSizeUnit2Bytes(unit)
}

/**
 * 字节数换算为文件大小字符串
 * @param size
 * @return
 */
public fun bytes2FileSize(size: Long): String {
    if (size <= 0)
        return "0B"

    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) +
            " " + fileSizeUnits[digitGroups]
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

/**
 * 准备目录
 */
public fun String.prepareDirectory(){
    val dir = File(this)
    if(!dir.exists())
        dir.mkdirs()
}

/**
 * 尝试创建文件, 如果已有同名文件, 则重命名为新文件, 加计数后缀
 * @return
 */
public fun File.createOrRename(): File {
    if (this.createNewFileSafely())
        return this

    val name = this.name
    val body: String
    val ext: String
    val dot = name.lastIndexOf(".")
    if (dot != -1) {
        body = name.substring(0, dot)
        ext = name.substring(dot)
    } else {
        body = name
        ext = ""
    }

    var count = 0
    var newfile: File
    do {
        count++
        val newName = body + count + ext
        newfile = File(this.parent, newName)
    }while (!newfile.createNewFileSafely() && count < 9999)

    return newfile
}

/**
 * 无异常的创建新文件
 * @return
 */
private fun File.createNewFileSafely(): Boolean {
    try {
        return createNewFile()
    } catch (ignored: IOException) {
        return false
    }
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
    // 分隔符
    val sp = File.separatorChar
    if(isJar()){ // 遍历jar
        val conn = openConnection() as JarURLConnection
        val jarFile = conn.jarFile
        for (entry in jarFile.entries()){
            val isDir = entry.name.endsWith(sp)
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
         *
         * => classLoader根目录开头多了一个/符号
         */
        val os = System.getProperty("os.name")
        // println("操作系统：" + os)
        if(os.startsWith("Windows", true))
            rootLen--

        /**
         * fix bug: 测试环境下路径对不上
         * classLoader根目录: /home/shi/code/java/java/jksoa/jksoa-rpc/jksoa-rpc-client/out/test/classes/
         * 文件绝对路径:       /home/shi/code/java/java/jksoa/jksoa-rpc/jksoa-rpc-client/out/production/classes/com/jksoa/example/EchoService.class
         * 参考: ClientTests.testScanClass()
         *
         * classLoader根目录: /home/shi/code/java/jksoa/jksoa-job/out/test/classes/
         * 文件绝对路径:       /home/shi/code/java/jksoa/jksoa-common/out/production/classes/com/jksoa/example/ISimpleService$DefaultImpls.class
         * 参考: JobTests.testRpcJob()
         *
         * => 直接取classes/后面的部分
         */
        //println("是否单元测试环境: " + Application.isJunitTest)

        /**
         * fix bug: gretty环境下路径对不上, 主要是启动gretty时连带启动rpc server
         * classLoader根目录: /home/shi/code/java/jksoa/jksoa-dtx/jksoa-dtx-demo/jksoa-dtx-order/build/classes/kotlin/main/
         * 文件绝对路径:       /home/shi/code/java/jksoa/jksoa-rpc/jksoa-rpc-server/build/classes/kotlin/main/net/jkcode/jksoa/rpc/example/SimpleService.class
         * 参考: gradle :jksoa-dtx:jksoa-dtx-demo:jksoa-dtx-order:appRun
         *
         * => 直接取classes/kotlin/main/后面的部分
         */
        //println("是否单元测试环境: " + Application.isJunitTest)

        File(path).travel {
            // println("文件绝对路径：" + it.path)
            val relativePath = if(Application.isJunitTest) // 测试环境下取classes/后面的部分
                                    it.path.split("classes$sp")[1]
                               else if(Application.isGretty) // gretty环境下取classes/kotlin/main/后面的部分
                                    it.path.split("classes${sp}kotlin${sp}main${sp}")[1]
                                else
                                    it.path.substring(rootLen)
            // println("文件相对路径：" + relativePath)
            action(relativePath, it.isDirectory)
        }
    }
}