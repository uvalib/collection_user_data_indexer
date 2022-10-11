package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SirsiIRADataMap {
	class SirsiIRAData {
		private String filename;
		private String dateSaltedHash;
		private String saltedHash;
		private String computingID;
		private String universityID;
		private String checkoutDateMin;
		private String checkoutDateMax;
		private String academicCareerDesc;
		private String degreeSeeking;
		private String primaryAcademicProgramDesc;
		private String academicPlansDesc;
		private String planDegree;
		private String jobFamilyGroup;
		private String jobTitle;
		private String organizationName;
		private String mbuName;
		private String jobFamilyName;
		
		public SirsiIRAData(String[] lineParts)
		{
			this.filename = lineParts[0];
			this.dateSaltedHash = lineParts[1];
			this.saltedHash = lineParts[2];
			this.computingID = lineParts[3];
			this.universityID = lineParts[4];
			String datefix = fixDate(lineParts[5]);
			this.checkoutDateMin = datefix;
			this.checkoutDateMax = datefix;
			this.academicCareerDesc = lineParts[6];
			this.degreeSeeking = lineParts[7];
			this.primaryAcademicProgramDesc = lineParts[8];
			this.academicPlansDesc = lineParts[9];
			this.planDegree = (lineParts.length >= 11) ? lineParts[10] : "";
			this.jobFamilyGroup = (lineParts.length >= 12) ? lineParts[11] : "";
			this.jobTitle = (lineParts.length >= 13) ? lineParts[12] : "";
			this.organizationName = (lineParts.length >= 14) ? lineParts[13] : "";
			this.mbuName = (lineParts.length >= 15) ? lineParts[14] : "";
			this.jobFamilyName = (lineParts.length >= 16) ? lineParts[15] : "";
		}
		
		public String getUniversityID() {
			return universityID;
		}

		public String getCheckoutDateMin() {
			return checkoutDateMin;
		}

		public String getCheckoutDateMax() {
			return checkoutDateMax;
		}

		public String getAcademicCareerDesc() {
			return academicCareerDesc;
		}

		public String getDegreeSeeking() {
			return degreeSeeking;
		}

		public String getPrimaryAcademicProgramDesc() {
			return primaryAcademicProgramDesc;
		}

		public String getAcademicPlansDesc() {
			return academicPlansDesc;
		}

		public String getPlanDegree() {
			return planDegree;
		}

		public String getJobFamilyGroup() {
			return jobFamilyGroup;
		}

		public String getJobTitle() {
			return jobTitle;
		}

		public String getOrganizationName() {
			return organizationName;
		}

		public String getMbuName() {
			return mbuName;
		}

		public String getJobFamilyName() {
			return jobFamilyName;
		}

		boolean isEqualish(SirsiIRAData other)
		{
			boolean equals = saltedHash.equals(other.saltedHash) &&
					computingID.equals(other.computingID) &&
					universityID.equals(other.universityID) &&
					academicCareerDesc.equals(other.academicCareerDesc) &&
					degreeSeeking.equals(other.degreeSeeking) &&
					primaryAcademicProgramDesc.equals(other.primaryAcademicProgramDesc) &&
					academicPlansDesc.equals(other.academicPlansDesc) &&
					planDegree.equals(other.planDegree) &&
					jobFamilyGroup.equals(other.jobFamilyGroup) &&
					jobTitle.equals(other.jobTitle) &&
					organizationName.equals(other.organizationName) &&
					mbuName.equals(other.mbuName);
			return(equals);
		}
		
		boolean dateMatches(SirsiIRAData other)
		{
			boolean matches = dateMatches(other.checkoutDateMin) && dateMatches(other.checkoutDateMax);
			return(matches);
		}
		
		boolean dateMatches(String otherDate)
		{
			String otherDateTrimmed = otherDate.substring(0, 10);
			boolean matches = checkoutDateMin.compareTo(otherDateTrimmed) <= 0 &&
					checkoutDateMax.compareTo(otherDateTrimmed) >= 0;
			return(matches);
		}
		
		SirsiIRAData merge(SirsiIRAData other)
		{
			checkoutDateMin = (checkoutDateMin.compareTo(other.checkoutDateMin) < 0 ? checkoutDateMin : other.checkoutDateMin);
			checkoutDateMax = (checkoutDateMax.compareTo(other.checkoutDateMax) >= 0 ? checkoutDateMax : other.checkoutDateMax);
			return this;
		}

		SirsiIRAData combine(SirsiIRAData other)
		{
			checkoutDateMin = (checkoutDateMin.compareTo(other.checkoutDateMin) < 0 ? checkoutDateMin : other.checkoutDateMin);
			checkoutDateMax = (checkoutDateMax.compareTo(other.checkoutDateMax) >= 0 ? checkoutDateMax : other.checkoutDateMax);
			this.filename = combineStr(this.filename, other.filename);
			this.checkoutDateMin = (this.checkoutDateMin.compareTo(this.checkoutDateMin) < 0 ? this.checkoutDateMin : other.checkoutDateMin);
			this.checkoutDateMax = (this.checkoutDateMax.compareTo(this.checkoutDateMax) < 0 ? this.checkoutDateMax : other.checkoutDateMax);
			this.academicCareerDesc = combineStr(this.academicCareerDesc, other.academicCareerDesc);
			this.degreeSeeking = combineStr(this.degreeSeeking, other.degreeSeeking);
			this.primaryAcademicProgramDesc = combineStr(this.primaryAcademicProgramDesc, other.primaryAcademicProgramDesc);
			this.academicPlansDesc = combineStr(this.academicPlansDesc, other.academicPlansDesc);
			this.planDegree = combineStr(this.planDegree, other.planDegree);
			this.jobFamilyGroup = combineStr(this.jobFamilyGroup, other.jobFamilyGroup);
			this.jobTitle = combineStr(this.jobTitle, other.jobTitle);
			this.organizationName = combineStr(this.organizationName, other.organizationName);
			this.mbuName = combineStr(this.mbuName, other.mbuName);
			this.jobFamilyName = combineStr(this.jobFamilyName, other.jobFamilyName);
			return this;
		}

		private String combineStr(String str1, String str2)
		{
			String result;
			if (str1.length() == 0)   return(str2);
			if (str2.length() == 0)   return(str1);
			if (str1.contains(str2))  return(str1);
			else 
			{
				result = str1 + "@" + str2;
			}
			return result;
		}

		public int dateDistance(String dateInQuestion)
		{
			int dateMin = dateAsInt(checkoutDateMin);
			int dateMax = dateAsInt(checkoutDateMax);
			int dateQ = dateAsInt(dateInQuestion);
			int diff1 = Math.abs(dateMin - dateQ);
			int diff2 = Math.abs(dateMax - dateQ);
			return(Math.min(diff1, diff2));
		}

		private int dateAsInt(String dateStr)
		{
			int year = Integer.parseInt(dateStr.substring(0, 4));
			int month = Integer.parseInt(dateStr.substring(5, 7));
			int day = Integer.parseInt(dateStr.substring(8, 10));
			return year * 366 + month * 31 + day;
		}

	};
	
	private  Map<String, List<SirsiIRAData>> mapIraData = null;
	private int errCount = 0;
	private int expectedNumParts = -1;
	
	public SirsiIRADataMap()
	{
		mapIraData = new LinkedHashMap<String, List<SirsiIRAData>>();	
	}

	public static String fixDate(String dateIn)
	{
		Pattern mmddyyyy = Pattern.compile("(\\d\\d?)[/-](\\d\\d?)[/-](\\d\\d\\d\\d)");
		Matcher match = mmddyyyy.matcher(dateIn);
		String result;
		if (match.matches())
		{
			result = match.group(3) + "-" + fixMonthorDay(match.group(1)) + "-" + fixMonthorDay(match.group(2));
			return(result);
		}
		return null;
	}
	
	public static String fixMonthorDay(String dateIn)
	{
		if (dateIn.length() == 2) return(dateIn);
		else if (dateIn.length() == 1) return("0"+dateIn);
		else return("00");
	}

	public void init(String saltyfilename)
	{
		BufferedReader saltyReader = null;
		try {
			saltyReader = new BufferedReader(new InputStreamReader(new FileInputStream(saltyfilename)));
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = saltyReader.readLine() ) != null)
			{
//				if (line.endsWith("Filename")) continue;
				put(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int put(String line)
	{
		String lineparts[];
		// sed -e 's/"\([^,]\+\),\([^,"]\+\),\([^,"]\+\),\([^,"]\+\),\([^,"]\+\)"/\1@\2@\3@\4@\5/g' | 
		// sed -e 's/"\([^,]\+\),\([^,"]\+\),\([^,"]\+\),\([^,"]\+\)"/\1@\2@\3@\4/g' | 
		// sed -e 's/"\([^,]\+\),\([^,"]\+\),\([^,"]\+\)"/\1@\2@\3/g' | 
		// sed -e 's/"\([^,]\+\),\([^,"]\+\)"/\1@\2/g' | cut -d ',' -f 10 | sed -e 's/@ /, /g' | tr '@' '\n' | uniq | sort | uniq > data/student_department.txt^C

		String lineFixed1 = line.replaceAll(",\"([^,\"]+),([^,\"]+),([^,\"]+),([^,\"]+),([^\"]+)\"", ",$1@$2@$3@$4@$5"); 
		String lineFixed2 = lineFixed1.replaceAll(",\"([^,\"]+),([^,\"]+),([^,\"]+),([^\"]+)\"", ",$1@$2@$3@$4"); 
		String lineFixed3 = lineFixed2.replaceAll(",\"([^,\"]+),([^,\"]+),([^\"]+)\"", ",$1@$2@$3"); 
		String lineFixed4 = lineFixed3.replaceAll(",\"([^,\"]+),([^\"]+)\"", ",$1@$2"); 
		String lineFixed5 = lineFixed4.replaceAll(",\"([^,\"]+)\"", ",$1"); 
		
		lineparts = lineFixed5.split(",", expectedNumParts);
		if (expectedNumParts == -1) 
			expectedNumParts = lineparts.length;
		for (int i = 0; i < lineparts.length; i++)
		{
			if (lineparts[i].contains("@ "))
			{
				lineparts[i] = lineparts[i].replaceAll("@ ", ", ");
			}
		}
		try { 
			if (lineparts.length != expectedNumParts)
			{
				if (lineparts.length > expectedNumParts)
				{
					String fixedlineparts[] = new String[expectedNumParts];
					int copyTo = 0;
					for (int i = 0; i < lineparts.length; i++)
					{
						if (lineparts[i].startsWith("\"") && !lineparts[i].endsWith("\""))
						{
							String buildIt = lineparts[i].substring(1);
							while (!lineparts[i+1].endsWith("\""))
							{
								buildIt = buildIt + "," + lineparts[i+1];
								i++;
							}
							buildIt = buildIt + "," + lineparts[i+1].substring(0, lineparts[i+1].length()-1);
							fixedlineparts[copyTo++] = buildIt;
							i++;
						}
						else
						{
							fixedlineparts[copyTo++] = lineparts[i];
						}
					}
					lineparts = fixedlineparts;
				}
				else
				{
					System.err.println("Length of data incorrect "+ lineparts.length);
					throw new RuntimeException("Length of data incorrect "+ lineparts.length);
				}
			}
		}
		catch (ArrayIndexOutOfBoundsException oobe)
		{
			int i = 0;
			for (String part : lineparts)
			{
				System.err.println(part);
			}
		}
		return put(lineparts);
	}

	private int put(String[] lineParts) 
	{
		if (lineParts.length != expectedNumParts) 
		{
			System.err.println("Length of data incorrect "+ lineParts.length);
			throw new RuntimeException("Length of data incorrect "+ lineParts.length);
		}
		if (lineParts[0].endsWith("Filename")) return -1;
		SirsiIRAData entry = new SirsiIRAData(lineParts);
		put(entry);
		return 0;
	}

	private void put(SirsiIRAData entry)
	{
		String saltedHash = entry.saltedHash;
		boolean added = false;
		if (mapIraData.containsKey(saltedHash))
		{
			List<SirsiIRAData> data = mapIraData.get(saltedHash);
			for (SirsiIRAData entry1 : data)
			{
				if (entry1.isEqualish(entry))
				{
					entry1.merge(entry);
					added = true;
					break;
				}
				else if (entry1.dateMatches(entry))
				{
					//data.add(entry);
					// errCount++;
					// System.err.println("new item inside date range, but with different values : "+errCount);
//					throw new RuntimeException("new item inside date range, but with different values");
				}
			}
			if (!added) data.add(entry);
		}
		else
		{
			List<SirsiIRAData> data = new ArrayList<SirsiIRAData>();
			data.add(entry);
			mapIraData.put(entry.saltedHash, data);
		}
	}

	public SirsiIRAData lookup(String salted_hash_id, String id)
	{
		String dateInQuestion = id.replace(salted_hash_id, "");
		int dateDistanceMin = 10000;
		int dateDistance;
		SirsiIRAData combined = null;
		SirsiIRAData dateMin = null;
		if (mapIraData.containsKey(salted_hash_id))
		{
			List<SirsiIRAData> listData = mapIraData.get(salted_hash_id);
			if (listData.size() == 1) 
			{
				return(listData.get(0));
			}
			List<SirsiIRAData> results = new ArrayList<SirsiIRAData>();
			for (SirsiIRAData data : listData)
			{
				if (data.dateMatches(dateInQuestion))
				{
					results.add(data);
					if (combined == null) combined = data;
					else combined.combine(data);
				}
				else if ((dateDistance = data.dateDistance(dateInQuestion)) < dateDistanceMin)
				{
					dateDistanceMin = dateDistance;
					dateMin = data;
				}
			}
			if (results.size() == 1) 
			{
				return(results.get(0));
			}
			if (dateMin != null)
			{
				return(dateMin);
			}
			return(combined);
			//System.err.println("date match not found ");
			//throw new RuntimeException("date match not found");
		}
		return(null);
	}
}
