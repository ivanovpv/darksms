package ru.ivanovpv.gorets.psm.cipher;

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

import java.math.BigInteger;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 15.10.13
 * Time: 14:53
 * To change this template use File | Settings | File Templates.
 */
public abstract class EllipticCurve extends KeyExchange
{
    protected int ecGroup=NativeLib.EC_GROUP_UNDEFINED;

    protected EllipticCurve(byte[] privateKey, int ecGroup) {
        super(privateKey);
        this.ecGroup=ecGroup;
        publicKey=NativeLib.getECPublicKey(privateKey, ecGroup);
    }

    @Override
    public byte[] getSharedKey(byte[] composite) {
        byte[] sharedKey;
        sharedKey=NativeLib.getECSharedKey(privateKey, composite, ecGroup);
        return sharedKey;
    }

    /**
     * To save from Attacks on Parameter Authentication one can send hash of parameters to other party
     * @return byte[] of what???
     */
    @Override
    public byte[] getParametersHash() {
        byte[] parms=NativeLib.getECParameters(ecGroup);
        SHA1 sha1=new SHA1(BLOCK_SIZE);
        sha1.update(parms, 0, parms.length);
        sha1.generate();
        byte[] digest=sha1.getDigest();
        return digest;
    }

    @Override
    public boolean checkParametersHash(byte[] hash) {
        if(hash==null || hash.length!=BLOCK_SIZE)
            return false;
        byte[] parms=NativeLib.getECParameters(ecGroup);
        SHA1 sha1=new SHA1(BLOCK_SIZE);
        sha1.update(parms, 0, parms.length);
        sha1.generate();
        byte[] digest=sha1.getDigest();
        for(int i=0; i < digest.length; i++)
            if(digest[i]!=hash[i])
                return false;
        return true;
    }

    public int getEcGroup() {
        return ecGroup;
    }

}
