package net.jkcode.jkmvc.tests.entity

import net.jkcode.jkmvc.orm.OrmEntity

/**
 * 比赛
 */
public class Game : OrmEntity() {

    public var id: Int by property()

    public var title: String by property()

    public var score: Int by property()

}