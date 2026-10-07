package io.ticket.common.mail;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * SMTP through Spring Mail: multipart/alternative (text then html), fixed Message-ID, no Reply-To
 * (DOC-27 §7).
 */
@Component
public class SmtpMailSender implements MailSender {

  private final JavaMailSender mailSender;
  private final MailProperties properties;

  public SmtpMailSender(JavaMailSender mailSender, MailProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
    if (mailSender instanceof JavaMailSenderImpl impl) {
      String millis = Long.toString(properties.smtp().timeout().toMillis());
      var javaMail = impl.getJavaMailProperties();
      javaMail.put("mail.smtp.connectiontimeout", millis);
      javaMail.put("mail.smtp.timeout", millis);
      javaMail.put("mail.smtp.writetimeout", millis);
    }
  }

  @Override
  public void send(OutgoingEmail email) {
    try {
      mailSender.send(build(email));
    } catch (MailException e) {
      throw new EmailDeliveryException(
          "SMTP send failed: " + e.getClass().getSimpleName(), isPermanent(e), e);
    } catch (MessagingException | UnsupportedEncodingException e) {
      throw new EmailDeliveryException(
          "SMTP message could not be built: " + e.getClass().getSimpleName(), false, e);
    }
  }

  private MimeMessage build(OutgoingEmail email)
      throws MessagingException, UnsupportedEncodingException {
    Session session =
        mailSender instanceof JavaMailSenderImpl impl
            ? impl.getSession()
            : Session.getInstance(new java.util.Properties());
    String id = "<" + email.messageId() + ">";
    MimeMessage message =
        new MimeMessage(session) {
          @Override
          protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", id);
          }
        };
    message.setFrom(new InternetAddress(properties.from(), false));
    message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email.to(), false));
    message.setSubject(email.content().subject(), "UTF-8");
    message.setHeader("Content-Language", email.locale().getLanguage());

    var alternative = new MimeMultipart("alternative");
    var plain = new MimeBodyPart();
    plain.setText(email.content().text(), "UTF-8", "plain");
    var rich = new MimeBodyPart();
    rich.setText(email.content().html(), "UTF-8", "html");
    alternative.addBodyPart(plain);
    alternative.addBodyPart(rich);
    message.setContent(alternative);
    return message;
  }

  /** A 5xx refusal of the recipient will not get better with time. */
  private static boolean isPermanent(MailException e) {
    for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
      if (t instanceof SendFailedException failed) {
        Address[] invalid = failed.getInvalidAddresses();
        if (invalid != null && invalid.length > 0) {
          return true;
        }
      }
    }
    return false;
  }
}
