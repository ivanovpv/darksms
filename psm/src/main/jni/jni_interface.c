/* Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Oleksandr Lashchenko (gsorron@gmail.com)2012-2013. All Rights Reserved.
   $Author: jim_bo $
   $Rev: 435 $
   $LastChangedDate: 2013-11-28 11:45:25 +0400 (Чт, 28 ноя 2013) $
   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/jni/jni_interface.c $#include <string.h>
   */
#include <jni.h>
#include <string.h>
#include <openssl/ec.h>
#include <openssl/ecdh.h>
#include <openssl/sha.h>
#include <openssl/whrlpool.h>
#include <openssl/rand.h>
#include <openssl/obj_mac.h>
#include <android/log.h>

#define LOGV(TAG,...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(TAG,...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(TAG,...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(TAG,...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(TAG,...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)

#define MAX_HASH_SIZE       64
#define MAX_EC_KEY_SIZE     66 // EC 521 bit + 2 bit - max

jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_generatePK( JNIEnv* env,
                                                  jobject thiz, jstring password, jstring salt,
                                                  jint hashType, jint keySize)
{

    const char * saltArr = NULL;
    const char * passArr = NULL;
    const char * saltPtr = NULL;
    unsigned saltLength = 0;
    unsigned passLength = 0;
    unsigned char digest[MAX_HASH_SIZE];
    unsigned digestSize = 0;
    jbyteArray byteArr = NULL;
    WHIRLPOOL_CTX wp_ctx;
    SHA512_CTX sha_ctx;

    if ( ((hashType != 1) && (hashType != 2)) || (keySize <= 0))
    {
        return NULL;
    }

    digestSize = (keySize <= MAX_HASH_SIZE) ? keySize : MAX_HASH_SIZE;

    saltArr = (*env)->GetStringUTFChars(env, salt, NULL);
    if(saltArr == NULL)
    {
        return NULL;
    }

    passArr = (*env)->GetStringUTFChars(env, password, NULL);
    if(saltArr == NULL)
    {
        (*env)->ReleaseStringUTFChars(env, salt, saltArr);
        return NULL;
    }

    saltLength = (*env)->GetStringUTFLength(env, salt);
    passLength = (*env)->GetStringUTFLength(env, password);

    switch (hashType) //
    {
    case 1: // WP
        WHIRLPOOL_Init(&wp_ctx);
        saltPtr = saltArr;
        WHIRLPOOL_Update(&wp_ctx, (const void *)saltPtr, saltLength/2);
        saltPtr += saltLength/2;
        saltLength -= saltLength/2;
        WHIRLPOOL_Update(&wp_ctx, (const void *)passArr, passLength);
        WHIRLPOOL_Update(&wp_ctx, (const void *)saltPtr, saltLength);
        WHIRLPOOL_Final((unsigned char *)digest, &wp_ctx);
        break;
    case 2: // SHA512
        SHA512_Init(&sha_ctx);
        saltPtr = saltArr;
        SHA512_Update(&sha_ctx, (const void *)saltPtr, saltLength/2);
        saltPtr += saltLength/2;
        saltLength -= saltLength/2;
        SHA512_Update(&sha_ctx, (const void *)passArr, passLength);
        SHA512_Update(&sha_ctx, (const void *)saltPtr, saltLength);
        SHA512_Final((unsigned char *)digest, &sha_ctx);
        break;
    }

    (*env)->ReleaseStringUTFChars(env, salt, saltArr);
    (*env)->ReleaseStringUTFChars(env, salt, passArr);

    byteArr  =  (*env)->NewByteArray(env, digestSize);
    if (byteArr == NULL)
        return NULL;

    (*env)->SetByteArrayRegion(env, byteArr,
                            0, digestSize, (const jbyte*)digest);
    memset (digest, 0, digestSize);

    return byteArr;
}

jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_generateHash( JNIEnv* env,
                                                  jobject thiz,  jbyteArray arr,
                                                  jint hashType, jint times)
{


    unsigned char * rawArr = NULL;
    unsigned arrSize = 0;
    unsigned i = 0;
    unsigned char digest1[MAX_HASH_SIZE], digest2[MAX_HASH_SIZE];
    unsigned char * data = NULL, * digest = NULL, * tempArr = NULL;
    unsigned digestSize = 0;
    jbyteArray byteArr = NULL;

    if ( ((hashType != 1) && (hashType != 2)) || (times < 0))
    {
        return NULL;
    }

    digestSize = MAX_HASH_SIZE;

    arrSize = (*env)->GetArrayLength(env, arr);
    if (arrSize <= 0)
    {
        goto cleanup;
    }

    if( (rawArr = (*env)->GetByteArrayElements(env, arr, NULL)) == NULL )
    {
        goto cleanup;
    }

    switch (hashType) //
    {
    case 1: // WP
        if (WHIRLPOOL((const unsigned char *)rawArr, arrSize, digest1) == NULL)
        {
            goto cleanup;
        }
        data = digest1;
        digest = digest2;
        for(i = 0; i < times; i++)
        {
            if (WHIRLPOOL((const unsigned char *)data, digestSize, digest) == NULL)
            {
                goto cleanup;
            }
            tempArr = data;
            data = digest;
            digest = tempArr;
        }
        tempArr = data;
        data = digest;
        digest = tempArr;
        break;
    case 2: // SHA512
        if (SHA512((const unsigned char *)rawArr, arrSize, digest1) == NULL)
        {
            goto cleanup;
        }
        data = digest1;
        digest = digest2;
        for(i = 0; i < times; i++)
        {
              if (SHA512((const unsigned char *)data, digestSize, digest) == NULL)
              {
                  goto cleanup;
              }
              tempArr = data;
              data = digest;
              digest = tempArr;
        }
        tempArr = data;
        data = digest;
        digest = tempArr;
        break;
    }

    byteArr  =  (*env)->NewByteArray(env, digestSize);
    if (byteArr == NULL)
    {
        goto cleanup;
    }

    (*env)->SetByteArrayRegion(env, byteArr,
                            0, digestSize, (const jbyte*)digest);
    memset (digest1, 0, digestSize);
    memset (digest2, 0, digestSize);

cleanup:

    if ( rawArr )
        (*env)->ReleaseByteArrayElements(env, arr, rawArr, JNI_ABORT);

    return byteArr;
}

jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_getECPublicKey( JNIEnv* env, jobject thiz,
                                                    jbyteArray privKey, jint ecGroup)
{
    EC_GROUP * ec_group = NULL;
    EC_POINT * ec_pub = NULL;
    BN_CTX * bn_ctx = NULL;
    BIGNUM * bn_priv = NULL;
    jbyte * privKeyArr = NULL;
    jbyte pubKeyArr[MAX_EC_KEY_SIZE];
    jbyteArray pubKey = NULL;
    jint privKeySize = 0;
    size_t pubKeySize = 0;

    if (  ((ecGroup != 1) && (ecGroup != 2) && (ecGroup != 3)) || (privKey == NULL) )
    {
        goto cleanup;
    }

    privKeySize = (*env)->GetArrayLength(env, privKey);
    if (privKeySize <= 0)
    {
        goto cleanup;
    }

    switch(ecGroup)
    {
    case 1:
        if ( privKeySize < (112/8) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp112r1);
        break;
    case 2:
        if ( privKeySize < (256/8) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp256k1);
        break;
    case 3:
        if ( privKeySize < (384/8) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp384r1);
        break;
    default:
        goto cleanup;
    }

    if (!ec_group)
    {
        goto cleanup;
    }

    if( (bn_priv = BN_new()) == NULL)
    {
        goto cleanup;
    }

    if( (privKeyArr = (*env)->GetByteArrayElements(env, privKey, NULL)) == NULL )
    {
        goto cleanup;
    }

    if ( BN_bin2bn((const unsigned char *)privKeyArr, (int) privKeySize, bn_priv) == NULL )
    {
        goto cleanup;
    }

    if ( (bn_ctx = BN_CTX_new()) == NULL )
    {
        goto cleanup;
    }

    if ( (ec_pub = EC_POINT_new(ec_group)) == NULL)
    {
        goto cleanup;
    }

    if (!EC_POINT_mul((const EC_GROUP *) ec_group, ec_pub, (const BIGNUM *)bn_priv, NULL, NULL, bn_ctx))
    {
        goto cleanup;
    }

    if ( (pubKeySize = EC_POINT_point2oct((const EC_GROUP *) ec_group, (const EC_POINT *) ec_pub,
    	POINT_CONVERSION_COMPRESSED, (unsigned char *) pubKeyArr, (size_t) MAX_EC_KEY_SIZE, bn_ctx)) == 0 )
    {
        goto cleanup;
    }

    pubKey  =  (*env)->NewByteArray(env, pubKeySize);
    if (pubKey == NULL)
    {
        goto cleanup;
    }

    (*env)->SetByteArrayRegion(env, pubKey,
                0, pubKeySize, (const jbyte*)pubKeyArr);

    memset (pubKeyArr, 0, pubKeySize);

cleanup:

    if ( ec_group )
        EC_GROUP_clear_free( ec_group );

    if ( ec_pub )
        EC_POINT_clear_free( ec_pub );

    if ( bn_priv )
        BN_clear_free( bn_priv );

    if ( privKeyArr )
        (*env)->ReleaseByteArrayElements(env, privKey, privKeyArr, JNI_ABORT);

    return pubKey;

}

jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_getECSharedKey( JNIEnv* env, jobject thiz,
                                                    jbyteArray privKey, jbyteArray pubKey, jint ecGroup)
{
    EC_GROUP * ec_group = NULL;
    EC_POINT * ec_pub = NULL;
    EC_POINT * ec_res = NULL;
    BN_CTX * bn_ctx = NULL;
    BIGNUM * bn_priv = NULL;
    jbyte * privKeyArr = NULL;
    jbyte * pubKeyArr = NULL;
    jbyteArray resKey = NULL;
    jbyte resKeyArr[MAX_EC_KEY_SIZE];
    jint privKeySize = 0;
    jint pubKeySize = 0;
    size_t resKeySize = 0;

    if (  ((ecGroup != 1) && (ecGroup != 2) && (ecGroup != 3)) || (privKey == NULL) || (pubKey == NULL) )
    {
        goto cleanup;
    }

    privKeySize = (*env)->GetArrayLength(env, privKey);
    pubKeySize = (*env)->GetArrayLength(env, pubKey);
    if ( (privKeySize <= 0) || (pubKeySize <= 0) )
    {
        goto cleanup;
    }

    switch(ecGroup)
    {
    case 1:
        if ( ( privKeySize < (112/8) ) || ( pubKeySize < (112/8+1) ) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp112r1);
        break;
    case 2:
        if ( ( privKeySize < (256/8) ) || ( pubKeySize < (256/8+1) ) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp256k1);
        break;
    case 3:
        if ( ( privKeySize < (384/8) ) || ( pubKeySize < (384/8+1) ) )
            goto cleanup;
        ec_group = EC_GROUP_new_by_curve_name(NID_secp384r1);
        break;
    default:
        goto cleanup;
    }

    if (!ec_group)
    {
        goto cleanup;
    }

    if( (bn_priv = BN_new()) == NULL)
    {
        goto cleanup;
    }

    if( (privKeyArr = (*env)->GetByteArrayElements(env, privKey, NULL)) == NULL )
    {
        goto cleanup;
    }

    if ( BN_bin2bn((const unsigned char *)privKeyArr, (int) privKeySize, bn_priv) == NULL )
    {
        goto cleanup;
    }

    if ( (bn_ctx = BN_CTX_new()) == NULL )
    {
        goto cleanup;
    }

    if ( (ec_pub = EC_POINT_new(ec_group)) == NULL)
    {
        goto cleanup;
    }

    if( (pubKeyArr = (*env)->GetByteArrayElements(env, pubKey, NULL)) == NULL )
    {
            goto cleanup;
    }

    if ( EC_POINT_oct2point((const EC_GROUP *) ec_group, (EC_POINT *) ec_pub,
        (unsigned char *) pubKeyArr, (size_t) pubKeySize, bn_ctx) == 0 )
    {
        goto cleanup;
    }

    if ( (ec_res = EC_POINT_new(ec_group)) == NULL)
    {
            goto cleanup;
    }


    if (!EC_POINT_mul((const EC_GROUP *) ec_group, ec_res, (const BIGNUM *)NULL, ec_pub, bn_priv, bn_ctx))
    {
        goto cleanup;
    }


    if ( (resKeySize = EC_POINT_point2oct((const EC_GROUP *) ec_group, (const EC_POINT *) ec_res,
        	POINT_CONVERSION_COMPRESSED, (unsigned char *) resKeyArr, (size_t) MAX_EC_KEY_SIZE, bn_ctx)) == 0 )
    {
        goto cleanup;
    }

    resKey  =  (*env)->NewByteArray(env, resKeySize);
    if (resKey == NULL)
    {
        goto cleanup;
    }

    (*env)->SetByteArrayRegion(env, resKey,
                0, resKeySize, (const jbyte*)resKeyArr);

    memset (resKeyArr, 0, resKeySize);


cleanup:

    if ( ec_group )
        EC_GROUP_clear_free( ec_group );

    if ( ec_pub )
        EC_POINT_clear_free( ec_pub );

    if ( ec_res )
        EC_POINT_clear_free( ec_res );

    if ( bn_priv )
        BN_clear_free( bn_priv );

    if ( privKeyArr )
        (*env)->ReleaseByteArrayElements(env, privKey, privKeyArr, JNI_ABORT);

    if ( pubKeyArr )
        (*env)->ReleaseByteArrayElements(env, pubKey, pubKeyArr, JNI_ABORT);

    return resKey;
}

jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_getRandomBytes( JNIEnv* env,
                                                  jobject thiz, jint length)
{
    unsigned char * rawArr = NULL;
    jbyteArray byteArr = NULL;

    if ( (length <= 0))
    {
        return NULL;
    }

    if( !(rawArr = malloc(length)))
    {
        return NULL;
    }

    if(!RAND_bytes(rawArr, length))
    {
        goto cleanup;
    }

    byteArr  =  (*env)->NewByteArray(env, length);
    if (byteArr == NULL)
    {
        goto cleanup;
    }

    (*env)->SetByteArrayRegion(env, byteArr,
                            0, length, (const jbyte*)rawArr);

    memset (rawArr, 0, length);

cleanup:

    if ( rawArr )
        free( rawArr );

    return byteArr;
}

/*jbyteArray
Java_ru_ivanovpv_gorets_psm_nativelib_NativeLib_getECParameters( JNIEnv* env, jint ecGroup)
{
    int i, resKeySize;
    jbyteArray resKey = NULL;
    switch(ecGroup) {
        case 1:
            resKeySize=20+24*6;
            resKey=(*env)->NewByteArray(env, resKeySize);
            for (i=0; i < resKeySize; i++)
                resKey[i]=_EC_NIST_PRIME_192.data[i];
            break;
        case 2:
            resKeySize=20+28*6;
            resKey=(*env)->NewByteArray(env, resKeySize);
            for (i=0; i < resKeySize; i++)
                resKey[i]=_EC_NIST_PRIME_224.data[i];
            break;
        case 3:
            resKeySize=20+48*6;
            resKey=(*env)->NewByteArray(env, resKeySize);
            for (i=0; i < resKeySize; i++)
                resKey[i]=_EC_NIST_PRIME_384.data[i];
            break;
    }
    return resKey;
} */