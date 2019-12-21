package cn.edu.hit.model;

public class CompressResult
{
	private byte[] readsResult;
	private byte[] exceptionResult;
	private byte[] readQuaReasult;
	private byte[] exceptionQuaResult;
	private byte[] startResult;
	
	public byte[] getStartResult()
	{
		return startResult;
	}

	public void setStartResult(byte[] startResult)
	{
		this.startResult = startResult;
	}

	public CompressResult()
	{
		
	}

	public byte[] getReadsResult()
	{
		return readsResult;
	}

	public void setReadsResult(byte[] readsResult)
	{
		this.readsResult = readsResult;
	}

	public byte[] getExceptionResult()
	{
		return exceptionResult;
	}

	public void setExceptionResult(byte[] exceptionResult)
	{
		this.exceptionResult = exceptionResult;
	}

	public byte[] getReadQuaReasult()
	{
		return readQuaReasult;
	}

	public void setReadQuaReasult(byte[] readQuaReasult)
	{
		this.readQuaReasult = readQuaReasult;
	}

	public byte[] getExceptionQuaResult()
	{
		return exceptionQuaResult;
	}

	public void setExceptionQuaResult(byte[] exceptionQuaResult)
	{
		this.exceptionQuaResult = exceptionQuaResult;
	}
	
//	这里面还需要构造各个变量的大小
	

}
