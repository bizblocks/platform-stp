package com.groupstp.platform.web.email;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.EmailAttachment;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Email send UI dialog
 *
 * @author adiatullin
 */
public class EmailDialog extends AbstractWindow {
    private static final Logger log = LoggerFactory.getLogger(EmailDialog.class);

    public static final String SCREEN_ID = "platform-email-dialog";

    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String SUBJECT = "subject";
    private static final String MESSAGE = "message";
    private static final String DETAILS = "details";

    /**
     * Show email send dialog to user
     *
     * @param frame   calling UI frame
     * @param from    email address
     * @param to      emails address
     * @param subject email title
     * @param message email message
     * @return opened email dialog
     */
    public static EmailDialog show(Frame frame, @Nullable String from, Set<String> to, @Nullable String subject, @Nullable String message) {
        return show(frame, from, to, subject, message, null);
    }

    /**
     * Show email send dialog to user
     *
     * @param frame   calling UI frame
     * @param from    email address
     * @param to      emails address
     * @param subject email title
     * @param message email message
     * @param details additional details to show (like a tips)
     * @return opened email dialog
     */
    public static EmailDialog show(Frame frame, @Nullable String from, @Nullable Set<String> to, @Nullable String subject, @Nullable String message, @Nullable String details) {
        Preconditions.checkNotNullArgument(frame, "Frame is empty");

        return (EmailDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(FROM, from, TO, to, SUBJECT, subject, MESSAGE, message, DETAILS, details));
    }

    @Inject
    private EmailService emailService;

    @Inject
    private TextField from;
    @Inject
    private TextField to;
    @Inject
    private TextField subject;
    @Inject
    private FileUploadField attachment;
    @Inject
    private GroupBoxLayout detailsBox;
    @Inject
    private Field detailsField;
    @Inject
    private RichTextArea message;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initData(params);
        initRequirements();
    }

    private void initData(Map<String, Object> params) {
        from.setValue(params.get(FROM));
        //noinspection unchecked
        to.setValue(CollectionUtils.isEmpty((Set) params.get(TO)) ? null : ((Set<String>) params.get(TO)).stream().collect(Collectors.joining(", ")));
        subject.setValue(params.get(SUBJECT));
        message.setValue(params.get(MESSAGE));
        if (params.get(DETAILS) != null) {
            detailsField.setValue(params.get(DETAILS));
        }
        detailsBox.setVisible(!StringUtils.isEmpty(detailsField.getValue()));
    }

    private void initRequirements() {
        String requireFormat = messages.getMainMessage("validation.required.defaultMsg");

        to.setRequired(true);
        to.setRequiredMessage(String.format(requireFormat, getMessage("emailDialog.to")));

        subject.setRequired(true);
        subject.setRequiredMessage(String.format(requireFormat, getMessage("emailDialog.subject")));
    }

    public void onSend() {
        if (validateAll()) {
            try {
                String fromAddress = ((String) from.getValue()).replaceAll("\\s", StringUtils.EMPTY);
                String toAddresses = ((String) to.getValue()).replaceAll("\\s", StringUtils.EMPTY);
                String subjectText = subject.getValue();

                EmailAttachment emailAttachment = null;
                FileDescriptor fd = attachment.getFileDescriptor();
                if (fd != null) {
                    //noinspection ConstantConditions
                    emailAttachment = new EmailAttachment(IOUtils.toByteArray(attachment.getFileContent()), fd.getName());
                }

                String messageBody = message.getValue();
                if (!StringUtils.isEmpty(messageBody) && !messageBody.startsWith("<html>")) {//correct layout
                    messageBody = "<html><body>" + messageBody + "</body></html>";
                }

                EmailInfo emailInfo;
                if (emailAttachment != null) {
                    emailInfo = new EmailInfo(toAddresses, subjectText, fromAddress, messageBody, EmailInfo.HTML_CONTENT_TYPE, emailAttachment);
                } else {
                    emailInfo = new EmailInfo(toAddresses, subjectText, fromAddress, messageBody, EmailInfo.HTML_CONTENT_TYPE);
                }

                emailService.sendEmail(emailInfo);
                showNotification(getMessage("emailDialog.sent"), NotificationType.HUMANIZED);

                close(COMMIT_ACTION_ID, true);
            } catch (Exception e) {
                log.error("Failed to send email", e);

                showNotification(getMessage("emailDialog.error.caption"),
                        String.format(getMessage("emailDialog.error"),
                                StringUtils.isEmpty(e.getLocalizedMessage()) ? StringUtils.EMPTY : e.getLocalizedMessage()),
                        NotificationType.WARNING);
            }
        }
    }

    public void onClose() {
        close(CLOSE_ACTION_ID);
    }
}
