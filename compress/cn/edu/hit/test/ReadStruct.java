package cn.edu.hit.test;

import java.util.List;
/**
 * read转换为0,1.带有起始位置和终止位置
 * @author yangli
 *
 */
public class ReadStruct {
	
	//起始位置
	int startAlignment;
	//已经处理过的Reads序列，这里仅0、1表示一下，方便处理
	List<Integer> reads;
	int endAlignment;
	
	//异常信息记录进来
	List<String> exception;
	
	//把质量数等信息也加入进来吧
	private String readQuality;
	private List<String> exceptionQuality;	//这里是有问题的，正确的处理方式，在这里应该List的形式
	
	
	
	public String getReadQuality() {
		return readQuality;
	}
	public void setReadQuality(String readQuality) {
		this.readQuality = readQuality;
	}
	public List<String> getExceptionQuality() {
		return exceptionQuality;
	}
	public void setExceptionQuality(List<String> exceptionQuality) {
		this.exceptionQuality = exceptionQuality;
	}
	public List<String> getException() {
		return exception;
	}
	public void setException(List<String> exception) {
		this.exception = exception;
	}
	public int getEndAlignment() {
		return endAlignment;
	}
	public void setEndAlignment(int endAlignment) {
		this.endAlignment = endAlignment;
	}
	public int getStartAlignment() {
		return startAlignment;
	}
	public void setStartAlignment(int startAlignment) {
		this.startAlignment = startAlignment;
	}
	public List<Integer> getReads() {
		return reads;
	}
	public void setReads(List<Integer> reads) {
		this.reads = reads;
	}
	
	
}
