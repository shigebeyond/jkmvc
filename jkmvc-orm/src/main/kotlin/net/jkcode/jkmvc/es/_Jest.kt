package net.jkcode.jkmvc.es

import io.searchbox.client.JestResult

/**
 * 从结果中获得游标id
 *   兼容 SearchResult/ScrollSearchResult
 */
val JestResult.scrollId: String?
    get() {
        return jsonObject["_scroll_id"]?.asString
    }