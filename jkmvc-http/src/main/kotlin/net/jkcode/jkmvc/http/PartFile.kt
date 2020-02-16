package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.*
import java.io.File
import java.net.URLDecoder
import java.nio.file.FileSystems
import javax.servlet.http.Part

/**
 * 上传的文件
 */
class PartFile(protected val part: Part): Part by part {

    companion object{
        /**
         * 上传配置
         */
        public val uploadConfig: IConfig = Config.instance("upload")

        /**
         * 上传目录
         */
        public val uploadRootDirectory: String = uploadConfig.getString("uploadRootDirectory")!!.trim("", File.separator) // 去掉最后的路径分隔符

        /**
         * 禁止上传的文件扩展名
         */
        protected val forbiddenExt: List<String> = uploadConfig.getString("forbiddenExt")!!.split(',')

        /**
         * 检查文件是否禁止上传
         *
         * @param fileName 上传文件名
         * @return boolean
         */
        public fun isForbiddenUploadFile(fileName: String): Boolean {
            val ext = fileName.substringAfterLast('.')
            return forbiddenExt.any {
                it.equals(ext, true)
            }
        }


        /**
         * 准备好上传目录 = 根目录/子目录
         *
         * @param uploadDirectory 上传子目录
         * @return
         */
        protected fun prepareUploadDirectory(uploadDirectory:String): String {
            // 1 绝对目录
            if(FileSystems.getDefault().getPath(uploadDirectory).isAbsolute){
                // 如果目录不存在，则创建
                uploadDirectory.prepareDirectory()
                return uploadDirectory
            }

            // 2 相对目录
            // 上传目录 = 根目录/子目录
            var path:String = uploadRootDirectory + File.separatorChar
            if(uploadDirectory != "")
                path = path + uploadDirectory + File.separatorChar
            // 如果目录不存在，则创建
            path.prepareDirectory()
            return path
        }

        /**
         * 准备好上传文件路径
         *   保存文件前被调用
         *
         * @param fileName 文件名
         * @param uploadDirectory 上传子目录
         * @return
         */
        protected fun prepareUploadFile(fileName: String, uploadDirectory:String): File {
            //准备好上传目录, 并构建文件
            val file = File(prepareUploadDirectory(uploadDirectory), fileName)
            // 文件创建或重命名
            return file.createOrRename()
        }

        /**
         * 获得指定文件的相对路径
         *
         * @param file
         * @return
         */
        public fun getFileRelativePath(file: String): String {
            return file.substringAfter(uploadRootDirectory)
        }
    }

    init {
        if(part.isText())
            throw IllegalArgumentException("非文件域");

        if(isForbiddenUploadFile(part.submittedFileName))
            throw UnsupportedOperationException("文件域[$name]的文件为[${part.submittedFileName}], 属于禁止上传的文件类型")
    }

    /**
     * 存储的相对路径
     */
    protected var relativePath: String? = null

    /**
     * 获得存储后的文件名
     */
    public val storedFileName: String
        get() {
            if (relativePath == null)
                throw IllegalStateException("未存储");

            return relativePath!!.substringAfterLast(File.separatorChar)
        }

    /**
     * 存储的相对路径
     */
    //public val relativePath: String

    /**
     * 另存文件, 并记录相对路径
     * @param uploadDirectory
     */
    public override fun write(path: String) {
        val relativePath = getFileRelativePath(path)
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
        val file = prepareUploadFile(fileName, uploadDirectory)
        // 另存文件
        write(file.absolutePath)

        // 返回相对路径
        return relativePath!!
    }

}