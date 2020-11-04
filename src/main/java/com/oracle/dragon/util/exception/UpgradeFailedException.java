package com.oracle.dragon.util.exception;

import java.io.IOException;
import java.net.URISyntaxException;

public class UpgradeFailedException extends DSException {
    public UpgradeFailedException(int statusCode) {
        super(ErrorCode.UpgradeFailed,String.format("Upgrade failed with error code %d", statusCode));
    }

    public UpgradeFailedException(URISyntaxException e) {
        super(ErrorCode.UpgradeFailed,"Upgrade failed because of wrong URL!",e);
    }

    public UpgradeFailedException(IOException e) {
        super(ErrorCode.UpgradeFailed,"Upgrade failed because of network communication error!",e);
    }

    public UpgradeFailedException(String msg) {
        super(ErrorCode.UpgradeFailed,String.format("Upgrade failed because of %s", msg));
    }

    public UpgradeFailedException(String link, int statusCode) {
        super(ErrorCode.UpgradeFailed,String.format("Upgrade failed with error code %d while downloading %s", statusCode, link));
    }
}
