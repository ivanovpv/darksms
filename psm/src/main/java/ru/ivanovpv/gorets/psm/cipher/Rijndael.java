package ru.ivanovpv.gorets.psm.cipher;

//import java.io.PrintWriter;

import java.security.InvalidKeyException;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/Rijndael.java $
 */

/**
 * <p>Rijndael --pronounced Reindaal-- is the AES. It is a variable block-size
 * (128-, 192- and 256-bit), variable key-size (128-, 192- and 256-bit)
 * symmetric key block cipher.</p>
 * <p/>
 * <p>References:</p>
 * <p/>
 * <ol>
 * <li><a href="http://www.esat.kuleuven.ac.be/~rijmen/rijndael/">The
 * Rijndael Block Cipher - AES Proposal</a>.<br>
 * <a href="mailto:vincent.rijmen@esat.kuleuven.ac.be">Vincent Rijmen</a> and
 * <a href="mailto:daemen.j@protonworld.com">Joan Daemen</a>.</li>
 * </ol>
 *
 * @version $Revision: 246 $
 */
public final class Rijndael extends Cipher
{
    private Object sessionKey;
    private int blockSize;

    // Debugging methods and variables
    // -------------------------------------------------------------------------

    //   private static final String NAME = "rijndael";
    private static final boolean DEBUG=false;
    private static final int debuglevel=9;
//   private static final PrintWriter err = new PrintWriter(System.out, true);
//   private static void debug(String s) {
//      err.println(">>> "+NAME+": "+s);
//   }

    // Constants and variables
    // -------------------------------------------------------------------------

    private static final int DEFAULT_BLOCK_SIZE=16; // in bytes
    private static final int DEFAULT_KEY_SIZE=16; // in bytes
    private static final String SS=
            "\u637C\u777B\uF26B\u6FC5\u3001\u672B\uFED7\uAB76" +
                    "\uCA82\uC97D\uFA59\u47F0\uADD4\uA2AF\u9CA4\u72C0" +
                    "\uB7FD\u9326\u363F\uF7CC\u34A5\uE5F1\u71D8\u3115" +
                    "\u04C7\u23C3\u1896\u059A\u0712\u80E2\uEB27\uB275" +
                    "\u0983\u2C1A\u1B6E\u5AA0\u523B\uD6B3\u29E3\u2F84" +
                    "\u53D1\u00ED\u20FC\uB15B\u6ACB\uBE39\u4A4C\u58CF" +
                    "\uD0EF\uAAFB\u434D\u3385\u45F9\u027F\u503C\u9FA8" +
                    "\u51A3\u408F\u929D\u38F5\uBCB6\uDA21\u10FF\uF3D2" +
                    "\uCD0C\u13EC\u5F97\u4417\uC4A7\u7E3D\u645D\u1973" +
                    "\u6081\u4FDC\u222A\u9088\u46EE\uB814\uDE5E\u0BDB" +
                    "\uE032\u3A0A\u4906\u245C\uC2D3\uAC62\u9195\uE479" +
                    "\uE7C8\u376D\u8DD5\u4EA9\u6C56\uF4EA\u657A\uAE08" +
                    "\uBA78\u252E\u1CA6\uB4C6\uE8DD\u741F\u4BBD\u8B8A" +
                    "\u703E\uB566\u4803\uF60E\u6135\u57B9\u86C1\u1D9E" +
                    "\uE1F8\u9811\u69D9\u8E94\u9B1E\u87E9\uCE55\u28DF" +
                    "\u8CA1\u890D\uBFE6\u4268\u4199\u2D0F\uB054\uBB16";

    private static final byte[] S=new byte[256];
    private static final byte[] Si=new byte[256];
    private static final int[] T1=new int[256];
    private static final int[] T2=new int[256];
    private static final int[] T3=new int[256];
    private static final int[] T4=new int[256];
    private static final int[] T5=new int[256];
    private static final int[] T6=new int[256];
    private static final int[] T7=new int[256];
    private static final int[] T8=new int[256];
    private static final int[] U1=new int[256];
    private static final int[] U2=new int[256];
    private static final int[] U3=new int[256];
    private static final int[] U4=new int[256];
    private static final byte[] rcon=new byte[30];

    private static final int[][][] shifts=new int[][][]
            {
                    {
                            {0, 0}, {1, 3}, {2, 2}, {3, 1}
                    },
                    {
                            {0, 0}, {1, 5}, {2, 4}, {3, 3}
                    },
                    {
                            {0, 0}, {1, 7}, {3, 5}, {4, 4}
                    }
            };

    /**
     * KAT vector (from ecb_vk):
     * I=96
     * KEY=0000000000000000000000010000000000000000000000000000000000000000
     * CT=E44429474D6FC3084EB2A6B8B46AF754
     */
    private static final byte[] KAT_KEY=
            ByteUtils.stringToByteArray("0000000000000000000000010000000000000000000000000000000000000000");
    private static final byte[] KAT_CT=
            ByteUtils.stringToByteArray("E44429474D6FC3084EB2A6B8B46AF754");

    /**
     * caches the result of the correctness test, once executed.
     */
    private static Boolean valid;

    // Static code - to intialise lookup tables --------------------------------

    static
    {
        long time=System.currentTimeMillis();

        int ROOT=0x11B;
        int i, j=0;

        // S-box, inverse S-box, T-boxes, U-boxes
        int s, s2, s3, i2, i4, i8, i9, ib, id, ie, t;
        char c;
        for(i=0; i < 256; i++)
        {
            c=SS.charAt(i >>> 1);
            S[i]=(byte) (((i & 1) == 0) ? c >>> 8 : c & 0xFF);
            s=S[i] & 0xFF;
            Si[s]=(byte) i;
            s2=s << 1;
            if(s2 >= 0x100)
            {
                s2^=ROOT;
            }
            s3=s2 ^ s;
            i2=i << 1;
            if(i2 >= 0x100)
            {
                i2^=ROOT;
            }
            i4=i2 << 1;
            if(i4 >= 0x100)
            {
                i4^=ROOT;
            }
            i8=i4 << 1;
            if(i8 >= 0x100)
            {
                i8^=ROOT;
            }
            i9=i8 ^ i;
            ib=i9 ^ i2;
            id=i9 ^ i4;
            ie=i8 ^ i4 ^ i2;

            T1[i]=t=(s2 << 24) | (s << 16) | (s << 8) | s3;
            T2[i]=(t >>> 8) | (t << 24);
            T3[i]=(t >>> 16) | (t << 16);
            T4[i]=(t >>> 24) | (t << 8);

            T5[s]=U1[i]=t=(ie << 24) | (i9 << 16) | (id << 8) | ib;
            T6[s]=U2[i]=(t >>> 8) | (t << 24);
            T7[s]=U3[i]=(t >>> 16) | (t << 16);
            T8[s]=U4[i]=(t >>> 24) | (t << 8);
        }
        //
        // round constants
        //
        int r=1;
        rcon[0]=1;
        for(i=1; i < 30; i++)
        {
            r<<=1;
            if(r >= 0x100)
            {
                r^=ROOT;
            }
            rcon[i]=(byte) r;
        }

        time=System.currentTimeMillis() - time;

        if(DEBUG && debuglevel > 8)
        {
/*            System.out.println("==========");
            System.out.println();
            System.out.println("Static Data");
            System.out.println();
            System.out.println("S[]:");
            for(i=0; i < 16; i++)
            {
                for(j=0; j < 16; j++)
                {
                    System.out.print(ByteUtils.byteToHex(S[i * 16 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("Si[]:");
            for(i=0; i < 16; i++)
            {
                for(j=0; j < 16; j++)
                {
                    System.out.print(ByteUtils.byteToHex(Si[i * 16 + j]) + ", ");
                }
                System.out.println();
            }

            System.out.println();
            System.out.println("T1[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T1[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T2[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T2[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T3[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T3[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T4[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T4[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T5[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T5[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T6[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T6[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T7[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T7[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("T8[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(T8[i * 4 + j]) + ", ");
                }
                System.out.println();
            }

            System.out.println();
            System.out.println("U1[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(U1[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("U2[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(U2[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("U3[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.print(ByteUtils.byteToHex(U3[i * 4 + j]) + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println("U4[]:");
            for(i=0; i < 64; i++)
            {
                for(j=0; j < 4; j++)
                {
                    System.out.println(ByteUtils.byteToHex(U4[i * 4 + j]) + ", ");
                }
                System.out.println();
            }

            System.out.println();
            System.out.println("rcon[]:");
            for(i=0; i < 5; i++)
            {
                for(j=0; j < 6; j++)
                {
                    System.out.print(ByteUtils.byteToHex(rcon[i * 6 + j]) + ", ");
                }
                System.out.println();
            }     */

            System.out.println();
            System.out.println("Total initialization time: " + time + " ms.");
            System.out.println();
        }
    }

    // Constructor(s)
    // -------------------------------------------------------------------------

    /**
     * Trivial 0-arguments constructor.
     */
    public Rijndael(byte[] key, int blockSize)
    {
        //super(Registry.RIJNDAEL_CIPHER, DEFAULT_BLOCK_SIZE, DEFAULT_KEY_SIZE);
        this.blockSize=blockSize;
        this.sessionKey=this.makeKey(key, this.blockSize);
    }

    // Class methods
    // -------------------------------------------------------------------------

    /**
     * <p>Returns the number of rounds for a given Rijndael's key and block
     * sizes.</p>
     *
     * @param ks the size of the user key material in bytes.
     * @param bs the desired block size in bytes.
     * @return the number of rounds for a given Rijndael's key and block sizes.
     */
    public static int getRounds(int ks, int bs)
    {
        switch(ks)
        {
            case 16:
                return bs == 16 ? 10 : (bs == 24 ? 12 : 14);
            case 24:
                return bs != 32 ? 12 : 14;
            default: // 32 bytes = 256 bits
                return 14;
        }
    }

    private static void rijndaelEncrypt(byte[] in, int inOffset, byte[] out, int outOffset, Object sessionKey, int bs)
    {
        Object[] sKey=(Object[]) sessionKey; // extract encryption round keys
        int[][] Ke=(int[][]) sKey[0];

        int BC=bs / 4;
        int ROUNDS=Ke.length - 1;
        int SC=BC == 4 ? 0 : (BC == 6 ? 1 : 2);
        int s1=shifts[SC][1][0];
        int s2=shifts[SC][2][0];
        int s3=shifts[SC][3][0];
        int[] a=new int[BC];
        int[] t=new int[BC]; // temporary work array
        int i, tt;

        for(i=0; i < BC; i++)
        { // plaintext to ints + key
            t[i]=(in[inOffset++] << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) << 8 |
                    (in[inOffset++] & 0xFF)) ^ Ke[0][i];
        }

        for(int r=1; r < ROUNDS; r++)
        { // apply round transforms
            for(i=0; i < BC; i++)
            {
                a[i]=(T1[(t[i] >>> 24)] ^
                        T2[(t[(i + s1) % BC] >>> 16) & 0xFF] ^
                        T3[(t[(i + s2) % BC] >>> 8) & 0xFF] ^
                        T4[t[(i + s3) % BC] & 0xFF]) ^ Ke[r][i];
            }

            System.arraycopy(a, 0, t, 0, BC);

/*            if(DEBUG && debuglevel > 6)
            {
                System.out.println("CT" + r + "=" + ByteUtils.byteToHex(t));
            }*/
        }

        for(i=0; i < BC; i++)
        { // last round is special
            tt=Ke[ROUNDS][i];
            out[outOffset++]=(byte) (S[(t[i] >>> 24)] ^ (tt >>> 24));
            out[outOffset++]=(byte) (S[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
            out[outOffset++]=(byte) (S[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
            out[outOffset++]=(byte) (S[t[(i + s3) % BC] & 0xFF] ^ tt);
        }

/*        if(DEBUG && debuglevel > 6)
        {
            System.out.println("CT=" + Util.toString(out, outOffset - bs + 1, bs));
            System.out.println();
        }*/
    }

    private static void rijndaelDecrypt(byte[] in, int inOffset, byte[] out, int outOffset,
                                        Object sessionKey, int bs)
    {
        Object[] sKey=(Object[]) sessionKey; // extract decryption round keys
        int[][] Kd=(int[][]) sKey[1];

        int BC=bs / 4;
        int ROUNDS=Kd.length - 1;
        int SC=BC == 4 ? 0 : (BC == 6 ? 1 : 2);
        int s1=shifts[SC][1][1];
        int s2=shifts[SC][2][1];
        int s3=shifts[SC][3][1];
        int[] a=new int[BC];
        int[] t=new int[BC]; // temporary work array
        int i, tt;

        for(i=0; i < BC; i++)
        { // ciphertext to ints + key
            t[i]=(in[inOffset++] << 24 |
                    (in[inOffset++] & 0xFF) << 16 |
                    (in[inOffset++] & 0xFF) << 8 |
                    (in[inOffset++] & 0xFF)) ^ Kd[0][i];
        }

        for(int r=1; r < ROUNDS; r++)
        { // apply round transforms
            for(i=0; i < BC; i++)
            {
                a[i]=(T5[(t[i] >>> 24)] ^
                        T6[(t[(i + s1) % BC] >>> 16) & 0xFF] ^
                        T7[(t[(i + s2) % BC] >>> 8) & 0xFF] ^
                        T8[t[(i + s3) % BC] & 0xFF]) ^ Kd[r][i];
            }

            System.arraycopy(a, 0, t, 0, BC);

            /*       if(DEBUG && debuglevel > 6)
            {
                System.out.println("PT" + r + "=" + Util.toString(t));
            }*/
        }

        for(i=0; i < BC; i++)
        { // last round is special
            tt=Kd[ROUNDS][i];
            out[outOffset++]=(byte) (Si[(t[i] >>> 24)] ^ (tt >>> 24));
            out[outOffset++]=(byte) (Si[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
            out[outOffset++]=(byte) (Si[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
            out[outOffset++]=(byte) (Si[t[(i + s3) % BC] & 0xFF] ^ tt);
        }

        /*if(DEBUG && debuglevel > 6)
        {
            System.out.println("PT=" + Util.toString(out, outOffset - bs + 1, bs));
            System.out.println();
        } */
    }

    private static void aesEncrypt(byte[] in, int i, byte[] out, int j, Object key)
    {
        int[][] Ke=(int[][]) ((Object[]) key)[0]; // extract encryption round keys
        int ROUNDS=Ke.length - 1;
        int[] Ker=Ke[0];

        // plaintext to ints + key
        int t0=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Ker[0];
        int t1=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Ker[1];
        int t2=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Ker[2];
        int t3=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Ker[3];

        int a0, a1, a2, a3;
        for(int r=1; r < ROUNDS; r++)
        { // apply round transforms
            Ker=Ke[r];
            a0=(T1[(t0 >>> 24)] ^
                    T2[(t1 >>> 16) & 0xFF] ^
                    T3[(t2 >>> 8) & 0xFF] ^
                    T4[t3 & 0xFF]) ^ Ker[0];
            a1=(T1[(t1 >>> 24)] ^
                    T2[(t2 >>> 16) & 0xFF] ^
                    T3[(t3 >>> 8) & 0xFF] ^
                    T4[t0 & 0xFF]) ^ Ker[1];
            a2=(T1[(t2 >>> 24)] ^
                    T2[(t3 >>> 16) & 0xFF] ^
                    T3[(t0 >>> 8) & 0xFF] ^
                    T4[t1 & 0xFF]) ^ Ker[2];
            a3=(T1[(t3 >>> 24)] ^
                    T2[(t0 >>> 16) & 0xFF] ^
                    T3[(t1 >>> 8) & 0xFF] ^
                    T4[t2 & 0xFF]) ^ Ker[3];
            t0=a0;
            t1=a1;
            t2=a2;
            t3=a3;

/*            if(DEBUG && debuglevel > 6)
            {
                System.out.println("CT" + r + "=" + Util.toString(t0) + Util.toString(t1)
                        + Util.toString(t2) + Util.toString(t3));
            }*/
        }

        // last round is special
        Ker=Ke[ROUNDS];
        int tt=Ker[0];
        out[j++]=(byte) (S[(t0 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (S[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (S[(t2 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (S[t3 & 0xFF] ^ tt);
        tt=Ker[1];
        out[j++]=(byte) (S[(t1 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (S[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (S[(t3 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (S[t0 & 0xFF] ^ tt);
        tt=Ker[2];
        out[j++]=(byte) (S[(t2 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (S[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (S[(t0 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (S[t1 & 0xFF] ^ tt);
        tt=Ker[3];
        out[j++]=(byte) (S[(t3 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (S[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (S[(t1 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (S[t2 & 0xFF] ^ tt);

        /*if(DEBUG && debuglevel > 6)
        {
            System.out.println("CT=" + Util.toString(out, j - 15, 16));
            System.out.println();
        } */
    }

    private static void aesDecrypt(byte[] in, int i, byte[] out, int j, Object key)
    {
        int[][] Kd=(int[][]) ((Object[]) key)[1]; // extract decryption round keys
        int ROUNDS=Kd.length - 1;
        int[] Kdr=Kd[0];

        // ciphertext to ints + key
        int t0=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Kdr[0];
        int t1=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Kdr[1];
        int t2=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Kdr[2];
        int t3=(in[i++] << 24 |
                (in[i++] & 0xFF) << 16 |
                (in[i++] & 0xFF) << 8 |
                (in[i++] & 0xFF)) ^ Kdr[3];

        int a0, a1, a2, a3;
        for(int r=1; r < ROUNDS; r++)
        { // apply round transforms
            Kdr=Kd[r];
            a0=(T5[(t0 >>> 24)] ^
                    T6[(t3 >>> 16) & 0xFF] ^
                    T7[(t2 >>> 8) & 0xFF] ^
                    T8[t1 & 0xFF]) ^ Kdr[0];
            a1=(T5[(t1 >>> 24)] ^
                    T6[(t0 >>> 16) & 0xFF] ^
                    T7[(t3 >>> 8) & 0xFF] ^
                    T8[t2 & 0xFF]) ^ Kdr[1];
            a2=(T5[(t2 >>> 24)] ^
                    T6[(t1 >>> 16) & 0xFF] ^
                    T7[(t0 >>> 8) & 0xFF] ^
                    T8[t3 & 0xFF]) ^ Kdr[2];
            a3=(T5[(t3 >>> 24)] ^
                    T6[(t2 >>> 16) & 0xFF] ^
                    T7[(t1 >>> 8) & 0xFF] ^
                    T8[t0 & 0xFF]) ^ Kdr[3];
            t0=a0;
            t1=a1;
            t2=a2;
            t3=a3;

/*            if(DEBUG && debuglevel > 6)
            {
                System.out.println("PT" + r + "=" + Util.toString(t0) + Util.toString(t1)
                        + Util.toString(t2) + Util.toString(t3));
            }*/
        }

        // last round is special
        Kdr=Kd[ROUNDS];
        int tt=Kdr[0];
        out[j++]=(byte) (Si[(t0 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (Si[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (Si[(t2 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (Si[t1 & 0xFF] ^ tt);
        tt=Kdr[1];
        out[j++]=(byte) (Si[(t1 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (Si[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (Si[(t3 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (Si[t2 & 0xFF] ^ tt);
        tt=Kdr[2];
        out[j++]=(byte) (Si[(t2 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (Si[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (Si[(t0 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (Si[t3 & 0xFF] ^ tt);
        tt=Kdr[3];
        out[j++]=(byte) (Si[(t3 >>> 24)] ^ (tt >>> 24));
        out[j++]=(byte) (Si[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[j++]=(byte) (Si[(t1 >>> 8) & 0xFF] ^ (tt >>> 8));
        out[j++]=(byte) (Si[t0 & 0xFF] ^ tt);

/*        if(DEBUG && debuglevel > 6)
        {
            System.out.println("PT=" + Util.toString(out, j - 15, 16));
            System.out.println();
        }*/
    }

    // Instance methods
    // -------------------------------------------------------------------------

    // java.lang.Cloneable interface implementation ----------------------------

/*    public Object clone()
    {
        Rijndael result=new Rijndael();
    //    result.currentBlockSize=this.currentBlockSize;

        return result;
    }*/

    // IBlockCipherSpi interface implementation --------------------------------

/*    public Iterator blockSizes()
    {
        ArrayList al=new ArrayList();
        al.add(new Integer(128 / 8));
        al.add(new Integer(192 / 8));
        al.add(new Integer(256 / 8));

        return Collections.unmodifiableList(al).iterator();
    }

    public Iterator keySizes()
    {
        ArrayList al=new ArrayList();
        al.add(new Integer(128 / 8));
        al.add(new Integer(192 / 8));
        al.add(new Integer(256 / 8));

        return Collections.unmodifiableList(al).iterator();
    }*/

    /**
     * Expands a user-supplied key material into a session key for a designated
     * <i>block size</i>.
     *
     * @param k  the 128/192/256-bit user-key to use.
     * @param bs the block size in bytes of this Rijndael.
     * @return an Object encapsulating the session key.
     * @throws IllegalArgumentException if the block size is not 16, 24 or 32.
     * @throws InvalidKeyException      if the key data is invalid.
     */
    public Object makeKey(byte[] k, int bs)
    {
        if(k == null)
        {
            throw new IllegalArgumentException("Empty key");
        }
        if(!(k.length == 16 || k.length == 24 || k.length == 32))
        {
            byte[] newKey=new byte[DEFAULT_KEY_SIZE];
            System.arraycopy(k, 0, newKey, 0, DEFAULT_KEY_SIZE);
            k=new byte[DEFAULT_KEY_SIZE];
            System.arraycopy(newKey, 0, k, 0, DEFAULT_KEY_SIZE);
        }
        if(!(bs == 16 || bs == 24 || bs == 32))
        {
            bs=DEFAULT_BLOCK_SIZE;
        }

        int ROUNDS=getRounds(k.length, bs);
        int BC=bs / 4;
        int[][] Ke=new int[ROUNDS + 1][BC]; // encryption round keys
        int[][] Kd=new int[ROUNDS + 1][BC]; // decryption round keys
        int ROUND_KEY_COUNT=(ROUNDS + 1) * BC;
        int KC=k.length / 4;
        int[] tk=new int[KC];
        int i, j;

        // copy user material bytes into temporary ints
        for(i=0, j=0; i < KC; )
        {
            tk[i++]=k[j++] << 24 |
                    (k[j++] & 0xFF) << 16 |
                    (k[j++] & 0xFF) << 8 |
                    (k[j++] & 0xFF);
        }
        // copy values into round key arrays
        int t=0;
        for(j=0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++)
        {
            Ke[t / BC][t % BC]=tk[j];
            Kd[ROUNDS - (t / BC)][t % BC]=tk[j];
        }
        int tt, rconpointer=0;
        while(t < ROUND_KEY_COUNT)
        {
            // extrapolate using phi (the round key evolution function)
            tt=tk[KC - 1];
            tk[0]^=(S[(tt >>> 16) & 0xFF] & 0xFF) << 24 ^
                    (S[(tt >>> 8) & 0xFF] & 0xFF) << 16 ^
                    (S[tt & 0xFF] & 0xFF) << 8 ^
                    (S[(tt >>> 24)] & 0xFF) ^
                    rcon[rconpointer++] << 24;
            if(KC != 8)
            {
                for(i=1, j=0; i < KC; )
                {
                    tk[i++]^=tk[j++];
                }
            }
            else
            {
                for(i=1, j=0; i < KC / 2; )
                {
                    tk[i++]^=tk[j++];
                }
                tt=tk[KC / 2 - 1];
                tk[KC / 2]^=(S[tt & 0xFF] & 0xFF) ^
                        (S[(tt >>> 8) & 0xFF] & 0xFF) << 8 ^
                        (S[(tt >>> 16) & 0xFF] & 0xFF) << 16 ^
                        S[(tt >>> 24) & 0xFF] << 24;
                for(j=KC / 2, i=j + 1; i < KC; )
                {
                    tk[i++]^=tk[j++];
                }
            }
            // copy values into round key arrays
            for(j=0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++)
            {
                Ke[t / BC][t % BC]=tk[j];
                Kd[ROUNDS - (t / BC)][t % BC]=tk[j];
            }
        }
        for(int r=1; r < ROUNDS; r++)
        { // inverse MixColumn where needed
            for(j=0; j < BC; j++)
            {
                tt=Kd[r][j];
                Kd[r][j]=U1[(tt >>> 24)] ^
                        U2[(tt >>> 16) & 0xFF] ^
                        U3[(tt >>> 8) & 0xFF] ^
                        U4[tt & 0xFF];
            }
        }

        return new Object[]{Ke, Kd};
    }

    public void encrypt(byte[] in, int i, byte[] out, int j)
    {
        if(!(blockSize == 16 || blockSize == 24 || blockSize == 32))
            throw new IllegalArgumentException();

        if(blockSize == DEFAULT_BLOCK_SIZE)
            aesEncrypt(in, i, out, j, sessionKey);
        else
            rijndaelEncrypt(in, i, out, j, sessionKey, blockSize);
    }

    public void decrypt(byte[] in, int i, byte[] out, int j)
    {
        if(!(blockSize == 16 || blockSize == 24 || blockSize == 32))
            throw new IllegalArgumentException();

        if(blockSize == DEFAULT_BLOCK_SIZE)
            aesDecrypt(in, i, out, j, sessionKey);
        else
            rijndaelDecrypt(in, i, out, j, sessionKey, blockSize);
    }

    public static void main(String[] args)
    {
        try
        {
            byte[] key={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            Rijndael r=new Rijndael(key, 16);
            byte[] in={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            System.out.println("in=" + ByteUtils.bytesToHex(in));
            byte[] out=new byte[in.length];
            r.encrypt(in, 0, out, 0);
            System.out.println("encrypted=" + ByteUtils.bytesToHex(out));
            r.decrypt(out, 0, in, 0);
            System.out.println("decrypted=" + ByteUtils.bytesToHex(in));
        }
        catch(Exception e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    protected int getBlockSize()
    {
        return this.blockSize;
    }

    /**
     * Rounds buffer in accordance with Rijndael padding policy (variable)
     *
     * @param buffer byte array to be rounded/padded
     * @return rounded byte array (always new buffer)
     */
    @Override
    protected final byte[] roundBuffer(byte[] buffer)
    {
        int length=buffer.length;
        int size=blockSize * (length / blockSize) + blockSize * ((length % blockSize == 0) ? 0 : 1); //rounded buffer size including padding bytes
        byte[] newbuf=new byte[size];
        System.arraycopy(buffer, 0, newbuf, 0, length);
        for(int i=length; i < newbuf.length; i++)
            newbuf[i]=(byte )0xff;
        return newbuf;
    }

    @Override
    public byte[] encryptBlock(byte[] block)
    {
        byte[] out=new byte[block.length];
        encrypt(block, 0, out, 0);
        return out;
    }

    @Override
    public byte[] decryptBlock(byte[] block)
    {
        byte[] out=new byte[block.length];
        decrypt(block, 0, out, 0);
        return out;
    }

    @Override
    public void clean()
    {
        sessionKey=null;
        blockSize=0;
    }

    @Override
    public char getType() {
        return Cipher.CIPHER_RIJNDAEL;
    }
}
