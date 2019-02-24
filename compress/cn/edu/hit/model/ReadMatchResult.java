package cn.edu.hit.model;

import java.util.List;

/**
 * 比对后的纵向、横向信息
 * @author rivers
 * Jun 11, 2017 3:22:55 PM
 *
 */
public class ReadMatchResult {
	private int alignmentStart;
	private int alignmentEnd;
	//reads比对后的结果
	private List<READSYMBOL> reads;	//liyang:在这里每个read上的任意位置的字符都可以取到
	//异常信息
	private List<String> exceptionInfo;
	//是否需要对比对结果处理，主要是针对insert和*的情形
	private boolean reCheck;

	private String readQuality;
	
	private List<String> exceptionQuality;
	
	
	
	
	public String getReadQuality() {
		return readQuality;
	}
	public void setReadQuality(String readQuality) {
		this.readQuality = readQuality;
	}
	public List<READSYMBOL> getReads() {
		return reads;
	}
	public void setReads(List<READSYMBOL> reads) {
		this.reads = reads;
	}
	public List<String> getExceptionInfo() {
		return exceptionInfo;
	}
	public void setExceptionInfo(List<String> exceptionInfo) {
		this.exceptionInfo = exceptionInfo;
	}
	public List<String> getExceptionQuality() {
		return exceptionQuality;
	}
	public void setExceptionQuality(List<String> exceptionQuality) {
		this.exceptionQuality = exceptionQuality;
	}
	public boolean isReCheck() {
		return reCheck;
	}
	public void setReCheck(boolean reCheck) {
		this.reCheck = reCheck;
	}
	public int getAlignmentStart() {
		return alignmentStart;
	}
	public void setAlignmentStart(int alignmentStart) {
		this.alignmentStart = alignmentStart;
	}
	public int getAlignmentEnd() {
		return alignmentEnd;
	}
	public void setAlignmentEnd(int alignmentEnd) {
		this.alignmentEnd = alignmentEnd;
	}
	
	
	
}
