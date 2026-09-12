package com.umc.product.notification.adapter.out.external.smtp;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.util.MailConnectException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;

/** SMTP 예외에서 원문과 주소를 보존하지 않고 진단에 필요한 분류와 숫자 코드만 추출한다. */
record SmtpFailureDetails(
    String errorReason,
    String causeErrorClass,
    String failureStage,
    Integer smtpResponseCode,
    String smtpEnhancedStatusCode
) {

    private static final int MAX_CAUSES = 32;
    private static final Pattern SMTP_REPLY = Pattern.compile(
        "\\A([45][0-9]{2})(?:[ -]([245]\\.[0-9]{1,3}\\.[0-9]{1,3})(?=\\s|$))?(?=\\s|-|$)"
    );

    static SmtpFailureDetails from(Exception exception) {
        List<Cause> causes = causes(exception);
        Cause selected = causes.getFirst();
        String reason = "UNKNOWN";
        for (Cause cause : causes) {
            String candidateReason = reason(cause.exception());
            if (priority(candidateReason) >= priority(reason)) {
                selected = cause;
                reason = candidateReason;
            }
        }

        String failureStage = "UNKNOWN";
        Integer responseCode = null;
        String enhancedStatusCode = null;
        boolean typedResponseCode = false;
        // 다른 메일의 실패나 연결 종료 예외를 선택한 원인의 단계/응답 코드와 섞지 않는다.
        for (Cause path = selected; path != null; path = path.parent()) {
            Throwable cause = path.exception();
            if ("UNKNOWN".equals(failureStage)) {
                failureStage = stage(cause);
            }

            Integer typedCode = responseCode(cause);
            if (typedCode != null && !typedResponseCode) {
                responseCode = typedCode;
                enhancedStatusCode = null;
                typedResponseCode = true;
            }
            Matcher reply = reply(cause);
            if (reply != null) {
                int parsedCode = Integer.parseInt(reply.group(1));
                if (responseCode == null) {
                    responseCode = parsedCode;
                    enhancedStatusCode = reply.group(2);
                } else if (responseCode == parsedCode && enhancedStatusCode == null) {
                    enhancedStatusCode = reply.group(2);
                }
            }
        }

        return new SmtpFailureDetails(reason, selected.exception().getClass().getSimpleName(),
            failureStage, responseCode, enhancedStatusCode);
    }

    private static List<Cause> causes(Throwable exception) {
        List<Cause> causes = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        addCause(exception, null, causes, visited);
        for (int index = 0; index < causes.size(); index++) {
            Cause parent = causes.get(index);
            Throwable cause = parent.exception();
            addCause(cause.getCause(), parent, causes, visited);
            if (cause instanceof MessagingException messagingException) {
                addCause(messagingException.getNextException(), parent, causes, visited);
            }
            if (cause instanceof MailSendException mailSendException) {
                for (Exception messageException : mailSendException.getMessageExceptions()) {
                    addCause(messageException, parent, causes, visited);
                    if (causes.size() == MAX_CAUSES) {
                        break;
                    }
                }
            }
        }
        return causes;
    }

    private static void addCause(Throwable cause, Cause parent, List<Cause> causes, Set<Throwable> visited) {
        if (cause != null && causes.size() < MAX_CAUSES && visited.add(cause)) {
            causes.add(new Cause(cause, parent));
        }
    }

    private static String reason(Throwable cause) {
        if (cause instanceof SSLException) {
            return "TLS_FAILED";
        }
        if (cause instanceof UnknownHostException) {
            return "DNS_FAILED";
        }
        if (cause instanceof SocketTimeoutException) {
            return "TIMEOUT";
        }
        if (cause instanceof ConnectException || cause instanceof MailConnectException) {
            return "CONNECTION_FAILED";
        }
        if (cause instanceof MailAuthenticationException || cause instanceof AuthenticationFailedException) {
            return "AUTHENTICATION_FAILED";
        }
        return responseCode(cause) != null || reply(cause) != null ? "SMTP_REJECTED" : "UNKNOWN";
    }

    private static int priority(String reason) {
        return switch (reason) {
            case "TLS_FAILED", "DNS_FAILED", "TIMEOUT" -> 3;
            case "CONNECTION_FAILED", "AUTHENTICATION_FAILED" -> 2;
            case "SMTP_REJECTED" -> 1;
            default -> 0;
        };
    }

    private static String stage(Throwable cause) {
        if (cause instanceof SSLHandshakeException) {
            return "TLS_HANDSHAKE";
        }
        if (cause instanceof ConnectException || cause instanceof MailConnectException) {
            return "CONNECT";
        }
        if (cause instanceof MailAuthenticationException || cause instanceof AuthenticationFailedException) {
            return "AUTHENTICATE";
        }
        return responseCode(cause) != null ? "SMTP_COMMAND" : "UNKNOWN";
    }

    private static Integer responseCode(Throwable cause) {
        int code = switch (cause) {
            case SMTPSendFailedException smtp -> smtp.getReturnCode();
            case SMTPAddressFailedException smtp -> smtp.getReturnCode();
            default -> -1;
        };
        return code >= 400 && code <= 599 ? code : null;
    }

    private static Matcher reply(Throwable cause) {
        if (cause instanceof MessagingException || cause instanceof MailAuthenticationException) {
            String message = cause.getMessage();
            if (message != null) {
                Matcher reply = SMTP_REPLY.matcher(message);
                return reply.find() ? reply : null;
            }
        }
        return null;
    }

    private record Cause(Throwable exception, Cause parent) {
    }
}
