package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.detectedCharset
import java.io.File
import java.io.Reader
import java.net.URLDecoder
import java.nio.charset.Charset
import javax.servlet.http.Part

/**
 * 上传的文件
 */
class PartFile(protected val part: Part): Part by part {

    init {
        if(part.isText())
            throw IllegalArgumentException("非文件域");

        if(FileManager.isForbiddenUploadFile(part.submittedFileName))
            throw UnsupportedOperationException("文件域[$name]的文件为[${part.submittedFileName}], 属于禁止上传的文件类型")
    }

    /**
     * 存储的相对路径
     */
    protected var relativePath: String? = null

    /**
     * reader
     */
    public fun reader(charset: Charset = Charsets.UTF_8): Reader{
        return inputStream.reader()
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
            this.inputStream.reader()
            if (relativePath == null)
                throw IllegalStateException("未存储");

            return relativePath!!.substringAfterLast(File.separatorChar)
        }

    /**
     * 另存文件, 并记录相对路径
     * @param uploadDirectory
     */
    public override fun write(path: String) {
        val relativePath = FileManager.getFileRelativePath(path)
        if(this.relativePath != null){
            if(this.relativePath != relativePath)
                throw IllegalStateException("两次调用保存上传文件的目录不一致")

            return
        }

        // 另存文件
        this.part.write(path)

        // 返回相对路径
        this.relativePath = relativePath
    }

    /**
     * 另存文件, 并返回相对路径
     * @param uploadDirectory
     * @return
     */
    public fun storeAndGetRelativePath(uploadDirectory:String = ""): String {
        // 文件名
        val fileName = URLDecoder.decode(part.submittedFileName, "UTF-8")
        // 准备好上传文件路径
        val file = FileManager.prepareUploadFile(fileName, uploadDirectory)
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