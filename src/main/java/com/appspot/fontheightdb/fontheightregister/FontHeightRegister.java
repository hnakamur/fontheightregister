package com.appspot.fontheightdb.fontheightregister;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FontHeightRegister {
  // private static final String HOST = "localhost:8080";
  private static final String HOST = "fontheightdb.appspot.com";

  private static final int[] SIZES = {
      6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 22, 24, 26, 28, 30,
      32, 34, 36, 40, 44, 48, 56, 64, 72, 94, 144, 288};

  private void run() {
    List<String> registeredNames = getRegisteredNames();
    System.out.println(registeredNames);

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    Image img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics g = img.getGraphics();
    Font[] fonts = ge.getAllFonts();
    try {
      for (int i = 0; i < fonts.length; i++) {
        Font font = fonts[i];
        String name = font.getFontName(Locale.US);
        int index = registeredNames.indexOf(name);
        if (index == -1) {
          JSONObject obj = new JSONObject();
          obj.put("name", name);
          JSONArray sizes = new JSONArray();
          JSONArray heights = new JSONArray();
          JSONArray ascents = new JSONArray();
          JSONArray descents = new JSONArray();
          for (int j = 0; j < SIZES.length; j++) {
            int size = SIZES[j];

            Font f = font.deriveFont((float) size);
            sizes.put(f.getSize());
            FontMetrics metrics = g.getFontMetrics(f);
            heights.put(metrics.getHeight());
            ascents.put(metrics.getAscent());
            descents.put(metrics.getDescent());
          }
          obj.put("sizes", sizes);
          obj.put("heights", heights);
          obj.put("ascents", ascents);
          obj.put("descents", descents);
          regist(obj);
        }
      }
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private void regist(JSONObject jsonObject) {
    String jsonString = jsonObject.toString();

    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpPost request = new HttpPost("http://" + HOST + "/fontHeights/");
    List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair("data", jsonString));
    try {
      request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    try {
      HttpResponse response = httpclient.execute(request);
      System.out.println("register '" + jsonObject.getString("name") + "' status="
          + response.getStatusLine());
    } catch (ClientProtocolException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> getRegisteredNames() {
    List<String> names = new ArrayList<String>();
    DefaultHttpClient httpclient = new DefaultHttpClient();
    HttpGet request = new HttpGet("http://" + HOST + "/fontNames?format=text");
    try {
      HttpResponse response = httpclient.execute(request);
      System.out.println("response status=" + response.getStatusLine());

      HttpEntity entity = response.getEntity();

      if (entity != null) {
        InputStream instream = entity.getContent();
        try {
          BufferedReader reader = new BufferedReader(new InputStreamReader(
              instream, "UTF-8"));
          String line;
          while (true) {
            line = reader.readLine();
            if (line == null) {
              break;
            }
            names.add(line);
          }
        } finally {
          instream.close();
        }
      }
    } catch (ClientProtocolException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return names;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new FontHeightRegister().run();
  }
}
