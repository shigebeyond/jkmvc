package net.jkcode.jkmvc.common;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;

import java.util.*;

/**
 * 有固定key的map的工厂
 *    主要是为2类场景准备
 *    1. 存储从jdbc结果集中读取的行数据
 *    2. OrmEntity 中的 data 属性的类型
 *
 * 参考 qpid-java-old 的 org.apache.qpid.server.util.FixedKeyMapCreator
 * https://github.com/moazbaghdadi/qpid-java-old/blob/b266b647c8525d531b1dfbacd56757977dacd38b/broker-core/src/main/java/org/apache/qpid/server/util/FixedKeyMapCreator.java
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
    protected final int[] _keyHashCodes;

    /**
     * key集
     */
    protected final Set<String> _keySet = new AbstractSet<String>() {
        @Override
        public Iterator<String> iterator() {
            return new KeysIterator();
        }

        @Override
        public int size() {
            return _keys.length;
        }
    };

    /**
     * 构造函数
     * @param keys
     */
    public FixedKeyMapFactory(final String... keys) {
        _keys = keys;
        _keyHashCodes = new int[keys.length];

        // 检查key是否有重复
        Set<String> uniqueKeys = new HashSet(Arrays.asList(keys));
        if (uniqueKeys.size() != keys.length) {
            List<String> duplicateKeys = new ArrayList(Arrays.asList(keys));
            duplicateKeys.removeAll(uniqueKeys);
            throw new IllegalArgumentException("The supplied keys must be unique, but the following keys are duplicated: " + duplicateKeys);
        }

        for (int i = 0; i < keys.length; i++)
            _keyHashCodes[i] = keys[i].hashCode();
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
            if (_keyHashCodes[i] == keyHashCode && _keys[i].equals(key))
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
         * 构造函数
         * @param values
         */
        protected FixedKeyMap(final Object[] values) {
            _values = values;
        }

        @Override
        public Object get(final Object key) {
            int i = indexOf(key);
            if(i == -1)
                return null;

            return _values[i];
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsValue(final Object value) {
            // null
            if (value == null) {
                for (Object o : _values)
                    if (o == null)
                        return true;

                return false;
            }

            // 非null
            for (Object o : _values)
                if (value.equals(o))
                    return true;
            return false;
        }

        @Override
        public boolean containsKey(final Object key) {
            int i = indexOf(key);
            return i > -1;
        }

        @Override
        public Object put(final String key, final Object value) {
            int i = indexOf(key);
            if(i == -1)
                throw new IllegalArgumentException("Unkown key: " + key);

            Object oldValue = _values[i];
            _values[i] = value;
            return oldValue;
        }

        @Override
        public Object remove(final Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(final Map<? extends String, ?> m) {
            for (Map.Entry<? extends String, ?> e : m.entrySet())
                put(e.getKey(), e.getValue());
        }

        @Override
        public void clear() {
            //throw new UnsupportedOperationException();
            // Orm.delete()中需要调用该clear()方法,不能直接抛异常
            for(int i = 0; i < _values.length; i++)
                _values[i] = null;
        }

        @Override
        public Set<String> keySet() {
            return _keySet;
        }

        @Override
        public Collection<Object> values() {
            return Arrays.asList(_values);
        }

        @Override
        public int size() {
            return _values.length;
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new EntrySet();
        }

        protected class EntrySet extends AbstractSet<Entry<String, Object>> {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new EntrySetIterator();
            }

            @Override
            public int size() {
                return _keys.length;
            }
        }

        /**
         * 键值对的迭代器
         */
        protected class EntrySetIterator implements Iterator<Entry<String, Object>> {
            protected int _position = 0;

            public boolean hasNext() {
                return _position < _keys.length;
            }

            public Entry<String, Object> next() {
                try {
                    final String key = _keys[_position];
                    final Object value = _values[_position++];
                    return new DefaultMapEntry(key, value);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * key的迭代器
     */
    protected class KeysIterator implements Iterator<String> {
        protected int _position = 0;

        public boolean hasNext() {
            return _position < _keys.length;
        }

        public String next() {
            try {
                return _keys[_position++];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
