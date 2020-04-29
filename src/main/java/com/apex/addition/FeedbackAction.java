package com.apex.addition;

public enum FeedbackAction {

    BAN("ban"),
    WHITELIST("whitelist"),
    IGNORE("ignore");

    private final String action;

    FeedbackAction(final String action){
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
