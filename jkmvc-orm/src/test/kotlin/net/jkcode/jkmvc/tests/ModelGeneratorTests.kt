package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.util.ModelGenerator
import org.junit.Test

class ModelGeneratorTests{

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jksoa/jksoa-tracer/src/main/kotlin", "net.jkcode.jksoa.tracer.model", "default", "shijianhang")
        // 生成model文件
//        generator.genenateModelFile("UserModel", "用户", "user")
//        generator.genenateModelFile("AddressModel", "地址", "address")
//        generator.genenateModelFile("ParcelModel", "包裹", "parcel")
        generator.genenateModelFile("MessageModel", "消息", "message")

//        generator.genenateModelFile("AppModel", "应用信息", "app")
//        generator.genenateModelFile("ServiceModel", "应用信息", "service")
//        generator.genenateModelFile("TraceModel", "trace", "trace")
//        generator.genenateModelFile("SpanModel", "span", "span")
//        generator.genenateModelFile("AnnotationModel", "span的标注信息", "annotation")
    }
}