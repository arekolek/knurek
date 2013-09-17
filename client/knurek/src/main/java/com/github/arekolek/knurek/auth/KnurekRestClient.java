package com.github.arekolek.knurek.auth;

import com.googlecode.androidannotations.annotations.rest.Accept;
import com.googlecode.androidannotations.annotations.rest.Get;
import com.googlecode.androidannotations.annotations.rest.Rest;
import com.googlecode.androidannotations.api.rest.MediaType;

import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Rest(rootUrl = Constants.ROOT_URL, converters = {
        GsonHttpMessageConverter.class
})
@Accept(MediaType.APPLICATION_JSON)
public interface KnurekRestClient {

    @Get("/api/auth/")
    Auth getIdentifier();

    @Get("/api/auth/?identifier={identifier}")
    Auth getUsername(String identifier);

    RestTemplate getRestTemplate();

    void setRestTemplate(RestTemplate restTemplate);

}