package com.jkmvc.util.id

import com.jkmvc.common.IIdWorker
import com.jkmvc.db.Db
import java.util.concurrent.ConcurrentHashMap

/**
 * 使用db来生成全局唯一id
 *    TODO：支持根据不同的模块来生成不同的id
 *
 * @author shijianhang
 * @create 2018-04-16 下午10:07
 **/
class DbIdWorker: IIdWorker {

    val db: Db = Db.instance()

    val idSequences: ConcurrentHashMap<String, IdSequenceModel> = ConcurrentHashMap();

    init{
        // 建表
        db.execute("""
        CREATE TABLE IF NOT EXISTS `id_sequence` (
            `id` int(11) NOT NULL AUTO_INCREMENT,
            `module` varchar(100) NOT NULL COMMENT '模块名',
            `max_id` bigint(19) NOT NULL COMMENT '最新的id',
            `step` int(11) NOT NULL COMMENT '自增的步长',
            PRIMARY KEY (`id`),
            UNIQUE `idx_module` (`module`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='id序列表';
        """);
        println("创建id序列表")

        // 加载数据
        val items = IdSequenceModel.queryBuilder().findAllModels<IdSequenceModel>()
        for (item in items){
            idSequences[item.module] = item
        }
        println("加载id序列")
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return
     */
    override fun nextId(): Long {
        // TODO: module可通过参数指定
        val module = "default"
        val seq = idSequences.getOrPut(module){
            val item = IdSequenceModel()
            item.maxId = 0
            item.module = module
            item.create()
            item
        }
        return seq.nextId()
    }

}