/*
 * $Id: MultiMap.java 20320 2010-11-24 15:03:31Z dfeist $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.multipart;

// ========================================================================
// Copyright (c) 2004-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* ------------------------------------------------------------ */
/**
 * A multi valued Map. This Map specializes HashMap and provides methods that operate
 * on multi valued items.
 * <P>
 * Implemented as a map of LazyList values
 * 
 * @see LazyList
 */
public class MultiMap<K> implements ConcurrentMap<K, Object>
{
    Map<K, Object> _map;
    ConcurrentMap<K, Object> _cmap;

    public MultiMap()
    {
        _map = new HashMap<K, Object>();
    }

    public MultiMap(final Map<K, Object> map)
    {
        if (map instanceof ConcurrentMap)
            _map = _cmap = new ConcurrentHashMap<K, Object>(map);
        else
            _map = new HashMap<K, Object>(map);
    }

    public MultiMap(final int capacity)
    {
        _map = new HashMap<K, Object>(capacity);
    }

    public MultiMap(final boolean concurrent)
    {
        if (concurrent)
            _map = _cmap = new ConcurrentHashMap<K, Object>();
        else
            _map = new HashMap<K, Object>();
    }

    /* ------------------------------------------------------------ */
    /**
     * Get multiple values. Single valued entries are converted to singleton lists.
     * 
     * @param name The entry key.
     * @return Unmodifieable List of values.
     */
    public <E> List<E> getValues(final Object name)
    {
        return LazyList.getList(_map.get(name), true);
    }

    /* ------------------------------------------------------------ */
    /**
     * Get a value from a multiple value. If the value is not a multivalue, then
     * index 0 retrieves the value or null.
     * 
     * @param name The entry key.
     * @param i Index of element to get.
     * @return Unmodifieable List of values.
     */
    public Object getValue(final Object name, final int i)
    {
        final Object l = _map.get(name);
        if (i == 0 && LazyList.size(l) == 0) return null;
        return LazyList.get(l, i);
    }

    /* ------------------------------------------------------------ */
    /**
     * Get value as String. Single valued items are converted to a String with the
     * toString() Object method. Multi valued entries are converted to a comma
     * separated List. No quoting of commas within values is performed.
     * 
     * @param name The entry key.
     * @return String value.
     */
    public String getString(final Object name)
    {
        final Object l = _map.get(name);
        switch (LazyList.size(l))
        {
            case 0 :
                return null;
            case 1 :
                final Object o = LazyList.get(l, 0);
                return o == null ? null : o.toString();
            default :
            {
                final StringBuilder values = new StringBuilder(128);
                for (int i = 0; i < LazyList.size(l); i++)
                {
                    final Object e = LazyList.get(l, i);
                    if (e != null)
                    {
                        if (values.length() > 0) values.append(',');
                        values.append(e.toString());
                    }
                }
                return values.toString();
            }
        }
    }

    /* ------------------------------------------------------------ */
    public Object get(final Object name)
    {
        final Object l = _map.get(name);
        switch (LazyList.size(l))
        {
            case 0 :
                return null;
            case 1 :
                final Object o = LazyList.get(l, 0);
                return o;
            default :
                return LazyList.getList(l, true);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * Put and entry into the map.
     * 
     * @param name The entry key.
     * @param value The entry value.
     * @return The previous value or null.
     */
    public Object put(final K name, final Object value)
    {
        return _map.put(name, LazyList.add(null, value));
    }

    /* ------------------------------------------------------------ */
    /**
     * Put multi valued entry.
     * 
     * @param name The entry key.
     * @param values The List of multiple values.
     * @return The previous value or null.
     */
    public Object putValues(final K name, final List<Object> values)
    {
        return _map.put(name, values);
    }

    /* ------------------------------------------------------------ */
    /**
     * Put multi valued entry.
     * 
     * @param name The entry key.
     * @param values The String array of multiple values.
     * @return The previous value or null.
     */
    public Object putValues(final K name, final String[] values)
    {
        Object list = null;
        for (int i = 0; i < values.length; i++)
            list = LazyList.add(list, values[i]);
        return put(name, list);
    }

    /* ------------------------------------------------------------ */
    /**
     * Add value to multi valued entry. If the entry is single valued, it is
     * converted to the first value of a multi valued entry.
     * 
     * @param name The entry key.
     * @param value The entry value.
     */
    public void add(final K name, final Object value)
    {
        final Object lo = _map.get(name);
        final Object ln = LazyList.add(lo, value);
        if (lo != ln) _map.put(name, ln);
    }

    /* ------------------------------------------------------------ */
    /**
     * Add values to multi valued entry. If the entry is single valued, it is
     * converted to the first value of a multi valued entry.
     * 
     * @param name The entry key.
     * @param values The List of multiple values.
     */
    public void addValues(final K name, final List<Object> values)
    {
        final Object lo = _map.get(name);
        final Object ln = LazyList.addCollection(lo, values);
        if (lo != ln) _map.put(name, ln);
    }

    /* ------------------------------------------------------------ */
    /**
     * Add values to multi valued entry. If the entry is single valued, it is
     * converted to the first value of a multi valued entry.
     * 
     * @param name The entry key.
     * @param values The String array of multiple values.
     */
    public void addValues(final K name, final String[] values)
    {
        final Object lo = _map.get(name);
        final Object ln = LazyList.addCollection(lo, Arrays.asList(values));
        if (lo != ln) _map.put(name, ln);
    }

    /* ------------------------------------------------------------ */
    /**
     * Remove value.
     * 
     * @param name The entry key.
     * @param value The entry value.
     * @return true if it was removed.
     */
    public boolean removeValue(final K name, final Object value)
    {
        final Object lo = _map.get(name);
        Object ln = lo;
        final int s = LazyList.size(lo);
        if (s > 0)
        {
            ln = LazyList.remove(lo, value);
            if (ln == null)
                _map.remove(name);
            else
                _map.put(name, ln);
        }
        return LazyList.size(ln) != s;
    }

    /* ------------------------------------------------------------ */
    /**
     * Put all contents of map.
     * 
     * @param m Map
     */
    public void putAll(final Map<? extends K, ? extends Object> m)
    {
        final boolean multi = m instanceof MultiMap;

        for (final Map.Entry<? extends K, ? extends Object> entry : m.entrySet())
        {
            if (multi)
                _map.put(entry.getKey(), LazyList.clone(entry.getValue()));
            else
                put(entry.getKey(), entry.getValue());
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Map of String arrays
     */
    public Map<K, String[]> toStringArrayMap()
    {
        final Map<K, String[]> map = new HashMap<K, String[]>(_map.size() * 3 / 2);

        for (final Map.Entry<K, Object> entry : _map.entrySet())
        {
            final Object l = entry.getValue();
            final String[] a = LazyList.toStringArray(l);
            map.put(entry.getKey(), a);
        }
        return map;
    }

    public void clear()
    {
        _map.clear();
    }

    public boolean containsKey(final Object key)
    {
        return _map.containsKey(key);
    }

    public boolean containsValue(final Object value)
    {
        return _map.containsValue(value);
    }

    public Set<Entry<K, Object>> entrySet()
    {
        return _map.entrySet();
    }

    @Override
    public boolean equals(final Object o)
    {
        return _map.equals(o);
    }

    @Override
    public int hashCode()
    {
        return _map.hashCode();
    }

    public boolean isEmpty()
    {
        return _map.isEmpty();
    }

    public Set<K> keySet()
    {
        return _map.keySet();
    }

    public Object remove(final Object key)
    {
        return _map.remove(key);
    }

    public int size()
    {
        return _map.size();
    }

    public Collection<Object> values()
    {
        return _map.values();
    }

    public Object putIfAbsent(final K key, final Object value)
    {
        if (_cmap == null) throw new UnsupportedOperationException();
        return _cmap.putIfAbsent(key, value);
    }

    public boolean remove(final Object key, final Object value)
    {
        if (_cmap == null) throw new UnsupportedOperationException();
        return _cmap.remove(key, value);
    }

    public boolean replace(final K key, final Object oldValue, final Object newValue)
    {
        if (_cmap == null) throw new UnsupportedOperationException();
        return _cmap.replace(key, oldValue, newValue);
    }

    public Object replace(final K key, final Object value)
    {
        if (_cmap == null) throw new UnsupportedOperationException();
        return _cmap.replace(key, value);
    }

}
