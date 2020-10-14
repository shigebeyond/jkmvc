package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.detectedCharset
import net.jkcode.jkutil.common.httpLogger
import java.io.File
import java.io.Reader
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.servlet.http.Part
import net.jkcode.jkutil.common.writeFromInput
import java.io.FileOutputStream

/**
 * 上传的文件
 */
class PartFile(protected val part: Part): Part by part {

    init {
        if(part.isText)
            throw IllegalArgumentException("Not file field");

        if(UploadFileUtil.isForbiddenUploadFile(part.submittedFileName))
            throw UnsupportedOperationException("File field [$name] has a forbidden file [${part.submittedFileName}]")
    }

    /**
     * 存储的相对路径
     */
    protected var relativePath: String? = null

    /**
     * reader
     */
    public fun reader(charset: Charset = Charsets.UTF_8): Reader{
        return inputStream.reader(charset)
    }

    /**
     * 转为字节数组
     */
    public fun readBytes(): ByteArray{
        return inputStream.readBytes()
    }

    /**
     * 转为文本
     */
    public fun readText(): String {
        return inputStream.reader().readText()
    }

    /**
     * 获得存储后的文件名
     */
    public val storedFileName: String
        get() {
            if (relativePath == null)
                throw IllegalStateException("Not stored, you must call storeAndGetRelativePath()/write() first");

            return relativePath!!.substringAfterLast(File.separatorChar)
        }

    /**
     * 另存文件, 并记录相对路径
     * @param uploadDirectory
     */
    public override fun write(path: String) {
        val relativePath = UploadFileUtil.getFileRelativePath(path)
        if(this.relativePath != null){
            if(this.relativePath != relativePath)
                throw IllegalStateException("Twice call write(path), and use different path")

            return
        }

        // 另存文件
        // bug: 报错 FileNotFoundException, 参考 https://bz.apache.org/bugzilla/show_bug.cgi?id=54971
        // 原因: 参数只能是文件的相对路径, 相对于 MultipartConfig.location, jetty/tomcat 会存入该目录
        //this.part.write(path)
        // 解决: 只能读 Part.inputStream 来写文件
        FileOutputStream(path).writeFromInput(this.part.inputStream)

        // 返回相对路径
        this.relativePath = relativePath
    }

    /**
     * 另存文件, 并返回相对路径
     * @param uploadDirectory
     * @return
     */
    public fun storeAndGetRelativePath(uploadDirectory:String = ""): String? {
        // 文件名
        var fileName = part.submittedFileName
        if(fileName.isNullOrEmpty() /* && part.size == 0L */){
            httpLogger.warn("上传文件名为空")
            return null
        }

        fileName = URLDecoder.decode(fileName, "UTF-8")
        // 准备好上传文件路径
        val file = UploadFileUtil.prepareUploadFile(fileName, uploadDirectory)
        // 另存文件
        write(file.absolutePath)

        // 返回相对路径
        return relativePath!!
    }

    /**
     * 是否utf8编码
     */
    public fun isValidUTF8(): Boolean {
        return "UTF-8".equals(this.detectedCharset(), true)
    }

    /**
     * 识别编码
     */
    public fun detectedCharset(): String {
        return inputStream.detectedCharset()
    }

}