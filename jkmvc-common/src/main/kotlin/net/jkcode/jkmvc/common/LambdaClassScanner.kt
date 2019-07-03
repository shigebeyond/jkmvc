package net.jkcode.jkmvc.common

/**
 * 用lambda封装对扫描到的类文件的处理
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-23 7:56 PM
 */
class LambdaClassScanner(protected val lambda: (String) -> Unit) : ClassScanner() {
    /**
     * 收集类文件
     *
     * @param relativePath 类文件相对路径
     */
    override fun collectClass(relativePath: String) {
        lambda.invoke(relativePath)
    }

}