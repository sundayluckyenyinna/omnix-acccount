
package com.accionmfb.omnix.account.security;

import com.accionmfb.omnix.account.payload.GenericPayload;
import com.accionmfb.omnix.account.payload.ValidationPayload;



/**
 *
 * @author dofoleta
 */
public interface AesService {
    
    public String encryptString(String textToEncrypt, String encryptionKey);
    public String decryptString(String textToDecrypt, String encryptionKey);
    
     public String encryptFlutterString(String strToEncrypt, String secret) ;
     public String decryptFlutterString(final String textToDecrypt, final String encryptionKey);
     
     public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
