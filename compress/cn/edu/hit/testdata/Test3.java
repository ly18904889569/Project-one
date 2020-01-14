package cn.edu.hit.testdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cn.edu.hit.model.ReadElemEnum;
import cn.edu.hit.test.ReadStruct;


public class Test3
{
	public ArrayList<ReadStruct> readsList;
//	我们需要按照结构体的要求创造数据
	public Test3(int num)
	{
		Random rand = new Random();
		readsList = new ArrayList<>();
		switch (num)
		{
//		仍然是先是创造一个最普通的情况，对齐长度
		case 1:
			int avLen = 8;
			int n = 8;
			for(int i=0; i<n; i++)
			{
				ReadStruct read = new ReadStruct();
				int startAlignment = 1;
				int endAlignment = 8;
				List<Integer> list = new ArrayList<Integer>();
				String readQuality = null;
				List<String> exception = new ArrayList<>();
				List<String> exQuality = new ArrayList<>();
				int flag=0;
				
				for(int j=0; j<avLen - 1; j++)
				{
					Character qua;
					String exqua;
					String exbase;
					int base = rand.nextInt(2);
					if (base == 1)
					{
						base = rand.nextInt(2);
					}
					list.add(base);
					if (base == 0)
					{
						qua = Test1.randCreateQua();
//						这样做字符串的加操作速度会非常的慢，但是我们生成少量数据，这里不重要
						readQuality+=qua;
					}
					else
					{
						exqua = Test1.randCreatexQua();
						exQuality.add(exqua);
						readQuality+=exqua;
						exbase = Test1.randCreatex();
						// 这里应该没有必要。
						if (exbase.length()>0)
						{
							exception.add(exbase);
							flag = 1;
						}	
					}
				}
				list.add(ReadElemEnum.END.ordinal());
				read.setReads(list);
				read.setReadQuality(readQuality);
				if (flag == 1)
				{
					read.setException(exception);
					read.setExceptionQuality(exQuality);
				}
				read.setStartAlignment(startAlignment);
				read.setEndAlignment(endAlignment);
				
				readsList.add(read);
			}
			
			break;

		default:
			break;
		}
	}
	
}
