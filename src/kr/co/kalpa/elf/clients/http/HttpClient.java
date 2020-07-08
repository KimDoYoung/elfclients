package kr.co.kalpa.elf.clients.http;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import kr.co.kalpa.elf.clients.Client;
import kr.co.kalpa.elf.utils.CommUtils;
import kr.co.kalpa.elf.utils.DebugPrinter;

public class HttpClient extends Client {
	public static String VERSION = "1.0";
	private String url;
	private DebugPrinter log;
	
	private List<BasicNameValuePair> headers;
	private List<BasicNameValuePair> params;
	private CloseableHttpResponse response = null;

	public HttpClient(String url, DebugPrinter log) {
		this.url = url;
		headers = new ArrayList<BasicNameValuePair>();
		params  = new ArrayList<BasicNameValuePair>();
		this.log = log;
		this.log.debug("httpClient created version: " + VERSION);
		this.log.debug("url=" + url);
	}
	public void addHeader(String name, String value){
		headers.add(new BasicNameValuePair(name, value));
	}
	public void addHeader(Properties propsHeader) {
		if(propsHeader == null){
			return;
		}
		List<BasicNameValuePair> list = CommUtils.propertyToList(propsHeader);
		headers.addAll(list);
	}
	
	public void addParam(String name, String value){
		params.add(new BasicNameValuePair(name, value));
	}
	public void addParam(Properties propsParam) {
		if(propsParam == null){
			return;
		}
		List<BasicNameValuePair> list = CommUtils.propertyToList(propsParam);
		params.addAll(list);
	}

	public String sendGet(){
		response = null;
		String result = "000";
		URIBuilder uriBuilder = null;
		HttpGet request = null;
		try {
			uriBuilder  = new URIBuilder(url);
			for(BasicNameValuePair pair : params){
				uriBuilder.addParameter(pair.getName(), pair.getValue());
			}
			request = new HttpGet(uriBuilder.build());
			for (BasicNameValuePair pair : headers) {
				request.addHeader(pair.getName(), pair.getValue());
			}
			
		} catch (URISyntaxException e) {
			return result = " " + e.getMessage();
		}
		try(
				CloseableHttpClient client = HttpClients.createDefault();
				CloseableHttpResponse response = client.execute(request)
				){
			result = Integer.toString(response.getStatusLine().getStatusCode());
			HttpEntity entity = response.getEntity();
			if(entity != null){
				result += EntityUtils.toString(entity);
			}
		}catch(Exception e){
			result += " " + e.getMessage();
		}
		return result;
	}
	public String sendPost(){
		response = null;
		String result = "000";
		HttpPost request = new HttpPost(url);
		try {
			for (BasicNameValuePair pair : headers) {
				request.addHeader(pair.getName(), pair.getValue());
			}
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
			request.setEntity(entity);
			if(log.isDebug()){
				System.out.println("Entity: " + request.getEntity().toString());
			}
		} catch (UnsupportedEncodingException e) {
			if(log.isDebug()) e.printStackTrace();
			return " " + e.getMessage();
		}
		if(log.isDebug()){
			System.out.println(status());
		}
		try (
				CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request)
				){
			result = Integer.toString(response.getStatusLine().getStatusCode());
			result += " ";
			HttpEntity entity = response.getEntity();
			if(entity != null){
				result += EntityUtils.toString(entity);
			}
			return result;
		} catch (Exception e) {
			if(log.isDebug()) e.printStackTrace();
			result += " " + e.getMessage(); 
		}
		return result;
	}
	public String sendJson(String json){
		return sendJson(json, "UTF-8");
	}
	public String sendJson(String json, String charsetName){
		response = null;
		String result = "000";
		HttpPost request = new HttpPost(url);
		addHeader("Content-Type", "application/json; charset=" + charsetName);
		for (BasicNameValuePair pair : headers) {
			request.addHeader(pair.getName(), pair.getValue());
		}
		request.setEntity(new ByteArrayEntity(json.getBytes()));
		try (
				CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(request)
				){
			result = Integer.toString(response.getStatusLine().getStatusCode());
			result += " ";
			HttpEntity entity = response.getEntity();
			if(entity != null){
				result += EntityUtils.toString(entity);
			}
			return result;
		} catch (Exception e) {
			if(log.isDebug()) e.printStackTrace();
			result += " " + e.getMessage(); 
		}
		return result;
	}
	public String status() {
		StringBuilder sb = new StringBuilder();
		sb.append("URL: " + url).append("\n");
		sb.append("Headers: ").append("\n");
		for (BasicNameValuePair pair : headers) {
			sb.append("\t").append(pair.getName()).append("=").append(pair.getValue()).append("\n");
		}
		sb.append("Params: ").append("\n");
		for (BasicNameValuePair pair : params) {
			sb.append("\t").append(pair.getName()).append("=").append(pair.getValue()).append("\n");
		}
		if(response == null){
			sb.append("Response: NULL").append("\n");
		}else{
			sb.append("\t").append("status code: ").append(response.getStatusLine().getStatusCode()).append("\n");
		}
		return sb.toString();
	}

}
