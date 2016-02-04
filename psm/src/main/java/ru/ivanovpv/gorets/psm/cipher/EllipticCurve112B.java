package ru.ivanovpv.gorets.psm.cipher;

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 22.10.13
 * Time: 9:25
 * To change this template use File | Settings | File Templates.
 */
public final class EllipticCurve112B extends EllipticCurve {

    public EllipticCurve112B(byte[] privateKey) {
          super(privateKey, NativeLib.EC_GROUP_112B);
      }

    @Override
    public char getType() {
        return KEY_EXCHANGE_ELLIPTIC_CURVE_112B;
    }
}
