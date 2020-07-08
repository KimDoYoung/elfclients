package kr.co.kalpa.elf.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class CommandParser {
	private static Set<String> RESERVED;
	public String name = null;
	public int argCount = 0;
	public String[] args = null;
	public String line = null;
	public Queue<String> macroQueue = null;
	
	public CommandParser(){
		String[] keys = {"pwd", "cd"};
		RESERVED = new HashSet<String>();
		RESERVED.addAll(Arrays.asList(keys));
		macroQueue = new LinkedList<String>();
		
		init();
	}
	public void parsing(String line){
		init();
		this.line = line;
		args = this.line.trim().split("\\s+");
		if(args.length > 0){
			this.name= args[0];
		}
		argCount = args.length - 1;
		
	}
	private void init(){
		name = null;
		argCount = 0;
		args = null;
		line = null;
		macroQueue.clear();
	}
	public String getCmd(){
		return name;
	}
	public String getArg(int i){
		if(args == null) return "";
		if(i<= argCount){
			return args[i];
		}else {
			return "";
		}
	}
	public boolean isValid(String line, int count){
		return (line.equals(name) && argCount == count);
	}
	public boolean isValid(String line){
		return isValid(line, 0);
	}
	public boolean isEmpty(){
		return (name.trim().length() < 1);
	}
	public void setMacro(String s){
		String[] lines = s.trim().split("\\s*[;]\\s*", -1);
		macroQueue.clear();
		for (String line : lines) {
			if(line.trim().length() < 1) continue;
			macroQueue.offer(line);
		}
	}
	public String getMacro(){
		if(macroQueue.isEmpty())return null;
		return macroQueue.poll();
	}
	@Override
	public String toString(){
		String s="";
		for(int i=1; i< args.length;i++){
			s += args[i] + " ";
		}
		return "cmd:" + name + ", argCount: " + argCount + ", args: "+s + "\n";
	}
	
}
