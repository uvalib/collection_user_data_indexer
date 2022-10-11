package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;




public class SirsiRecordData {
	
	// {"id":"aad9ga","communityUser":false,"department":"Arts \u0026 Sciences Graduate","email":"aad9ga@virginia.edu","displayName":"Aldona A Dye",
//	"private":"false","description":"Graduate Student","barcode":"112907149","key":"454704","profile":"Graduate Student","standing":"OK","sirsiEm
//	ail":"aad9ga@virginia.edu","homeLibrary":"LEO","amountOwed":"0.00"}
//	protected static class ItemRecordList {
//		List<ItemRecord> itemRecords;
//	};
	
	
	protected static class SirsiRecord {
		String catkey;
		String language;
		String publishedYear;
		String title;
		String callNumberNarrow;
		List<String> itemRecords;
		List<String> subjectHeadings;
		private SirsiRecord()
		{
			catkey = "";
			language = "";
			publishedYear = "";
			callNumberNarrow = "";
			title = "";
			itemRecords = new ArrayList<String>();
			subjectHeadings = new ArrayList<String>();
		}
		public String getCatkey() {
			return catkey;
		}
		public String getLanguage() {
			return language;
		}
		public String getTitle() {
			return title;
		}
		public String getPublishedYear() {
			return publishedYear;
		}
		public List<String> getSubjectHeadings() {
			return subjectHeadings;
		}
		public String getCallNumberNarrow() {
			return callNumberNarrow;
		}
		public List<String> getItemRecords() {
			return itemRecords;
		}
		public String getItemWithCallNum(String callnum) {
			for (String item : this.getItemRecords())
			{
				if (item.contains("|$a|"+callnum+"|"))
				{
					return(item);
				}
			}
			return(null);
		}
	};
	
    public static Map<String, String[]> callnum2catkeymap = null;
    public static Map<String, SirsiRecord> catkeydatamap = null;

	private SirsiRecordData()
	{
	}
	
	public static SirsiRecord getSirsiRecordFromCallNumber(String callnum) 
	{
		SirsiRecord record = null;
		if (callnum2catkeymap == null)
		{
			callnum2catkeymap = new LinkedHashMap<String, String[]>();
			initializeCallNums("data", "callnum2catkey.txt");
		}
		if (catkeydatamap == null)
		{
			catkeydatamap = new LinkedHashMap<String, SirsiRecord>();
			initializeSirsiRecordData("data", "records_index.txt");
		}
		String fixCallNum = callnum.replaceAll(" ", "_").replaceAll("/", "-").replaceAll("^\"", "").replaceAll("\"$", "");
		if (callnum2catkeymap.containsKey(fixCallNum))
		{
			String[] catKeys = callnum2catkeymap.get(fixCallNum);
			if (catKeys.length == 1)
			{
				String catKey = catKeys[0];
				if (catkeydatamap.containsKey(catKey))
				{
					record = catkeydatamap.get(catKey);
					return(record);
				}
				else 
				{
		        	System.err.println("Didn't find record data for id: "+ catKey);
				}
			}
			else if (catKeys.length > 1)
			{
				String catKeyList = "";
				for (String catKey : catKeys)
				{
					catKeyList = catKeyList + catKey + " ";
					if (catkeydatamap.containsKey(catKey))
					{
						SirsiRecord tmprecord = catkeydatamap.get(catKey);
						if (tmprecord.getItemWithCallNum(callnum) != null)
					    {
							record = tmprecord;
					    }
					}
				}
				if (record == null) 
				{
		        	System.err.println("Didn't find matching record data for ids: "+ catKeyList);
				}

			}
		}
        else 
        {
        	System.err.println("Didn't find record id for callnum: "+ fixCallNum);
        }

		return(record);
	}
	
	//  |$a|M 1654 .N67|||$w|ALPHANUM|||$c|1|||$i|X030073907|||$d|6/28/1996|||$l|MCGR-VAULT|||$m|SPEC-COLL|||$r|Y|||$s|Y|||$t|RAREBOOK|||$u|6/28/1996|||$x|H-NOTIS|
	
	public static String getSubfieldDataForTag(String itemString, char tag)
	{
		String searchString = "|$"+tag+"|";
		int indexStart = itemString.indexOf(searchString);
		if (indexStart >= 0 && indexStart+6 < itemString.length()) 
		{
			String result = itemString.substring(indexStart+4, itemString.indexOf("|", indexStart+4));
			return(result);
		}
		return(null);
	}
	
	private static void initializeSirsiRecordData(String dir, String fname) 
	{
		File dataFile = new File(dir, fname);
		BufferedReader reader = null;
		if (dataFile.exists()) 
		{
			System.err.println("Opening file:  "+ dataFile.getAbsolutePath());
			try {
				reader = new BufferedReader(new FileReader(dataFile));
				String line = null;
				String prevCatKey = "";
				List<String> recordData = new ArrayList<String>();
				while ((line = reader.readLine()) != null)
				{
					if (line.startsWith(" INFO ")) continue;
					String[]  keyValue = line.split(" : ", 2);
					if (keyValue[0].equals(prevCatKey))
					{
						recordData.add(keyValue[1]);
					}
					else
					{
						if (prevCatKey.length() > 0)
						{
							SirsiRecord record = initRecordDataObj(prevCatKey, recordData);
							catkeydatamap.put(prevCatKey, record);
						}
						prevCatKey = keyValue[0];
						recordData = new ArrayList<String>();
						recordData.add(keyValue[1]);
					}
				}
			}
			catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else 
		{
			System.err.println("Didn't find file: "+ dataFile.getAbsolutePath());
		}
	}

	private static SirsiRecord initRecordDataObj(String prevCatKey, List<String> recordData) 
	{
		SirsiRecord record = new SirsiRecord();

		for (String data : recordData)
		{
			String[] keyValue = data.split(" = ", 2);
			if (keyValue[0].equals("language_str_stored"))
			{
				record.language = keyValue[1];
			}
			else if (keyValue[0].equals("pub_year_str_stored"))
			{
				record.publishedYear = keyValue[1];
			}
			else if (keyValue[0].equals("id"))
			{
				record.catkey = keyValue[1];
			}
			else if (keyValue[0].equals("call_number_narrow_f_stored"))
			{
				record.callNumberNarrow = keyValue[1];
			}
			else if (keyValue[0].equals("subject_str_stored"))
			{
				record.subjectHeadings.add(keyValue[1]);
			}
			else if (keyValue[0].equals("title_tsearch_stored"))
			{
				record.title = keyValue[1];
			}
			else if (keyValue[0].equals("999all"))
			{
				record.itemRecords.add(keyValue[1]);
			}
		}
		return(record);
	}

	public static void initializeCallNums(String dir, String fname)
	{
		File dataFile = new File(dir, fname);
		BufferedReader reader = null;
		if (dataFile.exists()) 
		{
			System.err.println("Opening file:  "+ dataFile.getAbsolutePath());
			try {
				reader = new BufferedReader(new FileReader(dataFile));
				String userData = null;
				while ((userData = reader.readLine()) != null)
				{
					String[]  keyValue = userData.split("[ ]?=[ ]?", 2);
					if (keyValue.length == 2)
					{
						String value = keyValue[1].replaceAll(":$", "");
						String[] catkeys = value.split(":");
						if (catkeys.length == 1 && !callnum2catkeymap.containsKey(keyValue[0]))
						{
							callnum2catkeymap.put(keyValue[0], catkeys);
						}
						else if (catkeys.length > 1 && !callnum2catkeymap.containsKey(keyValue[0]))
						{
							callnum2catkeymap.put(keyValue[0], catkeys);
						}
					}
				}
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally {
				if (reader != null) 
			    {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			    }
			}
		}
	}
	
	public static void initializeMapFromFile(SirsiRecordData data, File dataFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(dataFile));
		String userData = reader.readLine();
		reader.close();
		String[]  userDataParts = userData.replaceAll("[{}]",  "").split(",");
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
