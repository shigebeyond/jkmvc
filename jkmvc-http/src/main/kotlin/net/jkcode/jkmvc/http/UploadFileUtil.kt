package net.jkcode.jkmvc.http

import net.jkcode.jkutil.common.*
import net.jkcode.jkutil.common.createOrRename
import net.jkcode.jkutil.common.prepareDirectory
import java.awt.Container
import java.awt.MediaTracker
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import javax.imageio.ImageIO

/**
 * 上传文件工具类
 * 　　原来 FileManager
 */
object UploadFileUtil {

    /**
     * 上传配置
     */
    public val uploadConfig: IConfig = Config.instance("upload")

    /**
     * 上传目录
     */
    public var uploadRootDirectory: String = uploadConfig.getString("uploadRootDirectory")!!.trim("", File.separator) // 去掉最后的路径分隔符

    /********************* 上传处理 *********************/
    /**
     * 禁止上传的文件扩展名
     */
    private val forbiddenExt: List<String> = uploadConfig.getString("forbiddenExt")!!.split(',')

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
     * 准备好的子目录
     */
    private val preparedDirs: MutableSet<String> = HashSet<String>()

    /**
     * 准备好上传目录 = 根目录/子目录
     *
     * @param uploadDirectory 上传子目录
     * @return
     */
    private fun prepareUploadDirectory(uploadDirectory: String): String {
        /*// 1 绝对目录
        if(FileSystems.getDefault().getPath(uploadDirectory).isAbsolute){
            // 如果目录不存在，则创建
            uploadDirectory.prepareDirectory()
            return uploadDirectory
        }
        */
        // 2 相对目录
        // 上传目录 = 根目录/子目录
        var path: String = uploadRootDirectory + File.separatorChar
        if(uploadDirectory != "")
            path = path + uploadDirectory + File.separatorChar
        // 如果目录不存在，则创建
        if(!preparedDirs.contains(uploadDirectory)) {
            preparedDirs.add(uploadDirectory)
            path.prepareDirectory()
        }
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
    public fun prepareUploadFile(fileName: String, uploadDirectory: String): File {
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

    /********************* 读/删文件 *********************/
    /**
     * Gets the temporary file from temporary files folder by relative path
     * @param path
     * @return
     */
    fun getFileByPath(path: String): File? {
        if(path == null)
            return null

        val file = File(uploadRootDirectory, URLDecoder.decode(path, "UTF-8"))
        if (file.exists() && !file.isDirectory)
            return file

        return null
    }

    /**
     * Deletes the temporary file from temporary files folder by relative path
     * @param path
     */
    fun deleteFileByPath(path: String) {
        val file = getFileByPath(path)
        val directory = file!!.parentFile

        if (file != null && file.exists())
            file.delete()

        if (directory != null && directory.exists() && !directory.absolutePath.endsWith(uploadRootDirectory))
            directory.delete()
    }

    /**
     * Deletes a file
     * @param file
     */
    fun deleteFile(file: File) {
        if (!file.exists())
            return

        if (file.isDirectory) {
            for (f in file.listFiles()!!) {
                deleteFile(f) // 递归调用
            }
        }
        file.delete()
    }

    /********************* 生成缩略图 *********************/
    val THUMBNAIL_SIZE = 60

    val THUMBNAIL_EXT = ".thumb.jpg"

    /**
     * Generates a thumbnail of a image file in temporary files folder by relative path
     * 创建缩略图
     *   输出路径为: $path.thumb.jpg
     * @param path 原始图的相对路径
     * @param thumbWidth
     * @param thumbHeight
     * @return 相对路径
     */
    fun createThumbnail(path: String, thumbWidth: Int?, thumbHeight: Int?): String {
        var width = thumbWidth ?: THUMBNAIL_SIZE
        var height = thumbHeight ?: THUMBNAIL_SIZE

        val imageFile = File(uploadRootDirectory, URLDecoder.decode(path, "UTF-8"))
        val image = Toolkit.getDefaultToolkit().getImage(imageFile.absolutePath)
        val mediaTracker = MediaTracker(Container())
        mediaTracker.addImage(image, 0)
        mediaTracker.waitForID(0)

        val thumbRatio = width.toDouble() / height
        val imageWidth = image.getWidth(null)
        val imageHeight = image.getHeight(null)
        val imageRatio = imageWidth.toDouble() / imageHeight.toDouble()
        if (thumbRatio < imageRatio) {
            height = (width / imageRatio).toInt()
        } else {
            width = (height * imageRatio).toInt()
        }

        val thumbImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics2D = thumbImage.createGraphics()
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics2D.drawImage(image, 0, 0, width, height, null)

        // 输出路径为: $path.thumb.jpg
        val outputPath = imageFile.absolutePath + THUMBNAIL_EXT
        BufferedOutputStream(FileOutputStream(outputPath)).use { out ->
            ImageIO.write(thumbImage, "jpeg", out)
            out.flush()
        }

        // 返回相对路径
        return path + THUMBNAIL_EXT
    }
}