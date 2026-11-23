package com.itzetech.dpdms.mail;

/** Sends a plain-text e-mail. In development, e-mails are written to the log instead. */
public interface MailService {

    void send(String to, String subject, String body);
}
