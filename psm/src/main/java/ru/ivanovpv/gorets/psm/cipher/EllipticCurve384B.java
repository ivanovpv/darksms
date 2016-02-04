package ru.ivanovpv.gorets.psm.cipher;

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 22.10.13
 * Time: 9:29
 * To change this template use File | Settings | File Templates.
 */
public class EllipticCurve384B extends EllipticCurve {

    public EllipticCurve384B(byte[] privateKey) {
          super(privateKey, NativeLib.EC_GROUP_384B);
      }

    @Override
    public char getType() {
        return KEY_EXCHANGE_ELLIPTIC_CURVE_384B;
    }
}
