package com.jkmvc.tests

import com.jkmvc.db.*
import com.jkmvc.http.View
import org.apache.velocity.VelocityContext
import org.apache.velocity.io.VelocityWriter
import org.apache.velocity.runtime.RuntimeInstance
import org.junit.Test
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import org.apache.velocity.app.Velocity
import java.util.*


class ViewTests{

    @Test
    fun testVelocity(){
        // 构建属性
        val props = Properties()
        props.setProperty("input.encoding", "UTF-8")
        props.setProperty("output.encoding", "UTF-8")
        props.setProperty("file.resource.loader.path", "/oldhome/shi/code/java/jkmvc/src/main/webapp")

        // 单例用 Velocity
        // val instance = Velocity();
        // 非单例用 RuntimeInstanc：同时使用多个Velocity
        val instance = RuntimeInstance()
        instance.init(props)

        // 获得模板文件
        val template = instance.getTemplate("index.html", "UTF-8");

        // 构建上下文：要渲染的数据
        val context = VelocityContext()
        context.put("name", "shi");

        // 渲染模板
        var vwriter: VelocityWriter? = null
        val writer = BufferedWriter(OutputStreamWriter(System.out));
        try {
            vwriter = VelocityWriter(writer)
            template.merge(context, vwriter) // 合并上下文，根据数据渲染并输出
            vwriter.flush()
        } finally {
            writer.close()
            if (vwriter != null)
                vwriter.recycle(null)
        }
    }
}