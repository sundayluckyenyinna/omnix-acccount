/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account.service;

import com.accionmfb.omnix.account.jwt.JwtTokenUtil;
import com.accionmfb.omnix.account.model.Branch;
import com.accionmfb.omnix.account.model.UserActivity;
import com.accionmfb.omnix.account.payload.AccountStatementPayload;
import com.accionmfb.omnix.account.payload.ChargeTypes;
import com.accionmfb.omnix.account.payload.FundsTransferRequestPayload;
import com.accionmfb.omnix.account.payload.LocalTransferWithInternalPayload;
import com.accionmfb.omnix.account.payload.NotificationPayload;
import com.accionmfb.omnix.account.payload.SMSRequestPayload;
import com.accionmfb.omnix.account.repository.AccountRepository;
import com.google.gson.Gson;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
//import com.itextpdf.kernel.color.Color;
//import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.color.Color;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
//import com.itextpdf.layout.border.Border;
//import com.itextpdf.layout.border.SolidBorder;
import com.itextpdf.layout.border.Border;
import com.itextpdf.layout.border.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import com.jbase.jremote.DefaultJConnectionFactory;
import com.jbase.jremote.JConnection;
import com.jbase.jremote.JDynArray;
import com.jbase.jremote.JResultSet;
import com.jbase.jremote.JStatement;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.temenos.tocf.t24ra.T24Connection;
import com.temenos.tocf.t24ra.T24DefaultConnectionFactory;
import com.temenos.tocf.t24ra.T24Exception;
import java.io.File;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */

@Slf4j
@Service
public class GenericServiceImpl implements GenericService {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    NotificationService notificationService;
    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;
    @Value("${omnix.mail.username}")
    private String mailUsername;
    @Value("${omnix.mail.password}")
    private String mailPassword;
    @Value("${omnix.mail.host}")
    private String mailHost;
    @Value("${omnix.mail.port}")
    private String mailPort;
    @Value("${omnix.mail.protocol}")
    private String mailProtocol;
    @Value("${omnix.mail.trust}")
    private String mailTrust;
    @Value("${server.temp.dir}")
    private String tempDirectory;
    @Value("${omnix.t24.host}")
    private String HOST_ADDRESS;
    @Value("${omnix.t24.port}")
    private String PORT_NUMBER;
    @Value("${omnix.t24.ofs.id}")
    private String OFS_ID;
    @Value("${omnix.t24.ofs.source}")
    private String OFS_STRING;
    private static SecretKeySpec secretKey;
    private static byte[] key;

    @Value("${omnix.middleware.host.local.ip}")
    private String middlewareHostIP;
    @Value("${omnix.middleware.host.local.port}")
    private String middlewareHostPort;
    @Value("${omnix.middleware.authorization}")
    private String middlewareAuthorization;
    @Value("${omnix.middleware.signature.method}")
    private String middlewareSignatureMethod;
    @Value("${omnix.middleware.user.secret}")
    private String middlewareUserSecretKey;
    @Value("${omnix.middleware.username}")
    private String middlewareUsername;

    // Test credentials for Email
    @Value("${zepto.host}")
    private String emailHost;

    @Value("${zepto.port}")
    private String emailPort;

    @Value("${zepto.username}")
    private String emailUsername;

    @Value("${zepto.password}")
    private String emailPassword;

    @Value("${zepto.from}")
    private String emailFrom;

    @Autowired
    Gson gson;
    @Autowired
    MessageSource messageSource;
    Logger logger = LoggerFactory.getLogger(GenericServiceImpl.class);

    @Override
    public void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId) {
        try {
            String requestBy = jwtToken.getUsernameFromToken(token);
            String remoteIP = jwtToken.getIPFromToken(token);
            String channel = jwtToken.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase(Locale.ENGLISH));
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase(Locale.ENGLISH)).append(":").append(requestBy.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(app.toUpperCase(Locale.ENGLISH).toUpperCase(Locale.ENGLISH)).append(":").append(requestId.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            if ("INFO".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isInfoEnabled()) {
                    logger.info(strBuilder.toString());
                }
            }

            if ("DEBUG".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isDebugEnabled()) {
                    logger.error(strBuilder.toString());
                }
            }

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
    }

    @Override
    public void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status) {
        UserActivity newActivity = new UserActivity();
        newActivity.setCustomerId(accountNumber);
        newActivity.setActivity(activity);
        newActivity.setAmount(amount);
        newActivity.setChannel(channel);
        newActivity.setCreatedAt(LocalDateTime.now());
        newActivity.setMessage(message);
        newActivity.setMobileNumber(mobileNumber);
        newActivity.setStatus(status);

        accountRepository.createUserActivity(newActivity);
    }

    @Override
    public String postToT24(String requestBody) {
        T24Connection t24Connection = null;
        String ofsResponse = "";
        try {

            T24DefaultConnectionFactory connectionFactory = new T24DefaultConnectionFactory();
            connectionFactory.setHost(HOST_ADDRESS);
            connectionFactory.setPort(Integer.valueOf(PORT_NUMBER));
            connectionFactory.enableCompression();

            Properties properties = new Properties();
            properties.setProperty("allow input", "true");
            properties.setProperty(OFS_STRING, OFS_ID);
            connectionFactory.setConnectionProperties(properties);

            t24Connection = connectionFactory.getConnection();
            ofsResponse = t24Connection.processOfsRequest(requestBody);

        } catch (Exception ex) {
            return ex.getMessage();
        } finally {
            try {
                t24Connection.close();
            } catch (T24Exception ex) {
                java.util.logging.Logger.getLogger(GenericServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return ofsResponse;
    }

    private String hash(String plainText, String algorithm) {
        StringBuilder hexString = new StringBuilder();
        if ("SHA512".equals(algorithm)) {
            algorithm = "SHA-512";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase(Locale.ENGLISH);
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String validateT24Response(String responseString) {
        String responsePayload = null;
        if (responseString.contains("Authentication failed")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.authfailed", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Maximum T24 users")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.maxuser", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Failed to receive message")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.failedmessage", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No records were found") || responseString.contains("No entries for the period")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.norecord", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("INVALID COMPANY SPECIFIED")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.invalidcoy", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("java.lang.OutOfMemoryError")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.outofmem", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Failed to connect to host")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.failedhost", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No Cheques found")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nocheque", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Unreadable")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.unreadable", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("MANDATORY INPUT")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.inputman", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Some errors while encountered")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.someerrors", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("Some override conditions have not been met")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.override", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("don't have permissions to access this data")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nopermission", new Object[0], Locale.ENGLISH);
        }

        if ("<Unreadable>".equals(responseString)) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.unreadable", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("User has no id")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.noid", new Object[0], Locale.ENGLISH);
        }

        if (responseString.equals("java.net.SocketException: Unexpected end of file from server")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.endoffile", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("No Cash available")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.nocash", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("INVALID ACCOUNT")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.invalidaccount", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("MISSING") && !responseString.substring(0, 4).equals("\"//1")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.net.SocketException")
                || responseString.contains("java.net.ConnectException")
                || responseString.contains("java.net.NoRouteToHostException")
                || responseString.contains("Connection timed out")
                || responseString.contains("Connection refused")) {
            responsePayload = responseString;
        }

        if (responseString.contains("SECURITY VIOLATION")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NOT SUPPLIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NO EN\\\"\\t\\\"TRIES FOR PERIOD")) {
            responsePayload = messageSource.getMessage("appMessages.error.messages.norecord", new Object[0], Locale.ENGLISH);
        }

        if (responseString.contains("CANNOT ACCESS RECORD IN ANOTHER COMPANY")) {
            responsePayload = responseString;
        }

        if ("NO DATA PRESENT IN MESSAGE".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("//-1") || responseString.contains("//-2")) {
            responsePayload = responseString;
        }

        if ("RECORD MISSING".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID/ NO SIGN ON NAME SUPPLIED DURING SIGN ON PROCESS")) {
            responsePayload = responseString;
        }

        return responsePayload == null ? null : responsePayload;
    }

    @Override
    public String getT24TransIdFromResponse(String response) {
        String[] splitString = response.split("/");
        return splitString[0].replace("\"", "");
    }

    @Override
    public String getTextFromOFSResponse(String ofsResponse, String textToExtract) {
        try {
            String[] splitOfsResponse = ofsResponse.split(",");
            for (String str : splitOfsResponse) {
                String[] splitText = str.split("=");
                if (splitText[0].equalsIgnoreCase(textToExtract)) {
                    return splitText[1].isBlank() ? "" : splitText[1].trim();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    @Override
    public String formatDateWithHyphen(String dateToFormat) {
        StringBuilder newDate = new StringBuilder(dateToFormat);
        if (dateToFormat.length() == 8) {
            newDate.insert(4, "-").insert(7, "-");
            return newDate.toString();
        }

        return "";
    }

//    @Override
//    public String generateMnemonic(int max) {
//        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        StringBuilder mnemonic = new StringBuilder();
//        for (int i = 0; i < 5; i++) {
//            for (int t = max; t >= 0; t--) {
//                int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
//                mnemonic.append(ALPHA_NUMERIC_STRING.charAt(character));
//            }
//            if (!Character.isDigit(mnemonic.toString().charAt(0))) {
//                return "A".concat(mnemonic.toString().substring(0, max-1));
//            }
//        }
//        return mnemonic.toString().substring(0, max);
//    }

    @Override
    public String generateMnemonic(int max) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder mnemonic = new StringBuilder();
        Random r = new Random();
        int[] randomNumbers = r.ints(max, 0, 35).toArray();
        for(int i = 0; i < randomNumbers.length; i++){
            int randomIndex = randomNumbers[i];
            char token = ALPHA_NUMERIC_STRING.charAt(randomIndex);
            if(i == 0 && Character.isDigit(token)){
                token = 'A';
            }
            mnemonic.append(token);
        }
        return mnemonic.toString();
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String hashLocalTransferValidationRequest(FundsTransferRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getAisInputter().trim());
        rawString.add(requestPayload.getAisAuthorizer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashLocalTransferWithInternalValidationRequest(LocalTransferWithInternalPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber());
        rawString.add(requestPayload.getDebitAccount());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        if (requestPayload.getChargeTypes() != null) {
            for (ChargeTypes ch : requestPayload.getChargeTypes()) {
                rawString.add(ch.getChargeType());
                rawString.add(ch.getChargeAmount());
            }
        }
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.valueOf(startMorning);
        int morningEnd = Integer.valueOf(endMorning);
        int afternoonStart = Integer.valueOf(startAfternoon);
        int afternoonEnd = Integer.valueOf(endAfternoon);
        int eveningStart = Integer.valueOf(startEvening);
        int eveningEnd = Integer.valueOf(endEvening);
        int nightStart = Integer.valueOf(startNight);
        int nightEnd = Integer.valueOf(endNight);
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }

    @Override
    public Branch getBranchUsingBranchCode(String branchCode) {
        return accountRepository.getBranchUsingBranchCode(branchCode);
    }

    @Override
    public String hashSMSNotificationRequest(SMSRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getMessage().trim());
        rawString.add(requestPayload.getSmsFor().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Async
    @Override
    public CompletableFuture<String> sendAccountOpeningSMS(NotificationPayload requestPayload) {
        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Thanks for opening a ");
        smsMessage.append(requestPayload.getAccountType()).append(" account at our ");
        smsMessage.append(requestPayload.getBranch()).append(" branch. Your account number is ");
        smsMessage.append(requestPayload.getAccountNumber()).append(". Your future is Bright. #StaySafe");

        generateLog("SMS", "", smsMessage.toString(), "API SMS Request", "INFO", requestPayload.getRequestId());

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("N");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
        notificationService.smsNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Async
    @Override
    public CompletableFuture<String> sendAccountStatementMail(NotificationPayload requestPayload, List<AccountStatementPayload> statementPayload) {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(emailHost);  // before mailHost
            mailSender.setPort(Integer.parseInt(emailPort));  // before mailPort

            mailSender.setUsername(emailUsername);  //mailUsername
            mailSender.setPassword(emailPassword);  // mailPassword

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", mailProtocol);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "true");
            props.put("mail.smtp.ssl.trust", "smtp.zeptomail.com");

            MimeMessage emailDetails = mailSender.createMimeMessage();
            emailDetails.setFrom(emailFrom);   // mailUsername
            String addresses[] = new String[]{requestPayload.getEmail()};
            Address addrss[] = {};
            List<Address> addressList = new ArrayList<>();
            //Create address out of the emails
            for (String addr : addresses) {
                Address address = new InternetAddress(addr);
                addressList.add(address);
            }
            emailDetails.setRecipients(Message.RecipientType.TO, addressList.toArray(addrss));
            emailDetails.setSubject(requestPayload.getEmailSubject());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(accountStatementMessageBody(requestPayload), "text/html");

            //Add the attachment
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            String filename = generateAccountStatement(requestPayload, statementPayload);
            DataSource source = new FileDataSource(filename);
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName("Account Statement");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentBodyPart);
            emailDetails.setContent(multipart);

            mailSender.send(emailDetails);
            System.out.println("Email sending went well.");
            return CompletableFuture.completedFuture("Success");
        } catch (Exception ex) {
            System.out.println("Error occurred while sending email: " + ex.getMessage());
            generateLog("Account Statement Email", "", ex.getMessage(), "API Request", "INFO", "");
            return CompletableFuture.completedFuture(ex.getMessage());
        }
    }

    private String accountStatementMessageBody(NotificationPayload requestPayload) {
        return "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Account Statement Notification!</b><br/>"
                + "<b>Hello " + requestPayload.getLastName() + ", We have attached your requested transaction statement to this email." + "</b><br/>"
                + "<p>Also, you can always view your transaction history in your Accion Mobile App.</p>"
                + "<br/><br/>"
                + "<p>For further clarification on your statement or transaction details, please contact us on 07000222466</p>"
                + "</div>";
    }

    private String generateAccountStatement(NotificationPayload requestPayload, List<AccountStatementPayload> statementPayload) {
        try {
            String letterHead = tempDirectory.replace("temp", "") + "letter_head.jpg";
            String destinationDirectory = tempDirectory + File.separator + "statement" + File.separator;
            File file = new File(destinationDirectory + requestPayload.getAccountNumber() + "_" + LocalDate.now().toString().replace("-", "") + ".pdf");
            file.createNewFile();
            String dest = destinationDirectory + requestPayload.getAccountNumber() + "_" + LocalDate.now().toString().replace("-", "") + ".pdf";
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc, PageSize.A4);
            PdfCanvas canvas = new PdfCanvas(pdfDoc.addNewPage());

            ImageData logoImageData = ImageDataFactory.create(letterHead);
            Image logoImage = new Image(logoImageData)
                    .setWidth(100)
                    .setHeight(100);
            doc.setFontSize(6f);

            String openingBalance = "";
            String closingBalance = "";
            List<AccountStatementPayload> statementSummary = statementPayload.stream().filter(t -> t.getReferenceNo().equals("Summary")).collect(Collectors.toList());
            for (AccountStatementPayload trans : statementSummary) {
                openingBalance = trans.getOpeningBalance();
                closingBalance = trans.getClosingBalance();
            }

            Table tableHeader = new Table(new float[]{90, 400, 100, 100, 100, 100}).useAllAvailableWidth();
            Cell mainDataHeader = new Cell(1, 6)
                    .setBorder(Border.NO_BORDER).add(new Paragraph("AMFB ACCOUNT STATEMENT")
                    .setFontSize(10));
            Cell logoImageCell = new Cell(1, 6)
                    .setBorder(Border.NO_BORDER).add(logoImage)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setWidth(100).setHeight(100);
            mainDataHeader.setTextAlignment(TextAlignment.CENTER);
            mainDataHeader.setPadding(5);
            mainDataHeader.setBold().setFontColor(new DeviceRgb(234, 118, 0));
            tableHeader.addCell(logoImageCell);
            tableHeader.addCell(mainDataHeader);
            tableHeader.setMargins(90, 10, 0, 100);

            tableHeader.addCell((new Cell()).add("Account Name:").setBorder(Border.NO_BORDER));
            tableHeader.addCell((new Cell()).add(requestPayload.getLastName() + ", " + requestPayload.getOtherName()).setBorder(Border.NO_BORDER));
            tableHeader.addCell((new Cell()).add(" ").setBorder(Border.NO_BORDER));
            tableHeader.addCell((new Cell()).add(" ").setBorder(Border.NO_BORDER));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Account Number:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(requestPayload.getAccountNumber()));

            //Next row
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Branch Name:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(requestPayload.getBranch()));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Currency:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("NGN"));

            //Next row
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Account Type:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(requestPayload.getAccountType()));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Opening Balance:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(openingBalance));

            //Next row
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Print Date:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(requestPayload.getStartDate() + " To " + requestPayload.getEndDate()));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(" "));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add("Available Balance:"));
            tableHeader.addCell((new Cell()).setBorder(Border.NO_BORDER).add(closingBalance));
            doc.add(tableHeader);

            Table table = new Table(new float[]{100, 300, 100, 100, 100, 100}).useAllAvailableWidth();
            table.setMargins(2, 10, 10, 100);
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Value Date"));
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Narration"));
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Post Date"));
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Credits").setTextAlignment(TextAlignment.RIGHT));
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Debits").setTextAlignment(TextAlignment.RIGHT));
            table.addCell((new Cell()).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(Color.DARK_GRAY, 1)).add("Balance").setTextAlignment(TextAlignment.RIGHT));

            List<AccountStatementPayload> transactionRecords = (List<AccountStatementPayload>) statementPayload;
            for (AccountStatementPayload trans : transactionRecords) {
                if (trans.getId() != 99999) {
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getValueDate()));
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getRemarks()));
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getTransDate()));
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getCredits()).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getDebits()).setTextAlignment(TextAlignment.RIGHT));
                    table.addCell((new Cell()).setBorder(Border.NO_BORDER).add(trans.getBalance()).setTextAlignment(TextAlignment.RIGHT));
                }
            }

            // Add image
            String imagePath =  tempDirectory + "/stamp.jpg";
            ImageData imageData = ImageDataFactory.create(imagePath);
            Image stampImage = new Image(imageData);
            stampImage.setWidth(100).setHeight(100);
            table.addCell(new Cell(1,6).setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                    .setWidth(100).setHeight(100)
                    .add(stampImage));

            doc.add(table);
            doc.close();
            return tempDirectory + File.separator + "statement" + File.separator + requestPayload.getAccountNumber() + "_" + LocalDate.now().toString().replace("-", "") + ".pdf";
        } catch (Exception ex) {
            System.out.println("Error occurred while trying to generate account statement");
            return ex.getMessage();
        }
    }

    @Override
    public String getBranchCode(String accountNumber) {
        try {
            String jQLQuery = "LIST FBNK.ACCOUNT CO.CODE WITH @ID='" + accountNumber + "' OR ALT.ACCT.ID='" + accountNumber + "'";
            DefaultJConnectionFactory cfx = new DefaultJConnectionFactory();
            cfx.setHost(HOST_ADDRESS);
            cfx.setPort(Integer.valueOf(PORT_NUMBER));
            JConnection cx = cfx.getConnection();
            JStatement jStatement = cx.createStatement();
            jStatement.setFetchSize(1);
            JResultSet resultList = jStatement.execute(jQLQuery);
            String branchCode = "";
            while (resultList.next()) {
                JDynArray row = resultList.getRow();
                branchCode = row.get(2);
            }

            cx.close();
            return branchCode;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String formatOfsUserCredentials(String ofs, String userCredentials) {
        String[] userCredentialsSplit = userCredentials.split("/");
        String newUserCredentials = userCredentialsSplit[0] + "/#######";
        String newOfsRequest = ofs.replace(userCredentials, newUserCredentials);
        return newOfsRequest;
    }

    @Override
    public String postToMiddleware(String requestEndPoint, String requestBody) {
        log.info("Middleware host: {}", middlewareHostIP);
        log.info("Middleware port: {}", middlewareHostPort);

        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (UnirestException ex) {
            return ex.getMessage();
        }
    }
    
    @Override
    public String postToSMSService(String requestEndPoint, String requestBody) {
        log.info("SMS Host: {}", middlewareHostIP);
        log.info("SMS Port: {}", middlewareHostPort);
        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (UnirestException ex) {
            return ex.getMessage();
        }
    }

    @Async
    @Override
    public CompletableFuture<String> sendSMS(NotificationPayload requestPayload) {
        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Thanks for opening a ");
        smsMessage.append(requestPayload.getAccountType()).append(" account at our ");
        smsMessage.append(requestPayload.getBranch()).append(" branch. Your account number is ");
        smsMessage.append(requestPayload.getAccountNumber()).append(". Your future is Bright. #StaySafe");

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("C");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setToken(requestPayload.getToken());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
        notificationService.smsNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("");
    }

    @Override
    public String getDateStringFromUnformattedDate(String unFormattedDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy");
        String formattedDateString = preParseDateString(unFormattedDate);
        if (formattedDateString == null) {
            return null;
        }
        return LocalDate.parse(formattedDateString, formatter).toString();
    }

    private static String preParseDateString(String dateString) {
        if (dateString == null) {
            return null;
        }

        dateString = dateString.trim().replace(" ", "");

        if (dateString.length() != 7) {
            return null;
        }
        String day = dateString.substring(0, 2);
        String month = getMonthNumber(dateString.substring(2, 5));
        String year = dateString.substring(5, 7);
        return String.join("-", day, month, year);
    }

    private static String getMonthNumber(String month) {
        List<String> months = Arrays.asList(
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        );
        String monthNumber = "";
        for (int i = 0; i < months.size(); i++) {
            String current = months.get(i);
            if (current.equalsIgnoreCase(month)) {
                int index = i + 1;
                if (String.valueOf(index).length() < 2) {
                    monthNumber = "0" + String.valueOf(index);
                } else {
                    monthNumber = String.valueOf(index);
                }

            }
        }

        return monthNumber;
    }

}
