package ugia.moscow14attendees.api.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.OkHttpClient;
import ugia.moscow14attendees.application.Constants;

/**
 * Created by joseluisugia on 10/04/14.
 */
public class Request implements Runnable {

    public static enum Method {GET, POST}

    private static OkHttpClient client;

    private String uri;
    private Method method;
    private String body;
    private RequestCallback callback;

    public Request() {
        if (client == null) {
            client = new OkHttpClient();
            client.setConnectTimeout(60L, TimeUnit.SECONDS);
        }
    }

    public Request forUri(String uri) {
        return forUri(uri, Method.GET);
    }

    public Request forUri(String uri, Method method) {
        this.uri = uri;
        this.method = method;
        return this;
    }

    public Request withBody(String body) {
        this.body = body;
        return this;
    }

    public Request callsBackTo(RequestCallback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void run() {

        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStreamReader in = null;

        int statusCode;
        String responseBody;

        try {
            URL url = new URL("https", Constants.API_HOST, uri);
            connection = client.open(url);
            connection.setRequestMethod(method.toString());

            if (body != null) {
                connection.setDoOutput(true);
                out = connection.getOutputStream();
                byte[] bytes = body.getBytes(Charset.forName("UTF-8"));
                out.write(bytes);
                out.close();
            }

            InputStream is = connection.getInputStream();
            in = new InputStreamReader(is);

            StringBuffer buffer = new StringBuffer();
            int len;
            char[] buf = new char[1024];
            while ((len = in.read(buf)) > 0) {
                buffer.append(buf, 0, len);
            }

            statusCode = connection.getResponseCode();
            responseBody = buffer.toString();

        } catch (java.net.MalformedURLException e) {
            statusCode = 500;
            responseBody = e.getMessage();
        } catch (java.net.ProtocolException e) {
            statusCode = 500;
            responseBody = e.getMessage();
        } catch (java.io.FileNotFoundException e) {
            statusCode = 404;
            responseBody = e.getMessage();
        } catch (java.io.IOException e) {
            statusCode = 500;
            responseBody = e.getMessage();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                // Fuff, too much
            }
        }

        if (callback != null) {
            if (statusCode >= 200 && statusCode < 400) {
                callback.onSuccess(responseBody);
            } else {
                callback.onFailure(statusCode, responseBody);
            }
        }

    }

    public interface RequestCallback {

        public void onSuccess(String responseBody);

        public void onFailure(int statusCode, String message);
    }
}
