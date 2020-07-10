package kr.co.kalpa.elf.clients.sftp;

import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import kr.co.kalpa.elf.clients.Client;
import kr.co.kalpa.elf.exception.ClientException;
import kr.co.kalpa.elf.utils.CommandParser;
import kr.co.kalpa.elf.utils.DebugPrinter;

public class Main {

	public static String PROGRAM_NAME = "sftpClient";

	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("Usage : " + PROGRAM_NAME + " <userid>@<url>");
			System.out.println("more information : "  + PROGRAM_NAME + " -h");
			System.exit(1);
		}
		
		Option debug = new Option("d", "debug", false, "print debugging information");
		Option help = new Option("h", "help", false, "print this mesage");
		Option version = new Option("v", "version", false, "print the version information");
		
		Option password = Option.builder("p").longOpt("password").argName("password")
											.hasArg().required(false)
											.desc("user password")
											.build();
		Option port = Option.builder("P").longOpt("port").argName("port").hasArg()
				.type(Integer.class).required(false)
				.desc("port number defalut 22")
				.build();
		Option report = new Option("r", "report", false, "write or not final report");
		
		Options options = new Options();
		options.addOption(debug);
		options.addOption(help);
		options.addOption(version);
		options.addOption(password);
		options.addOption(port);
		options.addOption(report);
		
		CommandLineParser parser = new DefaultParser();
		DebugPrinter log = new DebugPrinter(false);
		Scanner scanner = null;
		SftpClient sftp = null;
		boolean isReport = false;
		try {
			if (args.length == 0) {
				printHelp(options);
			}
			//help가 있으면 도움말을 보여주고 종료 
			for(String s : args){
				if(s.equals("-h") || s.equals("--help")){
					printHelp(options);
					System.exit(1);
				}
			}
			//version 옵션이 있으면 버젼을 보여주고 종료 
			for(String s : args){
				if(s.equals("-v") || s.equals("--version")){
					System.out.println(PROGRAM_NAME + " version : " + SftpClient.VERSION);
					System.exit(1);
				}
			}
			
			CommandLine line = parser.parse(options, args);
			
			if(line.hasOption("d")){
				log.setVerbose(true);
			}
			if(line.hasOption("r")){
				isReport = true;
			}
			
			int portNumber = 22;
			
			if(line.hasOption("P")) {
				String sPort = line.getOptionValue("P");
				portNumber = Integer.valueOf(sPort);
			}
			sftp = new SftpClient(args[0], log);
			sftp.setPort(portNumber);
			String passwd = null;
			if(line.hasOption("p")){
				passwd = line.getOptionValue("p");
				sftp.setPassword(passwd);
			}
			CommandParser cmd = new CommandParser();
			sftp.open();
			scanner = new Scanner(System.in);
			
			System.out.println("connection to " + sftp.getUserId()+"@" + sftp.getHost() + " OK");
			System.out.println("welcom sftpClient (exit or quit, help)");

			String input = null;
			while(true){
				input = cmd.getMacro();
				if(input == null){
					System.out.print("sftp> ");
					input = scanner.nextLine();
				}
				if(log.isDebug()){
					System.out.println(input);
				}
				cmd.parsing(input);
				if(cmd.isValid("help")){
					display(sftp, sftp.help());
				}else if(cmd.isValid("exit") || cmd.isValid("quit")){
					break; //quit
				} else if(input.contains(";")) {
					cmd.setMacro(input);
				} else if(cmd.isValid("pwd")){
					display(sftp, sftp.pwd());
				} else if(cmd.isValid("lpwd")){
					display(sftp, sftp.pwd());					
				} else if(cmd.isValid("cwd")){
					display(sftp, sftp.cwd());					
				} else if(cmd.name.equals("lcd")){
					if(cmd.argCount == 0) {
						display(sftp, sftp.lcd("."));
					}else if(cmd.argCount == 1){
						display(sftp, sftp.lcd("."));
					}else {
						display(sftp, "NK: lcd has zero or one argument");
					}
				} else if(cmd.name.equals("cd")){
					if(cmd.argCount == 0){
						display(sftp, sftp.cd("."));
					}else if(cmd.argCount == 1){
						display(sftp, sftp.cd(cmd.getArg(1)));
					}else {
						display(sftp, "NK: cd has zero or one argument");
					}
				} else if(cmd.isValid("rename", 2)){
					display(sftp, sftp.rename(cmd.getArg(1), cmd.getArg(2)));
				} else if(cmd.isValid("rmdir", 1)){
					display(sftp, sftp.rmDir(cmd.getArg(1)));
				} else if(cmd.isValid("rm", 1)){
					display(sftp, sftp.rm(cmd.getArg(1)));
				}	else if(cmd.name.equals("ls") || cmd.name.equals("dir") || cmd.name.equals("ll")){
					if(cmd.argCount == 0){
						display(sftp, sftp.ls());
					}else if(cmd.argCount == 1){
						display(sftp, sftp.ls(cmd.getArg(1)));
					}
				} else if(cmd.name.equals("lls") || cmd.name.equals("ldir") || cmd.name.equals("lll")){
					if(cmd.argCount == 0){
						display(sftp, sftp.lls());
					}else if(cmd.argCount == 1) {
						display(sftp, sftp.lls(cmd.getArg(1)));
					}
				} else if(cmd.name.equals("put")){
					if(cmd.argCount == 2){
						display(sftp, sftp.put(cmd.getArg(1), cmd.getArg(2), false));
					}else if(cmd.argCount == 2){
						display(sftp, sftp.put(cmd.getArg(1), false));
					}
				} else if(cmd.name.equals("puf")){
					if(cmd.argCount == 2){
						display(sftp, sftp.put(cmd.getArg(1), cmd.getArg(2), true));
					}else if(cmd.argCount == 2){
						display(sftp, sftp.put(cmd.getArg(1), true));
					}
				} else if(cmd.name.equals("get")){
					if(cmd.argCount == 2){
						display(sftp, sftp.get(cmd.getArg(1), cmd.getArg(2)));
					}else if(cmd.argCount == 2){
						display(sftp, sftp.get(cmd.getArg(1)));
					}
				} else if(cmd.isValid("mget", 1)){
					display(sftp, sftp.mget(cmd.getArg(1)));
				} else if(cmd.isValid("mput", 1)){
					display(sftp, sftp.mput(cmd.getArg(1), false));
				} else if(cmd.isValid("mpuf", 1)) {
					display(sftp, sftp.mput(cmd.getArg(1), true));
				} else if(cmd.isValid("echo", 1)){
					display(sftp, sftp.echo(cmd.getArg(1)));
				} else if(cmd.isEmpty()) {
					;
				} else {
					display(sftp, "NK: " + cmd.name + " is NOT valid command or invalid arguments");
				}
			}
			scanner.reset();
			scanner.close();
			System.out.println(sftp.close());
			System.out.println("Bye");
			sftp.workEnd();
			if(isReport){
				System.out.println(sftp.finalReport());
			}
			System.exit(0);
		} catch (ParseException exp) {
			System.out.println("Parsing failed. Reason: " + exp.getMessage());
			System.exit(1);
		} catch(ClientException sftpEx){
			System.out.println(sftpEx.getMessage());
			System.exit(1);
		}catch(Exception e){
			System.out.println(e.getMessage());
			System.exit(1);
			
		} finally{
			if(scanner != null){
				scanner.close();
			}
			if(sftp != null){
				sftp.close();
				
			}
		}
	}

	private static void display(Client client, String response) {
		if(response.startsWith("OK")){
			if(response.length() > 2){
				String ss = response.substring(2);
				if(ss.indexOf(":") >=0) {
					ss = ss.substring(ss.indexOf(":") + 1);
				}
				System.out.println(ss);
			}else {
				System.out.println(response);
			}
		}else if(response.startsWith("NK:")){
			client.reportNK(response);
			System.out.println(response);
		}else {
			System.out.println("ERROR: " + response);
		}
		
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter;
		formatter = new HelpFormatter();
		formatter.printHelp(PROGRAM_NAME + " userId@host [-p <password>]", options, true);
	}
}
