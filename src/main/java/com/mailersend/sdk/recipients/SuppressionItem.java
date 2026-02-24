/*************************************************
 * MailerSend Java SDK
 * https://github.com/mailersend/mailersend-java
 * 
 * @author MailerSend <support@mailersend.com>
 * https://mailersend.com
 **************************************************/
package com.mailersend.sdk.recipients;

import static java8.CompatUtil.isBlank;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * <p>SuppressionItem class.</p>
 *
 * @author mailersend
 * @version $Id: $Id
 */
public class SuppressionItem {

    @SerializedName("id")
    public String id;
    
    @SerializedName("reason")
    public String reason;
    
    @SerializedName("created_at")
    private String createdAtString;
    
    @SerializedName("recipient")
    public Recipient recipient;

    public Date createdAt;

    /**
     * Does all the needed actions after deserialization
     */
    public void postDeserialize() {
        parseDates();
    }
    
    /**
     * Converts the retrieved dates to java.util.Date
     */
    protected void parseDates() {
        
        TemporalAccessor ta;
        Instant instant;
        
        if (createdAtString != null && !isBlank(createdAtString)) {
            
            ta = DateTimeFormatter.ISO_INSTANT.parse(createdAtString);
            instant = Instant.from(ta);
            createdAt = Date.from(instant);
        }
        
        if (recipient != null) {
        	recipient.parseDates();
        }
    }
}
