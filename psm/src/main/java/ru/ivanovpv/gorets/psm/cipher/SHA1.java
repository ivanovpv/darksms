package ru.ivanovpv.gorets.psm.cipher;
/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 377 $
 *   $LastChangedDate: 2013-10-22 14:42:56 +0400 (Вт, 22 окт 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/SHA1.java $
 */

/**
 * SHA-1 message digest implementation, translated from C source code (the
 * origin is unknown).
 */
public class SHA1
{
    /**
     * size of a SHA-1 digest in octets
     */
	public final static int DIGEST_SIZE = 20;
    private int digest_size;
    ///////////////////////////////////////////////////////////////////////////

    private int[] state;
    private long count;
    private byte[] digest_bits;
    private int[] block;
    private int block_index;

    ///////////////////////////////////////////////////////////////////////////

    public int getDigestSize()
    {
        return this.digest_size;
    }

    /**
     * Default constructor.
     *
     * @param digest_size desired size of digest
     */
    public SHA1(int digest_size)
    {
        this.state=new int[5];
        this.block=new int[16];
        this.digest_size=digest_size;
        this.digest_bits=new byte[DIGEST_SIZE];
        reset();
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Clears all data, use reset() to start again.
     */
    public void clear()
    {
        int i;

        for(i=0; i < this.state.length; i++)
        {
            this.state[i]=0;
        }
        for(i=0; i < this.digest_bits.length; i++)
        {
            this.digest_bits[i]=0;
        }
        for(i=0; i < this.block.length; i++)
        {
            this.block[i]=0;
        }
        this.count=0;
        this.block_index=0;
    }

    ///////////////////////////////////////////////////////////////////////////

    final static int rol(int value, int bits)
    {
        return ((value << bits) | (value >>> (32 - bits)));
    }

    ///////////////////////////////////////////////////////////////////////////

    final int blk0(
            int i)
    {
        return
                this.block[i]=
                        ((rol(this.block[i], 24) & 0xff00ff00) |
                                (rol(this.block[i], 8) & 0x00ff00ff));
    }

    ///////////////////////////////////////////////////////////////////////////

    final int blk(
            int i)
    {
        return (
                this.block[i & 15]=
                        rol(
                                this.block[(i + 13) & 15] ^
                                        this.block[(i + 8) & 15] ^
                                        this.block[(i + 2) & 15] ^
                                        this.block[i & 15],
                                1));
    }

    ///////////////////////////////////////////////////////////////////////////

    final void r0(int data[], int v, int w, int x, int y, int z, int i)
    {
        data[z]+=((data[w] & (data[x] ^ data[y])) ^ data[y])
                + blk0(i)
                + 0x5a827999
                + rol(data[v], 5);
        data[w]=rol(data[w], 30);
    }

    final void r1(int data[], int v, int w, int x, int y, int z, int i)
    {
        data[z]+=((data[w] & (data[x] ^ data[y])) ^ data[y])
                + blk(i)
                + 0x5a827999
                + rol(data[v], 5);
        data[w]=rol(data[w], 30);
    }

    final void r2(int data[], int v, int w, int x, int y, int z, int i)
    {
        data[z]+=(data[w] ^ data[x] ^ data[y])
                + blk(i)
                + 0x6eD9eba1
                + rol(data[v], 5);
        data[w]=rol(data[w], 30);
    }

    final void r3(int data[], int v, int w, int x, int y, int z, int i)
    {
        data[z]
                +=(((data[w] | data[x]) & data[y]) | (data[w] & data[x]))
                + blk(i)
                + 0x8f1bbcdc
                + rol(data[v], 5);
        data[w]=rol(data[w], 30);
    }

    final void r4(int data[], int v, int w, int x, int y, int z, int i)
    {
        data[z]+=(data[w] ^ data[x] ^ data[y])
                + blk(i)
                + 0xca62c1d6
                + rol(data[v], 5);
        data[w]=rol(data[w], 30);
    }

    ///////////////////////////////////////////////////////////////////////////

    void transform()
    {

        int[] dd=new int[5];
        dd[0]=this.state[0];
        dd[1]=this.state[1];
        dd[2]=this.state[2];
        dd[3]=this.state[3];
        dd[4]=this.state[4];
        r0(dd, 0, 1, 2, 3, 4, 0);
        r0(dd, 4, 0, 1, 2, 3, 1);
        r0(dd, 3, 4, 0, 1, 2, 2);
        r0(dd, 2, 3, 4, 0, 1, 3);
        r0(dd, 1, 2, 3, 4, 0, 4);
        r0(dd, 0, 1, 2, 3, 4, 5);
        r0(dd, 4, 0, 1, 2, 3, 6);
        r0(dd, 3, 4, 0, 1, 2, 7);
        r0(dd, 2, 3, 4, 0, 1, 8);
        r0(dd, 1, 2, 3, 4, 0, 9);
        r0(dd, 0, 1, 2, 3, 4, 10);
        r0(dd, 4, 0, 1, 2, 3, 11);
        r0(dd, 3, 4, 0, 1, 2, 12);
        r0(dd, 2, 3, 4, 0, 1, 13);
        r0(dd, 1, 2, 3, 4, 0, 14);
        r0(dd, 0, 1, 2, 3, 4, 15);
        r1(dd, 4, 0, 1, 2, 3, 16);
        r1(dd, 3, 4, 0, 1, 2, 17);
        r1(dd, 2, 3, 4, 0, 1, 18);
        r1(dd, 1, 2, 3, 4, 0, 19);
        r2(dd, 0, 1, 2, 3, 4, 20);
        r2(dd, 4, 0, 1, 2, 3, 21);
        r2(dd, 3, 4, 0, 1, 2, 22);
        r2(dd, 2, 3, 4, 0, 1, 23);
        r2(dd, 1, 2, 3, 4, 0, 24);
        r2(dd, 0, 1, 2, 3, 4, 25);
        r2(dd, 4, 0, 1, 2, 3, 26);
        r2(dd, 3, 4, 0, 1, 2, 27);
        r2(dd, 2, 3, 4, 0, 1, 28);
        r2(dd, 1, 2, 3, 4, 0, 29);
        r2(dd, 0, 1, 2, 3, 4, 30);
        r2(dd, 4, 0, 1, 2, 3, 31);
        r2(dd, 3, 4, 0, 1, 2, 32);
        r2(dd, 2, 3, 4, 0, 1, 33);
        r2(dd, 1, 2, 3, 4, 0, 34);
        r2(dd, 0, 1, 2, 3, 4, 35);
        r2(dd, 4, 0, 1, 2, 3, 36);
        r2(dd, 3, 4, 0, 1, 2, 37);
        r2(dd, 2, 3, 4, 0, 1, 38);
        r2(dd, 1, 2, 3, 4, 0, 39);
        r3(dd, 0, 1, 2, 3, 4, 40);
        r3(dd, 4, 0, 1, 2, 3, 41);
        r3(dd, 3, 4, 0, 1, 2, 42);
        r3(dd, 2, 3, 4, 0, 1, 43);
        r3(dd, 1, 2, 3, 4, 0, 44);
        r3(dd, 0, 1, 2, 3, 4, 45);
        r3(dd, 4, 0, 1, 2, 3, 46);
        r3(dd, 3, 4, 0, 1, 2, 47);
        r3(dd, 2, 3, 4, 0, 1, 48);
        r3(dd, 1, 2, 3, 4, 0, 49);
        r3(dd, 0, 1, 2, 3, 4, 50);
        r3(dd, 4, 0, 1, 2, 3, 51);
        r3(dd, 3, 4, 0, 1, 2, 52);
        r3(dd, 2, 3, 4, 0, 1, 53);
        r3(dd, 1, 2, 3, 4, 0, 54);
        r3(dd, 0, 1, 2, 3, 4, 55);
        r3(dd, 4, 0, 1, 2, 3, 56);
        r3(dd, 3, 4, 0, 1, 2, 57);
        r3(dd, 2, 3, 4, 0, 1, 58);
        r3(dd, 1, 2, 3, 4, 0, 59);
        r4(dd, 0, 1, 2, 3, 4, 60);
        r4(dd, 4, 0, 1, 2, 3, 61);
        r4(dd, 3, 4, 0, 1, 2, 62);
        r4(dd, 2, 3, 4, 0, 1, 63);
        r4(dd, 1, 2, 3, 4, 0, 64);
        r4(dd, 0, 1, 2, 3, 4, 65);
        r4(dd, 4, 0, 1, 2, 3, 66);
        r4(dd, 3, 4, 0, 1, 2, 67);
        r4(dd, 2, 3, 4, 0, 1, 68);
        r4(dd, 1, 2, 3, 4, 0, 69);
        r4(dd, 0, 1, 2, 3, 4, 70);
        r4(dd, 4, 0, 1, 2, 3, 71);
        r4(dd, 3, 4, 0, 1, 2, 72);
        r4(dd, 2, 3, 4, 0, 1, 73);
        r4(dd, 1, 2, 3, 4, 0, 74);
        r4(dd, 0, 1, 2, 3, 4, 75);
        r4(dd, 4, 0, 1, 2, 3, 76);
        r4(dd, 3, 4, 0, 1, 2, 77);
        r4(dd, 2, 3, 4, 0, 1, 78);
        r4(dd, 1, 2, 3, 4, 0, 79);
        this.state[0]+=dd[0];
        this.state[1]+=dd[1];
        this.state[2]+=dd[2];
        this.state[3]+=dd[3];
        this.state[4]+=dd[4];
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Initializes (or resets) the hasher for a new session.
     */
    public void reset()
    {

        this.state[0]=0x67452301;
        this.state[1]=0xefcdab89;
        this.state[2]=0x98badcfe;
        this.state[3]=0x10325476;
        this.state[4]=0xc3d2e1f0;
        this.count=0;
        this.block_index=0;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a single byte to the digest.
     *
     * @param b the byte to add
     */
    public void update(
            byte b)
    {

        int mask=(this.block_index & 3) << 3;

        this.count+=8;
        this.block[this.block_index >> 2]&=~(0xff << mask);
        this.block[this.block_index >> 2]|=(b & 0xff) << mask;
        this.block_index++;
        if(this.block_index == 64)
        {
            transform();
            this.block_index=0;
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a portion of a byte array to the digest.
     *
     * @param data the data to add
     * @param ofs  offset in data array
     * @param len  length of array
     */
    public void update(byte[] data, int ofs, int len)
    {
        for(int nEnd=ofs + len; ofs < nEnd; ofs++)
        {
            update(data[ofs]);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Finalizes the digest.
     */
    public void generate()
    {
        int i;
        byte bits[]=new byte[8];

        for(i=0; i < 8; i++)
        {
            bits[i]=(byte) ((this.count >>> (((7 - i) << 3))) & 0xff);
        }

        update((byte) 128);
        while(this.block_index != 56)
        {
            update((byte) 0);
        }

        for(i=0; i < bits.length; i++)
        {
            update(bits[i]);
        }

        for(i=0; i < 20; i++)
        {
            this.digest_bits[i]=
                    (byte) ((this.state[i >> 2] >> ((3 - (i & 3)) << 3)) & 0xff);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the digest.
     *
     * @return the digst bytes as an array if DIGEST_SIZE bytes
     */
    public byte[] getDigest() {
        byte[] result=new byte[digest_size];
        System.arraycopy(this.digest_bits, 0, result, 0, digest_size);
        return result;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Retrieves the digest into an existing buffer.
     *
     * @param buf buffer to store the digst into
     * @param offset where to write to
     * @return number of bytes written
     */
    public int getDigest(byte[] buf, int offset) {
        System.arraycopy(this.digest_bits, 0, buf, offset, digest_size);
        return digest_size;
    }

    ///////////////////////////////////////////////////////////////////////////

    // we need this table for the following method
    private final static String HEXTAB="0123456789abcdef";

    /**
     * makes a binhex string representation of the current digest
     *
     * @return the string representation
     */
    public String toString() {
        StringBuilder sbuf;
        sbuf=new StringBuilder(digest_size << 1);
        for(int i=0; i < digest_size; i++) {
            sbuf.append(HEXTAB.charAt((this.digest_bits[i] >>> 4) & 0x0f));
            sbuf.append(HEXTAB.charAt(this.digest_bits[i] & 0x0f));
        }
        return sbuf.toString();
    }

    ///////////////////////////////////////////////////////////////////////////

    // references for the selftest

    private final static String SELFTEST_MESSAGE=
            "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";

    private final static byte[] SELFTEST_DIGEST=
            {
                    (byte) 0x84, (byte) 0x98, (byte) 0x3e, (byte) 0x44, (byte) 0x1c,
                    (byte) 0x3b, (byte) 0xd2, (byte) 0x6e, (byte) 0xba, (byte) 0xae,
                    (byte) 0x4a, (byte) 0xa1, (byte) 0xf9, (byte) 0x51, (byte) 0x29,
                    (byte) 0xe5, (byte) 0xe5, (byte) 0x46, (byte) 0x70, (byte) 0xf1
            };

    /**
     * Runs an integrity test.
     *
     * @return true: selftest passed / false: selftest failed
     */
    public boolean selfTest()
    {
        int i;
        SHA1 tester;
        byte[] digest;

        tester=new SHA1(20);

        tester.update(SELFTEST_MESSAGE.getBytes(), 0,
                SELFTEST_MESSAGE.length());
        tester.generate();

        digest=tester.getDigest();

        for(i=0; i < tester.getDigestSize(); i++)
        {
            if(digest[i] != SELFTEST_DIGEST[i])
            {
                return false;
            }
        }
        return true;
    }

    /*public static String getFingerPrint(byte[] buffer) {
        SHA1 sha;
        sha=new SHA1(4);
        sha.update(buffer, 0, buffer.length);
        sha.generate();
        return sha.toDigitalString();
    } */
}
