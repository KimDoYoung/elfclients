package kr.co.kalpa.elf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.message.BasicNameValuePair;

public class CommUtils {

	private static final String DATE_TIME_FORMAT="yyyy-MM-dd HH:mm:ss";
	
	public static String elapsedTimeHuman(long l) {
		String s = String.format("%d min %d sec", TimeUnit.MICROSECONDS.toMinutes(l),
				TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MICROSECONDS.toSeconds(l));
		return s;
	}

	public static String createTimeOfFile(File file) {
		BasicFileAttributes attr;
		try {
			attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
			FileTime time = attr.creationTime();
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
			return sdf.format(new Date(time.toMillis()));
		} catch (IOException e) {
			return "0000-00-00 00:00:00";
		}
	}

	public static String rightPadding(long size) {
		return String.format("%12s", size);
	}

	public static boolean hasPathChar(String filePath) {
		return (filePath.contains("\\") || filePath.contains("/"));
	}

	public static File createFlagFile(String fileName, String flagExtension) {
		String fn = FilenameUtils.getName(fileName);
		String nameOnly = FilenameUtils.removeExtension(fn);
		String flgFileName = nameOnly + "." + flagExtension;
		String tmpdir = System.getProperty("java.io.tmpdir");
		String flgPath = tmpdir + "/" + flgFileName;
		File file = new File(flgPath);
		try {
			file.createNewFile();
			return file;
		} catch (IOException e) {
			return null;
		}
	}

	public static List<BasicNameValuePair> propertyToList(Properties props) {
		List<BasicNameValuePair> list = new ArrayList<>();
		for (String key : props.stringPropertyNames()) {
			list.add(new BasicNameValuePair(key, props.getProperty(key)));
		}
		return list;
	}

	public static String removingQuoting(String s) {
		return s.replaceAll("^[\'\"]+", "").replaceAll("[\'\"]+$", "").replaceAll("[\\^]", " ");
	}

	public static String readTextFile(String filePath, String charsetName) {
		StringBuilder sb = new StringBuilder();
		Charset charset = Charset.forName(charsetName);
		try (Stream<String> stream = Files.lines(Paths.get(filePath), charset)) {
			stream.forEach(s -> sb.append(s).append("\n"));
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR: " + e.getMessage();
		}
		return sb.toString();
	}

	public static String padHttp(String url) {
		if(url.startsWith("http://") == false){
			return "http://" + url;
		}
		return url;
	}

	public static Properties[] readPropertyFromFile(File file) throws IOException {
		Properties headerProp = new Properties();
		Properties paramProp = new Properties();
		
		Properties[] resultProp = { headerProp, paramProp };
		FileInputStream fis = new FileInputStream(file);
		InputStreamReader is = new InputStreamReader(fis, "UTF-8");
		BufferedReader reader = new BufferedReader(is);
		String s = null;
		String[] fields = null;
		String name = null;
		String value = null;
		
		while( (s = reader.readLine()) != null){
			s = s.trim();
			//빈라인 또는 #로 시작하는 라인은 skip
			if(s.length() < 1  || s.startsWith("#") ) {
				continue;
			}
			if(s.indexOf("#") > 0) {
				s = s.substring(0, s.indexOf("#"));
			}
			fields = s.split("[=]", -1);
			if(fields.length != 2){
				continue;
			}
			//header[name]=Hong gil dong
			if(s.startsWith("header")){
				name = extractBetween(s, "[", "]").trim();
				value=fields[1].trim();
				headerProp.put(name, value);
			}else if(s.startsWith("param")){
				name = extractBetween(s, "[", "]").trim();
				value=fields[1].trim();			
				paramProp.put(name, value);
			}
		}
		reader.close();
		return resultProp;
		
	}

	private static String extractBetween(String s, String start, String end) {
		int pos1 = s.indexOf(start);
		int pos2 = s.indexOf(end);
		if(pos1 != -1 && pos2 != -1 && pos2 > pos1){
			return s.substring(pos1, pos2);
		}
		return "";
	}

	public static Properties mergeProp(Properties prop1, Properties prop2) {
		if (prop1 == null && prop2 == null) {
			return null;
		}
		Properties mProp = new Properties();
		if(prop1 != null){
			mProp.putAll(prop1);
		}
		if(prop2 != null){
			mProp.putAll(prop2);
		}
		return mProp;
	}

	public static Object calendarToString(Calendar cal) {
		return calendarToString(cal, "yyyy-MM-dd HH:mm:ss");
	}

	private static String calendarToString(Calendar cal, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(cal.getTime());
	}

	public static String getCurrentWorkingDir() {
		File d = new File("").getAbsoluteFile();
		if (d.exists()) {
			return d.getAbsolutePath();
		}
		return null;
	}

	public static String dateToString(Date time) {
		return dateToString(time, "yyyy-MM-dd HH:mm:ss");
	}

	private static String dateToString(Date time, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(time);

	}

	public static boolean setCurrentWorkingDirectory(String path) {
		File d = new File(path).getAbsoluteFile();
		boolean r = false;
		if (d.exists()) {
			r = (System.setProperty("user.dir", d.getAbsolutePath())) != null;
		}
		return r;
	}

	public static boolean isWildcardMatch(String pattern, String targetStr) {
		if (pattern == null) {
			return true;
		}
		String regex = ("\\Q" + pattern + "\\E").replaceAll("*", "\\E.*\\Q");
		return targetStr.matches(regex);
	}

}
