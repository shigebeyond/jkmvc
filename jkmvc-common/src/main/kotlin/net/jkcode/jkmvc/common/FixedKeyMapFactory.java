package net.jkcode.jkmvc.common;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;

import java.util.*;

/**
 * 有固定key的map的工厂
 *    主要是为2类场景准备
 *    1. 存储从jdbc结果集中读取的行数据
 *    2. OrmEntity 中的 data 属性的类型
 *
 * @author shijianhang<772910474@qq.com>
 * @date 2019-03-12 8:22 PM
 */
public class FixedKeyMapFactory {

    /**
     * 固定的key
     */
    protected final String[] _keys;

    /**
     * 缓存key的哈希码, 加速key对比
     */
    protected final int[] _keyHashs;

    /**
     * 构造函数
     * @param keys
     */
    public FixedKeyMapFactory(final String... keys) {
        // 检查key是否有重复
        _keys = keys;
        List<String> allKeys = Arrays.asList(keys);
        Set<String> uniqueKeys = new HashSet(allKeys);
        if (uniqueKeys.size() != keys.length) {
            allKeys.removeAll(uniqueKeys);
            throw new IllegalArgumentException("The supplied keys must be unique, but the following keys are duplicated: " + allKeys);
        }

        // 计算key的哈希码
        _keyHashs = new int[keys.length];
        for (int i = 0; i < keys.length; i++)
            _keyHashs[i] = keys[i].hashCode();
    }

    /**
     * 创建有固定key的map
     * @param values
     * @return
     */
    public Map<String, Object> createMap(Object... values) {
        if (values.length == 0)
            values = new Object[_keys.length];

        if (values.length != _keys.length)
            throw new IllegalArgumentException("There are " + _keys.length + " keys, so that many values must be supplied");

        return new FixedKeyMap(values);
    }

    /**
     * 找到key的位置
     * @param key
     * @return -1表示未找到
     */
    public int indexOf(final Object key){
        int keyHashCode = key.hashCode();
        for (int i = 0; i < _keys.length; i++)
            if (_keyHashs[i] == keyHashCode && _keys[i].equals(key))
                return i;

        return -1;
    }

    /**
     * 有固定key的map
     */
    protected final class FixedKeyMap extends AbstractMap<String, Object> {

        /**
         * 值
         */
        protected final Object[] _values;

        /**
         * 被修改的key的标记, 就是标记被修改的key对应的index
         */
        protected final BitSet _dirtyBits = new BitSet(_keys.length);

        /**
         * entry集
         */
        protected transient Set<Entry<String, Object>> _entrySet;


        /**
         * 构造函数
         * @param values
         */
        protected FixedKeyMap(final Object[] values) {
            _values = values;
        }

        /**
         * 根据key获得value
         * @param key
         * @return
         */
        @Override
        public Object get(final Object key) {
            int i = indexOf(key);
            if(i == -1)
                return null;

            return _values[i];
        }

        /**
         * 检查是否没有元素
         * @return
         */
        @Override
        public boolean isEmpty() {
            return _dirtyBits.isEmpty();
        }

        /**
         * 检查是否包含key
         * @param key
         * @return
         */
        @Override
        public boolean containsKey(final Object key) {
            int i = indexOf(key);
            return i > -1 && _dirtyBits.get(i);
        }

        /**
         * 检查是否包含value
         * @param value
         * @return
         */
        @Override
        public boolean containsValue(final Object value) {
            // 遍历位
            for (int i = _dirtyBits.nextSetBit(0); i >= 0; i = _dirtyBits.nextSetBit(i+1)) {
                Object o = _values[i];
                if(value == null && o == null || value.equals(o))
                    return true;
            }
            return false;
        }

        /**
         * 添加单个元素
         * @param key
         * @param value
         * @return
         */
        @Override
        public Object put(final String key, final Object value) {
            int i = indexOf(key);
            if(i == -1)
                throw new IllegalArgumentException("Unkown key: " + key);

            Object oldValue = _values[i];

            _values[i] = value;
            _dirtyBits.set(i);
            return oldValue;
        }

        /**
         * 删除key对应的元素
         * @param key
         * @return
         */
        @Override
        public Object remove(final Object key) {
            int i = indexOf(key);
            return remove(i);
        }

        /**
         * 删除位置对应的元素
         * @param i
         * @return
         */
        public Object remove(int i) {
            Object oldValue = _values[i];

            _values[i] = null;
            _dirtyBits.clear(i);
            return oldValue;
        }

        /**
         * 清除所有元素
         */
        @Override
        public void clear() {
            for(int i = 0; i < _values.length; i++)
                _values[i] = null;

            _dirtyBits.clear();
        }

        /*********************** EntrySet ************************/
        @Override
        public Set<Entry<String, Object>> entrySet() {
            if(_entrySet == null)
                _entrySet = new EntrySet();
            return _entrySet;
        }

        protected class EntrySet extends IBitSet<Entry<String, Object>> {

            public EntrySet() {
                super(_dirtyBits);
            }

            @Override
            public Entry<String, Object> getElement(int index) {
                try{
                    return new DefaultMapEntry(_keys[index], _values[index]);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public boolean removeElement(int index) {
                return FixedKeyMap.this.remove(index) != null;
            }
        }
    }

}




