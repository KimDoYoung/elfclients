package kr.co.kalpa.elf.clients;

import kr.co.kalpa.elf.utils.Reporter;

public class Client {
	protected Reporter  reporter;
	public Client(){
		reporter = new Reporter();
	}
	protected void reportDownload(String s){
		reporter.addDownload(s);
	}
	protected void reportUpload(String s){
		reporter.addUpload(s);
	}
	public void reportNK(String s){
		reporter.addNk(s);
	}
	public void workStart(){
		reporter.start();
	}
	public void workEnd(){
		reporter.end();
	}
	public String finalReport(){
		return reporter.report();
	}
}
