package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.util.ModelGenerator
import org.junit.Test

class ModelGeneratorTests{

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jksoa-benchmark/common/src/main/kotlin", "net.jkcode.jksoa.benchmark.common.analyze", "default", "shijianhang")
        // 生成model文件
//        generator.genenateModelFile("UserModel", "用户", "user")
//        generator.genenateModelFile("AddressModel", "地址", "address")
//        generator.genenateModelFile("ParcelModel", "包裹", "parcel")
//        generator.genenateModelFile("MessageModel", "消息", "message")

//        generator.genenateModelFile("AppModel", "应用信息", "app")
//        generator.genenateModelFile("ServiceModel", "应用信息", "service")
//        generator.genenateModelFile("TraceModel", "trace", "trace")
//        generator.genenateModelFile("SpanModel", "span", "span")
//        generator.genenateModelFile("AnnotationModel", "span的标注信息", "annotation")

//        generator.genenateModelFile("TransactionMqModel", "事务消息", "transaction_mq")
//        generator.genenateModelFile("TccTransactionModel", "tcc事务", "tcc_transaction")

//        generator.genenateModelFile("ProductModel", "商品", "ord_product")
//        generator.genenateModelFile("OrderModel", "订单", "ord_order")
//        generator.genenateModelFile("OrderItemModel", "订单项", "ord_order_item")
//        generator.genenateModelFile("PayAccountModel", "支付账号", "pay_account")
//        generator.genenateModelFile("PayOrderModel", "支付订单", "pay_order")
//        generator.genenateModelFile("RedPacketModel", "红包", "red_packet")
        generator.genenateModelFile("BenchmarkResultModel", "性能测试结果", "benchmark_result")

    }
}