package cn.edu.hit.model;

import java.util.ArrayList;
import java.util.List;

public class ReadPbwtResult
{
	private ArrayList<ArrayList<Integer>> listsPBWT;
	
	private ArrayList<ArrayList<String>> listsExcep;
	
	private List<String> listsExcep2;
	
	private ArrayList<Character> listsQual;
		
	private ArrayList<ArrayList<Character>> listExQual;
		
	private ArrayList<Character> listExQual2;
	
	private ArrayList<ArrayList<Integer>> indexes;
	
	private int[] startPos;
	
	private int minStart;
	
	
	
	public int getMinStart()
	{
		return minStart;
	}

	public void setMinStart(int minStart)
	{
		this.minStart = minStart;
	}

	public ArrayList<ArrayList<Integer>> getIndexes()
	{
		return indexes;
	}

	public void setIndexes(ArrayList<ArrayList<Integer>> indexes)
	{
		this.indexes = indexes;
	}

	public ArrayList<Character> getListExQual2()
	{
		return listExQual2;
	}

	public void setListExQual2(ArrayList<Character> listExQual2)
	{
		this.listExQual2 = listExQual2;
	}

	public List<String> getListsExcep2()
	{
		return listsExcep2;
	}

	public void setListsExcep2(List<String> listsExcep2)
	{
		this.listsExcep2 = listsExcep2;
	}


	public int[] getStartPos()
	{
		return startPos;
	}

	public void setStartPos(int[] is)
	{
		this.startPos = is;
	}

	public ArrayList<ArrayList<Integer>> getListsPBWT()
	{
		return listsPBWT;
	}

	public void setListsPBWT(ArrayList<ArrayList<Integer>> listsPBWT)
	{
		this.listsPBWT = listsPBWT;
	}

	public ArrayList<Character> getListsQual()
	{
		return listsQual;
	}

	public void setListsQual(ArrayList<Character> listsQual)
	{
		this.listsQual = listsQual;
	}

	public ArrayList<ArrayList<String>> getListsExcep()
	{
		return listsExcep;
	}

	public void setListsExcep(ArrayList<ArrayList<String>> listsExcep)
	{
		this.listsExcep = listsExcep;
	}

	public ArrayList<ArrayList<Character>> getListExQual()
	{
		return listExQual;
	}

	public void setListExQual(ArrayList<ArrayList<Character>> listExQual)
	{
		this.listExQual = listExQual;
	}
	
	

}
