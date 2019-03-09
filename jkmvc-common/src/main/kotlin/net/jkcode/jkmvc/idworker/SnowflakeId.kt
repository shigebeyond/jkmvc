package net.jkcode.jkmvc.idworker

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.time

/**
 * Snowflake算法的id
 *    通过 `val timestamp = (id shr 22) + startTimestamp` 可大致知道请求时间
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-01-29 3:22 PM
 */
data class SnowflakeId(public val timestamp: Long /* 开始时间截  */,
                       public val datacenterId: Long /* 数据中心ID(0~31)  */,
                       public val workerId: Long /* 工作机器ID(0~31)  */,
                       public val sequence: Long /* 毫秒内序列(0~4095)  */
){
    companion object {

        /***************************** id元素的位数 *********************************/
        /**
         * 数据中心id所占的位数
         */
        public val datacenterIdBits: Int = 5

        /**
         * 机器id所占的位数
         */
        public val workerIdBits: Int = 5

        /**
         * 序列在id中占的位数: 毫秒内序列号12位自增，并发4096
         */
        public val sequenceBits: Int = 12

        /***************************** id元素的最大值 *********************************/
        /**
         * 支持的最大数据中心id，也用作掩码, 结果是31
         */
        public val maxDatacenterId: Long = -1L xor (-1L shl datacenterIdBits)

        /**
         * 支持的最大机器id，也用作掩码, 结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
         */
        public val maxWorkerId: Long = -1L xor (-1L shl workerIdBits)

        /**
         * 支持的最大序列号, 也用作掩码, 结果是4095
         */
        public val maxSequence: Long = -1L xor (-1L shl sequenceBits)

        /***************************** id元素的位偏移量 *********************************/
        /**
         * 机器ID向左移12位
         */
        public val workerIdShift: Int = sequenceBits

        /**
         * 数据中心id向左移17位(12+5)
         */
        public val datacenterIdShift: Int = sequenceBits + workerIdBits

        /**
         * 时间截向左移22位(5+5+12)
         */
        public val timestampLeftShift: Int = sequenceBits + workerIdBits + datacenterIdBits

        /***************************** id元素的掩码 *********************************/
        /**
         * 数据中心ID的掩码
         */
        public val datacenterIdMask: Long = maxDatacenterId

        /**
         * 机器ID的掩码
         */
        public val workerIdMask: Long = maxWorkerId

        /**
         * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
         */
        public val sequenceMask: Long = maxSequence

        /***************************** id元素的配置值 *********************************/
        /**
         * 配置
         */
        public val config = Config.instance("snow-flake-id", "properties")

        /**
         * 开始时间截
         */
        public val startTimestamp: Long = config["startTimestamp"]!!

        /**
         * 解析为id
         *    从64位的ID中, 通过移位+掩码运算来获得各个id元素
         *
         * @param id
         * @return
         */
        public fun fromLong(id: Long): SnowflakeId {
            val sequence = id and sequenceMask
            val workerId = (id shr workerIdShift) and workerIdMask
            val datacenterId = (id shr datacenterIdShift) and datacenterIdMask
            val timestamp = (id shr timestampLeftShift) + startTimestamp
            return SnowflakeId(timestamp, datacenterId, workerId, sequence)
        }

        /**
         * 转为Long
         *    通过移位+或运算拼到一起组成64位的ID
         *
         * @param timestamp 开始时间截
         * @param datacenterId 数据中心ID(0~31)
         * @param workerId 工作机器ID(0~31)
         * @param sequence 毫秒内序列(0~4095)
         * @return
         */
        internal inline fun toLong(timestamp: Long, datacenterId: Long, workerId: Long, sequence: Long): Long{
            //println("timestamp=$timestamp, sequence=$sequence")
            return ((timestamp - startTimestamp shl timestampLeftShift) // 开始时间截
                    or (datacenterId shl datacenterIdShift) // 数据中心ID
                    or (workerId shl workerIdShift) // 工作机器ID
                    or (sequence and sequenceMask)) // 毫秒内序列
        }
    }

    /**
     * 转为Long
     * @return
     */
    public fun toLong(): Long{
        return toLong(timestamp, datacenterId, workerId, sequence)
    }

}