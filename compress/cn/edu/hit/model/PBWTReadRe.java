package cn.edu.hit.model;

import java.util.ArrayList;
import java.util.List;

public class PBWTReadRe {
	
	// 回解回来的Reads值
	ArrayList<ArrayList<Integer>> readsResult;
	// 对应的异常信息情况
	ArrayList<ArrayList<String>> exception;
	
	ArrayList<Integer> startAlignment;
	
	ArrayList<Integer> endAlignment;
	
	//加入对质量数的处理
	ArrayList<Character> readsQual;
	String exceptionQual;
	
	
	public ArrayList<Character> getReadsQual() {
		return readsQual;
	}
	public void setReadsQual(ArrayList<Character> readsQual) {
		this.readsQual = readsQual;
	}
	public String getExceptionQual() {
		return exceptionQual;
	}
	public void setExceptionQual(String exceptionQual) {
		this.exceptionQual = exceptionQual;
	}
	public ArrayList<Integer> getStartAlignment() {
		return startAlignment;
	}
	public void setStartAlignment(ArrayList<Integer> startAlignment) {
		this.startAlignment = startAlignment;
	}
	public ArrayList<Integer> getEndAlignment() {
		return endAlignment;
	}
	public void setEndAlignment(ArrayList<Integer> endAlignment) {
		this.endAlignment = endAlignment;
	}
	public ArrayList<ArrayList<Integer>> getReadsResult() {
		return readsResult;
	}
	public void setReadsResult(ArrayList<ArrayList<Integer>> readsResult) {
		this.readsResult = readsResult;
	}
	public ArrayList<ArrayList<String>> getException() {
		return exception;
	}
	public void setException(ArrayList<ArrayList<String>> exception) {
		this.exception = exception;
	}
}
