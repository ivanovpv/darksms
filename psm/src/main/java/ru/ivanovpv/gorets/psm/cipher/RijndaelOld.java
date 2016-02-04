package ru.ivanovpv.gorets.psm.cipher;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/RijndaelOld.java $
 */

/**
 * The textbook being used gave this example:<br>
 * Plaintext:  0123456789abcdeffedcba9876543210<br>
 * Key:        0f1571c947d9e8590cb7add6af7f6798<br>
 * Ciphertext: ff0b844a0853bf7c6934ab4364148fb9
 *
 * @author Daniel Kotowski   * @version 1.0.0
 */
public final class RijndaelOld implements Cloneable
{
    private int[] rijndaelKey;
    private static final int[][] rijndaelSBox=
            {
                    {0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76},
                    {0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0},
                    {0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15},
                    {0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75},
                    {0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84},
                    {0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf},
                    {0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8},
                    {0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2},
                    {0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73},
                    {0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb},
                    {0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79},
                    {0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08},
                    {0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a},
                    {0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e},
                    {0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf},
                    {0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16}
            };
    private static final int[][] rijndaelInverseSBox=
            {
                    {0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb},
                    {0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb},
                    {0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e},
                    {0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25},
                    {0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92},
                    {0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84},
                    {0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06},
                    {0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b},
                    {0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73},
                    {0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e},
                    {0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b},
                    {0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4},
                    {0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f},
                    {0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef},
                    {0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61},
                    {0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d}
            };
    private int[] rijndaelRCon={0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36};
    private int[][] rijndaelMixColumnsMatrix=
            {
                    {0x02, 0x03, 0x01, 0x01},
                    {0x01, 0x02, 0x03, 0x01},
                    {0x01, 0x01, 0x02, 0x03},
                    {0x03, 0x01, 0x01, 0x02}
            };
    private int[][] rijndaelInverseMixColumnsMatrix=
            {
                    {0x0e, 0x0b, 0x0d, 0x09},
                    {0x09, 0x0e, 0x0b, 0x0d},
                    {0x0d, 0x09, 0x0e, 0x0b},
                    {0x0b, 0x0d, 0x09, 0x0e}
            };

    public RijndaelOld(int[] key) throws Exception
    {
        if(key.length != 16)
        {                          // Currently only supports 128-bit keys. Will be expanded in a future version
            throw new Exception("Key must be 16, 24, or 32 ints long");
        }
        else
        {
            rijndaelKey=key.clone();
        }
    }

    public int[] encrypt(int[] plaintext) throws Exception
    {
        if(plaintext.length != 16)
        {
            throw new Exception("plaintext must be 16 ints long");
        }
        else
        {
            int[] ciphertext=new int[16];
            int[][] ctMatrix=new int[4][4];
            int[][] ptMatrix=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                ptMatrix[i][0]=plaintext[4 * i];
                ptMatrix[i][1]=plaintext[4 * i + 1];
                ptMatrix[i][2]=plaintext[4 * i + 2];
                ptMatrix[i][3]=plaintext[4 * i + 3];
            }
            int[][][] rijndaelRKeys=rijndaelMakeRoundKeys(rijndaelKey);
            ctMatrix=rijndaelAddRoundKey(ptMatrix, rijndaelRKeys[0]);
            for(int i=1; i < 10; i++)
            {
                ctMatrix=rijndaelAddRoundKey(rijndaelMixColumns(rijndaelShiftRows(rijndaelSubints(ctMatrix))), rijndaelRKeys[i]);
            }
            ctMatrix=rijndaelAddRoundKey(rijndaelShiftRows(rijndaelSubints(ctMatrix)), rijndaelRKeys[10]);
            for(int i=0; i < 4; i++)
            {
                ciphertext[4 * i]=ctMatrix[i][0];
                ciphertext[4 * i + 1]=ctMatrix[i][1];
                ciphertext[4 * i + 2]=ctMatrix[i][2];
                ciphertext[4 * i + 3]=ctMatrix[i][3];
            }
            return ciphertext;
        }
    }

    public int[] decrypt(int[] ciphertext) throws Exception
    {
        if(ciphertext.length != 16)
        {
            throw new Exception("ciphertext must be 16 ints long");
        }
        else
        {
            int[] plaintext=new int[16];
            int[][] ptMatrix=new int[4][4];
            int[][] ctMatrix=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                ctMatrix[i][0]=ciphertext[4 * i];
                ctMatrix[i][1]=ciphertext[4 * i + 1];
                ctMatrix[i][2]=ciphertext[4 * i + 2];
                ctMatrix[i][3]=ciphertext[4 * i + 3];
            }
            int[][][] rijndaelRKeys=rijndaelMakeRoundKeys(rijndaelKey);
            ptMatrix=rijndaelAddRoundKey(ctMatrix, rijndaelRKeys[10]);
            for(int i=9; i > 0; i--)
            {
                ptMatrix=rijndaelInverseMixColumns(rijndaelAddRoundKey(rijndaelInverseSubints(rijndaelInverseShiftRows(ptMatrix)), rijndaelRKeys[i]));
            }
            ptMatrix=rijndaelAddRoundKey(rijndaelInverseSubints(rijndaelInverseShiftRows(ptMatrix)), rijndaelRKeys[0]);
            for(int i=0; i < 4; i++)
            {
                plaintext[4 * i]=ptMatrix[i][0];
                plaintext[4 * i + 1]=ptMatrix[i][1];
                plaintext[4 * i + 2]=ptMatrix[i][2];
                plaintext[4 * i + 3]=ptMatrix[i][3];
            }
            return plaintext;
        }
    }

    private int[][] rijndaelSubints(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] subbed=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    subbed[i][j]=rijndaelSBox[(B[i][j] >> 4) & 0x0f][B[i][j] & 0x0f];
                }
            }
            return subbed;
        }
    }

    private int[][] rijndaelInverseSubints(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] subbed=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    subbed[i][j]=rijndaelInverseSBox[B[i][j] >> 4][B[i][j] % 16];
                }
            }
            return subbed;
        }
    }

    private int[][] rijndaelShiftRows(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] shifted=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    shifted[i][j]=B[(i + j) % 4][j];
                }
            }
            return shifted;
        }
    }

    private int[][] rijndaelInverseShiftRows(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] shifted=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    shifted[i][j]=B[(4 + i - j) % 4][j];
                }
            }
            return shifted;
        }
    }

    private int[][] rijndaelMixColumns(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] mixed=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    for(int k=0; k < 4; k++)
                    {
                        mixed[j][i]^=rijndaelGFMult(rijndaelMixColumnsMatrix[i][k], B[j][k]);
                    }
                }
            }
            return mixed;
        }
    }

    private int[][] rijndaelInverseMixColumns(int[][] B) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else
        {
            int[][] mixed=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    for(int k=0; k < 4; k++)
                    {
                        mixed[j][i]^=rijndaelGFMult(rijndaelInverseMixColumnsMatrix[i][k], B[j][k]);
                    }
                }
            }
            return mixed;
        }
    }

    private int[][] rijndaelAddRoundKey(int[][] B, int[][] roundKey) throws Exception
    {
        if(B.length != 4 || B[0].length != 4)
        {
            throw new Exception("B must be 4x4 ints");
        }
        else if(roundKey.length != 4 || roundKey[0].length != 4)
        {
            throw new Exception("roundKey must be 4x4 ints");
        }
        else
        {
            int[][] keyed=new int[4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    keyed[i][j]=(B[i][j] ^ roundKey[i][j]);
                }
            }
            return keyed;
        }
    }

    private int[] rijndaelSubWord(int[] W) throws Exception
    {
        if(W.length != 4)
        {
            throw new Exception("W must be 4 ints");
        }
        else
        {
            int[] subbed=new int[4];
            for(int i=0; i < 4; i++)
            {
                subbed[i]=rijndaelSBox[(W[i] >> 4) & 0x0f][W[i] & 0x0f];
            }
            return subbed;
        }
    }

    private int[] rijndaelRotWord(int[] W) throws Exception
    {
        if(W.length != 4)
        {
            throw new Exception("W must be 4 ints");
        }
        else
        {
            int[] rotted=new int[4];
            rotted[0]=W[1];
            rotted[1]=W[2];
            rotted[2]=W[3];
            rotted[3]=W[0];
            return rotted;
        }
    }

    private int[][][] rijndaelMakeRoundKeys(int[] key) throws Exception
    {
        if(key.length != 16)
        {
            throw new Exception("key must be 16 ints long");
        }
        else
        {
            int[][] w=new int[44][4];
            int[][][] rijndaelRoundKeys=new int[11][4][4];
            for(int i=0; i < 4; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    w[i][j]=key[4 * i + j];
                }
            }
            int[] temp=new int[4];
            for(int i=4; i < 44; i++)
            {
                temp=w[i - 1];
                if((i % 4) == 0)
                {
                    temp=rijndaelRotWord(temp);
                    temp=rijndaelSubWord(temp);
                    temp[0]=(temp[0] ^ rijndaelRCon[(i / 4) - 1]);
                }
                for(int j=0; j < 4; j++)
                {
                    w[i][j]=(w[i - 4][j] ^ temp[j]);
                }
            }
            for(int i=0; i < 11; i++)
            {
                for(int j=0; j < 4; j++)
                {
                    rijndaelRoundKeys[i][j]=w[4 * i + j];
                }
            }
            return rijndaelRoundKeys;
        }
    }

    public static int rijndaelGFMult(int a, int b)
    {
        int p=0;
        boolean doXor;
        for(int i=0; i < 8; i++)
        {
            if((a & 1) == 1) p^=b;
            doXor=((b & 0x80) == 0x80);
            b=(b * 2) & 0xff;
            if(doXor) b^=0x1b;
            a>>>=1;
        }
        return p;
    }

    public int[] getRijndaelKey()
    {
        return rijndaelKey;
    }

    public void setRijndaelKey(int[] rijndaelKey)
    {
        this.rijndaelKey=rijndaelKey;
    }
}