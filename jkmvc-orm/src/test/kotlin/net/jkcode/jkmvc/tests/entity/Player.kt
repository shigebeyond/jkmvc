package net.jkcode.jkmvc.tests.entity

import net.jkcode.jkmvc.es.annotation.EsDoc
import net.jkcode.jkmvc.es.annotation.EsId
import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 运动员
 */
@EsDoc(index = "player_index")
public class Player: OrmEntity() {

    @EsId
    public var id: Int by property()

    public var name: String by property() // 姓名

    public var age: Int by property() // 年龄

    public var salary: Int by property() // 工资

    public var team: String by property() // 队伍

    public var position: String by property() // 职位:前锋/后卫

    public var games: List<Game> by property() // 比赛


}