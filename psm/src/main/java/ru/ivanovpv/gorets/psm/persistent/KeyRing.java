/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/KeyRing.java $
 */

package ru.ivanovpv.gorets.psm.persistent;

import ru.ivanovpv.gorets.psm.cipher.FingerPrint;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeMap;

public class KeyRing implements Serializable {
    private TreeMap<Long, TypedKey> keys;

    public KeyRing() {
        keys=new TreeMap<Long, TypedKey>();
    }

    private KeyRing getKeyRing(char type) {
        KeyRing subKeyRing=new KeyRing();
        for(Long time:keys.keySet()) {
            TypedKey typedKey=keys.get(time);
            if(typedKey.getKeyType()==type)
                subKeyRing.addTypedKey(typedKey);
        }
        return subKeyRing;
    }

    /**
     * Hot fix to avoid using floorKey which is absent for API < 9
     * @param time
     * @return
     */
    public Long floorKey(long time) {
        Set<Long> times=this.keys.keySet();
        Long floorTime=null;
        for(long tm:times) {
            if(tm > time)
                break;
            floorTime=tm;
        }
        return floorTime;
    }

    private TreeMap<Long, TypedKey> getKeys() {
        return this.keys;
    }

    public int size() {
        return keys.size();
    }

    public void addKey(byte[] key, long time, char type) {
        TypedKey typedKey=new TypedKey(key, time, type);
        keys.put(new Long(time), typedKey);
    }

    public void addTypedKey(TypedKey typedKey) {
        keys.put(typedKey.getTime(), typedKey);
    }

    /**
     * Returns most appropriate key of given type
     * @param time
     * @param type
     * @return
     */
    public byte[] getKey(long time, char type) {
        KeyRing subKeyRing=this.getKeyRing(type);
        //Long floorTime=subKeyRing.getKeys().floorKey(time);
        Long floorTime=subKeyRing.floorKey(time);
        if(floorTime!=null)
            return subKeyRing.getKeys().get(floorTime).getKey();
        return null;
    }

    /**
     * Returns most appropriate key doesn't matter of type
     * @param time
     * @return
     */
    public byte[] getKey(long time) {
        //Long floorTime=this.getKeys().floorKey(time);
        Long floorTime=this.floorKey(time);
        if(floorTime!=null)
            return this.getKeys().get(floorTime).getKey();
        return null;
    }

    public void removeKey(long time, char type) {
        for(Long mtime:keys.keySet()) {
            if(time==mtime) {
                TypedKey typedKey=keys.get(mtime);
                if(typedKey!=null && typedKey.getKeyType()==type)
                    keys.remove(time);
            }
        }
    }

    public Hashtable<Long, FingerPrint> getKeysFingerPrints() {
        FingerPrint fingerPrint;
        Hashtable<Long, FingerPrint> fingers=new Hashtable<Long, FingerPrint>();
        for(Long time:keys.keySet()) {
            TypedKey typedKey=keys.get(time);
            byte[] key=typedKey.getKey();
            fingerPrint=new FingerPrint(key, typedKey.getKeyType());
            fingers.put(time, fingerPrint);
        }
        return fingers;
    }
}
