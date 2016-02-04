package ru.ivanovpv.gorets.psm.mms;

/**
 * Created by pivanov on 11.02.2015.
 * Small helper class to do with MMS Access Points
 */
public class APN {
    public String MMSCenterUrl;
    public String MMSPort;
    public String MMSProxy;

    public APN(String MMSCenterUrl, String MMSPort, String MMSProxy) {
        this.MMSCenterUrl = MMSCenterUrl;
        this.MMSPort = MMSPort;
        this.MMSProxy = MMSProxy;
    }

    public APN() {
        MMSCenterUrl = "";
        MMSPort = "";
        MMSProxy = "";
    }
}