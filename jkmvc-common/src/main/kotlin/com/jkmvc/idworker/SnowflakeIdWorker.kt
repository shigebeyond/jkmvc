package com.jkmvc.idworker

import com.jkmvc.common.Config


/**
 * Twitter的Snowflake的id算法
 * 参考 https://blog.csdn.net/hnhygkx/article/details/78084909
 *
 * id构成元素如下(每部分用-分开):
 *   0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 *   1) 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0
 *   2) 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)得到的值，该开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（见startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69
 *   3) 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId
 *   4) 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号
 *   => 加起来刚好64位，为一个Long型。
 *
 * 优点
 *   整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
 *
 * @author shijianhang
 * @date 2017-10-8 下午8:02:47
 */
class SnowflakeIdWorker : IIdWorker {

    companion object {

        /***************************** id元素的位数 *********************************/
        /**
         * 机器id所占的位数
         */
        public val workerIdBits: Int = 5

        /**
         * 数据标识id所占的位数
         */
        public val datacenterIdBits: Int = 5

        /**
         * 序列在id中占的位数
         */
        public val sequenceBits: Int = 12

        /***************************** id元素的最大值 *********************************/
        /**
         * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
         */
        public val maxWorkerId: Long = -1L xor (-1L shl workerIdBits)

        /**
         * 支持的最大数据标识id，结果是31
         */
        public val maxDatacenterId: Long = -1L xor (-1L shl datacenterIdBits)

        /***************************** id元素的位偏移量 *********************************/
        /**
         * 机器ID向左移12位
         */
        public val workerIdShift: Int = sequenceBits

        /**
         * 数据标识id向左移17位(12+5)
         */
        public val datacenterIdShift: Int = sequenceBits + workerIdBits

        /**
         * 时间截向左移22位(5+5+12)
         */
        public val timestampLeftShift: Int = sequenceBits + workerIdBits + datacenterIdBits

        /**
         * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
         */
        public val sequenceMask: Long = -1L xor (-1L shl sequenceBits) // 微秒内序列号12位自增，并发4096

        /***************************** id元素的配置值 *********************************/
        /**
         * 配置
         */
        public val config = Config.instance("snow-flake-id", "properties")

        /**
         * 工作机器ID(0~31)
         */
        public var workerId: Long = config["workerId"]!!

        /**
         * 数据中心ID(0~31)
         */
        public var datacenterId: Long = config["datacenterId"]!!

        /**
         * 开始时间截
         */
        public val startTimestamp: Long = config["startTimestamp"]!!

        init {
            // 检查id元素范围
            if (workerId > maxWorkerId || workerId < 0) {
                throw IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId))
            }
            if (datacenterId > maxDatacenterId || datacenterId < 0) {
                throw IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId))
            }
        }
    }

    /**
     * 毫秒内序列(0~4095)
     */
    protected var sequence: Long = 0L

    /**
     * 上次生成ID的时间截
     */
    protected var lastTimestamp: Long = -1L

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return
     */
    @Synchronized
    public override fun nextId(): Long {
        var timestamp = time()

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp))
        }

        //如果是同一毫秒内生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = sequence + 1 and sequenceMask // 微秒内序列号12位自增，并发4096
            //毫秒内序列溢出
            if (sequence == 0L) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp)
            }
        } else { //时间戳改变，毫秒内序列重置
            sequence = 0L
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp

        //移位并通过或运算拼到一起组成64位的ID
        return (timestamp - startTimestamp shl timestampLeftShift // 开始时间截
                or (datacenterId shl datacenterIdShift) // 数据中心ID
                or (workerId shl workerIdShift) // 工作机器ID
                or sequence) // 毫秒内序列
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected fun tilNextMillis(lastTimestamp: Long): Long {
        var timestamp = time()
        while (timestamp <= lastTimestamp) {
            timestamp = time()
        }
        return timestamp
    }

    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    protected fun time(): Long {
        return System.currentTimeMillis()
    }
}