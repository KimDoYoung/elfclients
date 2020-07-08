package kr.co.kalpa.elf.clients.http;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import kr.co.kalpa.elf.utils.CommUtils;
import kr.co.kalpa.elf.utils.DebugPrinter;

public class Main {
	public static String PROGRAM_NAME = "httpClient";
	public static void main(String[] args){
		HttpClient http = null;
		String url = null;
		if(args.length < 1){
			System.out.println("Usage: " + PROGRAM_NAME + " <url>");
			System.out.println("more information: " + PROGRAM_NAME + " -h");
			System.exit(0);
		}
		url = CommUtils.padHttp(args[0]);
		//url check
		try {
			new URL(url).toURI();
		} catch (Exception e) {
			System.out.println("\nURL: "+args[0] + " is not valid URL");
			System.out.println("\nUsage: " + PROGRAM_NAME + " <url>");
			System.out.println("more information: " + PROGRAM_NAME + " -h");
			System.exit(0);
		}
		//Boolean
		Option help = new Option("h", "help", false, "print this message");
		Option version = new Option("v", "version", false, "print the version and exit");
		Option debug = new Option("d", "debug", false, "print debugging information");
		Option writeHP= new Option("w", "write", false, "print example of Header and Parameter define file");
		//Argument
		Option method = Option.builder("m").longOpt("method").argName("method")
				.hasArg().desc("method [get|post|json] 'get' method is default").build();
		Option jsonFilePath = Option.builder("f").longOpt("file").argName("jsonfilePath")
				.hasArg().desc("json file path").build();
		Option hpfile = Option.builder("r").longOpt("readfile").argName("headerParamFile")
				.hasArg().desc("read header and param from file").build();
		Option header = Option.builder("H").longOpt("header").argName("name=value")
				.numberOfArgs(2).valueSeparator().desc("header name=value").build();
		Option param = Option.builder("P").longOpt("param").argName("name=value")
				.numberOfArgs(2).valueSeparator().desc("parameter name=value").build();
		Option charset = Option.builder("c").longOpt("charset").argName("charset")
				.numberOfArgs(2).valueSeparator()
				.desc("charset name example:EUC-KR, UTF-8, default is UTF-8").build();
		
		Options options = new Options();
		options.addOption(help);
		options.addOption(version);
		options.addOption(debug);
		options.addOption(writeHP);
		options.addOption(method);
		options.addOption(jsonFilePath);
		options.addOption(hpfile);
		options.addOption(header);
		options.addOption(param);
		options.addOption(charset);

		CommandLineParser parser = new DefaultParser();
		DebugPrinter log = new DebugPrinter(false);
		try {
			if(args.length == 0){
				printHelp(options);
			}
			for (String s : args) {
				if(s.equals("-h") || s.equals("--help")){
					printHelp(options);
					System.exit(1);
				}
			}
			for (String s : args) {
				if(s.equals("-v") || s.equals("--version")){
					System.out.println(PROGRAM_NAME +  " version: " + HttpClient.VERSION);
					System.exit(1);
				}
			}
			for (String s : args) {
				if(s.equals("-w") || s.equals("--write")){
					System.out.println(writeHeaderAndParamExample());
					System.exit(1);
				}
			}
			CommandLine line = parser.parse(options, args);
			String methodName = "get";
			if(line.hasOption("m")){
				methodName = line.getOptionValue("m").trim();
			}
			if(line.hasOption("d")){
				log.setVerbose(true);
			}
			if(line.hasOption("m")){
				methodName = line.getOptionValue("m").trim();
			}
			String charsetName = "UTF-8";
			if(line.hasOption("c")){
				charsetName = line.getOptionValue("c").trim();
			}
			//헤더와 파라메터를 파일로부터 읽는다.
			String headerParamFile = null;
			Properties[] propFile = {null, null};
			if(line.hasOption("r")){
				headerParamFile = line.getOptionValue("r").trim();
				File pFile = new File(headerParamFile);
				if(pFile.exists() == false){
					System.out.println(pFile.getAbsolutePath() + " is not exist");
					System.exit(1);
				}
				propFile = CommUtils.readPropertyFromFile(pFile);
			}
			Properties propsParam = null;
			if(line.hasOption("P")){
				propsParam  = line.getOptionProperties("P");
			}
			Properties propsHeader= null;
			if(line.hasOption("H")){
				propsHeader= line.getOptionProperties("H");
			}
			
			Properties mHeaderProp = CommUtils.mergeProp(propFile[0], propsHeader);
			Properties mParamProp = CommUtils.mergeProp(propFile[0], propsParam);
			
			if(methodName.equals("json")){
				if(line.hasOption("f") == false){
					System.out.println("method json need option -f");
					System.exit(1);
				}else{
					log.debug("CHARSET is " + charsetName);
					String jsonFilepath = line.getOptionValue("f");
					log.debug("json file path: "+jsonFilepath);
					File file = new File(jsonFilepath);
					if(file.exists() == false){
						System.out.println("file not found: " + file.getAbsolutePath());
						System.exit(1);
					}
					http = new HttpClient(url, log);
					http.addHeader(mHeaderProp);
					String json = CommUtils.readTextFile(file.getAbsolutePath(), charsetName);
					log.debug("==>\n" + json + "\n<===");
					String result = http.sendJson(json, charsetName);
					System.out.println(result);
				}
			}else if(methodName.equals("get")){
				http = new HttpClient(url, log);
				http.addHeader(mHeaderProp);
				http.addParam(mParamProp);
				log.debug("-----------------------------------------");
				log.debug("GET to " + url);
				log.debug("-----------------------------------------");
				log.debug(http.status());
				String result = http.sendGet();
				System.out.println(result);
			}else if(methodName.equals("post")){
				
				http = new HttpClient(url, log);
				http.addHeader(mHeaderProp);
				http.addParam(mParamProp);
				log.debug("-----------------------------------------");
				log.debug("POST to " + url);
				log.debug("-----------------------------------------");
				log.debug(http.status());
				String result = http.sendPost();
				System.out.println(result);
			}else {
				log.error(methodName + " is not valid method name");
			}
			
		} catch (ParseException e) {
			System.err.println("Parsing command line failed: " + e.getMessage());
		} catch (IOException e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}
	private static String writeHeaderAndParamExample() {
		return "----------------------------------------------------------------\n"
				+ "# httpClient에서 -r 옵션에서 사용가능한 헤더와 파라메터의  정의 \n"
				+ "# 1.'#'는 주석임  \n"
				+ "# 2. 비어있는 라인은 무시함  \n"
				+ "# 3. 라인 중 '#' 가 나오면 그 이후의 문자는 주석처리함   \n"
				+ "# 4. header는 header[name]=value 로 정의함   \n"
				+ "# 5. param은 param[name]=value 로 정의함    \n"
				+ "# 6. name및 value의 앞뒤 스페이스는 제거함   \n"
				+ "----------------------------------------------------------------\n"
				+ "header[content-type]= application/json\n"
				+ "param[ prmId ]= sftp.sh\n"
				;
	}
	private static void printHelp(Options options){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(PROGRAM_NAME + " url", options, true);
		
	}
}
