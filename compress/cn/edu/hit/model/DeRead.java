package cn.edu.hit.model;

import java.util.ArrayList;

public class DeRead
{
	// 用于存储最终按一条read解压缩出来的序列
	private int start;
	private ArrayList<Integer> read;
	private ArrayList<Character> qual; // 用于记录正常质量分数 
	private ArrayList<Character> readQual; // 用于记录整条read质量分数
	private ArrayList<Character> exQual; // 用于记录异常质量分数
	private ArrayList<String>  exBase;
	private int flag; // 用于标记整条序列的组成情况
	
	public DeRead()
	{
		read = new ArrayList<>();
		qual = new ArrayList<>();
		readQual = new ArrayList<>();
		exQual = new ArrayList<>();
		exBase = new ArrayList<>();
		flag = 0;
	}

	public int getStart()
	{
		return start;
	}

	public void setStart(int start)
	{
		this.start = start;
	}

	public ArrayList<Integer> getRead()
	{
		return read;
	}

	public void setRead(ArrayList<Integer> read)
	{
		this.read = read;
	}

	public ArrayList<Character> getQual()
	{
		return qual;
	}

	public void setQual(ArrayList<Character> qual)
	{
		this.qual = qual;
	}

	public ArrayList<Character> getReadQual()
	{
		return readQual;
	}

	public void setReadQual(ArrayList<Character> readQual)
	{
		this.readQual = readQual;
	}

	public ArrayList<Character> getExQual()
	{
		return exQual;
	}

	public void setExQual(ArrayList<Character> exQual)
	{
		this.exQual = exQual;
	}

	public ArrayList<String> getExBase()
	{
		return exBase;
	}

	public void setExBase(ArrayList<String> exBase)
	{
		this.exBase = exBase;
	}

	public int getFlag()
	{
		return flag;
	}

	public void setFlag(int flag)
	{
		this.flag = flag;
	}
	
	
}
