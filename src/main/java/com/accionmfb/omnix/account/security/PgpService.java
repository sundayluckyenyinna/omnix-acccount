package com.accionmfb.omnix.account.security;

import com.accionmfb.omnix.account.payload.GenericPayload;
import com.accionmfb.omnix.account.payload.ValidationPayload;



/**
 *
 * @author dofoleta
 */
public interface  PgpService {
    public boolean generateKeyPairRSA(String userId, String privateKeyPassword, String publicKeyFileName, String privateKeyFileName);
    public String encryptString(String stringToEncrypt, String recipientPublicKeyFile);
    public String decryptString(String stringToDecrypt, String myPrivateKeyFile, String myPrivateKeyPassword);
    
    public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
