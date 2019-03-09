package net.jkcode.jkmvc.idworker

import net.jkcode.jkmvc.common.Config
import net.jkcode.jkmvc.common.time
import java.util.concurrent.atomic.AtomicLong


/**
 * Twitter的Snowflake的id算法
 * 参考+改进 https://blog.csdn.net/hnhygkx/article/details/78084909
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

        init {
            // 检查id元素范围
            if (datacenterId > SnowflakeId.maxDatacenterId || datacenterId < 0)
                throw IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", SnowflakeId.maxDatacenterId))
            if (workerId > SnowflakeId.maxWorkerId || workerId < 0)
                throw IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", SnowflakeId.maxWorkerId))
        }
    }

    /**
     * 毫秒内序列(0~4095)
     */
    protected var sequence: AtomicLong = AtomicLong(0)

    /**
     * 上次生成ID的时间截
     */
    @Volatile
    protected var lastTimestamp: Long = -1L

    /**
     * 获得下一个ID
     * @return
     */
    public override fun nextId(): Long {
        var timestamp = time()
        val lastTimestamp = this.lastTimestamp

        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp)
            throw RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp))

        // 1 同一毫秒
        if (lastTimestamp == timestamp) {
            //毫秒内序列自增
            if (sequence.incrementAndGet() <= SnowflakeId.maxSequence) // 无溢出
                return newId(timestamp)

            //溢出: 阻塞到下一个毫秒
            timestamp = blockUntilNextMillis(lastTimestamp)
        }else{
            this.lastTimestamp = timestamp
        }

        // 2 不同毫秒
        // 毫秒内序列重置
        val lastSequeue = sequence.get()
        if(!sequence.compareAndSet(lastSequeue, 0))
            sequence.incrementAndGet()

        // 返回新id
        return newId(timestamp)
    }

    /**
     * 构建新id
     * @param timestamp
     * @return
     */
    protected fun newId(timestamp: Long): Long {
        return SnowflakeId.toLong(timestamp, datacenterId, workerId, sequence.get())
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected fun blockUntilNextMillis(lastTimestamp: Long): Long {
        var timestamp = time()
        while (timestamp <= lastTimestamp)
            timestamp = time()
        return timestamp
    }
}