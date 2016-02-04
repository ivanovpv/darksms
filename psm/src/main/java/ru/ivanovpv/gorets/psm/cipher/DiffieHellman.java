package ru.ivanovpv.gorets.psm.cipher;

import java.math.BigInteger;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 364 $
 *   $LastChangedDate: 2013-10-15 15:22:33 +0400 (Вт, 15 окт 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/DiffieHellman.java $
 */

/**
 * Implements the underlying Diffie-Hellman cryptography.
 * <p/>
 * p - prime (modulus), g - base (generator)
 */
public final class DiffieHellman extends KeyExchange
{
    private static final boolean TRIVIAL_GRADE=false;
    private BigInteger modulus;   // i.e prime
    private BigInteger generator; // i.e. base
    /**
     * The default modulus defined in the specification.
     */
    public static BigInteger PRIME_DEFAULT;
    public static BigInteger BASE_DEFAULT;

    static
    {
        if(!TRIVIAL_GRADE)
        {
            PRIME_DEFAULT=new BigInteger("203956878356401977405765866929034577280193993314348263094772646453283062722701277632936616063144088173312372882677123879538709400158306567338328279154499698366071906766440037074217117805690872792848149112022286332144876183376326512083574821647933992961249917319836219304274280243803104015000563790123");
            BASE_DEFAULT=new BigInteger("531872289054204184185084734375133399408303613982130856645299464930952178606045848877129147820387996428175564228204785846141207532462936339834139412401975338705794646595487324365194792822189473092273993580587964571659678084484152603881094176995594813302284232006001752128168901293560051833646881436219");
        }
        else
        {
            PRIME_DEFAULT=new BigInteger("7791660392540923547853351598904723281714209151101708571406813134683121215344234705737758395685107294388204621531301041869123572501259417723973411730621153");
            //PRIME_DEFAULT=new BigInteger("203734385132387");
            BASE_DEFAULT=new BigInteger("268789439815151");
        }
    }

/*    private DiffieHellman(BigInteger modulus, BigInteger generator, BigInteger privateKey)
    {
        this.modulus=modulus;
        this.generator=generator;
        this.privateKey=privateKey;
        publicKey=generator.modPow(privateKey, modulus);
    }*/

    public DiffieHellman(byte[] privateKey)
    {
        super(privateKey);
        this.modulus=PRIME_DEFAULT;
        this.generator=BASE_DEFAULT;
        publicKey=generator.modPow(new BigInteger(privateKey), modulus).toByteArray();
    }


    public void getBits(byte[] composite)
    {
        System.out.println("Prime bits are: " + PRIME_DEFAULT.toByteArray().length*8);
        System.out.println("Base bits are: " + BASE_DEFAULT.toByteArray().length*8);
        System.out.println("Private key bits are: " + privateKey.length*8);
        System.out.println("Public key bits are: " + privateKey.length*8);
        System.out.println("Shared key bits are: " + getSharedKey(composite).length*8);
    }

    /**
     * Returns a Diffie-Hellman instance using default modulus and
     * generator. Note that each call to this method will return an instance
     * with random private key.
     * @return a DiffieHellman instance using modulus
     * ${#DiffieHellman.DEFAULT_MODULUS}, and generator
     * ${#DiffieHellman.DEFAULT_GENERATOR}.
    public static DiffieHellman getDefault()
    {
    BigInteger p=DiffieHellman.PRIME_DEFAULT;
    BigInteger g=DiffieHellman.BASE_DEFAULT;
    return new DiffieHellman(p, g);
    }
     */
    /*   /**
     * Creates a DiffieHellman instance.
     * @param mod the modulus to use. If null, use DEFAULT_MODULUS
     * @param gen the generator to use. If null, use DEFAULT_GENERATOR
   public DiffieHellman(BigInteger mod, BigInteger gen)
    {
        modulus=(mod != null ? mod : DiffieHellman.PRIME_DEFAULT);
        generator=(gen != null ? gen : DiffieHellman.BASE_DEFAULT);
        int bits=modulus.bitLength();
        BigInteger max=modulus.subtract(BigInteger.ONE);
        while(true)
        {
            BigInteger pkey=new BigInteger(bits, random);
            if(pkey.compareTo(max) >= 0)
                continue; //too large
            else if(pkey.compareTo(BigInteger.ONE) <= 0)
                continue;  //too small
            privateKey=pkey;
            publicKey=generator.modPow(privateKey, modulus);
            break;
        }
    }*/

    /**
     * Returns the shared secret.
     *
     * @param composite the composite number (public key) with which this
     *                  instance shares a secret.
     * @return the shared secret.
     */
    @Override
    public byte[] getSharedKey(byte[] composite) {
        BigInteger big=new BigInteger(composite);
        return big.modPow(new BigInteger(privateKey), modulus).toByteArray();
    }

    /**
     * To save from Attacks on Parameter Authentication one can send hash of parameters to other party
     * @return byte[] array of prime and base concatenation calculated as SHA1 function
     */
    @Override
    public byte[] getParametersHash()
    {
        String s=PRIME_DEFAULT.toString()+BASE_DEFAULT.toString();
        SHA1 sha1=new SHA1(BLOCK_SIZE);
        byte[] buf=ByteUtils.stringToByteArray(s);
        sha1.update(buf, 0, buf.length);
        sha1.generate();
        byte[] digest=sha1.getDigest();
        return digest;
    }

    @Override
    public boolean checkParametersHash(byte[] hash)
    {
        if(hash==null || hash.length!=BLOCK_SIZE)
            return false;
        String s=PRIME_DEFAULT.toString()+BASE_DEFAULT.toString();
        SHA1 sha1=new SHA1(BLOCK_SIZE);
        byte[] buf=ByteUtils.stringToByteArray(s);
        sha1.update(buf, 0, buf.length);
        sha1.generate();
        byte[] digest=sha1.getDigest();
        for(int i=0; i < digest.length; i++)
            if(digest[i]!=hash[i])
                return false;
        return true;
    }

    @Override
    public char getType()
    {
        return KeyExchange.KEY_EXCHANGE_DIFFIE_HELLMAN;
    }
}


