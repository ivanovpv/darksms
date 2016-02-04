package ru.ivanovpv.gorets.psm.cipher;
/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/Anubis.java $
 */

/**
 * <b>The Anubis block cipher.</b>
 *
 * <P>
 * <b>References</b>
 *
 * <P>
 * The Anubis algorithm was developed by
 * <a href="mailto:pbarreto@scopus.com.br">Paulo S. L. M. Barreto</a> and
 * <a href="mailto:vincent.rijmen@esat.kuleuven.ac.be">Vincent Rijmen</a>.
 *
 * See
 *		P.S.L.M. Barreto, V. Rijmen,
 *		``The Anubis block cipher,''
 *		NESSIE submission, 2000.
 *
 * @author  Paulo S.L.M. Barreto
 * @author	Vincent Rijmen.
 *
 * @version 1.0 (2000.09.29)
 *
 * =============================================================================
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

public final class Anubis
{
    private static final String sbox =
            "\ua7d3\ue671\ud0ac\u4d79\u3ac9\u91fc\u1e47\u54bd" +
            "\u8ca5\u7afb\u63b8\uddd4\ue5b3\uc5be\ua988\u0ca2" +
            "\u39df\u29da\u2ba8\ucb4c\u4b22\uaa24\u4170\ua6f9" +
            "\u5ae2\ub036\u7de4\u33ff\u6020\u088b\u5eab\u7f78" +
            "\u7c2c\u57d2\udc6d\u7e0d\u5394\uc328\u2706\u5fad" +
            "\u675c\u5548\u0e52\uea42\u5b5d\u3058\u5159\u3c4e" +
            "\u388a\u7214\ue7c6\ude50\u8e92\ud177\u9345\u9ace" +
            "\u2d03\u62b6\ub9bf\u966b\u3f07\u12ae\u4034\u463e" +
            "\udbcf\ueccc\uc1a1\uc0d6\u1df4\u613b\u10d8\u68a0" +
            "\ub10a\u696c\u49fa\u76c4\u9e9b\u6e99\uc2b7\u98bc" +
            "\u8f85\u1fb4\uf811\u2e00\u251c\u2a3d\u054f\u7bb2" +
            "\u3290\uaf19\ua3f7\u739d\u1574\ueeca\u9f0f\u1b75" +
            "\u8684\u9c4a\u971a\u65f6\ued09\ubb26\u83eb\u6f81" +
            "\u046a\u4301\u17e1\u87f5\u8de3\u2380\u4416\u6621" +
            "\ufed5\u31d9\u3518\u0264\uf2f1\u56cd\u82c8\ubaf0" +
            "\uefe9\ue8fd\u89d7\uc7b5\ua42f\u9513\u0bf3\ue037";

    private static int[] T0 = new int[256];
    private static int[] T1 = new int[256];
    private static int[] T2 = new int[256];
    private static int[] T3 = new int[256];
    private static int[] T4 = new int[256];
    private static int[] T5 = new int[256];

    ////////////////////////////////////////////////////////////////////////////

    public Anubis()
    {}

    static
    {
        for (int x = 0; x < 256; x++)
        {
            char c = sbox.charAt(x/2);
            int s1 = ((x & 1) == 0) ? c >>> 8 : c & 0xff;
            int s2 = s1 << 1;
            if (s2 >= 0x100)
            {
                s2 ^= 0x11d; // reduce s2 (mod ROOT)
            }
            int s4 = s2 << 1;
            if (s4 >= 0x100)
            {
                s4 ^= 0x11d; // reduce s4 (mod ROOT)
            }
            int s6 = s4 ^ s2;
            int s8 = s4 << 1;
            if (s8 >= 0x100)
            {
                s8 ^= 0x11d; // reduce s8 (mod ROOT)
            }
            int x2 = x  << 1;
            if (x2 >= 0x100)
            {
                x2 ^= 0x11d; // reduce x2 (mod ROOT)
            }
            int x4 = x2 << 1;
            if (x4 >= 0x100)
            {
                x4 ^= 0x11d; // reduce x4 (mod ROOT)
            }
            int x6 = x2 ^ x4;
            int x8 = x4 << 1;
            if (x8 >= 0x100)
            {
                x8 ^= 0x11d; // reduce x8 (mod ROOT)
            }

            T0[x] = (s1 << 24) | (s2 << 16) | (s4 << 8) | s6; // [ S[x], 2S[x], 4S[x], 6S[x]]
            T1[x] = (s2 << 24) | (s1 << 16) | (s6 << 8) | s4; // [2S[x],  S[x], 6S[x], 4S[x]]
            T2[x] = (s4 << 24) | (s6 << 16) | (s1 << 8) | s2; // [4S[x], 6S[x],  S[x], 2S[x]]
            T3[x] = (s6 << 24) | (s4 << 16) | (s2 << 8) | s1; // [6S[x], 4S[x], 2S[x],  S[x]]
            T4[x] = (s1 << 24) | (s1 << 16) | (s1 << 8) | s1; // [ S[x],  S[x],  S[x],  S[x]]
            T5[x] = (x  << 24) | (x2 << 16) | (x6 << 8) | x8; // [   x,    2x,    6x,    8x]
        }
    } // static

    protected int[/*R + 1*/][/*4*/] roundKeyEnc = null;
    protected int[/*R + 1*/][/*4*/] roundKeyDec = null;

    /**
     * Create the Anubis key schedule for a given cipher key.
     *
     * @param key   The 32N-bit cipher key.
     */
    public final void keySetup(byte[/*4*N*/] key)
    {

        // determine the N length parameter:
        int N = key.length/4; // consider only the first 4*N bytes
        if (N < 4 || N > 10)
        {
            throw new RuntimeException("Invalid Anubis key size: " + (32*N) + " bits.");
        }
        int[] kappa = new int[N];
        int[] inter = new int[N];

        // determine number of rounds from key size:
        int R = 8 + N;
        roundKeyEnc = new int[R + 1][4];
        roundKeyDec = new int[R + 1][4];

        // map byte array cipher key to initial key state (mu):
        for (int i = 0, pos = 0; i < N; i++)
        {
            kappa[i] =
                    ((key[pos++]       ) << 24) ^
                    ((key[pos++] & 0xff) << 16) ^
                    ((key[pos++] & 0xff) <<  8) ^
                    ((key[pos++] & 0xff)      );
        }

        // generate R + 1 round keys:
        for (int r = 0; r <= R; r++)
        {
            int K0, K1, K2, K3;
                        /*
                         * generate r-th round key K^r:
                         */
            K0 = T4[(kappa[N - 1] >>> 24)       ];
            K1 = T4[(kappa[N - 1] >>> 16) & 0xff];
            K2 = T4[(kappa[N - 1] >>>  8) & 0xff];
            K3 = T4[(kappa[N - 1]       ) & 0xff];
            for (int t = N - 2; t >= 0; t--)
            {
                K0 = T4[(kappa[t] >>> 24)       ] ^
                        (T5[(K0 >>> 24)       ] & 0xff000000) ^
                        (T5[(K0 >>> 16) & 0xff] & 0x00ff0000) ^
                        (T5[(K0 >>>  8) & 0xff] & 0x0000ff00) ^
                        (T5[(K0       ) & 0xff] & 0x000000ff);
                K1 = T4[(kappa[t] >>> 16) & 0xff] ^
                        (T5[(K1 >>> 24)       ] & 0xff000000) ^
                        (T5[(K1 >>> 16) & 0xff] & 0x00ff0000) ^
                        (T5[(K1 >>>  8) & 0xff] & 0x0000ff00) ^
                        (T5[(K1       ) & 0xff] & 0x000000ff);
                K2 = T4[(kappa[t] >>>  8) & 0xff] ^
                        (T5[(K2 >>> 24)       ] & 0xff000000) ^
                        (T5[(K2 >>> 16) & 0xff] & 0x00ff0000) ^
                        (T5[(K2 >>>  8) & 0xff] & 0x0000ff00) ^
                        (T5[(K2       ) & 0xff] & 0x000000ff);
                K3 = T4[(kappa[t]       ) & 0xff] ^
                        (T5[(K3 >>> 24)       ] & 0xff000000) ^
                        (T5[(K3 >>> 16) & 0xff] & 0x00ff0000) ^
                        (T5[(K3 >>>  8) & 0xff] & 0x0000ff00) ^
                        (T5[(K3       ) & 0xff] & 0x000000ff);
            }
            roundKeyEnc[r][0] = K0;
            roundKeyEnc[r][1] = K1;
            roundKeyEnc[r][2] = K2;
            roundKeyEnc[r][3] = K3;

                        /*
                         * compute kappa^{r+1} from kappa^r:
                         */
            for (int i = 0; i < N; i++)
            {
                inter[i] =
                        T0[(kappa[     i         ] >>> 24)       ] ^
                        T1[(kappa[(N + i - 1) % N] >>> 16) & 0xff] ^
                        T2[(kappa[(N + i - 2) % N] >>>  8) & 0xff] ^
                        T3[(kappa[(N + i - 3) % N]       ) & 0xff];
            }
            kappa[0] =
                    (T0[4*r    ] & 0xff000000) ^
                    (T1[4*r + 1] & 0x00ff0000) ^
                    (T2[4*r + 2] & 0x0000ff00) ^
                    (T3[4*r + 3] & 0x000000ff) ^
                    inter[0];
            for (int i = 1; i < N; i++)
            {
                kappa[i] = inter[i];
            }
        }

        // generate inverse key schedule: K'^0 = K^R, K'^R = K^0, K'^r = theta(K^{R-r}):
        for (int i = 0; i < 4; i++)
        {
            roundKeyDec[0][i] = roundKeyEnc[R][i];
            roundKeyDec[R][i] = roundKeyEnc[0][i];
        }
        for (int r = 1; r < R; r++)
        {
            for (int i = 0; i < 4; i++)
            {
                int v = roundKeyEnc[R - r][i];
                roundKeyDec[r][i] =
                        T0[T4[(v >>> 24)       ] & 0xff] ^
                        T1[T4[(v >>> 16) & 0xff] & 0xff] ^
                        T2[T4[(v >>>  8) & 0xff] & 0xff] ^
                        T3[T4[(v       ) & 0xff] & 0xff];
            }
        }
    } // keySetup

    /**
     * Either encrypt or ecrypt a data block, according to the key schedule.
     *
     * @param	block		the data block to be encrypted/decrypted.
     * @param	roundKey	the key schedule to be used.
     */
    protected final void crypt(byte[/*16*/] block, int[/*R + 1*/][/*4*/] roundKey)
    {
        int[] state = new int[4];
        int[] inter = new int[4];
        int R = roundKey.length - 1; // number of rounds

        /*
         * map byte array block to cipher state (mu)
         * and add initial round key (sigma[K^0]):
         */
        for (int i = 0, pos = 0; i < 4; i++)
        {
            state[i] =
                    ((block[pos++]       ) << 24) ^
                    ((block[pos++] & 0xff) << 16) ^
                    ((block[pos++] & 0xff) <<  8) ^
                    ((block[pos++] & 0xff)      ) ^
                    roundKey[0][i];
        }

        // R - 1 full rounds:
        for (int r = 1; r < R; r++)
        {
            inter[0] =
                    T0[(state[0] >>> 24)       ] ^
                    T1[(state[1] >>> 24)       ] ^
                    T2[(state[2] >>> 24)       ] ^
                    T3[(state[3] >>> 24)       ] ^
                    roundKey[r][0];
            inter[1] =
                    T0[(state[0] >>> 16) & 0xff] ^
                    T1[(state[1] >>> 16) & 0xff] ^
                    T2[(state[2] >>> 16) & 0xff] ^
                    T3[(state[3] >>> 16) & 0xff] ^
                    roundKey[r][1];
            inter[2] =
                    T0[(state[0] >>>  8) & 0xff] ^
                    T1[(state[1] >>>  8) & 0xff] ^
                    T2[(state[2] >>>  8) & 0xff] ^
                    T3[(state[3] >>>  8) & 0xff] ^
                    roundKey[r][2];
            inter[3] =
                    T0[(state[0]       ) & 0xff] ^
                    T1[(state[1]       ) & 0xff] ^
                    T2[(state[2]       ) & 0xff] ^
                    T3[(state[3]       ) & 0xff] ^
                    roundKey[r][3];
            for (int i = 0; i < 4; i++)
            {
                state[i] = inter[i];
            }
        }

        /*
         * last round:
         */
        inter[0] =
                (T0[(state[0] >>> 24)       ] & 0xff000000) ^
                (T1[(state[1] >>> 24)       ] & 0x00ff0000) ^
                (T2[(state[2] >>> 24)       ] & 0x0000ff00) ^
                (T3[(state[3] >>> 24)       ] & 0x000000ff) ^
                roundKey[R][0];
        inter[1] =
                (T0[(state[0] >>> 16) & 0xff] & 0xff000000) ^
                (T1[(state[1] >>> 16) & 0xff] & 0x00ff0000) ^
                (T2[(state[2] >>> 16) & 0xff] & 0x0000ff00) ^
                (T3[(state[3] >>> 16) & 0xff] & 0x000000ff) ^
                roundKey[R][1];
        inter[2] =
                (T0[(state[0] >>>  8) & 0xff] & 0xff000000) ^
                (T1[(state[1] >>>  8) & 0xff] & 0x00ff0000) ^
                (T2[(state[2] >>>  8) & 0xff] & 0x0000ff00) ^
                (T3[(state[3] >>>  8) & 0xff] & 0x000000ff) ^
                roundKey[R][2];
        inter[3] =
                (T0[(state[0]       ) & 0xff] & 0xff000000) ^
                (T1[(state[1]       ) & 0xff] & 0x00ff0000) ^
                (T2[(state[2]       ) & 0xff] & 0x0000ff00) ^
                (T3[(state[3]       ) & 0xff] & 0x000000ff) ^
                roundKey[R][3];

        // map cipher state to byte array block (mu^{-1}):
        for (int i = 0, pos = 0; i < 4; i++)
        {
            int w = inter[i];
            block[pos++] = (byte)(w >>> 24);
            block[pos++] = (byte)(w >>> 16);
            block[pos++] = (byte)(w >>>  8);
            block[pos++] = (byte)(w       );
        }

    } // crypt

    /**
     * Encrypt a data block.
     *
     * @param	block	the data buffer to be encrypted.
     */
    public final void encrypt(byte[/*16*/] block)
    {
        crypt(block, roundKeyEnc);
    } // encrypt

    /**
     * Decrypt a data block.
     *
     * @param	block	the data buffer to be decrypted.
     */
    public final void decrypt(byte[/*16*/] block)
    {
        crypt(block, roundKeyDec);
    } // decrypt

    public static String display(byte[] array)
    {
        char[] val = new char[2*array.length];
        String hex = "0123456789ABCDEF";
        for (int i = 0; i < array.length; i++)
        {
            int b = array[i] & 0xff;
            val[2*i] = hex.charAt(b >>> 4);
            val[2*i + 1] = hex.charAt(b & 15);
        }
        return String.valueOf(val);
    }

    /**
     * Generate the test vector set for Anubis.
     *
     * The test consists of the encryption of every block
     * with a single bit set under every key with a single
     * bit set for every allowed key size.
     */
    public static void makeTestVectors()
    {
        Anubis a = new Anubis();
        byte[] key, block;
        block = new byte[16];

        System.out.println("Anubis test vectors");
        System.out.println("--------------------------------------------------");
        for (int N = 4; N <= 10; N++)
        {
                        /*
                         * test vectors for 32N-bit keys:
                         */
            System.out.println("Test vectors for " + (32*N) + "-bit keys:");
            key = new byte[4*N];
            for (int i = 0; i < key.length; i++)
            {
                key[i] = 0;
            }
            for (int i = 0; i < block.length; i++)
            {
                block[i] = 0;
            }

                        /*
                         * iteration test -- encrypt the null block under the null key a large number of times:
                         */
                        /*
                        System.out.println("Long iteration test:");
                        System.out.print("\tKEY: " + display(key));
                        a.keySetup(key);
                        for (int k = 0; k < 1000000; k++) {
                                a.encrypt(block);
                        }
                        System.out.println("\tCT: " + display(block));
                        for (int k = 0; k < 1000000; k++) {
                                a.decrypt(block);
                        }
                        for (int i = 0; i < block.length; i++) {
                                if (block[i] != 0) {
                                        System.out.println("ERROR IN LONG ITERATION TEST!");
                                        return;
                                }
                        }
                         */
            System.out.println("Null block under all keys with a single bit set:");
            for (int k = 0; k < 32*N; k++)
            {
                // set the k-th bit:
                key[k/8] |= (byte)(0x80 >>> (k%8));
                System.out.print("\tKEY: " + display(key));
                // setup key:
                a.keySetup(key);
                // encrypt the null block:
                a.encrypt(block);
                System.out.println("\tCT: " + display(block));
                // getDecrypted:
                a.decrypt(block);
                for (int i = 0; i < block.length; i++)
                {
                    if (block[i] != 0)
                    {
                        System.out.println("ERROR IN SINGLE-BIT KEY TEST!");
                        return;
                    }
                }
                // reset the k-th key bit:
                key[k/8] = 0;
            }
            System.out.println("--------------------------------------------------");
        }
    }

    public static void clean()
    {
        for(int i=0; i < T0.length; i++)
            T0[i]=0;
        System.arraycopy(T0, 0, T1, 0, T1.length);
        System.arraycopy(T0, 0, T2, 0, T2.length);
        System.arraycopy(T0, 0, T3, 0, T3.length);
        System.arraycopy(T0, 0, T4, 0, T4.length);
        System.arraycopy(T0, 0, T5, 0, T5.length);
    }

/*    public static void main(String[] args)
    {
        Anubis.makeTestVectors();
    }*/

} // Anubis
