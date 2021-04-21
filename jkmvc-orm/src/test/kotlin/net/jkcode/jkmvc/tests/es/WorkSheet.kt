package net.jkcode.jkmvc.tests.es


import com.alibaba.fastjson.annotation.JSONField
import io.searchbox.annotations.JestId

import java.io.Serializable
import java.util.Date

/**
 * @author workOrder
 * 工单主表
 */
class WorkSheet : Serializable {

    @JestId
    var id: Int = 0

    /**
     * 工单编号
     */
    var workSheetNo: String? = null
        set(workSheetNo) {
            field = workSheetNo?.trim { it <= ' ' }
        }

    /**
     * 工单类型，一级分类
     */
    var sheetTypeOne: Int? = null

    /**
     * 工单类型，二级分类
     */
    var sheetTypeTwo: Int? = null

    /**
     * 工单类型，三级分类
     */
    var sheetTypeThree: Int? = null

    /**
     * 工单类型，四级分类
     */
    var sheetTypeFour: Int? = null

    /**
     * 工单来源
     */
    var sheetSource: Int? = 0

    /**
     * 联系方式
     */
    var contact: String? = ""
        set(contact) {
            field = contact?.trim { it <= ' ' }
        }

    /**
     * 工单分类，1、订单相关，2，订单无关
     */
    var sheetClassify: Int? = 1

    /**
     * 订单类型(1:普通用户订单, 2:企业用户订单)
     */
    var orderType: Int? = 0

    /**
     * 订单类型,1:随叫随到;2:预约用车;3:接机;5:送机;6:日租;7:半日租;10:多日接送；15深港线；16港深线
     */
    var serviceTypeId: Int? = 0

    /**
     * 司ID
     */
    var driverId: Int? = 0

    /**
     * 司机姓名
     */
    var driverName: String? = ""
        set(driverName) {
            field = driverName?.trim { it <= ' ' }
        }

    /**
     * 司机手机号
     */
    var driverPhone: String? = ""
        set(driverPhone) {
            field = driverPhone?.trim { it <= ' ' }
        }

    /**
     * 服务城市
     */
    var cityId: Int? = null

    /**
     * 车牌号
     */
    var licensePlates: String? = ""
        set(licensePlates) {
            field = licensePlates?.trim { it <= ' ' }
        }

    /**
     * 订单号
     */
    var orderNo: String? = ""
        set(orderNo) {
            field = orderNo?.trim { it <= ' ' }
        }

    /**
     * 乘客姓名
     */
    var riderName: String? = ""
        set(riderName) {
            field = riderName?.trim { it <= ' ' }
        }

    /**
     * 乘客手机号
     */
    var riderPhone: String? = ""
        set(riderPhone) {
            field = riderPhone?.trim { it <= ' ' }
        }

    /**
     * 责任部门
     */
    var dutyDept: Int? = 0

    /**
     * 1、普通,,2、紧急 3、很紧急
     */
    var sheetPriority: Int? = 1

    /**
     * 工单标签
     */
    var sheetTag: Int? = 0

    /**
     * 处理时效，1、1个小时，3、3个小时，24：1个工作日，48、2个工作日
     */
    var handleTime: Int? = 1

    /**
     * 备注
     */
    var memo: String? = ""
        set(memo) {
            field = memo?.trim { it <= ' ' }
        }

    /**
     * 工单当前状态0、待分配，1、待处理，2、处理完成，3、暂时关闭
     */
    var currentStatus: Int? = 0

    /**
     * 提交人ID
     */
    var commitUserId: String? = null
        set(commitUserId) {
            field = commitUserId?.trim { it <= ' ' }
        }

    /**
     * 提交人姓名
     */
    var commitUserName: String? = null
        set(commitUserName) {
            field = commitUserName?.trim { it <= ' ' }
        }

    /**
     * 当前处理人ID
     */
    var currentDealUserId: String? = ""
        set(currentDealUserId) {
            field = currentDealUserId?.trim { it <= ' ' }
        }

    /**
     * 当前处理人姓名
     */
    var currentDealUserName: String? = ""
        set(currentDealUserName) {
            field = currentDealUserName?.trim { it <= ' ' }
        }

    /**
     * 七陌进电ID
     */
    var callRecordId: Long? = 0L

    /**
     * 工单类型，一级分类
     */
    var confirmSheetTypeOne: Int? = 0

    /**
     * 工单类型，二级分类
     */
    var confirmSheetTypeTwo: Int? = 0

    /**
     * 工单类型，三级分类
     */
    var confirmSheetTypeThree: Int? = 0

    /**
     * 工单类型，四级分类
     */
    var confirmSheetTypeFour: Int? = 0

    /**
     * 创建时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    var createDate: Date? = null

    /**
     * 修改时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    var updateDate: Date? = null

    /**
     * 部门 id
     */
    var deptId: Int? = 0

    /**
     * 催促次数
     */
    var urgeTimes: Int? = 0

    /**
     * 重新打开次数
     */
    var reopenTimes: Int? = 0

    /**
     * 标签的排序值
     */
    var sheetTagSort: Int? = 0

    /**
     * 排序权重值
     */
    var weight: Int? = 0

    /**
     * 关注人ids，用于创建工单时传参
     */
    var attentionUserIds: String? = null

    override fun toString(): String {
        return "WorkSheet(id=$id, workSheetNo=$workSheetNo, sheetTypeOne=$sheetTypeOne, sheetTypeTwo=$sheetTypeTwo, sheetTypeThree=$sheetTypeThree, sheetTypeFour=$sheetTypeFour, sheetSource=$sheetSource, contact=$contact, sheetClassify=$sheetClassify, orderType=$orderType, serviceTypeId=$serviceTypeId, driverId=$driverId, driverName=$driverName, driverPhone=$driverPhone, cityId=$cityId, licensePlates=$licensePlates, orderNo=$orderNo, riderName=$riderName, riderPhone=$riderPhone, dutyDept=$dutyDept, sheetPriority=$sheetPriority, sheetTag=$sheetTag, handleTime=$handleTime, memo=$memo, currentStatus=$currentStatus, commitUserId=$commitUserId, commitUserName=$commitUserName, currentDealUserId=$currentDealUserId, currentDealUserName=$currentDealUserName, callRecordId=$callRecordId, confirmSheetTypeOne=$confirmSheetTypeOne, confirmSheetTypeTwo=$confirmSheetTypeTwo, confirmSheetTypeThree=$confirmSheetTypeThree, confirmSheetTypeFour=$confirmSheetTypeFour, createDate=$createDate, updateDate=$updateDate, deptId=$deptId, urgeTimes=$urgeTimes, reopenTimes=$reopenTimes, sheetTagSort=$sheetTagSort, weight=$weight, attentionUserIds=$attentionUserIds)"
    }


}