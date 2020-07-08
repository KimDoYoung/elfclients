package kr.co.kalpa.elf.clients.sftp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import kr.co.kalpa.elf.clients.Client;
import kr.co.kalpa.elf.exception.ClientException;
import kr.co.kalpa.elf.utils.CommUtils;
import kr.co.kalpa.elf.utils.DebugPrinter;

public class SftpClient extends Client {

	public static final String VERSION = "1.0";
	private Session session = null;
	private Channel channel = null;
	private ChannelSftp channelSftp = null;
	private String host;
	private String userId;
	private String password;
	private int port;
	DebugPrinter log = null;
	
	
	public SftpClient(String idHost, DebugPrinter log) throws ClientException{
		this.log = log;
		if(idHost.contains("@")){
			String[] tmp = idHost.split("@");
			if(tmp.length == 2){
				this.userId = tmp[0].trim();
				this.host = tmp[1].trim();
				log.debug("userId: " + userId + ", host: "+host);
			}else{
				throw new ClientException(idHost + " is not valid format, ex : hong@10.222.42.71" );
			}
		}else{
			throw new ClientException(idHost + " is not valid format, ex : hong@10.222.42.71" );
		}
	}
	
	
	public String getHost(){
		return host;
	}
	public String getUserId(){
		return userId;
	}
	public String getPassword(){
		return password;
	}
	public void setPassword(String password){
		this.password = password;
	}
	public int getPort(){
		return port;
	}
	public void setPort(int port){
		this.port = port;
	}
	
	private String upload(File file, String newFileName){
		
		try (FileInputStream in = new FileInputStream(file)){
			channelSftp.put(in, newFileName);
			reporter.addUpload(file.getAbsolutePath() + "(" + file.length() + ")");
			return "OK:" + newFileName + " uploaded";
		}catch(SftpException e){
			if(log.isDebug()) {
				e.printStackTrace();
			}
			return "NK:" + e.getMessage();
		}catch(FileNotFoundException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();
		} catch (IOException ee) {
			if(log.isDebug()) ee.printStackTrace();
			return "NK:" + ee.getMessage();
		} 
	}
	private String download(String fileName, String newFileName){
		InputStream in = null;
		FileOutputStream out = null;
		try{
			in = channelSftp.get(fileName);
		}catch(SftpException e){
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();
		}
		String path = null;
		try{
			path = channelSftp.lpwd() + "/" + newFileName;
			File localFile = new File(path);
			out = new FileOutputStream(localFile);
			int i;
			long size = 0L;
			while((i = in.read()) != -1){
				out.write(i);
			}
			reporter.addDownload(localFile.getAbsolutePath());
		}catch(IOException e){
			if(log.isDebug())e.printStackTrace();
			return "NK:" + e.getMessage();
		} finally{
			try {
				if(out != null) out.close();
				if(in != null) in.close();
			} catch (Exception e2) {
				if(log.isDebug())e2.printStackTrace();
			}
		}
		return "OK:" + newFileName + " downloaded";
	}
	public String open() throws ClientException{
		JSch jsch = new JSch();
		try {
			session = jsch.getSession(userId, host, port);
			if(password != null){
				session.setPassword(password);
				java.util.Properties config = new java.util.Properties();
				config.put("StrictHostKeyChecking",	"no");
				session.setConfig(config);
			}
			session.connect();
			channel = session.openChannel("sftp");
			//channel.connect();
		} catch (JSchException e) {
			if(log.isDebug()) e.printStackTrace();
			String s = userId + "@" + host + ", port :" + port;
			if(password != null){
				s += ", password: " + password;
			}
			throw new ClientException("NK: open fial with [" +s + "]");
		}
		channelSftp = (ChannelSftp)channel;
		return "OK:connected";
	}
	
	public String close(){
		if(channelSftp != null){
			channelSftp.quit();
			return "OK";
		}else{
			return "NK: open first";
		}
	}
	public String pwd(){
		try {
			return "OK:" + channelSftp.pwd();
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();
		}
	}
	public String lpwd(){
		return "OK:" + channelSftp.lpwd();
	}
	public String cwd(){
		try {
			return "OK:\nremote: " + channelSftp.pwd()+"\nlocal: "+ channelSftp.lpwd();
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();
		}
	}
	public String cd(String dir){
		try {
			if(dir.equals("~")){
				String home = channelSftp.getHome();
				channelSftp.cd(home);
			}else{
				channelSftp.cd(dir);
			}
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();

		}
		return "OK:" + pwd();
	}
	public String lcd(String dir){
		try {
			channelSftp.lcd(dir);
			return "OK:" + channelSftp.lpwd();
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();
		}
	}
	public String rename(String oldName, String newName){
		try {
			channelSftp.rename(oldName, newName);
			return "OK:" + oldName + "->" + newName;
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();			
		}
	}
	public String rmDir(String folderName){
		try {
			channelSftp.rmdir(folderName);
			return "OK";
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();			
		}
	}
	public String rm(String fileName){
		try {
			channelSftp.rm(fileName);
			return "OK";
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();			

		}
	}
	private String ls(String path, String pattern, String colSeperator, String rowSeperatoer){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String ymdTime = null;
		String strSize = null;
		StringBuilder sb = new StringBuilder();
		sb.append("OK:");
		Vector<ChannelSftp.LsEntry> list = null;
		try {
			channelSftp.cd(path);
			list = channelSftp.ls(pattern);
			for(LsEntry lsEntry : list){
				if( lsEntry.getAttrs().isDir()){
					Date date = new Date(lsEntry.getAttrs().getMTime()*1000L);
					sdf.setTimeZone(TimeZone.getTimeZone("GMT+9"));
					ymdTime = sdf.format(date);
					sb.append("D").append(colSeperator).append(lsEntry.getFilename())
						.append(colSeperator).append(ymdTime)
						.append(rowSeperatoer);
				}else if(lsEntry.getAttrs().isLink()){
					Date date = new Date(lsEntry.getAttrs().getMTime()*1000L);
					sdf.setTimeZone(TimeZone.getTimeZone("GMT+9"));
					ymdTime = sdf.format(date);
					sb.append("L").append(colSeperator).append(lsEntry.getFilename())
						.append(colSeperator).append(ymdTime)
						.append(rowSeperatoer);
				}else {
					Date date = new Date(lsEntry.getAttrs().getMTime()*1000L);
					sdf.setTimeZone(TimeZone.getTimeZone("GMT+9"));
					ymdTime = sdf.format(date);
					strSize = CommUtils.rightPadding( lsEntry.getAttrs().getSize() );
					sb.append("F").append(colSeperator).append(lsEntry.getFilename())
						.append(colSeperator).append(strSize)
						.append(colSeperator).append(ymdTime)
						.append(rowSeperatoer);					
				}
					
			}
			return sb.toString();
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();		
		}
	}
	public String ls(){
		return ls(".", "*", "\t", "\n");
	}
	public String ls(String pattern){
		return ls(".", pattern, "\t", "\n");
	}
	private String lls(String path, String pattern, String colSeperator, String rowSeperator ){
		try {
			channelSftp.lcd(path);
			String localCurrentDir = channelSftp.lpwd();
			File dir = new File(localCurrentDir);
			FileFilter  fileFilter = new WildcardFileFilter(pattern);
			File[] files = dir.listFiles(fileFilter);
			StringBuilder sb = new StringBuilder();
			for(File file : files){
				if(file.isDirectory()){
					sb.append("D").append(colSeperator).append(file.getName())
					.append(colSeperator).append( CommUtils.createTimeOfFile(file))
					.append(rowSeperator);
				}else if(file.isFile()){
					sb.append("F").append(colSeperator).append(file.getName())
					.append(colSeperator).append( CommUtils.rightPadding( file.length() ) )
					.append(colSeperator).append( CommUtils.createTimeOfFile(file))
					.append(rowSeperator);
				}
			}
			return "OK:" + sb.toString();
		} catch (SftpException e) {
			if(log.isDebug()) e.printStackTrace();
			return "NK:" + e.getMessage();		
		}
	}
	public String lls(){
		return lls(".", "*", "\t", "\n");
	}
	public String lls(String pattern){
		return lls(".", pattern, "\t", "\n");
	}
	public String mkdir(String dir){
		String[] folders = dir.split("/");
		for(String folder : folders){
			if(folder.length() > 0){
				try {
					try {
						channelSftp.cd(folder);
					} catch (Exception e) {
						channelSftp.mkdir(folder);
						channelSftp.cd(folder);
					}
				} catch (Exception e) {
					return "NK:" + e.getMessage();
				}
			}
		}
		return "OK";
	}
	public String put(String filePath, String newFileName, boolean flag){
		File file = new File(filePath);
		if(filePath.equals(newFileName)){
			if( CommUtils.hasPathChar(newFileName)){
				newFileName = file.getName();
			}
		}else{
			if( CommUtils.hasPathChar(newFileName)){
				return "NK: new file name have not allowed character";
			}
		}
		
		log.debug("remote file name: " + newFileName);
		if(file.exists() && file.isFile()) {
			String result = upload(file, newFileName);
			if(result.startsWith("OK") && flag){
				File flagFile = CommUtils.createFlagFile(newFileName, "flg");
				if(flagFile == null){
					return "NK: flag file create fail";
				}
				return upload(flagFile, flagFile.getName());
			}
			return result;
		}else {
			return "NK:" + file.getAbsolutePath() + " is not exist";
		}
	}
	public String put(String filePath, boolean flag){
		return put(filePath, filePath, flag);
	}
	
	public String get(String fileName){
		return get(fileName, fileName);
	}
	public String get(String fileName, String newFileName){
		return download(fileName, newFileName);
	}
	public String mget(String pattern){
		int i=0;
		String s = ls(pattern);
		if(s.startsWith("OK")){
			s = s.substring(3);
			String[] lines = s.split("\n");
			log.debug(lines.length + " files listed");
			for (String line : lines) {
				log.debug(line);
				String[] cols = line.split("\t");
				if(cols[0].equals("F")){
					String fileName = cols[1];
					String r= get(fileName);
					if(r.startsWith("OK")){
						i++;
					}
				}
			}
			return "OK:" + i + "files downloaded";
			
		}else{
			return s;
		}
	}
	public String mput(String pattern, boolean flag){
		String lpwd = channelSftp.lpwd();
		if( FilenameUtils.getFullPath(pattern).length() > 0	) {
			lpwd = FilenameUtils.getFullPath(pattern);
			pattern = FilenameUtils.getName(pattern);
		}
		File dir = new File(lpwd);
		FileFilter fileFilter = new WildcardFileFilter(pattern);
		File[] files = dir.listFiles(fileFilter);
		int success_count = 0, fail_count = 0;
		String fileName = null;
		String result = null;
		for (File file : files) {
			fileName = file.getAbsolutePath();
			result = put(fileName, flag);
			if(result.startsWith("OK")){
				success_count++;
			}else if(result.startsWith("NK")){
				fail_count++;
			}
		}
		return String.format("OK: total : %d, success: %d, fail: %d", (success_count + fail_count), success_count, fail_count);
	}
	public String echo(String arg){
		return "OK:" + arg;
	}
	public String help(){
		String s="OK:=== help for sftpClient " + VERSION + " ===\n\n"
				+ "help: display commands and short description\n"
				+ "exit(quit): close ftp session and quit sftpClient\n"
				+  "\n"
				+"pwd: display remote current directory\n"
				+"lpwd: display local current directory\n"
				+"cwd: display current directory of local and remote\n"
				+"cd dir : change remote current directory to dir\n"
				+"lcd dir : change local current directory to dir\n"
				+"ls(dir, ll) [pattern]: list files in remote current dir, pattern is like *.txt\n"
				+"lls(ldir, lll) [pattern]: list files in local current dir, pattern is like *.txt\n"
				+"\n"
				+"rename old new: change remote file name old to new\n"
				+"rmdir dir: remove remote dir if dir is empty\n"
				+"rm pattern: remove remote filename or pattern (ex: rm 1.txt, rm *.flg)\n"
				+"mkdir dir: create directory on remote\n"
				+"\n"
				+"get file: download file from remote to local\n"
				+"get file newfilename: download file from remote to local as newfilename\n"
				+"mget pattern: multi files download. (ex: mget *.txt)\n"
				+"\n"
				+"put file: upload file from local to remote\n"
				+"put file newfilename: upload file from local to remote as newfilename\n"
				+"mput pattern: multi files upload. (ex: mput *.txt)\n"
				+"\n"
				+"puf file: upload file and it's flag file\n"
				+"puf file newfilename: upload file and it's flag file as newfilename\n"
				+"mpuf pattern: multi files upload with flag files. (ex: mpuf *.txt)\n"
				+"\n"
				+ "echo msg: just display msg\n"
				+"multi command support: command1;command2 (ex: cd work; put 1.txt; get 2.txt; exit;)\n"
				+"\n"
				+"=== end help for sftpClient" + VERSION + " ===\n"
				+"\n"
				;
		return s;		
	}
}