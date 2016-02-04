# Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Oleksandr Lashchenko (gsorron@gmail.com)2012-2013. All Rights Reserved.
#   $Author: jim_bo $
#   $Rev: 415 $
#   $LastChangedDate: 2013-11-13 11:18:23 +0400 (Ср, 13 ноя 2013) $
#   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/jni/Android.mk

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := nativepsm
LOCAL_SRC_FILES := jni_interface.c \
					openssl/cryptlib.c openssl/mem.c openssl/mem_dbg.c openssl/o_init.c openssl/ex_data.c \
					openssl/stack/stack.c \
					openssl/objects/obj_dat.c \
					openssl/lhash/lhash.c \
					openssl/buffer/buf_err.c openssl/buffer/buffer.c openssl/buffer/buf_str.c \
					openssl/err/err.c openssl/err/err_all.c \
					openssl/sha/sha256.c openssl/sha/sha512.c openssl/sha/sha_dgst.c \
					openssl/whrlpool/wp_block.c openssl/whrlpool/wp_dgst.c openssl/mem_clr.c \
					openssl/ec/ec_check.c openssl/ec/ec_curve.c openssl/ec/ec_key.c \
					openssl/ec/ec_lib.c openssl/ec/ec_mult.c openssl/ec/ec_oct.c openssl/ec/ec_cvt.c  \
					openssl/ec/ecp_mont.c openssl/ec/ecp_oct.c openssl/ec/ecp_smpl.c openssl/ec/ecp_nist.c \
					openssl/ecdh/ech_key.c openssl/ecdh/ech_lib.c openssl/ecdh/ech_ossl.c \
					openssl/bn/bn_add.c openssl/bn/bn_asm.c openssl/bn/bn_const.c openssl/bn/bn_ctx.c \
					openssl/bn/bn_div.c openssl/bn/bn_gcd.c openssl/bn/bn_lib.c openssl/bn/bn_mod.c \
					openssl/bn/bn_mont.c openssl/bn/bn_mul.c openssl/bn/bn_shift.c openssl/bn/bn_sqr.c \
					openssl/bn/bn_sqrt.c openssl/bn/bn_word.c openssl/bn/bn_kron.c openssl/bn/bn_exp.c \
					openssl/bn/bn_rand.c openssl/bn/bn_recp.c openssl/bn/bn_nist.c \
					openssl/rand/md_rand.c openssl/rand/rand_lib.c openssl/rand/rand_unix.c openssl/rand/rand_egd.c

LOCAL_C_FLAGS += OPENSSL_NO_FP_API
LOCAL_C_INCLUDES += ./openssl
LOCAL_LDLIBS += -llog

include $(BUILD_SHARED_LIBRARY)


