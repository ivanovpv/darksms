package ru.ivanovpv.gorets.psm.cipher;

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 22.10.13
 * Time: 9:28
 * To change this template use File | Settings | File Templates.
 */
public class EllipticCurve256B extends EllipticCurve {
    public EllipticCurve256B(byte[] privateKey) {
          super(privateKey, NativeLib.EC_GROUP_256B);
      }

    @Override
    public char getType() {
        return KEY_EXCHANGE_ELLIPTIC_CURVE_256B;
    }

}
