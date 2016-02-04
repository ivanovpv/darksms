package ru.ivanovpv.gorets.psm.persistent;

import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.*;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 496 $
 *   $LastChangedDate: 2014-02-05 13:42:06 +0400 (Ср, 05 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/PhoneNumber.java $
 */

public class PhoneNumber implements Serializable {
    private final static String TAG=PhoneNumber.class.getName();
    private String id; //reference to db id (if null not saved yet)
    private String rawAddress;
    private String normalAddress;
    private String type;
    private boolean primary;
    private boolean alwaysProtect;
    private boolean alwaysEncrypt;
    private KeyRing publicKeys;
    private String contactKey;
    static PhoneNumberUtil phoneNumberUtil=PhoneNumberUtil.getInstance();

    public PhoneNumber() {
        rawAddress = "";
        normalAddress = "";
        type = "";
        primary = false;
        alwaysProtect = false;
        alwaysEncrypt = false;
        publicKeys = new KeyRing();
        contactKey=null;
    }

    public PhoneNumber(String address) {
        this.rawAddress = address;
        this.normalAddress = this.getNormalizedAddress(address);
        this.type="";
        this.primary=false;
        this.alwaysProtect=false;
        this.alwaysEncrypt = false;
        this.publicKeys=new KeyRing();
    }

    public PhoneNumber(String address, String type, boolean primary) {
        this.rawAddress = address;
        this.normalAddress = this.getNormalizedAddress(address);
        this.type=type;
        this.primary=primary;
        this.alwaysProtect=false;
        this.alwaysEncrypt = false;
        this.publicKeys=new KeyRing();
    }

    /**
     * Removes any formatting symbols except digits and + sign
     * @param address
     * @return
     */
    private static String stripAddress(String address) {
        if(address==null)
            return "";
        StringBuilder sb=new StringBuilder();
        for(char ch:address.toCharArray()) {
            if(Character.isDigit(ch))
                sb.append(ch);
            else if(ch=='+')
                sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * Formats address to normal form like +[country][prefix][localnumber]
     * @param address
     * @return
     */
    private static String getNormalizedAddress(String address) {
        String number=stripAddress(address);
        try {
            //parse number using current locale rules
            Phonenumber.PhoneNumber phonenumber=phoneNumberUtil.parseAndKeepRawInput(number, Locale.getDefault().getCountry());
            //Phonenumber.PhoneNumber phonenumber = phoneNumberUtil.parse(number, Locale.getDefault().getCountry());
            //format to international form
            //phoneNumberUtil
            number=phoneNumberUtil.format(phonenumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            //stripe any formatting symbols
            return stripAddress(number);
        }
        catch(Exception ex) {
            //in case of fail use default formatting rules
            number=PhoneNumberUtils.formatNumber(number);
            //stripe any formatting symbols
            return stripAddress(number);
        }
    }


    public boolean isSaveable() {
        if(primary || alwaysProtect || alwaysEncrypt)
            return true;
        if(publicKeys.size() > 0)
            return true;
        return false;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void copyFrom(PhoneNumber phoneNumber) {
        this.id=phoneNumber.getId();
        this.rawAddress = this.getRawAddress();
        this.normalAddress = this.getNormalAddress();
        this.type=phoneNumber.getType();
        this.primary=phoneNumber.isPrimary();
        this.alwaysProtect=phoneNumber.isAlwaysProtect();
        this.publicKeys =phoneNumber.getPublicKeys();
        this.alwaysEncrypt=phoneNumber.isAlwaysEncrypt();
        this.contactKey=phoneNumber.getContactKey();
        this.publicKeys=phoneNumber.getPublicKeys();
    }

    public boolean isAlwaysProtect() {
        return alwaysProtect;
    }

    public void setAlwaysProtect(boolean alwaysProtect) {
        this.alwaysProtect = alwaysProtect;
    }

    public String getNormalAddress() {
        return normalAddress;
    }

    public String getRawAddress() {
        return rawAddress;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return normalAddress + "; " + type+"("+primary+")";
    }

    public String getType() {
        return type;
    }

    public KeyRing getPublicKeys() {
        return publicKeys;
    }

    public void setPublicKeys(KeyRing publicKeys) {
        this.publicKeys = publicKeys;
    }

    public void addPublicKey(byte[] key, long time, char type) {
        publicKeys.addKey(key, time, type);
    }

    public boolean isPresentKey() {
        return isPresentKey(System.currentTimeMillis());
    }

    public boolean isPresentKey(long time) {
        if(publicKeys.getKey(time, Me.getDefaultKeyExchangeType())!=null)
            return true;
        return false;
    }


    public Hashtable<Long, FingerPrint> getKeysFingerPrints() {
        return publicKeys.getKeysFingerPrints();
    }

    public byte[] getSharedKey(long time) {
        byte[] publicKey=publicKeys.getKey(time, Me.getDefaultKeyExchangeType());
        if(publicKey==null)
            return null;
        KeyExchange keyExchange;
        switch(Me.getDefaultKeyExchangeType()) {
            case KeyExchange.KEY_EXCHANGE_DIFFIE_HELLMAN:
                keyExchange=new DiffieHellman(Me.getMe().getHashDAO().get().getKey());
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B:
                keyExchange=new EllipticCurve112B(Me.getMe().getHashDAO().get().getKey());
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B:
                keyExchange=new EllipticCurve256B(Me.getMe().getHashDAO().get().getKey());
                break;
            case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B:
                keyExchange=new EllipticCurve384B(Me.getMe().getHashDAO().get().getKey());
                break;
            case KeyExchange.KEY_EXCHANGE_DUMMY:
            default:
                throw new IllegalStateException("Unknown key exchange protocol!");
        }
        byte[] sharedKey=keyExchange.getSharedKey(publicKey);
        //shared key extension in order to support longer keys cipher
        return NativeLib.generateHash(sharedKey, NativeLib.HASH_SHA512, 0);
    }

    public byte[] getSessionKey(long time, int sessionIndex) {
        byte[] sharedKey=this.getSharedKey(time);
        //morphing shared key depending on session
        byte[] sessionKey=NativeLib.generateHash(sharedKey, NativeLib.HASH_SHA512, sessionIndex*10);
        return NativeLib.generateHash(sessionKey, NativeLib.HASH_WHIRLPOOL, (Protocol.SESSION_SIZE-sessionIndex)*10);
    }

    public boolean isAlwaysEncrypt() {
        return alwaysEncrypt;
    }

    public void setAlwaysEncrypt(boolean alwaysEncrypt) {
        this.alwaysEncrypt = alwaysEncrypt;
    }

    public boolean compareDefault(String address) {
        try {
            Phonenumber.PhoneNumber phoneNumber=phoneNumberUtil.parse(address, Locale.getDefault().getCountry());
            PhoneNumberUtil.MatchType matchType=phoneNumberUtil.isNumberMatch(phoneNumber, this.getRawAddress());
            if(matchType == PhoneNumberUtil.MatchType.EXACT_MATCH ||
                    matchType== PhoneNumberUtil.MatchType.NSN_MATCH ||
                    matchType == PhoneNumberUtil.MatchType.SHORT_NSN_MATCH)
                return true;
            matchType=phoneNumberUtil.isNumberMatch(phoneNumber, this.getNormalAddress());
            if(matchType == PhoneNumberUtil.MatchType.EXACT_MATCH ||
                    matchType== PhoneNumberUtil.MatchType.NSN_MATCH ||
                    matchType == PhoneNumberUtil.MatchType.SHORT_NSN_MATCH)
                return true;
            return false;
        }
        catch(Exception e) {
            Log.w(TAG, "Error comparing addresses='"+this.rawAddress+"' and '"+address+"'", e);
            return false;
        }
    }

    private static final String DIGITS = "\\p{Nd}";
    private static final int MIN_LENGTH_FOR_NSN = 2;
    static final String PLUS_CHARS = "+\uFF0B";
    static final String VALID_PUNCTUATION = "-x\u2010-\u2015\u2212\u30FC\uFF0D-\uFF0F " +
            "\u00A0\u00AD\u200B\u2060\u3000()\uFF08\uFF09\uFF3B\uFF3D.\\[\\]/~\u2053\u223C\uFF5E";

    private static final char STAR_SIGN = '*';
    private static final Map<Character, Character> ALPHA_MAPPINGS;
    static {
        HashMap<Character, Character> alphaMap = new HashMap<Character, Character>(40);
        alphaMap.put('A', '2');
        alphaMap.put('B', '2');
        alphaMap.put('C', '2');
        alphaMap.put('D', '3');
        alphaMap.put('E', '3');
        alphaMap.put('F', '3');
        alphaMap.put('G', '4');
        alphaMap.put('H', '4');
        alphaMap.put('I', '4');
        alphaMap.put('J', '5');
        alphaMap.put('K', '5');
        alphaMap.put('L', '5');
        alphaMap.put('M', '6');
        alphaMap.put('N', '6');
        alphaMap.put('O', '6');
        alphaMap.put('P', '7');
        alphaMap.put('Q', '7');
        alphaMap.put('R', '7');
        alphaMap.put('S', '7');
        alphaMap.put('T', '8');
        alphaMap.put('U', '8');
        alphaMap.put('V', '8');
        alphaMap.put('W', '9');
        alphaMap.put('X', '9');
        alphaMap.put('Y', '9');
        alphaMap.put('Z', '9');
        ALPHA_MAPPINGS = Collections.unmodifiableMap(alphaMap);
    }

    private static final String EXTN_PATTERNS_FOR_PARSING;
    static final String EXTN_PATTERNS_FOR_MATCHING;
    static {
        // One-character symbols that can be used to indicate an extension.
        String singleExtnSymbolsForMatching = "x\uFF58#\uFF03~\uFF5E";
        // For parsing, we are slightly more lenient in our interpretation than for matching. Here we
        // allow a "comma" as a possible extension indicator. When matching, this is hardly ever used to
        // indicate this.
        String singleExtnSymbolsForParsing = "," + singleExtnSymbolsForMatching;

        EXTN_PATTERNS_FOR_PARSING = createExtnPattern(singleExtnSymbolsForParsing);
        EXTN_PATTERNS_FOR_MATCHING = createExtnPattern(singleExtnSymbolsForMatching);
    }
    private static final String RFC3966_EXTN_PREFIX = ";ext=";
    private static final String CAPTURING_EXTN_DIGITS = "(" + DIGITS + "{1,7})";
    static final int REGEX_FLAGS = Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
    /**
     * Helper initialiser method to create the regular-expression pattern to match extensions,
     * allowing the one-char extension symbols provided by {@code singleExtnSymbols}.
     */
    private static String createExtnPattern(String singleExtnSymbols) {
        // There are three regular expressions here. The first covers RFC 3966 format, where the
        // extension is added using ";ext=". The second more generic one starts with optional white
        // space and ends with an optional full stop (.), followed by zero or more spaces/tabs and then
        // the numbers themselves. The other one covers the special case of American numbers where the
        // extension is written with a hash at the end, such as "- 503#".
        // Note that the only capturing groups should be around the digits that you want to capture as
        // part of the extension, or else parsing will fail!
        // Canonical-equivalence doesn't seem to be an option with Android java, so we allow two options
        // for representing the accented o - the character itself, and one in the unicode decomposed
        // form with the combining acute accent.
        return (RFC3966_EXTN_PREFIX + CAPTURING_EXTN_DIGITS + "|" + "[ \u00A0\\t,]*" +
                "(?:e?xt(?:ensi(?:o\u0301?|\u00F3))?n?|\uFF45?\uFF58\uFF54\uFF4E?|" +
                "[" + singleExtnSymbols + "]|int|anexo|\uFF49\uFF4E\uFF54)" +
                "[:\\.\uFF0E]?[ \u00A0\\t,-]*" + CAPTURING_EXTN_DIGITS + "#?|" +
                "[- ]+(" + DIGITS + "{1,5})#");
    }

    private static final String VALID_ALPHA =
            Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll("[, \\[\\]]", "") +
                    Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).toLowerCase().replaceAll("[, \\[\\]]", "");

    private static final String VALID_PHONE_NUMBER =
            DIGITS + "{" + MIN_LENGTH_FOR_NSN + "}" + "|" +
                    "[" + PLUS_CHARS + "]*+(?:[" + VALID_PUNCTUATION + STAR_SIGN + "]*" + DIGITS + "){3,}[" +
                    VALID_PUNCTUATION + STAR_SIGN + VALID_ALPHA + DIGITS + "]*";

    private static final Pattern VALID_PHONE_NUMBER_PATTERN =
            Pattern.compile(VALID_PHONE_NUMBER + "(?:" + EXTN_PATTERNS_FOR_PARSING + ")?", REGEX_FLAGS);


    public static boolean isPhoneNumber(String address) {
        return isPhoneNumberSoft(address);
    }

    public static boolean isPhoneNumberSoft(String address) {
        if (address==null || address.length() < 2) {
            return false;
        }
        Matcher m = VALID_PHONE_NUMBER_PATTERN.matcher(address);
        return m.matches();
    }

    public static boolean isPhoneNumberStrict(String address) {
        try {
            phoneNumberUtil.parse(address, "ZZ");
            return true;
        }
        catch(Exception e) {
            Log.w(TAG, "Error parsing address as unknown region="+address, e);
            try {
                phoneNumberUtil.parse(address, Locale.getDefault().getCountry());
                Log.w(TAG, "Error parsing address=" + address + " for region=" + Locale.getDefault().getCountry(), e);
                return true;
            }
            catch(Exception e2) {
                return false;
            }
        }
    }

    /*
     * Copyright (C) 2006 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    private static final char PAUSE = ',';
    private static final char WAIT = ';';
    private static final char WILD = 'N';
    private static final int MIN_MATCH = 7;

    public static boolean compareLoosely(String a, String b) {
        int ia, ib;
        int matched;
        int numNonDialableCharsInA = 0;
        int numNonDialableCharsInB = 0;

        if (a == null || b == null) return a == b;

        if (a.length() == 0 || b.length() == 0) {
            return false;
        }

        ia = indexOfLastNetworkChar (a);
        ib = indexOfLastNetworkChar (b);
        matched = 0;

        while (ia >= 0 && ib >=0) {
            char ca, cb;
            boolean skipCmp = false;

            ca = a.charAt(ia);

            if (!isDialable(ca)) {
                ia--;
                skipCmp = true;
                numNonDialableCharsInA++;
            }

            cb = b.charAt(ib);

            if (!isDialable(cb)) {
                ib--;
                skipCmp = true;
                numNonDialableCharsInB++;
            }

            if (!skipCmp) {
                if (cb != ca && ca != WILD && cb != WILD) {
                    break;
                }
                ia--; ib--; matched++;
            }
        }

        if (matched < MIN_MATCH) {
            int effectiveALen = a.length() - numNonDialableCharsInA;
            int effectiveBLen = b.length() - numNonDialableCharsInB;


            // if the number of dialable chars in a and b match, but the matched chars < MIN_MATCH,
            // treat them as equal (i.e. 404-04 and 40404)
            if (effectiveALen == effectiveBLen && effectiveALen == matched) {
                return true;
            }

            return false;
        }

        // At least one string has matched completely;
        if (matched >= MIN_MATCH && (ia < 0 || ib < 0)) {
            return true;
        }

        /*
         * Now, what remains must be one of the following for a
         * match:
         *
         *  - a '+' on one and a '00' or a '011' on the other
         *  - a '0' on one and a (+,00)<country code> on the other
         *     (for this, a '0' and a '00' prefix would have succeeded above)
         */

        if (matchIntlPrefix(a, ia + 1)
                && matchIntlPrefix (b, ib +1)
                ) {
            return true;
        }

        if (matchTrunkPrefix(a, ia + 1)
                && matchIntlPrefixAndCC(b, ib +1)
                ) {
            return true;
        }

        if (matchTrunkPrefix(b, ib + 1)
                && matchIntlPrefixAndCC(a, ia +1)
                ) {
            return true;
        }
        /**
         * added to support russian number formats
         */
        if (matchRussianPrefix(a, ia + 1) || matchRussianPrefix(b, ib +1))
            return true;

        return false;
    }

    /** index of the last character of the network portion
     *  (eg anything after is a post-dial string)
     */
    static private int indexOfLastNetworkChar(String a) {
        int pIndex, wIndex;
        int origLength;
        int trimIndex;

        origLength = a.length();

        pIndex = a.indexOf(PAUSE);
        wIndex = a.indexOf(WAIT);

        trimIndex = minPositive(pIndex, wIndex);

        if (trimIndex < 0) {
            return origLength - 1;
        } else {
            return trimIndex - 1;
        }
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD  */
    public final static boolean isDialable(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+' || c == WILD;
    }

    /** all of 'a' up to len must match non-US trunk prefix ('0') */
    private static boolean matchTrunkPrefix(String a, int len) {
        boolean found;

        found = false;

        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            if (c == '0' && !found) {
                found = true;
            } else if (isNonSeparator(c)) {
                return false;
            }
        }

        return found;
    }

    /** True if c is ISO-LATIN characters 0-9, *, # , +, WILD, WAIT, PAUSE   */
    public final static boolean isNonSeparator(char c) {
        return (c >= '0' && c <= '9') || c == '*' || c == '#' || c == '+'
                || c == WILD || c == WAIT || c == PAUSE;
    }

    /** or -1 if both are negative */
    static private int minPositive (int a, int b) {
        if (a >= 0 && b >= 0) {
            return (a < b) ? a : b;
        } else if (a >= 0) { /* && b < 0 */
            return a;
        } else if (b >= 0) { /* && a < 0 */
            return b;
        } else { /* a < 0 && b < 0 */
            return -1;
        }
    }

    /**
     * Phone numbers are stored in "lookup" form in the database
     * as reversed strings to allow for caller ID lookup
     *
     * This method takes a phone number and makes a valid SQL "LIKE"
     * string that will match the lookup form
     *
     */
    /** all of a up to len must be an international prefix or
     *  separators/non-dialing digits
     */
    private static boolean matchIntlPrefix(String a, int len) {
        /* '([^0-9*#+pwn]\+[^0-9*#+pwn] | [^0-9*#+pwn]0(0|11)[^0-9*#+pwn] )$' */
        /*        0       1                           2 3 45               */

        int state = 0;
        for (int i = 0 ; i < len ; i++) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                    break;

                default:
                    if (isNonSeparator(c)) return false;
                    break;

            }
        }

        return state == 1 || state == 3 || state == 5;
    }

    /**
     * phone # starts in russian style 8
     */
    private static boolean matchRussianPrefix(String a, int len) {
        for(int i=0; i < len; i++) {
            char c = a.charAt(0);
            if(Character.isSpaceChar(c))
                continue;
            if(c == '8')
                return true;
        }
        return false;
    }

    /** all of 'a' up to len must be a (+|00|011)country code)
     *  We're fast and loose with the country code. Any \d{1,3} matches */
    private static boolean
    matchIntlPrefixAndCC(String a, int len) {
        /*  [^0-9*#+pwn]*(\+|0(0|11)\d\d?\d? [^0-9*#+pwn] $ */
        /*      0          1 2 3 45  6 7  8                 */

        int state = 0;
        for (int i = 0 ; i < len ; i++ ) {
            char c = a.charAt(i);

            switch (state) {
                case 0:
                    if      (c == '+') state = 1;
                    else if (c == '0') state = 2;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 2:
                    if      (c == '0') state = 3;
                    else if (c == '1') state = 4;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 4:
                    if      (c == '1') state = 5;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 1:
                case 3:
                case 5:
                    if      (isISODigit(c)) state = 6;
                    else if (isNonSeparator(c)) return false;
                    break;

                case 6:
                case 7:
                    if      (isISODigit(c)) state++;
                    else if (isNonSeparator(c)) return false;
                    break;

                default:
                    if (isNonSeparator(c)) return false;
            }
        }

        return state == 6 || state == 7 || state == 8;
    }

    /** True if c is ISO-LATIN characters 0-9 */
    public static boolean
    isISODigit (char c) {
        return c >= '0' && c <= '9';
    }

    public String getContactKey()
    {
        return contactKey;
    }

    public void setContactKey(String contactKey)
    {
        this.contactKey=contactKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
