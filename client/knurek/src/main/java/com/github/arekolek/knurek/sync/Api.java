package com.github.arekolek.knurek.sync;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class Api {

    public Iterable<? extends Friend> getFriends(String user) {
        String url = "http://knurekapi.appspot.com/?user=" + user;
        HttpGet get = new HttpGet(url);
        HttpClient client = new DefaultHttpClient();
        try {
            HttpResponse response = client.execute(get);
            JSONArray friends = new JSONArray(EntityUtils.toString(response.getEntity()));
            return convert(friends);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    private Iterable<? extends Friend> convert(JSONArray friends) throws JSONException {
        ArrayList<Friend> result = new ArrayList<Friend>();
        for (int i = 0; i < friends.length(); ++i) {
            JSONObject f = friends.getJSONObject(i);

            result.add(convert(f));
        }
        return result;
    }

    private Friend convert(JSONObject f) throws JSONException {
        return new Friend(f.getString("name"), f.getString("realname"), f.getString("image"));
    }
}
