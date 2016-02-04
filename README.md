# darksms
Android app for encrypted SMS messaging
=======================================

Android OS based app - tool for secure messaging using plain SMS protocol.

Primary goals of project can be enlisted as:
   1. Simple user-friendly toolfor users who need strong encryption for their day-to-day activities:
like journalists, bloggers, non-government activists, just home users, etc;
   2. Independent on Internet connection availability (e.g. in remote places);
   3. Independent on presense/absense of any kind of intermediate servers (necessary to function for
popular chat messengers like WhatsApp, ICQ, Viber – names to count);
   4. Strong “end-to-end” conversation encryption based on open source projects;
   5. Possibility to encrypt sensible user conversation even those communicated using open channels.

We have selected to use as primary transport protocol GSM Short Message Service (SMS) protocol,
since it’s widely accepted and implemented practically by any 2G/3G/4G carriers around the World.
In spite of 160 symbols limitation in original SMS protocol, more or less any modern smartphone
supports function ofsmooth and invisible to users, splitting of long messages to series ofshort messages
and joining them into one message on target device. In order to use SMS transport protocol binary
messages encoded using modified Base64 algorithm.

Key exchange procedure implemented in Dark SMS as “Invitation”, we do believe that term “invitation”
is well-known even for simple-minded users, in contrary key-exchange is still known only for IT
specialists or proficient users.

Key exchange implemented using public key cryptography algorithms: either RSA/Diffie-Hellman or
ECC procedures. At the moment Dark SMS supports 5 kinds of them:RSA 512, RSA 1024,
ECC-112, ECC-255, ECC-384 bits.

During key exchange session (in terms of Dark SMS – invitation send/accept) users are notified about
received and send keys SHA-1 fingerprints, so if necessary users could easily check public keys validity
and protect themselves from MiTM attack. Key fingerprints are simple 4 pairs of digits (no any symbols
– just digits).

Private key for public key crypto calculated using device id, which is unique for virtually any device.
Key calculation procedure based on OpenSSL key procedures and use several thousands hashing iiterations 
of complicated combination of SALT and DeviceId. 

2 kinds of hashes used: SHA-512 and Whirlpool. Calculated private key does not stored in device, stored only random salt.
Used OpenSSL based Random Number Generator which utilize natural device enthropy from several sources.

For secure communication used 3 user selectable block ciphers in terms of Dark SMS privacy levels):
DES-56, AES-256 and Anubis-320 bits with CBC mode based on random IV. Symmetric session key
for block ciphers, calculated after key exchange procedure and modified using randomly selected
session id. Key modification is done using combination of SHA-512 and Whirlpool hashes.
Also Dark SMS supports:
   1. Calculation and checking of CRC for each message;
   2. Plain text messaging;
   3. Can combine plain text with cipher message;
   4. Can protect any plain text message.

Also available [small teaser on YouTube](https://www.youtube.com/watch?v=tyRfkn0FGXM) which explains how it works.

HOWTOS
-----
Some part of Dark SMS, namely heavy crypto calculations are written on native C. To build them one has to use Android NDK, all necessary build files (as ANT builds imported into Gradle) are supplied as well as prebuild native libraries (folder `/psm/src/main/libs`). To build those libraries from the scratch do followinf:
   1. Create/edit `psm/src/main/jni/ndk.properties` (example given in file `ndk.properties.canonical`)
   2. Launch either ANT build file `build_native.xml` or Gradle task `buildJni` - it's basically the same task
