package kr.co.kalpa.elf.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Reporter {

	private Long startTime;
	private Long endTime;
	private List<String> listDownload;
	private List<String> listUpload;
	private List<String> listNK;
	
	public Reporter(){
		listDownload = new ArrayList<String>();
		listUpload = new ArrayList<String>();
		listNK = new ArrayList<String>();
		start();
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	public void end() {
		endTime = System.currentTimeMillis();
	}
	public void addUpload(String s) {
		listUpload.add(s);
	}
	public void addDownload(String s) {
		listDownload.add(s);
	}
	public void addNk(String s){
		listNK.add(s);
	}
	public String report(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StringBuilder sb = new StringBuilder();
		sb.append("---> start time : " + sdf.format(startTime)).append("\n");
		if(listNK.size() > 0){
			sb.append("NK: fail Count: " + listNK.size()).append("\n");
			sb.append("\t").append("NK List").append("\n");
			for (String nk : listNK) {
				sb.append(nk).append("\n");
				
			}
		}else{
			sb.append("OK:\n");
		}
		sb.append("\t").append("upload(").append(listUpload.size()).append(")").append("\n");
		for (String up : listUpload) {
			sb.append("\t ").append(up).append("\n");
		}
		sb.append("\t").append("download(").append(listDownload.size()).append(")").append("\n");
		for (String down: listDownload) {
			sb.append("\t ").append(down).append("\n");
		}
		String elapseTime = CommUtils.elapsedTimeHuman(endTime-startTime);
		sb.append("<--- end time: " + sdf.format(endTime))
		.append(" (").append(elapseTime).append(")").append("\n");
		
		return sb.toString();
	}
}
