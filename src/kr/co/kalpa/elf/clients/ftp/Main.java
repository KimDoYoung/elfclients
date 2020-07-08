package kr.co.kalpa.elf.clients.ftp;

import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import kr.co.kalpa.elf.utils.CommandParser;
import kr.co.kalpa.elf.utils.DebugPrinter;

/**
 * 
 * Ftp Client
 * @author KimDoYoung
 *
 */
public class Main {
	public static String PROGRAM_NAME = "ftpClient";
	public static void main(String[] args){
		// boolean options
		Option passive = new Option("p", "passive mode", false, "set mode passive, default is active");
		Option debug = new Option("d", "debug", false, "print debugging information");
		Option report= new Option("r", "report", false, "print final report");
		Option help= new Option("h", "help", false, "print this message");
		Option version= new Option("v", "version", false, "print the version information");
		//argument options
		Option charset = Option.builder("c").longOpt("charset")
				.argName("charset").hasArg().required(false)
				.desc("charset default UTF-8").build();
		Option port = Option.builder("P").longOpt("port")
				.argName("port").hasArg().required(false).type(Integer.class)
				.desc("port number default 21").build();
		
		Options options = new Options();
		options.addOption(passive);
		options.addOption(debug);
		options.addOption(report);
		options.addOption(help);
		options.addOption(version);
		options.addOption(charset);
		options.addOption(port);
		
		CommandLineParser parser = new DefaultParser();
		DebugPrinter log = new DebugPrinter(false);
		Scanner scanner = null;
		FtpClient ftp = null;
		boolean isReport = false;
		try {
			for (String s : args) {
				if(s.equals("-h") || s.equals("--help")){
					printHelp(options);
					System.exit(1);
				}
			}
			for (String s : args) {
				if(s.equals("-v") || s.equals("--version")){
					System.out.println(PROGRAM_NAME + " version: " + FtpClient.VERSION);
					System.exit(1);
				}
			}
			if(args.length < 3){
				printShortHelp();
				System.exit(1);
			}
			String host = args[0];
			String userId= args[1];
			String userPw= args[2];
			
			//debug
			CommandLine line = parser.parse(options, args);
			if(line.hasOption("d")){
				log.setVerbose(true);
			}
			//report
			if(line.hasOption("r")){
				isReport = true;
			}
			//port
			int portNumber = 21 ; // default
			if (line.hasOption("P")){
				String sPort = line.getOptionValue("P");
				portNumber = Integer.valueOf(sPort);
			}
			//charset
			String charSet = "UTF-8";
			if (line.hasOption("c")){
				charSet = line.getOptionValue("c");
			}
			ftp = new FtpClient(host, log);
			ftp.setUserId(userId);
			ftp.setUserPw(userPw);
			ftp.setPort(portNumber);
			ftp.setCharset(charSet);

			String result = ftp.open();
			if(result.startsWith("NK:")){
				System.out.println(result);
				System.exit(1);
			}
			CommandParser cmd = new CommandParser();
			scanner = new Scanner(System.in);
			System.out.println("connection to " + ftp.getUserId()+ " on " + ftp.getHost() + " OK");
			System.out.println("welcom ftpClient (exit, help)");
			String input = null;
			while(true){
				input = cmd.getMacro();
				if(input == null){
					System.out.println("ftp> ");
					input = scanner.nextLine();
				}
				if(log.isDebug()){
					System.out.println(input);
				}
				cmd.parsing(input);
				if(cmd.isValid("help")){
					display(ftp, ftp.help());
				}else if(cmd.isValid("exit")){
					display(ftp, ftp.close());
					break; //quit
				}else if(input.contains(";")){
					cmd.setMacro(input);
				}else if(cmd.isValid("status")){
					display(ftp, ftp.status());
				}else if(cmd.isValid("pwd")){
					display(ftp, ftp.pwd());
				}else if(cmd.isValid("lpwd")){
					display(ftp, ftp.lpwd());
					
				}else if(cmd.isValid("cwd")){
					display(ftp, ftp.cwd());
					
				}else if( cmd.name.equals("ls") || cmd.name.equals("dir") || cmd.name.equals("ll")){  
					if(cmd.argCount == 0){
						display(ftp, ftp.ls(null));
					}else {
						display(ftp, ftp.ls(cmd.getArg(1)));
					}
				}else if( cmd.name.equals("lls") || cmd.name.equals("ldir") || cmd.name.equals("lll")){  
					if(cmd.argCount == 0){
						display(ftp, ftp.lls(null));
					}else {
						display(ftp, ftp.lls(cmd.getArg(1)));
					}
				}else if(cmd.name.equals("cd")){
					if(cmd.argCount == 0){
						display(ftp, ftp.cd("."));
					}else {
						display(ftp, ftp.cd(cmd.getArg(1)));
					}
				}else if(cmd.name.equals("lcd")){
					if(cmd.argCount == 0){
						display(ftp, ftp.lcd("."));
					}else {
						display(ftp, ftp.lcd(cmd.getArg(1)));
					}
				}else if(cmd.name.equals("asc") || cmd.name.equals("ascii")){
					display(ftp, ftp.ascii());
				}else if(cmd.name.equals("bin") || cmd.name.equals("binary")){
					display(ftp, ftp.binary());
//				}else if(cmd.name.equals("auto")){
//					display(ftp, ftp.fileType("auto"));
				}else if(cmd.isValid("rename",2)){
					display(ftp, ftp.rename(cmd.getArg(1), cmd.getArg(2)));
				}else if(cmd.isValid("rm",1)){
					display(ftp, ftp.rm(cmd.getArg(1)));
				}else if(cmd.isValid("rmdir",1)){
					display(ftp, ftp.rmdir(cmd.getArg(1)));
				}else if(cmd.isValid("mkdir",1)){
					display(ftp, ftp.mkdir(cmd.getArg(1)));
					
				}else if(cmd.isValid("put",1)){
					display(ftp, ftp.put(cmd.getArg(1), false));
				}else if(cmd.isValid("mput",1)){
					display(ftp, ftp.mput(cmd.getArg(1), false));
				}else if(cmd.isValid("puf",1)){
					display(ftp, ftp.put(cmd.getArg(1), true));
				}else if(cmd.isValid("mpuf",1)){
					display(ftp, ftp.mput(cmd.getArg(1), true));
					
				}else if(cmd.isValid("get",1)){
					display(ftp, ftp.get(cmd.getArg(1)));
				}else if(cmd.isValid("mget",1)){
					display(ftp, ftp.mget(cmd.getArg(1)));
				}else if(cmd.isValid("echo",1)){
					display(ftp, ftp.echo(cmd.getArg(1)));
				}else if(cmd.isEmpty()){
					;
				}else{
					display(ftp, "NK: " + cmd.name + " is NOT valid command or argument is NOT correct. enter help");
				}
			}
			scanner.reset();
			scanner.close();
			System.out.println("Bye");
			ftp.workEnd();
			if(isReport){
				System.out.println(ftp.finalReport());
			}
			System.exit(0);
		} catch (ParseException e) {
			printShortHelp();
			System.err.println("Parsing failed: " + e.getMessage());
		} finally{
			if(scanner != null) scanner.close();
			if(ftp != null){
				ftp.close();
			}
		}
	}
	private static void display(FtpClient client, String response) {
		if(response.startsWith("OK")){
			if(response.length()>2){
				System.out.println(response.substring(2));
			}else{
				System.out.println(response);
			}
		}else if(response.startsWith("NK")){
			client.reportNK(response);
			System.out.println(response);
		}else {
			System.out.println("ERROR:\r\n" + response);
		}
	}
	private static void printShortHelp() {
		System.out.println("Usage: " + PROGRAM_NAME + " <host> <userId> <password>");
		System.out.println("more information: " + PROGRAM_NAME + " -h");
	}
	private static void printHelp(Options options) {
		HelpFormatter formatter =  new HelpFormatter();
		formatter.printHelp(PROGRAM_NAME, options, true);
	}
}
