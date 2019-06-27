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
         * 被修改的key的标记, 就是标记被修改的key对应的index
         */
        protected final BitSet _dirtyBits = new BitSet(_keys.length);

        /**
         * key集
         */
        protected transient Set<String> _keySet;

        /**
         * value集
         */
        protected transient Collection<Object> _vals;

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
         * 检查是否包含value
         * @param value
         * @return
         */
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
         * 添加多个元素
         * @param m
         */
        @Override
        public void putAll(final Map<? extends String, ?> m) {
            for (Map.Entry<? extends String, ?> e : m.entrySet())
                put(e.getKey(), e.getValue());
        }

        /**
         * 清除所有元素
         */
        @Override
        public void clear() {
            //throw new UnsupportedOperationException();
            // Orm.delete()中需要调用该clear()方法,不能直接抛异常
            for(int i = 0; i < _values.length; i++)
                _values[i] = null;

            _dirtyBits.clear();
        }

        /**
         * 获得元素个数
         * @return
         */
        @Override
        public int size() {
            return _dirtyBits.cardinality();
        }

        /********************* KeySet ************************/
        @Override
        public Set<String> keySet() {
            if (_keySet == null)
                _keySet = new KeySet();

            return _keySet;
        }

        protected class KeySet extends IBitSet<String>{

            public KeySet() {
                super(_dirtyBits);
            }

            @Override
            public String getElement(int index) {
                try {
                    return _keys[index];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public boolean removeElement(int index) {
                return FixedKeyMap.this.remove(index) != null;
            }
        };

        /*********************** Values ************************/
        @Override
        public Collection<Object> values() {
            if(this._vals == null)
                _vals = new Values();
            return _vals;
        }

        final class Values extends IBitCollection<Object> {

            public Values() {
                super(_dirtyBits);
            }

            @Override
            public Object getElement(int index) {
                try{
                    return _values[index];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public boolean removeElement(int index) {
                return FixedKeyMap.this.remove(index) != null;
            }
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




