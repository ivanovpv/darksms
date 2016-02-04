package ru.ivanovpv.gorets.psm.cipher;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 22.10.13
 * Time: 14:03
 * To change this template use File | Settings | File Templates.
 */
public class FingerPrint {
    private char type=' '; //predefined for unknown key type
    private byte[] hash;
    private int algorithm=0;    //for future enhancement, so far using only SHA-1
    private final static String DIGTAB="01234567890123456789";

    public FingerPrint(byte[] data, char type) {
        if(data==null)
            return;
        SHA1 sha;
        sha=new SHA1(4);
        sha.update(data, 0, data.length);
        sha.generate();
        hash=sha.getDigest();
        this.type=type;
    }

    public FingerPrint(byte[] data) {
        if(data==null)
            return;
        SHA1 sha;
        sha=new SHA1(4);
        sha.update(data, 0, data.length);
        sha.generate();
        hash=sha.getDigest();
    }

    @Override
    public String toString()
    {
        if(hash==null)
            return "NULL";
        StringBuilder sbuf;
        sbuf=new StringBuilder(hash.length << 1);
        for(int i=0; i < hash.length; i++) {
            sbuf.append(DIGTAB.charAt((this.hash[i] >>> 4) & 0x0f));
            sbuf.append(DIGTAB.charAt(this.hash[i] & 0x0f));
            if(i < (hash.length-1))
                sbuf.append(' ');
        }
        return sbuf.toString();
    }


    public char getType()
    {
        return type;
    }

    public void setType(char type)
    {
        this.type=type;
    }

    public byte[] getHash()
    {
        return hash;
    }

    public int getAlgorithm()
    {
        return algorithm;
    }

    public void setAlgorithm(int algorithm)
    {
        this.algorithm=algorithm;
    }
}
