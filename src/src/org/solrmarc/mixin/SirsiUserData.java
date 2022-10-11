package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

public class SirsiUserData {
	
	// {"id":"aad9ga","communityUser":false,"department":"Arts \u0026 Sciences Graduate","email":"aad9ga@virginia.edu","displayName":"Aldona A Dye",
//	"private":"false","description":"Graduate Student","barcode":"112907149","key":"454704","profile":"Graduate Student","standing":"OK","sirsiEm
//	ail":"aad9ga@virginia.edu","homeLibrary":"LEO","amountOwed":"0.00"}
	String computingId;
	boolean communityUser=false;
	String department;
	String email;
	String fullName;
	boolean isPrivate=false;
	String description;
	String universityId;
	String key;   
	String profile;
	String sirsiEmail;
	String homeLibrary;
	String amountOwed;
	boolean noAccount=false;
	
	private SirsiUserData()
	{
		
	}
	
	public static SirsiUserData initialize(String dir, String computingID)
	{
		return(initialize(dir, computingID, true));
	}

	public static SirsiUserData initialize(String dir, String computingID, boolean showError)
	{
		SirsiUserData data = new SirsiUserData();
		File dataFile = new File(dir, computingID + ".data");
		if (dataFile.exists()) 
		{
			try {
				initializeFromFile(data, dataFile);
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		else
		{
			if (showError)  
				System.err.println("Didn't find file:  "+ dataFile.getAbsolutePath());
		}
		return(data);	
	}
		
	public static void initializeFromFile(SirsiUserData data, File dataFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(dataFile));
		String userData = reader.readLine();
		reader.close();
		String[]  userDataParts = userData.replaceAll("[{}]",  "").split("\"?,\"");
		for (String part : userDataParts)
		{
			String[] keyValue = part.split(":");
			String key = keyValue[0].replaceAll("\"", "").trim();
			Object value = keyValue[1].replaceAll("\"", "").trim();
			Field field;
			try { 
				if (key.equals("id")) key = "computingId";
				if (key.equals("barcode")) key = "universityId";
				if (key.equals("displayName")) key = "fullName";
				if (key.equals("private")) key = "isPrivate";
				field = data.getClass().getDeclaredField(key);
				if (field.getAnnotatedType().getType().getTypeName().contentEquals("boolean"))
				{
					value = Boolean.parseBoolean(value.toString());
				}
				field.set(data, value);
			}
			catch (NoSuchFieldException nsfe)
			{
				
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
		
	}
}
