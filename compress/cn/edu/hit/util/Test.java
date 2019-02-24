package cn.edu.hit.util;

public class Test {
	public static void main(String[] args){
	/*	System.out.println(ReadElemEnum.START.ordinal());
		//测试一下数组混合不同长度的情形
		List<Integer> list = new ArrayList();
		List<Integer> list2 = new ArrayList();
		List<List<Integer>> lists = new ArrayList();
		int i=3;
		while(i-->0)
			list.add(i);
		i=4;
		while(i-->0)
			list2.add(i);
		lists.add(list);
		lists.add(list2);
		System.out.println(lists.size());*/
		
		String cigar = "*";
		String[] cigars =cigar.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
		
		String str3 = "10000000";
		Byte by = (byte)Integer.parseInt(str3,2);
		String s2 = String.format("%8s", Integer.toBinaryString(by & 0xFF)).replace(' ', '0');
		System.out.println(by);
		System.out.println(s2);
	}
}
