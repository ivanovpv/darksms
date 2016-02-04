package ru.ivanovpv.gorets.psm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import android.util.Log;
import ru.ivanovpv.gorets.psm.cipher.*;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;
import ru.ivanovpv.gorets.psm.nativelib.NativeRandom;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolAccept;
import ru.ivanovpv.gorets.psm.protocol.ProtocolSecure;
import ru.ivanovpv.gorets.psm.protocol.ProtocolInvite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class ru.ivanovpv.gorets.psm.PSMActivity_Test \
 * ru.ivanovpv.gorets.psm.tests/android.test.InstrumentationTestRunner
 */
public class PSMActivity_Test extends AndroidTestCase
{
    private static final String TAG=PSMActivity_Test.class.getSimpleName();
    String passwordAlice= "Alice";
    String passwordBob= "Bob";
    public PSMActivity_Test() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void runTest() throws Throwable {
        Log.i(TAG, "Running all tests! I.e. methods like public void test*()");
         super.runTest();
    }

    @SmallTest
    public void testDiffieHellman() {
        byte[] hash1= Cipher.generateDigest(passwordAlice, Cipher.DIGEST_WHIRLPOOL, 512);
        byte[] privateKey1=hash1;
        byte[] hash2=Cipher.generateDigest(passwordAlice, Cipher.DIGEST_WHIRLPOOL, 512);
        byte[] privateKey2=hash2;

        long start=System.currentTimeMillis();
        DiffieHellman dh1=new DiffieHellman(privateKey1);
        long mid=System.currentTimeMillis();
        DiffieHellman dh2=new DiffieHellman(privateKey2);
        long end=System.currentTimeMillis();
        privateKey1=dh1.getPrivateKey();
        byte[] publicKey1=dh1.getPublicKey();
        privateKey2=dh2.getPrivateKey();
        byte[] publicKey2=dh2.getPublicKey();
        long start1=System.currentTimeMillis();
        byte[] sharedKey1=dh1.getSharedKey(publicKey2);
        long mid1=System.currentTimeMillis();
        byte[] sharedKey2=dh2.getSharedKey(publicKey1);
        long end1=System.currentTimeMillis();
        Log.i(TAG, "Party A:");
        Log.i(TAG, "Password A= " + passwordAlice);
        Log.i(TAG, "Whirlpool hash A= " + ByteUtils.bytesToHex(hash1));
        Log.i(TAG, "Private key A= " + privateKey1);
        Log.i(TAG, "Public key A= " + publicKey1);
        Log.i(TAG, "Calculated for= " + (mid - start) + " ms");
        Log.i(TAG, "Shared key A= " + sharedKey1);
        Log.i(TAG, "Calculated for= " + (mid1 - start1) + " ms");
        Log.i(TAG, "Party B:");
        Log.i(TAG, "Password B= " + passwordBob);
        Log.i(TAG, "Whirlpool hash B= " + ByteUtils.bytesToHex(hash2));
        Log.i(TAG, "Private key B= " + privateKey2);
        Log.i(TAG, "Public  key B= " + publicKey2);
        Log.i(TAG, "Calculated for= " + (end - mid) + " ms");
        Log.i(TAG, "Shared  key B= " + sharedKey2);
        Log.i(TAG, "Calculated for= " + (end1 - mid1) + " ms");
        assertEquals(ByteUtils.bytesToHex(sharedKey1), ByteUtils.bytesToHex(sharedKey2));
    }

    @SmallTest
    public void testNativeSHA512() {
        byte[] hash= NativeLib.generatePK("hello", "world", NativeLib.HASH_SHA512, 512);
        Log.i(TAG, "Native hash 512= "+ByteUtils.bytesToHex(hash));
        assertNotNull(hash);
    }

    @SmallTest
    public void testNativeGetECPubKey() {
        byte[] privKey = NativeLib.generatePK("hello", "world", NativeLib.HASH_SHA512, 512);
        Log.i(TAG, "EC Private key 512= "+ByteUtils.bytesToHex(privKey));
        Log.i(TAG, "Private key length = "+privKey.length*8 + " bits");
        assertNotNull(privKey);

        byte[] pubKey = NativeLib.getECPublicKey( privKey, NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC public key 112b = "+ByteUtils.bytesToHex(pubKey));
        Log.i(TAG, "Public key length = "+pubKey.length*8 + " bits");
        assertNotNull(pubKey);
        assert ( pubKey.length != 112/8+1);

        pubKey = NativeLib.getECPublicKey( privKey, NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC public key 256b = "+ByteUtils.bytesToHex(pubKey));
        Log.i(TAG, "Public key length = "+pubKey.length*8 + " bits");
        assertNotNull(pubKey);
        assert ( pubKey.length != 256/8+1);

        pubKey = NativeLib.getECPublicKey( privKey, NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC public key 384b = "+ByteUtils.bytesToHex(pubKey));
        Log.i(TAG, "Public key length = "+pubKey.length*8 + " bits");
        assertNotNull(pubKey);
        assert ( pubKey.length != 384/8+1);
    }

    @SmallTest
    public void testNativedDeriveECKey() {
        final String TAG= "testNativeDeriveECkey";
        byte[] privKey1 = NativeLib.generatePK("hello#1", "world#1", NativeLib.HASH_SHA512, 512);
        Log.i(TAG, "EC private key #1= "+ByteUtils.bytesToHex(privKey1));
        Log.i(TAG, "Key length = "+privKey1.length*8 + " bits");
        assertNotNull(privKey1);

        byte[] privKey2 = NativeLib.generatePK("hello#2", "world#2", NativeLib.HASH_WHIRLPOOL, 512);
        Log.i(TAG, "EC private key #2 == "+ByteUtils.bytesToHex(privKey2));
        Log.i(TAG, "Key length = "+privKey2.length*8 + " bits");
        assertNotNull(privKey2);

        byte[] pubKey1;
        byte[] pubKey2;
        byte[] resKey1;
        byte[] resKey2;

        // 112 bit
        pubKey1 = NativeLib.getECPublicKey( privKey1, NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC public key #1 112b = "+ByteUtils.bytesToHex(pubKey1));
        Log.i(TAG, "Key length = "+pubKey1.length*8 + " bits");
        assertNotNull(pubKey1);
        assert ( pubKey1.length != 112/8+1);

        pubKey2 = NativeLib.getECPublicKey( privKey2, NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC public key #2 112b = "+ByteUtils.bytesToHex(pubKey2));
        Log.i(TAG, "Key length = "+pubKey2.length*8 + " bits");
        assertNotNull(pubKey2);
        assert ( pubKey2.length != 112/8+1);

        resKey1 = NativeLib.getECSharedKey(privKey1, pubKey2, NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC shared key #1 112b = "+ByteUtils.bytesToHex(resKey1));
        Log.i(TAG, "Key length = "+resKey1.length*8 + " bits");
        assertNotNull(resKey1);
        assert ( resKey1.length != 112/8+1);

        resKey2 = NativeLib.getECSharedKey(privKey2, pubKey1, NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC shared key #2 112b = "+ByteUtils.bytesToHex(resKey2));
        Log.i(TAG, "Key length = "+resKey2.length*8 + " bits");
        assertNotNull(resKey2);
        assert ( resKey2.length != 112/8+1);

        assert (Arrays.equals(resKey1, resKey2));

        // 256 bit
        pubKey1 = NativeLib.getECPublicKey( privKey1, NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC public key #1 256b = "+ByteUtils.bytesToHex(pubKey1));
        Log.i(TAG, "Key length = "+pubKey1.length*8 + " bits");
        assertNotNull(pubKey1);
        assert ( pubKey1.length != 256/8+1);

        pubKey2 = NativeLib.getECPublicKey( privKey2, NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC public key #2 256b = "+ByteUtils.bytesToHex(pubKey2));
        Log.i(TAG, "Key length = "+pubKey2.length*8 + " bits");
        assertNotNull(pubKey2);
        assert ( pubKey2.length != 256/8+1);

        resKey1 = NativeLib.getECSharedKey(privKey1, pubKey2, NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC shared key #1 256b = "+ByteUtils.bytesToHex(resKey1));
        Log.i(TAG, "Key length = "+resKey1.length*8 + " bits");
        assertNotNull(resKey1);
        assert ( resKey1.length != 256/8+1);

        resKey2 = NativeLib.getECSharedKey(privKey2, pubKey1, NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC shared key #2 256b = "+ByteUtils.bytesToHex(resKey2));
        Log.i(TAG, "Key length = "+resKey2.length*8 + " bits");
        assertNotNull(resKey2);
        assert ( resKey2.length != 256/8+1);

        assert (Arrays.equals(resKey1, resKey2));

        // 384 bit
        pubKey1 = NativeLib.getECPublicKey( privKey1, NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC public key #1 384b = "+ByteUtils.bytesToHex(pubKey1));
        Log.i(TAG, "Key length = "+pubKey1.length*8 + " bits");
        assertNotNull(pubKey1);
        assert ( pubKey1.length != 384/8+1);

        pubKey2 = NativeLib.getECPublicKey( privKey2, NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC public key #2 384b = "+ByteUtils.bytesToHex(pubKey2));
        Log.i(TAG, "Key length = "+pubKey2.length*8 + " bits");
        assertNotNull(pubKey2);
        assert ( pubKey2.length != 384/8+1);

        resKey1 = NativeLib.getECSharedKey(privKey1, pubKey2, NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC shared key #1 384b = "+ByteUtils.bytesToHex(resKey1));
        Log.i(TAG, "Key length = "+resKey1.length*8 + " bits");
        assertNotNull(resKey1);
        assert ( resKey1.length != 384/8+1);

        resKey2 = NativeLib.getECSharedKey(privKey2, pubKey1, NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC shared key #2 384b = "+ByteUtils.bytesToHex(resKey2));
        Log.i(TAG, "Key length = "+resKey2.length*8 + " bits");
        assertNotNull(resKey2);
        assert ( resKey2.length != 384/8+1);

        assert (Arrays.equals(resKey1, resKey2));

    }


    @SmallTest
    public void testNativeWhirlpool() {
        final String TAG= "testNativeWhirlpool";
        byte[] hashNative= NativeLib.generatePK("hello", "", NativeLib.HASH_WHIRLPOOL, 512);
        assertNotNull(hashNative);
        Log.i(TAG, "Native Whirlpool hash= "+ByteUtils.bytesToHex(hashNative));
        byte[] hashJava= Cipher.generateDigest("hello", Cipher.DIGEST_WHIRLPOOL, 512);
        assertNotNull(hashJava);
        Log.i(TAG, "Java Whirlpool hash= "+ByteUtils.bytesToHex(hashJava));
        assertEquals(ByteUtils.bytesToHex(hashJava), ByteUtils.bytesToHex(hashJava));
    }

    @SmallTest
    public void testNativeHashSalt() {
        final String TAG= "testNativeHash";
        String password= "HelloWorld";
        String salt=new Salt(32).getSaltString();
        byte[] hashNative= NativeLib.generatePK(password, salt, NativeLib.HASH_WHIRLPOOL, 512);
        Log.i(TAG, "Native hash= "+ByteUtils.bytesToHex(hashNative));
        String preSalt=salt.substring(0, salt.length()/2);
        String postSalt=salt.substring(salt.length()/2);
        byte[] hashJava= Cipher.generateDigest(preSalt + password + postSalt, Cipher.DIGEST_WHIRLPOOL, 512);
        Log.i(TAG, "Java hash= "+ByteUtils.bytesToHex(hashNative));
        assertEquals(ByteUtils.bytesToHex(hashJava), ByteUtils.bytesToHex(hashJava));
    }

    /*@SmallTest
    public void testECParameters() {
        byte[] parm=NativeLib.getECParameters(NativeLib.EC_GROUP_112B);
        Log.i(TAG, "EC 192 parameters array= "+ByteUtils.bytesToHex(parm));
        assertNotNull(parm);
        parm=NativeLib.getECParameters(NativeLib.EC_GROUP_256B);
        Log.i(TAG, "EC 256 parameters array= "+ByteUtils.bytesToHex(parm));
        assertNotNull(parm);
        parm=NativeLib.getECParameters(NativeLib.EC_GROUP_384B);
        Log.i(TAG, "EC 384 parameters array= "+ByteUtils.bytesToHex(parm));
        assertNotNull(parm);
    } */

    @SmallTest
    public void testRate() {
        final String TAG= "testRate";
        byte[] data=new byte[1024];
        for(int i=0; i < data.length; i++)
            data[i]=0;
        long start=System.currentTimeMillis();
        byte[] hash=NativeLib.generateHash(data, NativeLib.HASH_SHA512, 10000);
        long end=System.currentTimeMillis();
        Log.i(TAG, "10000 iterations of sha-512 produced in= "+(end-start)+" ms");
        Log.i(TAG, "SHA-512 hash is= "+ByteUtils.bytesToHex(hash));

        start=System.currentTimeMillis();
        hash=NativeLib.generateHash(data, NativeLib.HASH_WHIRLPOOL, 10000);
        end=System.currentTimeMillis();
        Log.i(TAG, "10000 iterations of whirlpool hash produced in= "+(end-start)+" ms");
        Log.i(TAG, "Whirlpool hash is= "+ByteUtils.bytesToHex(hash));
    }

    @SmallTest
    public void testIterations() {
        final int numIterations=100;
        final String TAG= "testIterations";
        byte[] data=new byte[1024];
        for(int i=0; i < data.length; i++)
            data[i]=0;
        long start=System.currentTimeMillis();
        byte[] hash=NativeLib.generateHash(data, NativeLib.HASH_WHIRLPOOL, numIterations);
        long end=System.currentTimeMillis();
        Log.i(TAG, numIterations+ " iterations of native whirlpool hash produced in= "+(end-start)+" ms");
        Log.i(TAG, "Native whirlpool hash = "+ByteUtils.bytesToHex(hash));

        Whirlpool w=new Whirlpool();
        int length=data.length;
        byte[] digest=new byte[Whirlpool.DIGESTBYTES];
        start=System.currentTimeMillis();
        for(int i=0; i < numIterations+1; i++) {
            w.NESSIEinit();
            w.NESSIEadd(data, length*8);
            w.NESSIEfinalize(digest);
            System.arraycopy(digest, 0, data, 0, digest.length);
            length=digest.length;
        }
        end=System.currentTimeMillis();
        Log.i(TAG, numIterations + " iterations of Java whirlpool hash produced in= "+(end-start)+" ms");
        Log.i(TAG, "Java whirlpool hash = "+ByteUtils.bytesToHex(digest));
        assertEquals("Equality of hashes", ByteUtils.bytesToHex(digest), ByteUtils.bytesToHex(hash));
    }

   /* @SmallTest
    public void testProtocols() {
        final String TAG= "testProtocols";
        String orgMsg= "Hello World!";
        String receiveString= "";
        Log.i(TAG, "Original message= " + orgMsg);
        byte[] sharedKey= ByteUtils.stringToByteArray("example shared key"); //kinda shared key
        byte[] key=NativeLib.generateHash(sharedKey, NativeLib.HASH_SHA512, 0); //align shared key as 512 bits
        Cipher sendCipher=new Rijndael(key, 32);
        NativeRandom r=new NativeRandom();
        Protocol sendProtocol=new ProtocolSecure(sendCipher, r.getInt(Protocol.SESSION_SIZE));
        String sendMsg=sendProtocol.encodeString(orgMsg);
        Log.i(TAG, "Sending message= " + sendMsg);
        //
        try {
            Protocol receiveProtocol=Protocol.parseProtocol(sendMsg, key);
            receiveString=receiveProtocol.decodeString(sendMsg);
            Log.i(TAG, "Decoded message= " + receiveString);
        }
        catch(Exception ex) {
            Log.e(TAG, "Something went wrong", ex);
        }
        assertEquals(receiveString, orgMsg);
    }
     */

    @SmallTest
    public void testInviteDiffieHellman() {
        final String TAG= "testInviteDiffieHellman";
        byte[] privateKeyAlice={0,0,0,0,1,2,3,4,5,6,7,8,9};
        byte[] privateKeyBob={1,1,1,1};
        byte[] publicKeyAlice, publicKeyBob;
        try {
        //Alice
            KeyExchange keyExchange=new DiffieHellman(privateKeyAlice);
            Protocol protocol=new ProtocolInvite(keyExchange);
            publicKeyAlice=keyExchange.getPublicKey();
            String inviteMessage=protocol.encodeBytes(publicKeyAlice);
            Log.i(TAG,"Public key= "+ ByteUtils.bytesToHex(publicKeyAlice)+"\n");
            Log.i(TAG,"Invite message= "+inviteMessage);

        //Bob
            protocol=Protocol.parseProtocol(inviteMessage);
            byte[] buffer=protocol.decodeBytes(inviteMessage);
            Log.i(TAG,"Decoded key= "+ByteUtils.bytesToHex(buffer));

            BigInteger pkAlice=new BigInteger(publicKeyAlice);
            BigInteger decodedAlice=new BigInteger(buffer);
            assertEquals("Testing sent/received public keys for Alice",
                    ByteUtils.bytesToHex(decodedAlice.toByteArray()),
                    ByteUtils.bytesToHex(pkAlice.toByteArray()));
            keyExchange=new DiffieHellman(privateKeyBob);
            publicKeyBob=keyExchange.getPublicKey();
            protocol=new ProtocolAccept(keyExchange);
            String acceptMessage=protocol.encodeBytes(publicKeyBob);
            Log.i(TAG,"Accept message= "+acceptMessage);

        //again Alice
            protocol=Protocol.parseProtocol(acceptMessage);
            buffer=protocol.decodeBytes(acceptMessage);
            Log.i(TAG,"Decoded key= "+ByteUtils.bytesToHex(buffer));
            BigInteger pkBob=new BigInteger(publicKeyBob);
            BigInteger decodedBob=new BigInteger(buffer);
            assertEquals("Testing sent/received public keys",
                    ByteUtils.bytesToHex(decodedBob.toByteArray()), ByteUtils.bytesToHex(pkBob.toByteArray()));
        }
        catch(Exception ex) {
            Log.e(TAG,"Test failed!", ex);
        }
    }

    @SmallTest
    public void testInviteEC() {
        final String TAG= "testInviteEC";
        byte[] privateKeyAlice={2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2};
        byte[] privateKeyBob={1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
        byte[] publicKeyAlice, publicKeyBob;
        try {
        //Alice
            KeyExchange keyExchange=new EllipticCurve112B(privateKeyAlice);
            Protocol protocol=new ProtocolInvite(keyExchange);
            publicKeyAlice=keyExchange.getPublicKey();
            String inviteMessage=protocol.encodeBytes(publicKeyAlice);
            Log.i(TAG,"Public key= "+ ByteUtils.bytesToHex(publicKeyAlice)+"\n");
            Log.i(TAG,"Invite message= "+inviteMessage);

        //Bob
            protocol=Protocol.parseProtocol(inviteMessage);
            byte[] buffer=protocol.decodeBytes(inviteMessage);
            Log.i(TAG,"Decoded key= "+ByteUtils.bytesToHex(buffer));

            BigInteger pkAlice=new BigInteger(publicKeyAlice);
            BigInteger decodedAlice=new BigInteger(buffer);
            assertEquals("Testing sent/received public keys for Alice", ByteUtils.bytesToHex(decodedAlice.toByteArray()), ByteUtils.bytesToHex(pkAlice.toByteArray()));
            keyExchange=new EllipticCurve384B(privateKeyBob);
            publicKeyBob=keyExchange.getPublicKey();
            protocol=new ProtocolAccept(keyExchange);
            String acceptMessage=protocol.encodeBytes(publicKeyBob);
            Log.i(TAG,"Accept message= "+acceptMessage);

        //again Alice
            protocol=Protocol.parseProtocol(acceptMessage);
            buffer=protocol.decodeBytes(acceptMessage);
            Log.i(TAG,"Decoded key= "+ByteUtils.bytesToHex(buffer));
            BigInteger pkBob=new BigInteger(publicKeyBob);
            BigInteger decodedBob=new BigInteger(buffer);
            assertEquals("Testing sent/received public keys", ByteUtils.bytesToHex(decodedBob.toByteArray()), ByteUtils.bytesToHex(pkBob.toByteArray()));
        }
        catch(Exception ex) {
            Log.e(TAG, "Test failed!", ex);
        }
    }

    @SmallTest
    public void testNativeRandomGenerator() {
        final String TAG= "testNativeRandomGenerator";
        byte[] randArr = NativeLib.getRandomBytes(512);
        assert( randArr == null );
        Log.i(TAG, "Rand bytes= "+ByteUtils.bytesToHex(randArr));
        randArr = NativeLib.getRandomBytes(0);
        assert( randArr != null );
        NativeRandom r=new NativeRandom();
        for(int i=0; i < 100; i++) {
            int rnd=r.getInt(10);
            Log.i(TAG, "Random i>=0 and i < 10: "+rnd);
        }
    }

    @SmallTest
    public void testPublicKeyEncoding() {
        final String TAG= "testTempCipher";
        String mask="v5hm43mihd2qrchakab46oehahgslle91oju3msj2831usknr30";
        String publicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi5VJNsAPWa2t4ZG/gKSUt+CoOzk48529ag8Xyfff+is9VQMpcjVwRfnRnFJVuIWdDHYQ9AYBcLsbxCFRGEjLdvWetzSdoVefHSNaskQlkgWvd0vdANISUHAPZdNxgpQ+DX/hpsnfXgTAfI06DpO/hibpJ0ZYdsuXc4kgz3Q8WrEyVchUGrjf+C62rkEr/ceCeHIgGJP8YU9IR/b45eRPAZWkZGv5hVNUR8/6sQsNuLMkeeEVLBY7wov8XuIfhqrHX1tqbj6lCCizlV8KlhxnlQa/37yrtd7xp1B0pZEtyYjzieDzbW++Pdb1sd6SzqqvRpqbarfETBRUYii5Sxt98QIDAQAB";
        Log.i(TAG, "Original text="+publicKey);
        String s=Hash.encryptTemp(xorString(publicKey, mask));
        Log.i(TAG, "Encrypted text="+s);
        String ss=xorString(Hash.decryptTemp(s), mask);
        Log.i(TAG, "Decrypted text="+ss);
        assertEquals(ss, publicKey);
    }

    @SmallTest
    public void testMerchantId() {
        final String TAG= "testMerchantId";
        String mask="v5hm43mihd2qrchakab46oehahgslle91oju3msj2831usknr30";
        String merchantId="12999763169054705758";
        Log.i(TAG, "Original text="+merchantId);
        String s=Hash.encryptTemp(xorString(merchantId, mask));
        Log.i(TAG, "Encrypted text="+s);
        String ss=xorString(Hash.decryptTemp(s), mask);
        Log.i(TAG, "Decrypted text="+ss);
        assertEquals(ss, merchantId);
    }

    private String xorString(String original, String mask) {
        StringBuilder sb=new StringBuilder();
        for(int i = 0; i < original.length(); i++)
            sb.append((char)(original.charAt(i) ^ mask.charAt(i % mask.length())));
        return sb.toString();
    }

}

