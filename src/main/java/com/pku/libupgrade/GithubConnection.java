package com.pku.libupgrade;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;



public class GithubConnection {
    public static String getGitUrl(String repoNam) {
        String GITHUB_API_BASE_URL = "https://api.github.com/";
        String GITHUB_API_SEARCH_CODE_PATH = "search/repositories?q=";
        JSONObject result = getResult(GITHUB_API_BASE_URL+GITHUB_API_SEARCH_CODE_PATH+repoNam);
        assert result != null;
        JSONObject repo = result.getJSONArray("items").getJSONObject(0);
        return repo.getString("clone_url");
    }
    public static JSONObject getResult(String targetURL) {
        HttpURLConnection connection = null;

        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Authorization", "token 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");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setUseCaches(false);

            //Send request
//            DataOutputStream wr = new DataOutputStream (
//                    connection.getOutputStream());
//            wr.writeBytes(urlParameters);
//            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            JSONTokener tokener = new JSONTokener(bufferedReader);
            return new JSONObject(tokener);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
