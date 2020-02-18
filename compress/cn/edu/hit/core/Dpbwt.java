package cn.edu.hit.core;

import java.util.ArrayList;
import java.util.LinkedList;

import cn.edu.hit.model.PBWTReadRe;
import cn.edu.hit.model.ReadElemEnum;
import cn.edu.hit.model.ReadPbwtResult;
import cn.edu.hit.test.ReadStruct;

public class Dpbwt
{
	public Dpbwt()
	{
	}

	public ReadPbwtResult PBWTAlgo(ArrayList<ReadStruct> readsList, int[] start, int[] end)
	{
		ArrayList<Integer> a = new ArrayList<Integer>(); // 存储为0的值
		ArrayList<Integer> b = new ArrayList<Integer>(); // 存储为1的值
		ArrayList<Integer> c = new ArrayList<Integer>();// 存储3

		ArrayList<Character> qualA = new ArrayList<Character>();
		ArrayList<Integer> removeIndex = new ArrayList<Integer>();

		ArrayList<ArrayList<Integer>> listsPBWT = new ArrayList<ArrayList<Integer>>();
		ArrayList<Character> listsPbwtQual = new ArrayList<Character>();
		ArrayList<ArrayList<String>> exceptionList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Character>> exceptionListQual = new ArrayList<ArrayList<Character>>();

		ArrayList<ReadStruct> readsCurrList = new ArrayList<ReadStruct>();
		ReadPbwtResult result = new ReadPbwtResult();

		int enter = 0, currPosDist = 0;
		int readsIndex = 0;
		int endindex = end.length - 1;
		int tmp = -1;
		// 找到最大覆盖范围以及检测是否按顺序排列
		for (int i = end.length - 1; i >= 0; i--)
		{
			if (end[i] >= tmp)
			{
				tmp = end[i];
				endindex = i;
			}
			if (i > 0 && start[i] < start[i - 1])
			{
				System.out.println("It doesn't sort by start");
				System.exit(0);
			}
		}

		// 真正的pbwt处理过程，位置信息是从1开始的，所以说不可能是0
		int pos = 0;
		for (pos = start[0]; pos <= end[endindex] + 1; pos++)
		{
			// 保证第一个位置不为0
			if (pos == 0)
			{
				pos = start[++enter] - 1;
				continue;
			}
			if (currPosDist == 0) // currposdist减到0,加入下一条序列
			{
				while (enter < end.length && start[enter] == pos) // 有新元素进队列的情形 liyang：这里这个设计非常的完美pos一直加，而currposdist一直减
				{
					readsCurrList.add(readsList.get(readsIndex++)); // liyang:序列从横向的排列变成了纵向的排列，把所有的开头位置一样的先存储一下
					enter++;
				}
				if (enter < end.length)
				{
					currPosDist = start[enter] - pos;
				}
			}
			currPosDist--;
			if (!readsCurrList.isEmpty())
			{
				ArrayList<Integer> listPBWT = new ArrayList<Integer>();
				ArrayList<String> exception = new ArrayList<String>();
				ArrayList<Character> exceptionQual = new ArrayList<Character>();
				
				for (int i = 0; i < readsCurrList.size(); i++)
				{
					// 一条read上的相对位置，从0开始到最后length-1
					int curVal = readsCurrList.get(i).getReads().get(pos - readsCurrList.get(i).getStartAlignment());
					char curValQual = 0; // 这里初值设置为0非常重要，应对是3的情况
//					只有当序列为1或者0才有质量分数，排除3的情况
					if (curVal == 1 || curVal == 0)
					{
						curValQual = readsCurrList.get(i).getReadQuality()
								.charAt(pos - readsCurrList.get(i).getStartAlignment());
					}
					if (readsCurrList.get(i).getStartAlignment() == pos)
					{
						if (curVal == 0)
						{
							qualA.add(curValQual);
						} else
						{
							exception.add(readsCurrList.get(i).getException().get(0));
							readsCurrList.get(i).getException().remove(0);
							exceptionQual.add(curValQual);
						}
						c.add(curVal);
					} else
					{
						int preVal = readsCurrList.get(i).getReads()
								.get(pos - readsCurrList.get(i).getStartAlignment() - 1);
						if (preVal == 0)
						{
							// 这样的话0,1,3三种情况都有了
							// 如果是3的话没有质量分数，说明横着看read已经结束了，所以做法就是删除该read
							// 如果是0的话将质量分数加入到A中，
							// 如果是1的话需要将异常值和异常信息放到一个特殊的容器中，这样这两个信息就相当于提前了
							// 暂时不要这部分，采用另一种策略详见12.16
							if (curVal == ReadElemEnum.END.ordinal())
							{
								removeIndex.add(i);
							} else if (curVal == 0)
							{
								qualA.add(curValQual);
							} else
							{
								exception.add(readsCurrList.get(i).getException().get(0));
								readsCurrList.get(i).getException().remove(0);
								exceptionQual.add(curValQual);

							}
							a.add(curVal); // crazy:这里注意一下当为结束符号3的时候同样也会加入到其中

						} else if (preVal == 1)
						{
							if (curVal == ReadElemEnum.END.ordinal())
							{
								removeIndex.add(i);
							} else if (curVal == 0)
							{
								qualA.add(curValQual);
							} else
							{
								// ArrayList<String> exception = new ArrayList<String>();
								exception.add(readsCurrList.get(i).getException().get(0));
								readsCurrList.get(i).getException().remove(0);
								// qualB.add(curValQual);
								exceptionQual.add(curValQual);
							}
							b.add(curVal);
						} else
						{
							System.out.println("Invalid ELSE readsCurrList process..");
						}
						
					}
				}
				
				// 异常值加入
				exceptionList.add(exception);
				// read序列加入
				for (Integer ins : a)
				{
					listPBWT.add(ins);
				}
				for (Integer ins : b)
				{
					listPBWT.add(ins);
				}
				for (Integer ins : c)
				{
					listPBWT.add(ins);
				}
				listsPBWT.add(listPBWT);
				
				// 这里应该先是稀疏化处理，然后才是均质化处理，
				// 这里丢给质量分数处理函数，处理。应该是单步在这里进行完成的
				int avg = 0;
				for (Character chr : qualA)
				{
					avg += chr;
				}
				// 这里注意，如果是大小为0的话，也需要加上一个0，站住这个位置
				if (qualA.size() == 0)
				{
					listsPbwtQual.add(null);
				} else
				{
					listsPbwtQual.add((char) (avg / qualA.size()));
				}
				// 异常质量分数
				exceptionListQual.add(exceptionQual);
				a.clear();
				b.clear();
				c.clear();
				qualA.clear();
				
				int offset = 0;
				for (Integer ins : removeIndex)
				{
					readsCurrList.remove(ins - offset);
					offset++;
				}
				removeIndex.clear();
			}
			result.setListsPBWT(listsPBWT);
			result.setListsExcep(exceptionList);
			result.setListExQual(exceptionListQual);
			result.setListsQual(listsPbwtQual);
		}
		System.out.println("Original PBWT(pbwtResult.length):\t");
		return result;
	}
	
	public ArrayList<ArrayList<Integer>> PBWTAlgoRe(ArrayList<ArrayList<Integer>> pbwtResult)
	{
		PBWTReadRe pbwtReadRe = new PBWTReadRe();
		ArrayList<ArrayList<Integer>> readsResult = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> readsCurrListTemp = new ArrayList<ArrayList<Integer>>();
		pbwtReadRe.setReadsResult(readsResult);
		
		ArrayList<Integer> d = new ArrayList<Integer>();
		ArrayList<Integer> e = new ArrayList<Integer>();

		int preVal = 0, preSize = 0; // 定义之前的碱基值和之前列的大小
		int currVal = 0, currSize = 0, currAddSize = 0; // 定义当前的
		ArrayList<Integer> removeIndexRe = new ArrayList<Integer>();
		Boolean removeFlag = false;
		int pre = 0; // 索引空间大小
		int cur = 0; // currentlist当前被删除元素之前序列数量
		LinkedList<Integer> posIndex = new LinkedList<Integer>(); // 位置索引表，表示插入到输出list中位置
		
		// 比对完之后从左往右
		for (int col = 0; col < pbwtResult.size(); col++)
		{
			preSize = readsCurrListTemp.size();
			currSize = pbwtResult.get(col).size(); // liyang：没有currsize的大小是1,这就说明了这里只是拿出了第一个容器
			currAddSize = currSize > preSize ? (currSize - preSize) : 0; // crazy：这里的目的是为了后面构造还原空间的
			// 只有对第一列处理比较特殊，其他都是正常，前一个大小就是前一列的大小
			for (int i = 0; i < currAddSize; i++)
			{
				ArrayList<Integer> listTemp = new ArrayList<Integer>();
				readsCurrListTemp.add(listTemp);
			}
			if (col == 0)
			{
				// 针对第一列的情形，做一下特殊化的处理，全部加入到临时temp中
				for (int i = 0; i < readsCurrListTemp.size(); i++)
				{
					readsCurrListTemp.get(i).add(pbwtResult.get(col).get(i));
				}
				continue;
			}
			// 还原的时候是按照存放之前位置的索引，来从前往后进行还原
			// 比对完之后从上往下
			for (int row = 0; row < preSize; row++)
			{
				preVal = readsCurrListTemp.get(row).get(readsCurrListTemp.get(row).size() - 1);
				currVal = pbwtResult.get(col).get(row);
				if (currVal == ReadElemEnum.END.ordinal())
				{
					removeFlag = true;
				}
				if (preVal == 0)
				{
					d.add(row);
				} else if (preVal == 1)
				{
					e.add(row);
				} else
				{
					System.out.println("Invalid ELSE");
				}
			}
			Integer[] f = new Integer[currSize];
			int k = 0;
			for (Integer ins : d)
			{
				f[ins] = pbwtResult.get(col).get(k++);
			}
			for (Integer ins : e)
			{
				f[ins] = pbwtResult.get(col).get(k++);
			}
			for (; k < currSize; k++)
			{
				f[k] = pbwtResult.get(col).get(k);
			}
			for (int m = 0; m < currSize; m++)
			{
				readsCurrListTemp.get(m).add(f[m]);
			}
			// 这里是将要删除的索引位置加入到removeIndeRe中，这个位置是currentlist中的位置
			if (removeFlag)
			{
				for (int i = 0; i < readsCurrListTemp.size(); i++)
				{
					if (readsCurrListTemp.get(i).get(readsCurrListTemp.get(i).size() - 1) == ReadElemEnum.END.ordinal())
					{
						removeIndexRe.add(i);
					}
				}
			}
			int offset = 0; // 因为有偏移，所以需要offset
			for (Integer ins : removeIndexRe)
			{
				pre = posIndex.size();
				cur = ins - offset;
				if (pre == cur)
				{
					// 加在末尾
					readsResult.add(readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				} else if (pre < cur)
				{
					// 在末尾加上站位符号，并将删除的序列加入到最后
					for (int i = 0; i < (cur - pre); i++)
					{
						ArrayList<Integer> empty = new ArrayList<Integer>();
						readsResult.add(empty);
						posIndex.add(readsResult.size() - 1);
					}
					readsResult.add(readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				}
				// 此时表明之前没有出去的序列要出去
				else
				{
					int pos = posIndex.get(cur);
					posIndex.remove(cur);
					readsResult.set(pos, readsCurrListTemp.get(ins - offset));
//					System.out.println(readsCurrListTemp.get(ins - offset));
					readsCurrListTemp.remove(ins - offset);
				}
				offset++;
			}
			removeIndexRe.clear();
			d.clear();
			e.clear();
			removeFlag = false;
		}
		System.out.println("PBWT Convert Re.(readResult.length):\t");
		return readsResult;
	}
	
}
