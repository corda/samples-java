package net.corda.samples.chainmail.flows;

import net.corda.core.serialization.SerializationWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.*;


// Serialization whitelist.
public class TemplateSerializationWhitelist implements SerializationWhitelist {
    @NotNull
    @Override
    public List<Class<?>> getWhitelist() {
        return Arrays.asList(Content.class, Email.class, Mail.class, SendGrid.class);
    }

}
