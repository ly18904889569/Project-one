package cn.edu.hit.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.edu.hit.model.READSYMBOL;
import cn.edu.hit.model.ReadMatchResult;
import cn.edu.hit.model.ReadsPreProcessResult;

public class TestMain {
	public static void main(String[] args) {
		System.out.println("xxx");
		Boolean flag = false;
		TestMain main = new TestMain();
		main.refTest(flag);
		System.out.println(flag);
		
		List<String> list = new ArrayList<String>();
		list.add("ABC");
		list.add("ABCD");
		list.add("ABCDE");
		list.remove(list.size() - 1);
		list.add("ABCDEF");
		for (String str : list) {
			System.out.println(str);
		}
		System.out.println(1&0);
		System.out.println(4&1);
		
		main.hashTest();
		main.startHashTest();
		main.generateData();
		main.convertTest();
		
		main.testAnd();
		
	}
	
	int refTest(Boolean flag) {
		flag = Boolean.TRUE;
		return 1;
	}
	
	void hashTest(){
		int[] end = new int[]{1, 2, 3, 2, 2, 2, 3, 1};
		HashMap<Integer, Integer> endHash = new HashMap<Integer, Integer>();
		for (int i = 0; i < end.length ; i++){
			if (endHash.containsKey(end[i])) {
				;
				endHash.replace(end[i], endHash.get(end[i])+ 1);
			}else {
				endHash.put(end[i], 1);
			}
		}
		System.out.println(endHash.get(1));
		System.out.println(endHash.get(2));
		System.out.println(endHash.get(3));
	}
	
	void startHashTest() {
		
		Integer[] start = {1, 5, 9, 13};
		Integer[] end = new Integer[] {3, 7, 11, 17};
		HashMap<Integer, Integer> startHash = new HashMap<Integer, Integer>();
		for (int i = 0; i < start.length; i++) {
			if (startHash.containsKey(start[i])) {
				startHash.replace(start[i], startHash.get(start[i]) > end[i] ? startHash.get(start[i]) : end[i]);
			} else {
				startHash.put(start[i], end[i]);
			}
		}
		System.out.println("start hash");
		System.out.println(startHash.get(1));
		System.out.println(startHash.get(12));
		System.out.println(startHash.get(13));
		
		// 统计一下可能出现跳跃的位点
		HashMap<Integer, Integer> gapHash = new HashMap<Integer, Integer>();
		Set<Integer> mySet = new HashSet<Integer>(Arrays.asList(start));
		List<Integer> startSorted = new ArrayList<Integer>(mySet);
		Collections.sort(startSorted);
		for (int i = 1; i < startSorted.size(); i++) {
			if(startSorted.get(i) > startHash.get(startSorted.get(i - 1))) {
				gapHash.put(startHash.get(startSorted.get(i - 1)), startSorted.get(i) - startHash.get(startSorted.get(i - 1)));
			}
		}
		
		for(int i =0 ; i < end.length; i++)
		System.out.println("gapHash:\t" + gapHash.get(end[i]));
		
		
	}
	
	ReadsPreProcessResult generateData(){
		int[] start = new int[]{1, 2, 3, 5, 10, 11, 14};
		int[] end = new int[]{4, 5, 6, 8, 12, 14, 14};
		ReadsPreProcessResult res = new ReadsPreProcessResult();
		List<ReadMatchResult> models = new ArrayList<ReadMatchResult>();
		for (int i = 0; i < start.length; i++){
			ReadMatchResult m = new ReadMatchResult();
			m.setAlignmentStart(start[i]);
			m.setAlignmentEnd(end[i]);
			models.add(m);
		}
		
		READSYMBOL sy0 = READSYMBOL.E;
		READSYMBOL sy1 = READSYMBOL.I;
		models.get(0).setReads(Arrays.asList(sy1, sy0, sy1, sy0));
		models.get(1).setReads(Arrays.asList(sy0, sy0, sy1, sy1));
		System.out.println(models.get(1).getReads());
		
		return null;
	}
	
	void convertTest(){
		/*
		int val = 31;
		byte key = (byte)(val << 2 | 1);
		int key2 = val << 2 | 1;
		System.out.println(key);
		System.out.println(key2);
		System.out.println(key >>2);
		System.out.println(key & 3);
		*/
		int val = 33;
		int num1 = 33 / 32;
		int num2 = val % 32;
		byte by1 = (byte)(num1 | -128);
		byte by =  (byte)(num2 << 2 | 1);
		
		//转回来试试
		int byI1 = (by1 & 127) * 32;
		System.out.println(byI1);
		System.out.println(num2 >>2);
		System.out.println(by & 3);
		
		System.out.println((byte)((2 << 4) + 3));
		System.out.println(2 << 4);
		System.out.println(Byte.MAX_VALUE);
		System.out.println(7 ^ 8);
		System.out.println((7 & 8) > 0);
	}
	
	void testAnd(){
		System.out.println("TestAnd....");
		for(int i = 0; i < 16; i++){
			System.out.println(i & 7);
		}
	}
}
