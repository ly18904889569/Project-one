package cn.edu.hit.testdata;

import java.util.ArrayList;
import java.util.Random;

import cn.edu.hit.model.ReadElemEnum;

public class Test2
{
//	等长不对齐
	public ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
	
	public ArrayList<ArrayList<String>> exceptionListQual = new ArrayList<ArrayList<String>>();
	
	public ArrayList<ArrayList<Character>> listsPbwtQual = new ArrayList<ArrayList<Character>>();
	
	public ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
	
	public ArrayList<ArrayList<Character>> readQual = new ArrayList<ArrayList<Character>>();

	public int readStart[];
	
	public int readEnd[];
	
	public Test2()
	{
		int length = 8;	// read的长度
		int Num = 8;		// read的数量
		int[] start = new int[] { 1, 2, 3, 4, 5, 6, 7, 8};
		int[] end = new int[] { 8, 9, 10, 11, 12, 13, 14, 15};
		Random rand = new Random();
		for(int i = 0; i < Num; i++)
		{
			ArrayList<Integer> list = new ArrayList<Integer>();
			ArrayList<Character> qualist = new ArrayList<Character>();
			ArrayList<Character> readQualist = new ArrayList<Character>();
			ArrayList<String> exqualist = new ArrayList<String>();
			ArrayList<String> exlist = new ArrayList<String>();
			int flag=0;
			
			for (int j = 0; j < length - 1 ; j++)
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
				if(base == 0)
				{
					// 0的情况，不需要考虑异常质量分数，只需要随机生成正常质量分数就行
					qua = randCreateQua();
					qualist.add(qua);
					readQualist.add(qua);
				}
				else
				{
					// 1的情况，不需要考虑正常质量分数，只需要生成异常值和异常质量分数就行
					exqua = randCreatexQua();
					exqualist.add(exqua);
					readQualist.add(exqua.charAt(0));
					exbase = randCreatex();
					// 这里应该没有必要。
					if (exbase.length()>0)
					{
						exlist.add(exbase);
						flag = 1;
					}	
				}
				
			}
			list.add(ReadElemEnum.END.ordinal());
//			listsOri.add(list);
			listsPBWT.add(list);
			listsPbwtQual.add(qualist);
			readQual.add(readQualist);
//			exceptionListQual.add(exqualist);
			// 这里的问题就是，如果是全是0的情况下，reads记录缺少异常质量分数和异常值的信息
			if (flag ==1)
			{
				exceptionList.add(exlist);
				exceptionListQual.add(exqualist);
			}
			
		}
		readStart = start;
		readEnd = end;
	}
	
	private String randCreatex()
	{
		// 最简单的生成几个就行，异常情况为ATCGDN
		String str="";
		Random rand = new Random();
		int length = rand.nextInt(4)+1; // 1-4;
//		int cas = rand.nextInt(6)+1;
		for(int i=0; i<length; i++)
		{
			switch(rand.nextInt(6)+1)
			{
			case 1:
				// A
				str+="A";
				break;
			case 2:
				// T
				str+="T";
				break;
			case 3:
				// C
				str+="C";
				break;
			case 4:
				// G
				str+="G";
				break;
			case 5:
				// N
				str+="N";
				break;
			case 6:
				// D
				if(!str.isEmpty())
				{
					length = str.length();	// 能够终止循环
				}
				else
				{
					length = 1;
					str+="D";
				}
				break;
			default:
				break;
			}
		}
		return str;
	}
	
	private String randCreatexQua()
	{
		return randCreateQua().toString();
	}
	
	/**
	 * 产生匹配上的质量分数
	 * @return 字符ch
	 */
	private Character randCreateQua()
	{
		// 暂时简单化处理，随机生成的数字，之后在修改精细一下
		Random rand = new Random();
		char ch;
		int exqua = rand.nextInt(30)+40;
		ch = (char)exqua;
		return ch;
	}

	public ArrayList<ArrayList<String>> getExceptionList()
	{
		return exceptionList;
	}

	public void setExceptionList(ArrayList<ArrayList<String>> exceptionList)
	{
		this.exceptionList = exceptionList;
	}

	public ArrayList<ArrayList<String>> getExceptionListQual()
	{
		return exceptionListQual;
	}

	public void setExceptionListQual(ArrayList<ArrayList<String>> exceptionListQual)
	{
		this.exceptionListQual = exceptionListQual;
	}

	public ArrayList<ArrayList<Character>> getListsPbwtQual()
	{
		return listsPbwtQual;
	}

	public void setListsPbwtQual(ArrayList<ArrayList<Character>> listsPbwtQual)
	{
		this.listsPbwtQual = listsPbwtQual;
	}

	public ArrayList<ArrayList<Integer>> getListsPBWT()
	{
		return listsPBWT;
	}

	public void setListsPBWT(ArrayList<ArrayList<Integer>> listsPBWT)
	{
		this.listsPBWT = listsPBWT;
	}

	public ArrayList<ArrayList<Character>> getReadQual()
	{
		return readQual;
	}

	public void setReadQual(ArrayList<ArrayList<Character>> readQual)
	{
		this.readQual = readQual;
	}
	
	
	
}
