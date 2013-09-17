
package com.github.arekolek.knurek.auth;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;

public class RestUtils {

    public static void setClientTimeout(KnurekRestClient client, int timeout) {
        ClientHttpRequestFactory factory = client.getRestTemplate().getRequestFactory();
        if (factory instanceof SimpleClientHttpRequestFactory) {
            ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(timeout);
            ((SimpleClientHttpRequestFactory) factory).setReadTimeout(timeout);
        } else if (factory instanceof HttpComponentsClientHttpRequestFactory) {
            ((HttpComponentsClientHttpRequestFactory) factory).setConnectTimeout(timeout);
            ((HttpComponentsClientHttpRequestFactory) factory).setReadTimeout(timeout);
        }
    }

    public static String getErrorMessage(RestClientException e) {
        StringBuilder message = new StringBuilder();
        if (e.getCause() instanceof SocketTimeoutException) {
            message.append("Connection time out: ");
            message.append(e.getCause().getMessage());
        } else {
            message.append("Connection error: ");
            message.append(e.getMessage());
        }
        return message.toString();
    }

}
