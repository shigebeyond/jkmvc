package net.jkcode.jkmvc.common;

import java.util.*;

/**
 * 延迟申请内存的map
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-31 9:43 PM
 */
public class LazyAllocatedMap<K, V> extends AbstractMap<K, V> {

    /**
     * 被代理的map
     */
    protected Map<K, V> map = Collections.EMPTY_MAP;

    /**
     * 获得可添加元素的map
     * @return
     */
    protected Map<K, V> getPutableMap(){
        if(map == Collections.EMPTY_MAP)
            map = new HashMap();
        return map;
    }

    @Override
    public V get(final Object key) {
        return map.get(key);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsValue(final Object value) {
        return map.containsValue(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    @Override
    public V put(final K key, final V value) {
        return getPutableMap().put(key, value);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        getPutableMap().putAll(m);
    }

    @Override
    public V remove(final Object key) {
        if(map == Collections.EMPTY_MAP)
            return null;

        return map.remove(key);
    }

    @Override
    public void clear() {
        if(map == Collections.EMPTY_MAP)
            return;

        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}