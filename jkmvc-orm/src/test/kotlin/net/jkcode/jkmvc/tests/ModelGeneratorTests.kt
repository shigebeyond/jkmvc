package net.jkcode.jkmvc.tests

import net.jkcode.jkmvc.util.ModelGenerator
import org.junit.Test

class ModelGeneratorTests{

    @Test
    fun testCodeModel() {
        val generator = ModelGenerator("/home/shi/code/java/jkerp/wfengine/src/main/kotlin/org/joget/workflow/xpdl/beyond", "net.jkcode.jkerp.app", "default", "shijianhang")

        // 测试
        // 生成model文件
//        generator.genenateModelFile("UserModel", "用户", "user")
//        generator.genenateModelFile("AddressModel", "地址", "address")
//        generator.genenateModelFile("ParcelModel", "包裹", "parcel")
//        generator.genenateModelFile("MessageModel", "消息", "message")

        // 跟踪
//        generator.genenateModelFile("AppModel", "应用信息", "app")
//        generator.genenateModelFile("ServiceModel", "应用信息", "service")
//        generator.genenateModelFile("TraceModel", "trace", "trace")
//        generator.genenateModelFile("SpanModel", "span", "span")
//        generator.genenateModelFile("AnnotationModel", "span的标注信息", "annotation")

        // 事务
//        generator.genenateModelFile("TransactionMqModel", "事务消息", "transaction_mq")
//        generator.genenateModelFile("TccTransactionModel", "tcc事务", "tcc_transaction")

//        generator.genenateModelFile("ProductModel", "商品", "ord_product")
//        generator.genenateModelFile("OrderModel", "订单", "ord_order")
//        generator.genenateModelFile("OrderItemModel", "订单项", "ord_order_item")
//        generator.genenateModelFile("PayAccountModel", "支付账号", "pay_account")
//        generator.genenateModelFile("PayOrderModel", "支付订单", "pay_order")
//        generator.genenateModelFile("RedPacketModel", "红包", "red_packet")

//        generator.genenateModelFile("BenchmarkResultModel", "性能测试结果", "benchmark_result")

        // erp
//        generator.genenateModelFile("UserPasswordHistory", "", "dir_user_password_history")
//        generator.genenateModelFile("UserExtra", "", "dir_user_extra")
//        generator.genenateModelFile("PackageParticipant", "Represents a mapping for a workflow participant to directory users.", "app_package_participant")
//        generator.genenateModelFile("AuditTrail", "", "wf_audit_trail")
//        generator.genenateModelFile("FormDefinition", "Metadata for a Form.", "app_form")
//        generator.genenateModelFile("PackageActivityPlugin", "Represents a mapping for a workflow activity tool to a plugin.", "app_package_activity_plugin")
//        generator.genenateModelFile("DatalistDefinition", "Metadata for a datalist.", "app_datalist")
//        generator.genenateModelFile("EnvironmentVariable", "", "app_env_variable")
//        generator.genenateModelFile("UserReplacement", "", "dir_user_replacement")
//        generator.genenateModelFile("UserviewDefinition", "Metadata for a userview.", "app_userview")
//        generator.genenateModelFile("Message", "", "app_message")
//        generator.genenateModelFile("AppResource", "", "app_resource")
//        generator.genenateModelFile("PackageDefinition", "Metadata for a Workflow Package.", "app_package")
//        generator.genenateModelFile("PackageActivityForm", "Represents a mapping for a workflow activity to a form.", "app_package_activity_form")
//        generator.genenateModelFile("AppDefinition", "Metadata definition for an App. An App that consists of a workflow package, forms, lists, userviews.", "app_app")
//        generator.genenateModelFile("PluginDefaultProperties", "", "app_plugin_default")
//        generator.genenateModelFile("FormDataAuditTrail", "", "app_form_data_audit_trail")
//        generator.genenateModelFile("FormRow", "Represents a row of form data", "app_fd")
//        generator.genenateModelFile("ReportProcess", "", "app_report_process")
//        generator.genenateModelFile("ReportApp", "", "app_report_app")
//        generator.genenateModelFile("ReportWorkflowActivityInstance", "", "app_report_activity_instance")
//        generator.genenateModelFile("ReportWorkflowCase", "", "app_report_case")
//        generator.genenateModelFile("ReportWorkflowActivity", "", "app_report_activity")
//        generator.genenateModelFile("ReportWorkflowPackage", "", "app_report_package")
//        generator.genenateModelFile("ResourceBundleMessage", "", "wf_resource_bundle_message")
//        generator.genenateModelFile("Setting", "", "wf_setup")
//        generator.genenateModelFile("Department", "", "dir_department")
//        generator.genenateModelFile("EmploymentReportTo", "", "dir_employment_report_to")
//        generator.genenateModelFile("Grade", "岗位", "dir_grade")
//        generator.genenateModelFile("User", "用户", "dir_user")
//        generator.genenateModelFile("Group", "分组", "dir_group")
//        generator.genenateModelFile("Employment", "员工", "dir_employment")
//        generator.genenateModelFile("UserMetaData", "某个用户的k/v元数据", "dir_user_meta")
//        generator.genenateModelFile("Organization", "组织", "dir_organization")
//        generator.genenateModelFile("Role", "岗位", "dir_role")
//        generator.genenateModelFile("WorkflowProcessLink", "", "wf_case_link")
//        generator.genenateModelFile("SharkAssignment", "", "SHKAssignmentsTable")
//        generator.genenateModelFile("SharkActivityState", "", "SHKActivityStates")
//        generator.genenateModelFile("SharkActivity", "", "SHKActivities")
//        generator.genenateModelFile("SharkProcess", "", "SHKProcesses")
//        generator.genenateModelFile("Workitem", "工作项", "wf_workitems")
//        generator.genenateModelFile("Process", "流程实例", "wf_process")
        generator.genenateModelFile("Versions", "版本", "app_version")

    }
}