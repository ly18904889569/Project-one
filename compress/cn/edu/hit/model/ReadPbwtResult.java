package cn.edu.hit.model;

import java.util.ArrayList;

public class ReadPbwtResult
{
	private ArrayList<ArrayList<Integer>> listsPBWT;
	
	private ArrayList<Character> listsQual;
	
	private ArrayList<ArrayList<String>> listsExcep;
	
	private ArrayList<ArrayList<Character>> listExQual;

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
