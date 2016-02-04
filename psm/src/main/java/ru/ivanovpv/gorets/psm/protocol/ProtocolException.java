package ru.ivanovpv.gorets.psm.protocol;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 294 $
 *   $LastChangedDate: 2013-08-05 17:12:11 +0400 (Пн, 05 авг 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolException.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 12.09.12
 * Time: 15:02
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolException extends Exception{
    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable th) {
        super(message, th);
    }
}
