package cn.edu.hit.testdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.tools.ant.taskdefs.optional.clearcase.ClearCase;

import cn.edu.hit.core.VerticalEncoding;
import cn.edu.hit.model.ReadElemEnum;
import htsjdk.variant.variantcontext.writer.BCF2FieldEncoder.Flag;

public class Test1
{
	// crazy:先取验证最简单的情况，等长对齐的情况
	public ArrayList<ArrayList<String>> exceptionList; 
	
	public ArrayList<ArrayList<String>> exceptionListQual;
	
	public ArrayList<ArrayList<Character>> listsPbwtQual;
	
	public ArrayList<ArrayList<Integer>> listsPBWT;
	
	public ArrayList<ArrayList<Character>> readQual;
	
	public int readStart[];
	
	public int readEnd[];
	
//	static int length = 8;	// read的长度
//	static int Num = 8;		// read的数量
//	int[] start = new int[] { 1, 1, 1, 1, 1, 1, 1, 1};
//	int[] end = new int[] { 8, 8, 8, 8, 8, 8, 8, 8 };
	
//	List<List<Integer>> listsOri = new ArrayList<>();
	public Test1()
	{
		exceptionList = new ArrayList<ArrayList<String>>();
		exceptionListQual = new ArrayList<ArrayList<String>>();
		listsPbwtQual = new ArrayList<ArrayList<Character>>();
		listsPBWT = new ArrayList<ArrayList<Integer>>();
		readQual = new ArrayList<ArrayList<Character>>();
//		int length = 8;	// read的长度
//		int Num = 8;		// read的数量
//		int[] start = new int[] { 1, 1, 1, 1, 1, 1, 1, 1};
//		int[] end = new int[] { 8, 8, 8, 8, 8, 8, 8, 8 };
//		Random rand = new Random();
//		for(int i = 0; i < Num; i++)
//		{
//			ArrayList<Integer> list = new ArrayList<Integer>();
//			ArrayList<Character> qualist = new ArrayList<Character>();
//			ArrayList<Character> readQualist = new ArrayList<Character>();
//			ArrayList<String> exqualist = new ArrayList<String>();
//			ArrayList<String> exlist = new ArrayList<String>();
//			int flag=0;
//			
////			list.add(ReadElemEnum.START.ordinal());	
//			for (int j = 0; j < length - 1 ; j++)
//			{
//				Character qua;
//				String exqua;
//				String exbase;
//				int base = rand.nextInt(2);
//				if (base == 1)
//				{
//					base = rand.nextInt(2);
//				}
//				list.add(base);
//				if(base == 0)
//				{
//					// 0的情况，不需要考虑异常质量分数，只需要随机生成正常质量分数就行
//					qua = randCreateQua();
//					qualist.add(qua);
//					readQualist.add(qua);
//				}
//				else
//				{
//					// 1的情况，不需要考虑正常质量分数，只需要生成异常值和异常质量分数就行
//					exqua = randCreatexQua();
//					exqualist.add(exqua);
//					readQualist.add(exqua.charAt(0));
//					exbase = randCreatex();
//					// 这里应该没有必要。
//					if (exbase.length()>0)
//					{
//						exlist.add(exbase);
//						flag = 1;
//					}	
//				}
//				
//			}
//			list.add(ReadElemEnum.END.ordinal());
////			listsOri.add(list);
//			listsPBWT.add(list);
//			listsPbwtQual.add(qualist);
//			readQual.add(readQualist);
////			exceptionListQual.add(exqualist);
//			// 这里的问题就是，如果是全是0的情况下，reads记录缺少异常质量分数和异常值的信息
//			if (flag ==1)
//			{
//				exceptionList.add(exlist);
//				exceptionListQual.add(exqualist);
//			}
//		}
//		
//		readStart = start;
//		readEnd = end;
	}
	
	public Test1(int num)
	{
		exceptionList = new ArrayList<ArrayList<String>>();
		exceptionListQual = new ArrayList<ArrayList<String>>();
		listsPbwtQual = new ArrayList<ArrayList<Character>>();
		listsPBWT = new ArrayList<ArrayList<Integer>>();
		readQual = new ArrayList<ArrayList<Character>>();
		int start[];
		int end[];
		int length;
		int Num;
		int len[];
		Random rand = new Random();
		switch (num)
		{
//		最普通情况，等长对齐
		case 1:
			length = 8;	// read的长度
			Num = 8;		// read的数量
			start = new int[] { 1, 1, 1, 1, 1, 1, 1, 1};
			end = new int[] { 8, 8, 8, 8, 8, 8, 8, 8 };
			for(int i = 0; i < Num; i++)
			{
				ArrayList<Integer> list = new ArrayList<Integer>();
				ArrayList<Character> qualist = new ArrayList<Character>();
				ArrayList<Character> readQualist = new ArrayList<Character>();
				ArrayList<String> exqualist = new ArrayList<String>();
				ArrayList<String> exlist = new ArrayList<String>();
				int flag=0;
				
//				list.add(ReadElemEnum.START.ordinal());	
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
//				listsOri.add(list);
				listsPBWT.add(list);
				listsPbwtQual.add(qualist);
				readQual.add(readQualist);
//				exceptionListQual.add(exqualist);
				// 这里的问题就是，如果是全是0的情况下，reads记录缺少异常质量分数和异常值的信息
				if (flag ==1)
				{
					exceptionList.add(exlist);
					exceptionListQual.add(exqualist);
				}
			}
			
			readStart = start;
			readEnd = end;
			break;
//		等长不对齐（相互之间差一）
		case 2:
			length = 8;	// read的长度
			Num = 8;		// read的数量
			start = new int[] { 1, 2, 3, 4, 5, 6, 7, 8};
			end = new int[] { 8, 9, 10, 11, 12, 13, 14, 15};
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
//				listsOri.add(list);
				listsPBWT.add(list);
				listsPbwtQual.add(qualist);
				readQual.add(readQualist);
//				exceptionListQual.add(exqualist);
				// 这里的问题就是，如果是全是0的情况下，reads记录缺少异常质量分数和异常值的信息
				if (flag ==1)
				{
					exceptionList.add(exlist);
					exceptionListQual.add(exqualist);
				}
				
			}
			readStart = start;
			readEnd = end;
			break;
//	不等长，不对齐		
		case 3:
			len = new int[] { 8, 8, 10, 6, 20, 8, 9, 10};
			length = 8;	// read的长度
			Num = 8;		// read的数量
			start = new int[] { 1, 2, 2, 5, 6, 10, 11, 13};
			end = new int[8]; 
			for (int i = 0; i < Num; i++)
			{
				ArrayList<Integer> list = new ArrayList<Integer>();
				ArrayList<Character> qualist = new ArrayList<Character>();
				ArrayList<Character> readQualist = new ArrayList<Character>();
				ArrayList<String> exqualist = new ArrayList<String>();
				ArrayList<String> exlist = new ArrayList<String>();
				int flag=0;
				
				for (int j = 0; j < len[i] - 1 ; j++)
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
				listsPBWT.add(list);
				listsPbwtQual.add(qualist);
				readQual.add(readQualist);
				if (flag ==1)
				{
					exceptionList.add(exlist);
					exceptionListQual.add(exqualist);
				}
				end[i] = start[i] + len[i] - 1;
			}
			readStart = start;
			readEnd = end;
			break;
			
		case 4:
			len = new int[] { 6, 4, 2, 6, 6, 4};
//			length = 8;	// read的长度
			Num = 6;		// read的数量
			start = new int[] { 1, 2, 3, 10, 12, 12};
			end = new int[Num]; 
			for (int i = 0; i < Num; i++)
			{
				ArrayList<Integer> list = new ArrayList<Integer>();
				ArrayList<Character> qualist = new ArrayList<Character>();
				ArrayList<Character> readQualist = new ArrayList<Character>();
				ArrayList<String> exqualist = new ArrayList<String>();
				ArrayList<String> exlist = new ArrayList<String>();
				int flag=0;
				
				for (int j = 0; j < len[i] - 1 ; j++)
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
				listsPBWT.add(list);
				listsPbwtQual.add(qualist);
				readQual.add(readQualist);
				if (flag ==1)
				{
					exceptionList.add(exlist);
					exceptionListQual.add(exqualist);
				}
				end[i] = start[i] + len[i] - 1;
			}
			readStart = start;
			readEnd = end;
			break;
		case 5:
			len = new int[] { 6, 7, 8, 8, 11, 10, 9, 8, 17, 15, 13, 11, 18, 18, 9, 12, 9, 14};
			Num = 18;		// read的数量
			start = new int[] { 1, 1, 1, 1, 2, 3, 4, 5, 7, 8, 9, 30, 30, 31, 31, 31, 31, 32};
			end = new int[18]; 
			for (int i = 0; i < Num; i++)
			{
				ArrayList<Integer> list = new ArrayList<Integer>();
				ArrayList<Character> qualist = new ArrayList<Character>();
				ArrayList<Character> readQualist = new ArrayList<Character>();
				ArrayList<String> exqualist = new ArrayList<String>();
				ArrayList<String> exlist = new ArrayList<String>();
				int flag=0;
				
				for (int j = 0; j < len[i] - 1 ; j++)
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
				listsPBWT.add(list);
				listsPbwtQual.add(qualist);
				readQual.add(readQualist);
				if (flag ==1)
				{
					exceptionList.add(exlist);
					exceptionListQual.add(exqualist);
				}
				end[i] = start[i] + len[i] - 1;
			}
			readStart = start;
			readEnd = end;
		break;
		default:
			break;
		}
	}
	
	public ArrayList<ArrayList<String>> getExceptionList()
	{
		return exceptionList;
	}
	/**
	 * 生成异常信息，长度从1-4
	 * @return exception message
	 */
	public static String randCreatex()
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
	public static String randCreatexQua()
	{
		return randCreateQua().toString();
	}
	
	/**
	 * 产生匹配上的质量分数
	 * @return 字符ch
	 */
	public static Character randCreateQua()
	{
		// 暂时简单化处理，随机生成的数字，之后在修改精细一下
		Random rand = new Random();
		char ch;
		int exqua = rand.nextInt(30)+40;
		ch = (char)exqua;
		return ch;
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
	
//	public static void main(String[] args)
//	{
//		new Test1();
//	}
	
}
