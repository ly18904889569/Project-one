package cn.edu.hit.model;

public class AuxiliaryInfo
{
	public int rowReadsCount;
	public int exCount;
	public int exQualCount;
	public int QualCount;
	public int start;
	public int end;
	public int endRowReadsCount;
	public int endExCount;
	public int endExQualCount;
	public int endQualCount;
	public int realstart;
	
	public AuxiliaryInfo()
	{
		rowReadsCount = 0;
		exCount = 0;
		exQualCount = 0;
		QualCount = 0;
	}
	
	public AuxiliaryInfo(int start, int end)
	{
		rowReadsCount = 0;
		exCount = 0;
		exQualCount = 0;
		QualCount = 0;
		this.start = start;
		this.end = end;
	}
	
}
