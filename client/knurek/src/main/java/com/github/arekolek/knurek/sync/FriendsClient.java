
package com.github.arekolek.knurek.sync;

import com.github.arekolek.knurek.auth.Constants;
import com.googlecode.androidannotations.annotations.rest.Get;
import com.googlecode.androidannotations.annotations.rest.Rest;

import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Rest(rootUrl = Constants.ROOT_URL, converters = {
        GsonHttpMessageConverter.class, ByteArrayHttpMessageConverter.class
})
public interface FriendsClient {

    @Get("/api/friends/")
    FriendList getFriends();

    @Get("/api/friends/{friend}/avatar")
    byte[] getAvatar(String friend);

    RestTemplate getRestTemplate();

    void setRestTemplate(RestTemplate restTemplate);

}
