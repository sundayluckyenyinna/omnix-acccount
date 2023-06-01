package com.accionmfb.omnix.account.security;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author dofoleta
 */
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    JwtUtil jwtUtil;
    Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    @Override
    public void logInfo(String app, String token, String logMessage, String logType, String requestId) {
        logger.info(prepareLog(app, token, logMessage, logType, requestId));
    }

    @Override
    public void logError(String app, String token, String logMessage, String logType, String requestId) {
        logger.error(prepareLog(app, token, logMessage, logType, requestId));
    }

    private String prepareLog(String app, String token, String logMessage, String logType, String requestId) {
        try {
            String requestBy = jwtUtil.getUsernameFromToken(token);
            String remoteIP = jwtUtil.getIPFromToken(token);
            String channel = jwtUtil.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase(Locale.ENGLISH));
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase(Locale.ENGLISH)).append(":").append(requestBy.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(app.toUpperCase(Locale.ENGLISH).toUpperCase(Locale.ENGLISH)).append(":").append(requestId.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            return strBuilder.toString();

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
        return "";
    }
//    @Override
//    public void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status) {
//        UserActivity newActivity = new UserActivity();
//        newActivity.setCustomerId(accountNumber);
//        newActivity.setActivity(activity);
//        newActivity.setAmount(amount);
//        newActivity.setChannel(channel);
//        newActivity.setCreatedAt(LocalDateTime.now());
//        newActivity.setMessage(message);
//        newActivity.setMobileNumber(mobileNumber);
//        newActivity.setStatus(status);
//        customerRepository.createUserActivity(newActivity);
//    }

}
