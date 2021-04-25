package net.jkcode.jkmvc.es

import io.searchbox.annotations.JestId
import io.searchbox.client.JestResult
import net.jkcode.jkutil.common.getCachedAnnotation
import java.lang.reflect.Field

/**
 * 从结果中获得游标id
 *   兼容 SearchResult/ScrollSearchResult
 */
val JestResult.scrollId: String?
    get() {
        return jsonObject["_scroll_id"]?.asString
    }


/**
 * 获得@JestId注解的java属性
 */
public fun Class<*>.getJestIdField(): Field {
    return this.fields.first {
        // 有 @JestId 注解的属性
        it.getCachedAnnotation<JestId>() != null
    }
}