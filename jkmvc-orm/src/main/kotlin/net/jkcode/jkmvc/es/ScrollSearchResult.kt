package net.jkcode.jkmvc.es

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.searchbox.client.JestResult
import io.searchbox.cloning.CloneUtils
import io.searchbox.core.search.aggregation.MetricAggregation
import io.searchbox.core.search.aggregation.RootAggregation
import java.util.*


/**
 * Scroll search result
 *   由于不能复用 SearchResult, 因此重新实现一个
 *
 */
class ScrollSearchResult(source: JestResult) : JestResult(source) {

    companion object {

        const val EXPLANATION_KEY = "_explanation"

        const val HIGHLIGHT_KEY = "highlight"

        const val FIELDS_KEY = "fields"

        const val SORT_KEY = "sort"

        val PATH_TO_TOTAL: List<String> = "hits/total".split("/")

        val PATH_TO_MAX_SCORE: List<String> = "hits/max_score".split("/")
    }

    fun <T> getFirstHit(sourceType: Class<T>): Hit<T, Void>? {
        return getFirstHit(sourceType, Void::class.java)
    }

    fun <T, K> getFirstHit(sourceType: Class<T>?, explanationType: Class<K>? = null, addEsMetadataFields: Boolean = true): Hit<T, K>? {
        return getHits(sourceType, explanationType, true, addEsMetadataFields).firstOrNull()
    }

    fun <T> getHits(sourceType: Class<T>): List<Hit<T, Void>> {
        return getHits(sourceType, Void::class.java, true)
    }

    fun <T, K> getHits(sourceType: Class<T>?, explanationType: Class<K>? = null, addEsMetadataFields: Boolean = true): List<Hit<T, K>> {
        return getHits(sourceType, explanationType, false, addEsMetadataFields)
    }

    protected fun <T, K> getHits(sourceType: Class<T>?, explanationType: Class<K>?, returnSingle: Boolean, addEsMetadataFields: Boolean): List<Hit<T, K>> {
        if (jsonObject == null)
            return emptyList()

        val keys = keys
        // keys would never be null in a standard search scenario (i.e.: unless search class is overwritten)
        if (keys == null)
            return emptyList()

        val sourceList: MutableList<Hit<T, K>> = ArrayList()
        // 最后一级属性
        val sourceKey = keys[keys.size - 1]
        // 获得最后一级对象
        var obj = jsonObject[keys[0]]
        for (i in 1 until keys.size - 1) {
            obj = (obj as JsonObject)[keys[i]]
        }

        // 收集hit对象
        if (obj.isJsonObject) {
            val hit = extractHit(sourceType, explanationType, obj, sourceKey, addEsMetadataFields)
            if (hit != null)
                sourceList.add(hit)
        } else if (obj.isJsonArray) {
            for (hitElement in obj.asJsonArray) {
                val hit = extractHit(sourceType, explanationType, hitElement, sourceKey, addEsMetadataFields)
                if (hit != null) {
                    sourceList.add(hit)
                    // 只要一个
                    if (returnSingle)
                        break
                }
            }
        }


        return sourceList
    }

    protected fun <T, K> extractHit(sourceType: Class<T>?, explanationType: Class<K>?, hitElement: JsonElement, sourceKey: String?, addEsMetadataFields: Boolean): Hit<T, K>? {
        if (!hitElement.isJsonObject)
            return null

        val hitObject = hitElement.asJsonObject
        val id = hitObject["_id"]
        val index = hitObject["_index"].asString
        val type = hitObject["_type"].asString
        var score: Double? = null
        if (hitObject.has("_score") && !hitObject["_score"].isJsonNull) {
            score = hitObject["_score"].asDouble
        }
        val explanation = hitObject[EXPLANATION_KEY]
        val highlight = extractJsonObject(hitObject.getAsJsonObject(HIGHLIGHT_KEY))
        val fields = extractJsonObject(hitObject.getAsJsonObject(FIELDS_KEY))
        val sort = extractSort(hitObject.getAsJsonArray(SORT_KEY))
        var source = hitObject.getAsJsonObject(sourceKey) ?: JsonObject()

        // 添加es元字段:_id/_version
        if (addEsMetadataFields) {
            var clonedSource: JsonObject? = null
            for (metaField in META_FIELDS) {
                val metaElement = hitObject[metaField.esFieldName]
                if (metaElement != null) {
                    if (clonedSource == null) {
                        clonedSource = CloneUtils.deepClone(source) as JsonObject
                    }
                    clonedSource.add(metaField.internalFieldName, metaElement)
                }
            }
            if (clonedSource != null) {
                source = clonedSource
            }
        }

        return Hit(
                sourceType,
                source,
                explanationType,
                explanation,
                highlight,
                fields,
                sort,
                index,
                type,
                score
        )
    }

    protected fun extractSort(sort: JsonArray?): List<String>? {
        if (sort == null)
            return null

        return sort.map { sortValue ->
            if (sortValue.isJsonNull) "" else sortValue.asString
        }
    }

    protected fun extractJsonObject(highlight: JsonObject?): Map<String, List<String>>? {
        if (highlight == null)
            return null

        return highlight.entrySet().associate { (key, value) ->
            // 转list
            val fragments = value.asJsonArray.map {
                it.asString
            }
            key to fragments
        }
    }

    val total: Int?
        get() {
            val obj = getPath(PATH_TO_TOTAL)
            return obj?.asInt
        }

    val maxScore: Float?
        get() {
            return getPath(PATH_TO_MAX_SCORE)?.asFloat
        }

    protected fun getPath(path: List<String>): JsonElement? {
        if (jsonObject == null)
            return null

        var obj: JsonElement? = jsonObject
        for (prop in path) {
            if (obj == null)
                break

            obj = (obj as JsonObject)[prop]
        }

        return obj
    }

    val aggregations: MetricAggregation
        get() {
            val rootAggrgationName = "aggs"
            if (jsonObject == null)
                return RootAggregation(rootAggrgationName, JsonObject())

            if (jsonObject.has("aggregations"))
                return RootAggregation(rootAggrgationName, jsonObject.getAsJsonObject("aggregations"))

            if (jsonObject.has("aggs"))
                return RootAggregation(rootAggrgationName, jsonObject.getAsJsonObject("aggs"))

            return RootAggregation(rootAggrgationName, JsonObject())
        }

    /**
     * Immutable class representing a search hit.
     *
     * @param <T> type of source
     * @param <K> type of explanation
     * @author cihat keser
     */
    inner class Hit<T, K>(
            val source: T,
            val explanation: K? = null,
            val highlight: Map<String, List<String>>? = null,
            val fields: Map<String, List<String>>? = null,
            val sort: List<String>? = null,
            val index: String? = null,
            val type: String? = null,
            val score: Double? = null
    ) {

        constructor(sourceType: Class<T>?, source: JsonElement?, explanationType: Class<K>? = null, explanation: JsonElement? = null,
                    highlight: Map<String, List<String>>? = null, fields: Map<String, List<String>>? = null, sort: List<String>? = null,
                    index: String? = null, type: String? = null, score: Double? = null)
                : this(createSourceObject(source, sourceType), if (explanation == null) null else createSourceObject(explanation, explanationType),
                highlight, fields, sort, index, type, score)

        override fun hashCode(): Int {
            return Objects.hash(
                    source,
                    explanation,
                    highlight,
                    sort,
                    index,
                    type)
        }

        override fun equals(obj: Any?): Boolean {
            if (obj == null)
                return false

            if (obj === this)
                return true

            if (obj.javaClass != javaClass)
                return false

            val rhs = obj as Hit<*, *>?
            return (source == rhs!!.source
                    && explanation == rhs.explanation
                    && highlight == rhs.highlight
                    && sort == rhs.sort
                    && index == rhs.index
                    && type == rhs.type)
        }
    }

}