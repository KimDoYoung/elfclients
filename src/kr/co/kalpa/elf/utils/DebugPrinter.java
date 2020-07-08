package kr.co.kalpa.elf.utils;

public class DebugPrinter {

	private boolean verbose = false;
	
	public DebugPrinter() {
	}
	public DebugPrinter(boolean verbose) {
		this.verbose = verbose;

	}

	public void setVerbose(boolean verbose){
		this.verbose = verbose;
	}

	public boolean isDebug() {
		return verbose;
	}
	public void debug(String msg){
		if(verbose) {
			System.out.println(msg);
		}
	}
	public void error(String error){
		System.err.println("ERROR: " + error);
	}

}
