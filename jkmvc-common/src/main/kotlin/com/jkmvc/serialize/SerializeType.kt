package com.jkmvc.serialize

/**
 * 序列化类型
 *
 * @author shijianhang
 * @create 2017-10-04 下午3:29
 **/
enum class SerializeType {

    JDK {
        public override val serializer: ISerializer = JdkSerializer
    },

    FST {
        public override val serializer: ISerializer = FstSerializer
    };

    public abstract val serializer: ISerializer
}