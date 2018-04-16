package com.jkmvc.util.id

import com.jkmvc.orm.Orm
import com.jkmvc.orm.OrmMeta
import sun.management.MemoryUsageCompositeData.getMax
import java.util.concurrent.atomic.AtomicLong
import javax.swing.text.Segment



/**
 * id序列
 * @author shijianhang
 * @create 2018-04-16 下午10:39
 **/
class IdSequenceModel(id:Int? = null): Orm(id) {

    // 伴随对象就是元数据
    companion object m: OrmMeta(IdSequenceModel::class){

        // 步长  TODO: 可配
        public val step:Long = 100;
    }

    // 代理属性读写
    public var id:Int by property<Int>();

    public var module:String by property<String>();

    public var maxId:Long by property<Long>()

    private val currId: AtomicLong by lazy{
        AtomicLong(maxId)
    }


    /**
     * get next id when segment have it

     * @param segment
     * *
     * @return nextId
     */
    public fun nextId(): Long {
        if (currId.toLong() > maxId) {
            synchronized(this){
                maxId += step
                update() // TODO: sql自增
                currId.addAndGet(step)
            }
        }

        return currId.getAndIncrement()
    }
}