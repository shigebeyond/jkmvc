package com.jkmvc.http.view

import com.jkmvc.http.HttpRequest
import com.jkmvc.http.HttpResponse
import org.apache.velocity.VelocityContext
import org.apache.velocity.io.VelocityWriter
import org.apache.velocity.runtime.RuntimeInstance
import java.util.*

/**
 * Velocity模板视图
 *     放在web根目录下的html文件
 *
 * @author shijianhang<772910474@qq.com>
 * @date 8/25/17 9:49 AM
 */
class VelocityView(req: HttpRequest /* 请求对象 */, res: HttpResponse /* 响应对象 */, file:String/* 视图文件 */, data:MutableMap<String, Any?> /* 局部变量 */): View(req, res, file, data) {

    /**
     * 渲染Velocity模板
     */
    override fun render() {
        // 构建属性
        val props = Properties()
        props.setProperty("resource.loader", "file")
        props.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader")
        props.setProperty("file.resource.loader.path", req.getWebPath())
        props.setProperty("file.resource.loader.cache", "false")
        props.setProperty("file.resource.loader.modificationCheckInterval", "2")
        props.setProperty("input.encoding", "UTF-8")
        props.setProperty("output.encoding", "UTF-8")
        props.setProperty("default.contentType", "text/html; charset=UTF-8")
        props.setProperty("velocimarco.library.autoreload", "true")
        props.setProperty("runtime.log.error.stacktrace", "false")
        props.setProperty("runtime.log.warn.stacktrace", "false")
        props.setProperty("runtime.log.info.stacktrace", "false")
        props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem")
        props.setProperty("runtime.log.logsystem.log4j.category", "velocity_log")

        // 单例用 Velocity
        // val instance = Velocity();
        // 非单例用 RuntimeInstanc：同时使用多个Velocity
        val instance = RuntimeInstance()
        instance.init(props)

        // 获得模板文件
        val template = instance.getTemplate(file + ".html", "UTF-8");

        // 构建上下文：要渲染的数据
        data.putAll(globalData);
        val context = VelocityContext(data)

        // 渲染模板
        var vwriter: VelocityWriter? = null
        try {
            vwriter = VelocityWriter(res.getWriter())
            template.merge(context, vwriter) // 合并上下文，根据数据渲染并输出
            vwriter.flush()
        } finally {
            vwriter?.recycle(null)
        }
    }
}