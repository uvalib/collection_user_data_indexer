package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class MapDataFile extends ReadSpreadsheetData {

	public static void main(String[] args)
	{
		MapDataFile mdf = new MapDataFile();
		try {
			mdf.processIt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processIt() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = in.readLine()) != null)
		{
			String[] parts = line.split(",", 2);
			String school = mapString(parts[0], "translation_maps/school_map.xml");
			String [] deptParts = parts[1].split("@");
			for (String dept: deptParts)
			{
				String deptLookup = mapString(dept, "translation_maps/department_map.xml");
				System.out.println(school + "," + deptLookup);
			}
			
		}
	
	}
	
	String mapString(String value, String translationMapName)
	{
        if (value == null || value.isEmpty()) return "";
//        if (value.contains("@"))
//		{
//        	String valueParts[] = value.split("@");
//        	for (String v : valueParts)
//        	{
//        		addResultWithLookup(result, key, v, translationMapName);
//        	}
//		}
        if (translationMapName != null) 
        {
        	String mappedValue;
			try {
				mappedValue = mapEntry(value, translationMapName, null);
	        	return(mappedValue);
			} catch (Exception e) {
	        	return(value);
			}
        }
        else 
        {
        	return(value);
        }

	}

	@Override
	public Map<String, Map<String, String>> processInputLine(String[] labels, String[] parts) {
		// TODO Auto-generated method stub
		return null;
	}
}
