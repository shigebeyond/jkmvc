package net.jkcode.jkmvc.orm

import net.jkcode.jkutil.common.getInterfaceGenricType

/**
 * 实体化的IOrm
 *   可与实体类相互转换
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-09-12 9:26 AM
 */
interface IEntitiableOrm<E: OrmEntity>: IOrm {

    /**
     * 从其他实体对象中设置字段值
     *    对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *
     * @param from
     */
    public fun fromEntity(from: IOrmEntity){
        for(column in ormMeta.propsAndRelations) {
            val value:Any? = from[column]
            if(value is IOrmEntity){ // 如果是IOrmEntity，则为关联对象
                val realValue = getRelatedOrNew(column) // 创建关联对象
                (realValue as IEntitiableOrm<*>).fromEntity(value) // 递归设置关联对象的字段值
            }else
                set(column, value)
        }
    }

    /**
     * 转为实体对象
     *   对于关联对象字段值的设置: 只考虑一对一的关联对象, 不考虑一对多的关联对象
     *
     * @return
     */
    public fun toEntity(): E{
        // 获得实体类: 当前类实现 IEntitiableOrm 接口时, 指定的泛型类型
        val entityClass = this.javaClass.getInterfaceGenricType(IEntitiableOrm::class.java)!!
        //val entityClass = ormMeta.entityClass!!
        // 创建实体
        val entity = entityClass.newInstance() as E

        // 1 转关联对象
        for((name, relation) in ormMeta.relations){
            val value: Any? = this[name]
            if(value != null){
                entity[name] = when(value){
                    is Collection<*> -> (value as Collection<IEntitiableOrm<OrmEntity>>).toEntities() // 有多个
                    is IEntitiableOrm<*> -> value.toEntity()  // 有一个
                    else -> value
                }
            }
        }

        // 2 转当前对象：由于关联对象联查时不处理null值, 因此关联对象会缺少null值的字段，这里要补上
        for(prop in ormMeta.props)
            entity[prop] = this[prop]

        return entity
    }
}