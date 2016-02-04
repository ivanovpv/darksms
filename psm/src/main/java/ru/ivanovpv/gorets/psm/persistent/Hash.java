package ru.ivanovpv.gorets.psm.persistent;

import android.util.Log;
import com.google.gson.Gson;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.*;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 456 $
 *   $LastChangedDate: 2013-12-13 13:22:35 +0400 (Пт, 13 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/Hash.java $
 */

public final class Hash
{

    public final static String TAG=Hash.class.getName();
    private String id;
    private String hash;
    private final static char[] tPassword={38, 52, 1048, 1076, 1080, 1086, 1090, 1086, 1074, 1048, 1076, 1091, 1090, 1054, 1085, 1080, 1053, 1072, 1061, 1091, 1081, 37};
    //                               &   4   I     d     i     o     t     o     v     I     d     u     t     O     n     i     N     a     H     u     j     %

    private static Me me;

    // default empty hash
    public Hash() {
        HashConfiguration hashConfiguration=new HashConfiguration();
        this.hash=hashConfiguration.getHashString();
        this.id=new Long(System.currentTimeMillis()).toString();
    }

    //create new hash from old one (change password)
    public Hash(String password, Hash hash) {
        HashConfiguration hashConfiguration=Hash.restoreFromHashString(hash.getHash());
        HashConfiguration newHashConfiguration=new HashConfiguration(password, hashConfiguration.getDeviceSalt(), hashConfiguration.isEnabled());
        this.hash=newHashConfiguration.getHashString();
        this.id=hash.getId();
    }

    public Hash(String passwordHash, String id, String deviceSalt) {
        HashConfiguration hashConfiguration=new HashConfiguration(passwordHash, deviceSalt);
        this.hash=hashConfiguration.getHashString();
        this.id=id;
    }

    private static HashConfiguration restoreFromHashString(String hashString) {
        String jsonString=decryptTemp(hashString);
        final Gson gson = new Gson();
        return gson.fromJson(jsonString, HashConfiguration.class);
    }

    public static void initApplicationContext() {
        me = Me.getMe();
    }

    public static long getInstallStamp()
    {
        long installStamp=System.currentTimeMillis();
        String installationId=null;
        try {
            installationId= me.psmSettings.installationId().get();
            installStamp=Long.parseLong(decryptTemp(installationId));
            if(installStamp==0L)
                installStamp=System.currentTimeMillis();
        }
        catch(Exception ex) {
        }
        if(installationId==null || installationId.length()==0)
            me.psmSettings.installationId().put(encryptTemp(new Long(installStamp).toString())); //save in prefs
        return installStamp;
    }

    public static boolean isFirstRun()
    {
        boolean firstRun = me.psmSettings.firstRun().get();
        return firstRun;
    }

    public boolean checkPassword(String password)
    {
        HashConfiguration hashConfiguration=restoreFromHashString(this.hash);
        return hashConfiguration.checkPassword(password);
    }

    public boolean isEnabled() {
        HashConfiguration hashConfiguration=restoreFromHashString(this.hash);
        return hashConfiguration.isEnabled();
    }

    public void enable() {
        HashConfiguration hashConfiguration=restoreFromHashString(this.hash);
        hashConfiguration.setEnabled(true);
        this.hash=hashConfiguration.getHashString();
    }

    public void disable() {
        HashConfiguration hashConfiguration=restoreFromHashString(this.hash);
        hashConfiguration.setEnabled(false);
        this.hash=hashConfiguration.getHashString();
    }

    public static Cipher getDefaultSymmetricCipher()
    {
        return getDefaultProtectionCipher(Me.getMe().getHashDAO().get().getKey());
    }

    public static Cipher getDefaultProtectionCipher(byte[] key) {
        Cipher cipher;
        switch(me.getProtectionPrivacyLevel()) {
            case 0:
                cipher=new CipherDES(key); //64 bits key
                break;
            case 1:
                cipher=new Rijndael(key, 32); //256 bits key
                break;
            case 2:
                cipher=new CipherAnubis(key);  //320 bit key
                break;
            default:
                cipher=new CipherDES(key);
        }
        return cipher;
    }

    public static Cipher getDefaultMessagingCipher(byte[] key) {
        Cipher cipher;
        switch(me.getMessagingPrivacyLevel()) {
            case 0:
                cipher=new CipherDES(key); //64 bits key
                break;
            case 1:
                cipher=new Rijndael(key, 32); //256 bits key
                break;
            case 2:
                cipher=new CipherAnubis(key);  //320 bit key
                break;
            default:
                cipher=new CipherDES(key);
        }
        return cipher;
    }

    public static KeyExchange getDefaultKeyExchange(byte[] key) {
        KeyExchange keyExchange;
        switch(me.getDefaultKeyExchangeType()) {
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B:
                keyExchange=new EllipticCurve112B(key); //112 bits elliptic curve
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B:
                keyExchange=new EllipticCurve256B(key); //256 bits elliptic curve
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B:
                keyExchange=new EllipticCurve384B(key); //384 bits elliptic curve
                break;
            default:
                keyExchange=new DiffieHellman(key); //Diffie-Hellman
        }
        return keyExchange;
    }

    public byte[] getKey()
    {
        if(me==null)
            me=Me.getMe();
        String deviceId = me.getAndroidId(); //device id - single point of attack
        HashConfiguration hashConfiguration=restoreFromHashString(this.hash);
        byte[] key = NativeLib.generatePK(deviceId, hashConfiguration.getDeviceSalt(), NativeLib.HASH_WHIRLPOOL, 512);
        //Log.i(TAG, "Getting device key as="+ByteUtils.bytesToHex(key));
        return key;
    }

    public static String encryptTemp(String text)
    {
        Cipher tempCipher=new CipherAnubis(new String(tPassword));
        byte[] buffer= new byte[0];
        buffer = tempCipher.encryptBuffer(ByteUtils.stringToByteArray(text));
        return ByteUtils.bytesToHex(buffer);
    }

    public static String decryptTemp(String text)
    {
        Cipher tempCipher=new CipherAnubis(new String(tPassword));
        byte[] buffer=null;
        buffer=ByteUtils.hexToBytes(text);
        byte[] buf= new byte[0];
        try {
            buf = tempCipher.decryptBuffer(buffer);
        } catch (CipherException e) {
            Log.e(TAG, "Error during temp decryption", e);
        }
        String s=ByteUtils.byteArrayToString(buf, 0);
        return s;
    }

    public String getId() {
        return id;
    }

    public String getHash() {
        return hash;
    }
}

final class HashConfiguration {
    private String passwordHash; //password hash "as-is"
    private String deviceSalt; //device salt
    private boolean enabled; //is password enabled?

    HashConfiguration() {
        this.passwordHash="";
        Salt salt=new Salt(32);
        deviceSalt=salt.getEncodedSaltString();
        enabled=false;
    }

    HashConfiguration(String passwordHash, String deviceSalt) {
        this.passwordHash=passwordHash;
        this.deviceSalt=deviceSalt;
        if(passwordHash==null || passwordHash.length()==0)
            this.enabled=false;
        else
            this.enabled=true;
    }

    HashConfiguration(String password, String deviceSalt, boolean enabled) {
        this.passwordHash=this.calculateHash(password);
        this.deviceSalt=deviceSalt;
        this.enabled=enabled;
        if(passwordHash==null || passwordHash.length()==0)
            this.enabled=false;
        else
            this.enabled=true;
    }

    String getHashString() {
        final Gson gson = new Gson();
        String s=gson.toJson(this, HashConfiguration.class);
        return Hash.encryptTemp(s);
    }

    private String getPasswordSaltString() {
        String passwordSalt;
        String saltString= Me.getMe().psmSettings.passwordSalt().get();
        if(saltString==null || saltString.trim().length()==0)
        {
            //if no salt - create new one
            Salt salt=new Salt(32);
            passwordSalt=salt.getSaltString();
            saltString=Hash.encryptTemp(salt.getEncodedSaltString());
            Me.getMe().psmSettings.passwordSalt().put(saltString); //save in prefs
        }
        else
        {
            passwordSalt=Hash.decryptTemp(saltString);
            passwordSalt=(new Salt(passwordSalt)).getSaltString();
        }
        return passwordSalt;
    }

    boolean checkPassword(String password) {
        String checkHash=this.calculateHash(password);
        if(checkHash.compareTo(passwordHash)==0)
            return true;
        return false;
    }

    private String calculateHash(String password)
    {
        String passwordSalt=this.getPasswordSaltString();
        /*String preSalt=passwordSalt.substring(0, passwordSalt.length()/2);
        String postSalt=passwordSalt.substring(passwordSalt.length()/2);
        byte[] realHash = Cipher.generateDigest(preSalt + password + postSalt, Cipher.DIGEST_WHIRLPOOL, 512);*/
        byte[] realHash = NativeLib.generatePK(password, passwordSalt, NativeLib.HASH_WHIRLPOOL, 512);
        return ByteUtils.byteArrayToString(realHash, 0);
    }

    String getPasswordHash()
    {
        return passwordHash;
    }

    void setPasswordHash(String passwordHash)
    {
        this.passwordHash=passwordHash;
    }

    String getDeviceSalt()
    {
        return deviceSalt;
    }

    void setDeviceSalt(String deviceSalt)
    {
        this.deviceSalt=deviceSalt;
    }

    boolean isEnabled()
    {
        if(passwordHash==null || passwordHash.length()==0)
            enabled=false;
        return enabled;
    }

    void setEnabled(boolean enabled)
    {
        this.enabled=enabled;
    }
}

