package net.jkcode.jkmvc.es

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.jkcode.jkmvc.orm.IOrmEntity

/**
 * gson类型适配器, 主要针对IOrmEntity
 *
 * @author shijianhang
 * @date 2021-4-21 下午5:16:59
 *
 */
class EntityTypeAdapterFactory(protected val mapTypeAdapter: TypeAdapter<HashMap<*, *>>) : TypeAdapterFactory {

    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        val rawType = typeToken.rawType as Class<T>
        if (!IOrmEntity::class.java.isAssignableFrom(rawType))
            return null

        return Adapter(rawType)
    }

    private inner class Adapter<T>(public val clazz: Class<T>) : TypeAdapter<T>() {

        override fun read(`in`: JsonReader): T? {
            val map = mapTypeAdapter.read(`in`) as Map<String, Any?>?
            if(map == null)
                return null

            val item = clazz.newInstance() as T
            (item as IOrmEntity).fromMap(map)
            return item
        }

        override fun write(out: JsonWriter, item: T?) {
            if (item == null) {
                out.nullValue()
                return
            }

            val map = (item as IOrmEntity).toMap() as HashMap
            mapTypeAdapter.write(out, map)
        }
    }

}
