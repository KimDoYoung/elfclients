package kr.co.kalpa.elf.clients.ftp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import kr.co.kalpa.elf.clients.Client;
import kr.co.kalpa.elf.utils.CommUtils;
import kr.co.kalpa.elf.utils.DebugPrinter;

public class FtpClient extends Client {

	public static final String VERSION = "1.0";
	private static final String CS = "\t"; // column seperator
	private static final String RS = "\n"; // row seperator
	private final int KEEP_TIME = 300; // 5분

	private String host;
	private String userId;
	private String userPw;
	private int port;
	private DebugPrinter log;
	private String charset;

	private FTPClient ftp = null;

	public FtpClient(String host, DebugPrinter log) {
		this.host = host;
		this.log = log;
	}

	private String connect() {
		try {
			ftp.setDefaultPort(port);
			ftp.setControlKeepAliveReplyTimeout(KEEP_TIME);
			ftp.connect(host);
			ftp.login(userId, userPw);
			return "OK: connected on " + host + " " + userId + "/" + userPw;
		} catch (Exception e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String open() {
		if (ftp == null) {
			ftp = new FTPClient();
			return connect();
		} else if (ftp.isConnected()) {
			try {
				ftp.logout();
				ftp.disconnect();
				return connect();
			} catch (IOException e) {
				if (log.isDebug())
					e.printStackTrace();
				return "NK: " + e.getMessage();
			}
		} else {
			return connect();
		}
	}

	public String close() {
		if (ftp == null) {
			return "NK: ftp is not opened";
		} else if (ftp.isConnected()) {
			try {
				ftp.logout();
				ftp.disconnect();
				return "OK: closed";
			} catch (Exception e) {
				if (log.isDebug())
					e.printStackTrace();
				return "NK: " + e.getMessage();
			}
		} else {
			return "NK: ftp is not connected";
		}
	}

	public String cd(String path) {
		try {
			ftp.changeWorkingDirectory(path);
			String pwd = ftp.printWorkingDirectory();
			return "OK: " + pwd;
		} catch (IOException e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String lcd(String path) {
		boolean b = CommUtils.setCurrentWorkingDirectory(path);
		if (b) {
			return "OK: " + CommUtils.getCurrentWorkingDir();
		} else {
			return "NK: change local working directory failed";
		}
	}

	public String ls(String pattenr) {
		return ls();
	}

	public String ls() {
		String name = null;
		Long size = 0L;
		Calendar time = null;
		StringBuilder sb = new StringBuilder();
		try {
			FTPFile[] files = null;
			String pwd = ftp.printWorkingDirectory();
			log.debug("pwd: " + pwd);
			ftp.setFileType(FTP.BINARY_FILE_TYPE);

			files = ftp.listDirectories(pwd);
			for (FTPFile file : files) {
				name = file.getName();
				time = file.getTimestamp();
				sb.append("D").append(CS).append(name).append(CS).append(CommUtils.calendarToString(time)).append(RS);
			}

			files = ftp.listFiles(pwd);
			for (FTPFile file : files) {
				name = file.getName();
				size = file.getSize();
				time = file.getTimestamp();
				sb.append("F").append(CS).append(name).append(CS).append(size).append(CS)
						.append(CommUtils.calendarToString(time)).append(RS);
			}
			return "OK: " + sb.toString();
		} catch (Exception e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	private String lls(String path, String pattern, String colSperator, String rowSeperator) {
		CommUtils.setCurrentWorkingDirectory(path);
		String localCurrentDir = CommUtils.getCurrentWorkingDir();
		File dir = new File(localCurrentDir);
		FileFilter fileFilter = new WildcardFileFilter(pattern);
		File[] files = dir.listFiles(fileFilter);
		StringBuilder sb = new StringBuilder();
		for (File file : files) {
			if (file.isDirectory()) {
				sb.append("D").append(colSperator).append(file.getName()).append(colSperator)
						.append(CommUtils.createTimeOfFile(file)).append(rowSeperator);
			} else if (file.isFile()) {
				sb.append("F").append(colSperator).append(file.getName()).append(colSperator).append(file.length())
						.append(colSperator).append(CommUtils.createTimeOfFile(file)).append(rowSeperator);
			}
		}
		return "OK: " + sb.toString();
	}

	public String lls(String wildcard) {
		return lls(".", wildcard, CS, RS);
	}

	public String lls() {
		return lls(".", "*", CS, RS);
	}

	private String upload(File file) {
		return upload(file, file.getName());
	}

	private String upload(File file, String remoteFileName) {
		try (InputStream is = new FileInputStream(file)) {
			boolean success = ftp.storeFile(remoteFileName, is);
			if (success) {
				return "OK: " + remoteFileName + " uploaded on " + ftp.printWorkingDirectory();
			} else {				
				return "NK: upload fail " + getServerReply();
			}
		} catch (Exception e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String put(String filePath, boolean createFlagFile) {
		return put(filePath, FilenameUtils.getName(filePath), false);
	}

	public String put(String filePath, String newFileName, boolean createFlagFile) {
		File file = new File(filePath);
		if (file.exists() == false) {
			return "NK: " + file.getAbsolutePath() + " is not exist";
		}
		return upload(file, newFileName);
	}

	private String download(String remote, File localFile) {
		try (OutputStream os = new FileOutputStream(localFile)) {
			boolean success = ftp.retrieveFile(remote, os);
			if (success) {
				return "OK: " + remote + " is downloaded";
			} else {
				return "NK: " + remote + " is failed";
			}
		} catch (Exception e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String get(String fileName) {
		return get(fileName, fileName);
	}

	public String get(String remote, String newFileName) {
		String localFilePath = newFileName;
		File file = new File(localFilePath);
		if (file.isFile() == false) {
			return "NK: " + file.getAbsolutePath() + " is NOT file";
		} else if (file.exists() == false) {
			return "NK: " + file.getAbsolutePath() + " is NOT exist";
		}
		return download(remote, file);
	}

	public String mget(String arg) {
		return null;
	}

	public String mput(String arg, boolean createFlagFile) {
		return null;
	}

	public String pwd() {
		try {
			String pwd = ftp.printWorkingDirectory();
			return "OK: " + pwd;
		} catch (Exception e) {
			if (log.isDebug())
				e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String lpwd(){
			String cwd = CommUtils.getCurrentWorkingDir();
			if(cwd != null){
				return "OK: " + cwd;	
			}else{
				return "NK: fail local current working directory";
			}
	}

	public String cwd() {
		String pwd = null;
		String lpwd = null;
		try {
			pwd = ftp.printWorkingDirectory();
			lpwd = CommUtils.getCurrentWorkingDir();
			return "OK: remote: " + pwd + "\nlocal: " + lpwd;
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}
	public String status(){
		try {
			return "OK: " + ftp.getStatus();
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String ascii(){
		try {
			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			return "OK: file mode changed to ASCII";
		} catch (Exception e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}
	public String binary(){
		try {
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			return "OK: file mode changed to BINARY";
		} catch (Exception e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}
	
	public String rename(String oldName, String newName) {
		try {
			boolean b = ftp.rename(oldName, newName);
			if(b) {
				return "OK: rename success";
			}else {
				String serverReply = getServerReply();
				return "NK: rename failed " + serverReply; 				
			}
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}
	public String rm(String filePath) {
		try {
			boolean b = ftp.deleteFile(filePath);
			if(b) {
				return "OK: removed " + filePath;
			}else {
				String serverReply = getServerReply();
				return "NK: remove directory " +filePath + " is failed. " + serverReply; 				
			}
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String rmdir(String dir) {
		try {
			boolean b = ftp.removeDirectory(dir);
			if(b) {
				return "OK: removed " + dir;
			}else {
				String serverReply = getServerReply();
				return "NK: remove directory " + dir + " is failed. " + serverReply; 				
			}
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();
		}
	}

	public String mkdir(String dir) {
		try {
			boolean b = ftp.makeDirectory(dir);
			if(b) {
				return "OK: " + dir + " is created";
			}else {
				String serverReply = getServerReply();
				return "NK: create directory " + dir + " is failed. " + serverReply; 
			}
		} catch (IOException e) {
			if (log.isDebug()) e.printStackTrace();
			return "NK: " + e.getMessage();

		}
	}

	public String echo(String msg) {
		return "OK: " + msg;
	}

	
	public String help(){
		String s= "OK: === help for ftpClient " + VERSION +" ===\n"
		+ "exit: quit ftpClient\n"
		+ "pwd: display current working directory of remote\n"
		+ "lpwd : display current working directory of local\n"
		+ "cwd: display both local and remote current working directory\n"
		+ "asc[ii]: change file mode to ASCII\n"
		+ "bin[ary]: change file mode to BINARY\n"
		+ "ls(dir) [pattern]: display file list of remote with pattern\n"
		+ "lls(ldir) [pattern]: display file list of local with pattern\n"
		+ "status: display remote ftp server information\n"
		+ "cd dir: change remote current directory to dir\n"
		+ "lcd dir: change local current directory to dir\n"
		+ "get file: download file from remote to local\n"
		+ "mget pattern: download files from remote to local with pattern\n" 
		+ "put file: upload file to server\n"
		+ "mput pattern: upload files which match with pattern\n"
		+ "=== end help for ftpClient " + VERSION + " ===\n";
		
		return s;
				
				
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserPw() {
		return userPw;
	}

	public void setUserPw(String userPw) {
		this.userPw = userPw;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public DebugPrinter getLog() {
		return log;
	}

	public void setLog(DebugPrinter log) {
		this.log = log;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public int getKEEP_TIME() {
		return KEEP_TIME;
	}
    
	/**
	 * 서버가 명령을 처리한 후의 반응 메세지
	 * @return
	 */
	private String getServerReply() {
        String[] replies = ftp.getReplyStrings();
        if (replies != null && replies.length > 0) {
        	return String.join("\n" , replies);
        }
        return "";
    }
}
