<?php
namespace php\jkmvc\orm;

/**
 * Class Model
 * @package php\jkmvc\orm
 */
class Model
{
	/**
  	* 构造函数
     */
    function __construct(name: String) {
    }

    /**
     * 读属性
     */
    function __get($args) {
    }

    /**
     * 写属性
     */
    function __set($args) {
    }

    /**
     * 根据主键值来加载数据
     */
    function load($pk) {
    }

    /**
     * 根据主键值来查找数据
     */
    function find($pk) {
    }

    /**
     * 保存数据
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    function save($withHasRelations): bool{
    }

    /**
     * 插入数据: insert sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @param checkPkExists 是否检查主键存在
     * @return 新增数据的主键
     */
    function create($withHasRelations): int{
    }

    /**
     * 更新数据: update sql
     * @param withHasRelations 是否连带保存 hasOne/hasMany 的关联关系
     * @return
     */
    function update($withHasRelations): bool{
    }

    /**
     * 删除数据: delete sql
     * @param withHasRelations 是否连带删除 hasOne/hasMany 的关联关系
     * @return
     */
    function delete($withHasRelations): bool{
    }

}