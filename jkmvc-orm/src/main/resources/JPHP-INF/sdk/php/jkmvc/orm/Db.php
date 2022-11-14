<?php
namespace php\jkmvc\orm;

/**
 * Class Db
 * @package php\jkmvc\orm
 */
class Db
{
    /**
     * 获得单例
     */
    function static instance($name) {
    }

    /**
     * 私有的构造函数
     */
    protected function __construct($name) {
    }

    /**
     * 开启事务
     */
    function begin(){
    }


    /**
     * 提交
     */
    function commit(): bool{
    }

    /**
     * 回滚
     */
    function rollback(): bool{
    }

    /**
     * 预览sql
     * @param sql
     * @param params sql参数
     * @return
     */
    function previewSql(string $sql, array $params = []): string{
    }

    /**
     * 查询多行
     * @param sql
     * @param params 参数
     * @return
     */
    function query(string $sql, array $params = []): array {
    }

    /**
     * 执行更新
     * @param sql
     * @param params
     * @param generatedColumn 返回的自动生成的主键名
     * @return
     */
    function execute(string $sql, array $params = []): int{
    }

    /**
     * 批量更新: 每次更新sql参数不一样
     *
     * @param sql
     * @param paramses 多次处理的参数的汇总，一次处理取 paramSize 个参数，必须保证他的大小是 paramSize 的整数倍
     * @param paramSize 一次处理的参数个数
     */
    function batchExecute(string $sql, array $paramses, int $paramSize){
    }
}