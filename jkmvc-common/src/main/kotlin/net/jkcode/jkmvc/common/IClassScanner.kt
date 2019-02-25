package net.jkcode.jkmvc.common

/**
 * 扫描指定包下的类
 *
 * @author shijianhang
 * @date 2016-10-8 下午8:02:47
 */
interface IClassScanner {
    /**
     * 添加单个包
     *
     * @param pck 包名
     */
    fun addPackage(pck: String): Unit

    /**
     * 添加多个包
     *
     * @param pcks 包名
     */
    fun addPackages(pcks: Collection<String>): Unit

    /**
     * 扫描指定包下的类
     *
     * @param pck 包名
     */
    fun scan(pck: String): Unit

    /**
     * 收集类文件
     *
     * @param relativePath 类文件相对路径
     */
    fun collectClass(relativePath: String): Unit
}