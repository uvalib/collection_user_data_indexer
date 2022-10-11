package org.solrmarc.mixin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.solrmarc.callnum.CallNumUtils;
import org.solrmarc.mixin.SecureUtils;
import org.solrmarc.tools.DataUtil;

public class ReadUserResearchData extends ReadSpreadsheetDataVirgo4
{    
    public static byte[] salt;
    SirsiIRADataMap saltyData = null;
    int saltyLookupError = 0;
    
    public static void main(String[] args) 
    {
        try {
			salt = SecureUtils.getSalt();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	ReadUserResearchData userdata = new ReadUserResearchData();
        try
        {
        	userdata.initialize(args);
        	userdata.process();
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private ReadUserResearchData()
    {
    	super.haslabels = false;
    }
    
    /**
     * Circulation Data/Attribute
Description
ValueType
m=multi-valued*, s=single-valued
Assessment ID File 1 (Salted Hash) 
Unique user anonymous salted hash 
s
Date Checked Out
YYYY-MM-DD, e.g. 2015-02-08
s
Time Checked Out
mm:hh:ss e.g. 14:02:07 represents seven seconds after 2:02 PM
s
Station Library
The library that owns the user who logged into WorkFlows / 
Symphony Web
s
Item Library
The library that owns the item
s
Format
One of: MARC, MAP,  MANUSCRPT,  MRDF,  MUSIC, SERIAL, VM, 
COMMINFO, EQUIP,  ONTHEFLY, UNKNOWN 
s
Publication year
YYYY or UNKNOWN
s
Language
Three letter code identifying a work's primary language.
s
Item Type
One of 135 different values. Examples:  BOOK, BOOK-1DAY, MUSI-
SCORE, MUSIC-CD, VIDEO-DISC, VIDEO-DVD, JRNL1WK, JRNL2WK, 
JRNL4HR, JUV-BOOK
s
Item home location
The shelving location within a library where an item belongs, e.g. 
STACKS, REFERENCE,
s
Item Class Scheme
If this LC* (LCMULTI, LCPER, LC), use the Call Number to derive 
primary subject. Examples: ALPHANUM, ASIS, AUTO, DEWEY-PER, 
DEWEY, HS-SERFRST, LCMULTI, LCPER, LC, NLM, NUMERIC, SHLV-
BY-TI, SUDOC-S, SUDOC
s
Borrower Profile
Sirsi "user profile" or UNKNOWN.
s
Reserve Desk
Reserve desk, for items on reserve. Otherwise the value is "NOT 
ON RESERVE."
s
User library
Once upon a time was almost exclusively "UVA-LIB." Since go-live 
with Virgo demand management holds, community borrowers 
have library of UVA-LIB and UVA persons have library of 
HEALTHSCi, LAW, or most often LEO.
s
Subjects
Subject headings, pipe delimited when there are multiple 
headings. Fee floating subdivisions begin with "--".
m**
Subject Area Derived
Subject Area derived from 1-3 letters of call number and the call 
schema.
s
Roles ("uvaPersonIAMAffiliation" multivalue field)
All the roles held by the borrower
m
uvaPersonSponsoredType
Description of sponsored accounts. 
m
Registrar Classification ("uvaRegistrarClassification")
m
Circulation Assessment Affiliation 
Primary affiliation determined by anaysis
s
School ("uvaRegistrarSchool")
Student school(s)
m
Department ("uvaDisplayDepartment")
Borrower's department(s)
m
D' for degree seeking
'N' for non-degree seeking
s
Organizational Code ("uvaOrgCode")
Department code in Workday
m
 ACD-xxxxx 
 MC-xxxx 
 UPG-xxxx 
     */
    public void process()
    {
        if (outputSolrDoc) outputStart(writer);
        int linesRead = 0;
        try
        {
            String parts[];
            String labels[] = null;
            String inputline;
            if (haslabels) 
        	{
            	inputline = reader.readLine();
            	labels = inputline.split("\t");
        	}

            while ((inputline = reader.readLine()) != null)
            {
            	linesRead++;
            	parts = inputline.split("\t");
                if (parts.length < 3 || parts[0].equals("ID") || parts[0].contentEquals("Doc Type")) continue;
                if (outputSolrDoc)
                {
                    try { 
                        Map<String, Map<String, String>> result = processInputLine(labels, parts);
                        outputMap(writer, result);
                    }
                    catch (Exception e) 
                    {
                        e.printStackTrace(System.err);
                        showAllParts(parts);
                    }
                }
                else if (outputIRAdata)
                {
                    Map<String, Map<String, String>> result = processInputLine(labels, parts);
                    outputIRAData(writer, result);
                }
                else
                {
                    outputParts(writer, parts);
                    writer.flush();
                }
            }
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (outputSolrDoc) outputEnd(writer);
        writer.flush();
    }
    
    private void outputIRAData(PrintWriter writer2, Map<String, Map<String, String>> result) {
		List<String> columns = new ArrayList<String>();
		if (result.containsKey("id") && result.containsKey("computing_id") && result.containsKey("university_id"))
	    {
			columns.add(getFirstValue(result, "id"));
			columns.add(getFirstValue(result, "computing_id"));
			columns.add(getFirstValue(result, "university_id"));
			columns.add(getFirstValue(result, "salted_hash_str"));
			columns.add(getFirstValue(result, "checkout_date"));
			boolean first = true;
			for (String val: columns)
			{
				if (!first) 
				{ 
					writer2.print("\t"); 
				}
				writer2.print(val);
				first = false; 
			}
			writer2.println();
	    }
	}

	private String getFirstValue(Map<String, Map<String, String>> result, String key) {
		Map<String,String> valuemap = result.get(key);
		Collection<String> valuelist = valuemap.values();
		String value = (!valuelist.isEmpty()) ? valuelist.iterator().next() : "";
		return value;
	}

    public Map<String, Map<String,String>> processInputLine(String[] labels, String[] parts)
    {
        if (datasource.startsWith("sirsi"))
		{
        	return(processSirsiInputLine(labels, parts));
		}
        else if (datasource.startsWith("aeon"))
		{
        	return(processAeonInputLine(labels, parts));
		}
        else if (datasource.startsWith("tracksys"))
		{
        	return(processTracksysInputLine(labels, parts));
		}
        return(null);
    }
    
    public Map<String, Map<String,String>> processSirsiInputLine(String[] labels, String[] parts)
    {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

        // ID column 0
        String id = parts[12] + '#' + parts[13];
        String salted_hash_id = id;
        salted_hash_id = SecureUtils.getSecurePassword(id, salt);
        String borrowerProfile = parts[11];

        if (!borrowerProfile.equals("OTHERVAFAC") && !borrowerProfile.equals("VABORROWER") && parts[13].length() > 0)
        {
        	SirsiUserData userData = SirsiUserData.initialize("data/userid", parts[13], false);
            salted_hash_id = id;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);

            if (datasource.endsWith("IRA")) 
            {
                addResult(result, "computing_id", userData.computingId);
                addResult(result, "university_id", userData.universityId);
            }
        }
        else 
        {
            String lastName =  parts.length > 18 ? parts[18].toLowerCase() : parts[12];
            String firstName = parts[14].toLowerCase();
            if (firstName.equals("unknown")) firstName = parts[12];
            salted_hash_id = lastName + "#" + firstName;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);
            if (borrowerProfile.equals("OTHERVAFAC"))
            	addResult(result, "user_role_str_stored", "Community Patron");
            else if (borrowerProfile.equals("VABORROWER"))
            	addResult(result, "user_role_str_stored", "Community Patron");
        }

        addResult(result, "salted_hash_str", salted_hash_id);
        if (datasource.endsWith("IRA")) 
        {
            addResult(result, "computing_id", parts[13]);
            addResult(result, "university_id", parts[12]);

        }
        addResult(result, "datasource_str_stored", datasource);
        Collection<String> doctypes = new LinkedHashSet<String>();
        
        String callnumber = parts[9];
        int seconds = 0;
        int fracseconds = 0;

        String saltycallnum = SecureUtils.getSecurePassword(callnumber, salt);
        if (saltycallnum != null && saltycallnum.length() > 2) 
        {
        	String c1 = saltycallnum.substring(0, 2);
        	int i1 = Integer.parseInt(c1, 16);
        	seconds = i1 % 60;
        	String c2 = saltycallnum.substring(2, 5);
        	int i2 = Integer.parseInt(c2, 16);
        	fracseconds = i2 % 1000;
        }

        String checkoutDateTime = parts[0] +"T"+ parts[1] + "." + fracseconds +"Z";
        addResult(result, "checkout_date", checkoutDateTime);
        addResult(result, "checkout_daterange", checkoutDateTime);
        addResult(result, "checkout_time_str_stored", parts[1]);
        
        String solrid = salted_hash_id + checkoutDateTime;
        addResult(result, "id", solrid);

        addSaltyDataFromLookup(result, salted_hash_id, solrid);
       
        String checkoutLibrary = parts[2];
        addResult(result, "checkout_library_str_stored", checkoutLibrary);

        String itemLibrary = parts[3];
        addResult(result, "item_library_str_stored", itemLibrary);
        
        String formatType = parts[4];
        addResult(result, "format_str_stored", formatType);

        String publicationYear = parts[5];
        addResult(result, "pub_year_str_stored", publicationYear);

        String language = parts[6];
        addResult(result, "language_raw_str_stored", language);
        addResultWithLookup(result, "language_str_stored", language, "translation_maps/language_map.properties");
        
        String itemType = parts[7];
        addResult(result, "item_type_str_stored", itemType);
        
        String itemHomeLoc = parts[8];
        addResult(result, "home_loc_str_stored", itemHomeLoc);
 
        addResult(result, "callnumber_str_stored", callnumber);

        String itemClassScheme = parts[10];
        addResult(result, "item_class_scheme_str_stored", itemClassScheme);

        addResult(result, "borrower_profile_str_stored", borrowerProfile);
        
        String onReserve = parts[15];
        addResult(result, "reserve_str_stored", (onReserve.equals("RESERVE")? "yes": "no"));

        String userLibrary = parts[16];
        addResult(result, "user_library_str_stored", userLibrary);

        String subjectHeadingStr = parts.length > 17 ? parts[17] : "";
        String[] subjectHeadings = subjectHeadingStr.split("\\|");
        addSubjectHeadings(result, Arrays.asList(subjectHeadings));
        String call_numFacet = "";
        if (itemClassScheme.startsWith("LC") )
        {
        	try {
        		call_numFacet = getCallNumberPrefixNew(id, callnumber, "translation_maps/call_number_detail_map.properties", "0");
        	}
        	catch (Exception e)
        	{
        	
        	}
        }
        if (call_numFacet != null && call_numFacet.length() > 0)
        {
            addResult(result, "call_number_narrow_f_stored", call_numFacet);
        }
    return(result);
    }
    
    private void addSubjectHeadings(Map<String, Map<String, String>> result, Iterable<String> subjectHeadings)
    {
        for (String heading : subjectHeadings)
        {
        	if (heading.length() > 0)
        	{
        		addResult(result, "subject_str_stored", heading);
        		addResult(result, "subject_tsearch", heading);
        		int num = 0;
        		StringBuilder strbuild = new StringBuilder();
        		for (String subpart : heading.split("[ ]?--[ ]?"))
        		{
        			subpart = DataUtil.cleanData(subpart);
        			if (num == 0) 
        			{
        				strbuild.append(subpart);
        				addResult(result, "subject_main_str_stored", subpart);
        				num++;
        			}
        			else
        			{
        				strbuild.append(" -- ").append(subpart);
        				addResult(result, "subject_sub_str_stored", strbuild.toString());        				
        			}
        		}
        	}
        }
    }
    
    private void addSaltyDataFromLookup(Map<String, Map<String, String>> result, String salted_hash_id, String solrid)
    {
        if (saltyData == null && saltyFileName != null)
        {
        	saltyData = new SirsiIRADataMap();
        	saltyData.init(saltyFileName);
        }
        
        SirsiIRADataMap.SirsiIRAData data;
        if (saltyData != null)
        {
        	data = saltyData.lookup(salted_hash_id, solrid );
        	if (data == null)
        	{
        		if ((saltyLookupError % 1000) == 0) 
        		{
        			System.err.println("not found : "+ saltyLookupError);
        		}
        		saltyLookupError++;
        	}
        	else
        	{
        		addResult(result, "is_degree_seeking_str_stored", data.getDegreeSeeking());
        		addResult(result, "academic_plan_str_stored", data.getAcademicPlansDesc());
        		addResult(result, "academic_career_str_stored", data.getAcademicCareerDesc());
        		addResult(result, "job_family_group_str_stored", data.getJobFamilyGroup());
        		if (data.getJobFamilyGroup().contains("Student Worker"))
        		{
        			addResult(result, "student_appointment_str_stored", data.getJobTitle());
        		}
        		String jobtitlestr = data.getJobTitle();
        		if (jobtitlestr != null && jobtitlestr.length() > 0)
        			addResult(result, "job_title_str_stored", data.getJobTitle());
        		addResult(result, "organization_name_str_stored", data.getOrganizationName());
        		addResult(result, "plan_degree_str_stored", data.getPlanDegree());
        		addResult(result, "primary_academic_program_str_stored", data.getPrimaryAcademicProgramDesc());
        		addResult(result, "mbu_name_str_stored", data.getMbuName());
				addResultWithLookup(result, "school_str_stored", data.getMbuName(), "translation_maps/school_map.xml");
				addResultWithLookup(result, "school_str_stored", data.getPrimaryAcademicProgramDesc(), "translation_maps/school_map.xml");
        		addResultWithLookup(result, "department_str_stored", data.getOrganizationName(), "translation_maps/department_map.xml");
        		addResultWithLookup(result, "department_str_stored", data.getAcademicPlansDesc(), "translation_maps/department_map.xml");
        		addResultWithLookup(result, "user_role_str_stored", data.getAcademicCareerDesc(), "translation_maps/user_type_map.xml");
        		addResultWithLookup(result, "user_role_str_stored", data.getJobFamilyGroup(), "translation_maps/user_type_map.xml");
        		addResult(result, "job_family_name_str_stored", data.getJobFamilyName());

        	}
        }
    }

        
    public void addResultWithLookup(Map<String, Map<String,String>> result, String key, String value, String translationMapName)
    {
        if (value == null || value.isEmpty()) return;
        if (value.contains("@"))
		{
        	String valueParts[] = value.split("@");
        	for (String v : valueParts)
        	{
        		addResultWithLookup(result, key, v, translationMapName);
        	}
		}
        else if (translationMapName != null) 
        {
        	String mappedValue;
			try {
				mappedValue = mapEntry(value, translationMapName, null);
	        	addResult(result, key, mappedValue);
			} catch (Exception e) {
	        	addResult(result, key, value);
			}
        }
        else 
        {
        	addResult(result, key, value);
        }
    }

	public Map<String, Map<String,String>> processAeonInputLine(String[] labels, String[] parts)
    {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

        // ID column 0
        String computing_id = parts[5];
        
        String salted_hash_id;
        if (!computing_id.contains("@"))
        {
        	SirsiUserData userData = SirsiUserData.initialize("data/userid", computing_id);
            salted_hash_id = computing_id + '#' + userData.universityId;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);

            if (datasource.endsWith("IRA")) 
            {
                addResult(result, "computing_id", userData.computingId);
                addResult(result, "university_id", userData.universityId);
            }
            String userLibrary = userData.homeLibrary;
            addResult(result, "user_library_str_stored", userLibrary);
        }
        else 
        {
            String lastName = parts[4].toLowerCase();
            String firstName = parts[3].toLowerCase();
            salted_hash_id = lastName + "#" + firstName;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);
            
            String userLibrary = "UNKNOWN";
            addResult(result, "user_library_str_stored", userLibrary);
       }
        
        addResult(result, "datasource_str_stored", datasource);
        Collection<String> doctypes = new LinkedHashSet<String>();
        doctypes = null;

        addResult(result, "checkout_library_str_stored", "SPEC-COLL");

        addResult(result, "item_library_str_stored", "SPEC-COLL");
        
        addResult(result, "call_number_raw_str_stored", parts[2]);
        
        addResult(result, "reserve_str_stored", "no");



        SirsiRecordData.SirsiRecord record = SirsiRecordData.getSirsiRecordFromCallNumber(parts[2]);
        int seconds = 0;
        int fracseconds = 0;
        if (record != null) 
        {
        	String itemrec = record.getItemWithCallNum(parts[2]);
        
//        String formatType = SirsiRecordData.getSubfieldDataForTag(itemrec, 'l');
//        addResult(result, "format_str_stored", formatType);
        	if (itemrec != null)
	        {
        		String itemType = SirsiRecordData.getSubfieldDataForTag(itemrec, 't');
		        addResult(result, "item_type_str_stored", itemType);
		        
		        String itemHomeLoc = SirsiRecordData.getSubfieldDataForTag(itemrec, 'l');
		        addResult(result, "home_loc_str_stored", itemHomeLoc);
		 
		        String itemClassScheme = SirsiRecordData.getSubfieldDataForTag(itemrec, 'w');
		        addResult(result, "item_class_scheme_str_stored", itemClassScheme);
	        }
        	
        	String publicationYear = record.getPublishedYear();
	        addResult(result, "pub_year_str_stored", publicationYear);
	
	        String language = record.getLanguage();
	        //addResult(result, "language_str_stored", language);
	        addResult(result, "language_raw_str_stored", language);
	        addResultWithLookup(result, "language_str_stored", language, "translation_maps/language_map.properties");
	
	        addResult(result, "reserve_str_stored", "no");
	        List<String> subjectHeadings = record.getSubjectHeadings();
	        addSubjectHeadings(result, subjectHeadings);
	        String call_numFacet = record.getCallNumberNarrow();
	        if (call_numFacet != null && call_numFacet.length() > 0)
	        {
	            addResult(result, "call_number_narrow_f_stored", call_numFacet);
	        }
        }
        String callnumber = parts[2];
        addResult(result, "callnumber_str_stored", callnumber);

        String userRole = parts[6];
        addResultWithLookup(result, "user_role_str_stored", userRole, "translation_maps/user_type_map.xml");
        
        String callnum = parts[2];
        String saltyCallnum = SecureUtils.getSecurePassword(callnum, salt);
        if (saltyCallnum != null && saltyCallnum.length() > 2) 
        {
        	String c1 = saltyCallnum.substring(0, 2);
        	int i1 = Integer.parseInt(c1, 16);
        	seconds = i1 % 60;
        	String c2 = saltyCallnum.substring(2, 5);
        	int i2 = Integer.parseInt(c2, 16);
        	fracseconds = i2 % 1000;
        }

        SimpleDateFormat dformat = new SimpleDateFormat("d/MM/yyyy HH:mm");
        SimpleDateFormat dlocaldate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        SimpleDateFormat dtime = new SimpleDateFormat("HH:mm");
        Date date;
        String solrid;
        try {
			date = dformat.parse(parts[8]);
		    String checkoutDateTime = dlocaldate.format(date).replaceFirst(" ",  "T").replaceAll("-0[45]00", "Z").replaceAll(":00Z", ":"+String.format("%02d.%03d", seconds, fracseconds)+ "Z");
		    String checkoutTime = dtime.format(date);
		    addResult(result, "checkout_date", checkoutDateTime);
	        addResult(result, "checkout_daterange", checkoutDateTime);
	        addResult(result, "checkout_time_str_stored", checkoutTime);
	        solrid = salted_hash_id + checkoutDateTime;
	        addResult(result, "id", solrid);
	        
	        addSaltyDataFromLookup(result, salted_hash_id, solrid);

		} catch (ParseException e) {
			
		}
        

    return(result);
    }
    
    public Map<String, Map<String,String>> processTracksysInputLine(String[] labels, String[] parts)
    {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

        // ID column 0
        String computing_id = parts[2].replaceAll("@[Vv]irginia[.][Ee]du", "").trim();
        String salted_hash_id;
        if (!computing_id.contains("@"))
        {
        	SirsiUserData userData = SirsiUserData.initialize("data/userid", computing_id);
            salted_hash_id = computing_id + '#' + userData.universityId;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);

            if (datasource.endsWith("IRA")) 
            {
                addResult(result, "computing_id", userData.computingId);
                addResult(result, "university_id", userData.universityId);
            }
            String userLibrary = userData.homeLibrary;
            addResult(result, "user_library_str_stored", userLibrary);
        }
        else 
        {
            String lastName = parts[3].toLowerCase();
            String firstName = parts[4].toLowerCase();
            salted_hash_id = lastName + "#" + firstName;
            salted_hash_id = SecureUtils.getSecurePassword(salted_hash_id, salt);

            addResult(result, "salted_hash_str", salted_hash_id);
            String userLibrary = "UNKNOWN";
            addResult(result, "user_library_str_stored", userLibrary);
            String userrole = "Community Patron";
    		addResult(result, "user_role_str_stored", userrole);
        }
        
        addResult(result, "datasource_str_stored", datasource);
        Collection<String> doctypes = new LinkedHashSet<String>();
        doctypes = null;

        
        addResult(result, "checkout_library_str_stored", "SPEC-COLL");

        addResult(result, "item_library_str_stored", "SPEC-COLL");
        
        if (parts.length > 5) 
        {
        	addResult(result, "call_number_raw_str_stored", parts[5]);
        }
        else
        {
        	for (String part : parts)
        		System.err.println(part);
        }
        
        addResult(result, "reserve_str_stored", "no");

        String userLibrary = "UNKNOWN";
        addResult(result, "user_library_str_stored", userLibrary);

        String callnumber = parts[5];
        addResult(result, "callnumber_str_stored", callnumber);


        SirsiRecordData.SirsiRecord record = SirsiRecordData.getSirsiRecordFromCallNumber(parts[5]);
        int seconds = 0;
        int fracseconds = 0;
        if (record != null) 
        {
        	String itemrec = record.getItemWithCallNum(parts[5]);
        
//        String formatType = SirsiRecordData.getSubfieldDataForTag(itemrec, 'l');
//        addResult(result, "format_str_stored", formatType);
        	if (itemrec != null)
	        {
        		String itemType = SirsiRecordData.getSubfieldDataForTag(itemrec, 't');
		        addResult(result, "item_type_str_stored", itemType);
		        
		        String itemHomeLoc = SirsiRecordData.getSubfieldDataForTag(itemrec, 'l');
		        addResult(result, "home_loc_str_stored", itemHomeLoc);
		 
		        String itemClassScheme = SirsiRecordData.getSubfieldDataForTag(itemrec, 'w');
		        addResult(result, "item_class_scheme_str_stored", itemClassScheme);
	        }
        	
        	String publicationYear = record.getPublishedYear();
	        addResult(result, "pub_year_str_stored", publicationYear);
	
	        String language = record.getLanguage();
	        //addResult(result, "language_str_stored", language);
	        addResult(result, "language_raw_str_stored", language);
	        addResultWithLookup(result, "language_str_stored", language, "translation_maps/language_map.properties");
	
//	        String userRole = parts[6];
//	        addResult(result, "user_role_str_stored", userRole);
	        
	        addResult(result, "reserve_str_stored", "no");
	        List<String> subjectHeadings = record.getSubjectHeadings();
	        addSubjectHeadings(result, subjectHeadings);
	        
	        String call_numFacet = record.getCallNumberNarrow();
	        if (call_numFacet != null && call_numFacet.length() > 0)
	        {
	            addResult(result, "call_number_narrow_f_stored", call_numFacet);
	        }
        }
        
        String callnum = parts[5];
        String saltyCallnum = SecureUtils.getSecurePassword(callnum, salt);
        if (saltyCallnum != null && saltyCallnum.length() > 2) 
        {
        	String c1 = saltyCallnum.substring(0, 2);
        	int i1 = Integer.parseInt(c1, 16);
        	seconds = i1 % 60;
        	String c2 = saltyCallnum.substring(2, 5);
        	int i2 = Integer.parseInt(c2, 16);
        	fracseconds = i2 % 1000;
        }

        SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dlocaldate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        SimpleDateFormat dtime = new SimpleDateFormat("HH:mm");
        Date date;
        try {
			String datestr = parts[0].replace("T",  " ").replaceAll("Z",  "");
			date = dformat.parse(datestr);
		    String checkoutDateTime = dlocaldate.format(date).replaceFirst(" ",  "T").replaceAll("-0[45]00", "Z").replaceAll(":([0-5][0-9])Z", ":$1"+String.format(".%03d", fracseconds)+ "Z");
		    String checkoutTime = dtime.format(date);
		    addResult(result, "checkout_date", checkoutDateTime);
	        addResult(result, "checkout_daterange", checkoutDateTime);
	        addResult(result, "checkout_time_str_stored", checkoutTime);
	        String solrid = salted_hash_id + checkoutDateTime;
	        addResult(result, "id", solrid);

	        addSaltyDataFromLookup(result, salted_hash_id, solrid);

        } catch (ParseException e) {
			e.printStackTrace(System.err);
		}
    return(result);
    }
    
}
