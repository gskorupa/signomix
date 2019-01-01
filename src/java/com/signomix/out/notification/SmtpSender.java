/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.out.notification;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.cricketmsf.Adapter;
import org.cricketmsf.Event;
import org.cricketmsf.Kernel;
import org.cricketmsf.out.OutboundAdapter;

/**
 *
 * @author greg
 */
public class SmtpSender extends OutboundAdapter implements EmailSenderIface, Adapter {

    String from = "signode@signocom.com";
    String cc = null;
    String bcc = null;
    String mailhost = "85.128.192.150";
    String mailer = "msgsend";
    String protocol = "SMTP";
    String user = "signode@signocom.com";
    String password = "Ypsylon#2017#";
    String debugSession = null;
    boolean ready = true;
    boolean startTls = false;

    protected HashMap<String, String> statusMap = null;

    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {

        from = properties.getOrDefault("from", "");
        cc = properties.getOrDefault("cc", "");
        bcc = properties.getOrDefault("bcc", "");
        mailhost = properties.getOrDefault("mailhost", "");
        mailer = properties.getOrDefault("mailer", "signode");
        protocol = properties.getOrDefault("protocol", "SMTP");
        user = properties.getOrDefault("user", "");
        password = properties.getOrDefault("password", "");
        debugSession = properties.getOrDefault("debug-session", "false");
        //startTls = Boolean.getBoolean(properties.getOrDefault("starttls", "false"));
        startTls=false;
        if (from.isEmpty() || mailhost.isEmpty() || user.isEmpty() || password.isEmpty()) {
            ready = false;
        }
        Kernel.getInstance().getLogger().print("\tfrom: " + from);
        Kernel.getInstance().getLogger().print("\tcc: " + cc);
        Kernel.getInstance().getLogger().print("\tbcc: " + bcc);
        Kernel.getInstance().getLogger().print("\tmailhost: " + mailhost);
        Kernel.getInstance().getLogger().print("\tprotocol: " + protocol);
        Kernel.getInstance().getLogger().print("\tmailer: " + mailer);
        Kernel.getInstance().getLogger().print("\tuser: " + user);
        Kernel.getInstance().getLogger().print("\tstarttls: " + startTls);
        Kernel.getInstance().getLogger().print("\tpassword: " + (password.isEmpty() ? "" : "******"));
        Kernel.getInstance().getLogger().print("\tdebug-session: " + debugSession);
        //hasło do kont test@signocom.com signode@signocom.com = Ypsylon#2017#
    }

    @Override
    public String send(String recipient, String topic, String content) {
        if (!ready) {
            Kernel.handle(Event.logWarning(this.getClass().getSimpleName(), "not configured"));
            return "ERROR: not configured";
        }
        if (recipient == null || recipient.isEmpty()) {
            return null;
        }
        boolean debug = "true".equalsIgnoreCase(debugSession);
        String subject = topic;
        String to = recipient;
        String text = content;

        String result = "OK";

        //System.out.println("mailhost:[" + mailhost + "]");
        try {

            /*
	     * Initialize the JavaMail Session.
             */
            Properties props = System.getProperties();
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            if (startTls) {
                props.put("mail.smtp.host", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", mailhost);
                props.put("mail.smtp.port", "587");
            }else{
                props.put("mail.smtp.host", mailhost);
                props.put("mail.smtp.socketFactory.port", "465");
            }
            props.put("mail.smtp.auth", "true");

            // Get a Session object
            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            });

            if (debug) {
                session.setDebug(true);
            }

            /*
	     * Construct the message and send it.
             */
            Message msg = new MimeMessage(session);
            if (from != null) {
                msg.setFrom(new InternetAddress(from));
            } else {
                msg.setFrom();
            }

            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to, false));
            if (cc != null) {
                msg.setRecipients(Message.RecipientType.CC,
                        InternetAddress.parse(cc, false));
            }
            if (bcc != null) {
                msg.setRecipients(Message.RecipientType.BCC,
                        InternetAddress.parse(bcc, false));
            }

            msg.setSubject(subject);
            //msg.setText(text);
            msg.setContent(text, "text/html");

            msg.setHeader("X-Mailer", mailer);
            msg.setHeader("XPriority", "1");
            msg.setSentDate(new Date());

            // send the thing off
            if (mailhost.contains("office")) {
                Transport transport = session.getTransport("smtp");
                transport.connect (mailhost, 587, user, password);
                transport.sendMessage(msg, msg.getAllRecipients());
                transport.close();
            } else {
                Transport.send(msg);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Kernel.handle(Event.logWarning(this.getClass().getSimpleName(), e.getMessage()));
            result = "ERROR: " + e.getMessage();
        }

        return result;
    }

    @Override
    public void updateStatusItem(String key, String value) {
        statusMap.put(key, value);
    }

    @Override
    public Map<String, String> getStatus(String name) {
        if (statusMap == null) {
            statusMap = new HashMap();
            statusMap.put("name", name);
            statusMap.put("class", getClass().getName());
        }
        return statusMap;
    }
}
