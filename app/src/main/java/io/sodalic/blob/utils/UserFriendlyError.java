package io.sodalic.blob.utils;

/**
 * This a marker interface supposed to be used on custom
 * {@link Exception} that provide a user-friendly error text
 * that can be shown to the user.
 */
public interface UserFriendlyError {
    public String getMessage();
}
