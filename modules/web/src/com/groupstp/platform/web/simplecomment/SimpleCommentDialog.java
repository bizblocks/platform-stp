package com.groupstp.platform.web.simplecomment;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.TextArea;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

/**
 * Simple screen to provide user enter some additional comment
 *
 * @author adiatullin
 */
public class SimpleCommentDialog extends AbstractWindow {
    public static final String SCREEN_ID = "platform-simple-comment-dialog";

    private static final String COMMENT_REQUIRED = "comment-required";
    private static final String MAX_LENGTH = "max-length";

    /**
     * Show to user a comment dialog
     *
     * @param frame           calling UI frame
     * @param commentRequired is comment are required to provide or not
     * @return opened dialog instance
     */
    public static SimpleCommentDialog show(Frame frame, boolean commentRequired) {
        return show(frame, commentRequired, null);
    }

    /**
     * Show to user a comment dialog
     *
     * @param frame           calling UI frame
     * @param commentRequired is comment are required to provide or not
     * @param maxLength       maximum length of comment
     * @return opened dialog instance
     */
    public static SimpleCommentDialog show(Frame frame, boolean commentRequired, @Nullable Integer maxLength) {
        Preconditions.checkNotNullArgument(frame, "Frame is empty");

        return (SimpleCommentDialog) frame.openWindow(SCREEN_ID, WindowManager.OpenType.DIALOG,
                ParamsMap.of(COMMENT_REQUIRED, commentRequired, MAX_LENGTH, maxLength));
    }

    @Inject
    private TextArea commentField;

    /**
     * @return user entered comment
     */
    @Nullable
    public String getComment() {
        return commentField.getValue();
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        if (Boolean.TRUE.equals(params.get(COMMENT_REQUIRED))) {
            commentField.setRequired(true);
            commentField.setRequiredMessage(getMessage("simpleCommentDialog.emptyComment"));
        }
        Integer maxLength = (Integer) params.get(MAX_LENGTH);
        if (maxLength != null && maxLength > 0) {
            commentField.setMaxLength(maxLength);
        }
    }

    public void onOk() {
        if (validateAll()) {
            close(COMMIT_ACTION_ID, true);
        }
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID, true);
    }
}
