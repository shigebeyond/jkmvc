package com.jkmvc.common

/**
 * 应用信息
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-17 12:12 PM
 */
object Application {

    /**
     * 应用配置
     */
    public val config = Config.instance("application", "properties")

    /**
     * 环境
     */
    public val env: String = config["environment"]!!

    /**
     * 是否测试环境
     */
    public val isTest: Boolean = env == "test"

    /**
     * 是否开始环境
     */
    public val isDev: Boolean = env == "dev"


    /**
     * 是否线上环境
     */
    public val isProd: Boolean = env == "prod"

    /**
     * 是否debug环境
     */
    public val isDebug = System.getProperty("java.class.path").contains("debugger-agent.jar")

    /**
     * 是否单元测试环境
     */
    public val isJunitTest = System.getProperty("sun.java.command").contains("-junit")
}