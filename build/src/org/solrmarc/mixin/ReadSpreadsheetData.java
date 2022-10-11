package org.solrmarc.mixin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.solrmarc.callnum.CallNumUtils;
import org.solrmarc.callnum.LCCallNumber;
import org.solrmarc.driver.Boot;
import org.solrmarc.tools.DataUtil;
import org.solrmarc.tools.Utils;

public abstract class ReadSpreadsheetData
{
    int argoffset = 0;
    boolean outputSolrDoc = true;
    boolean outputIRAdata = false;
    boolean haslabels = true;
    BufferedReader reader;
    PrintWriter writer;
    String inputEncoding = "UTF-8";
    String saltyFileName;
    String translationMapDir = "./data";
    Map<String, Properties> propertiesMap = new LinkedHashMap<String, Properties>();
    protected String datasource = "sirsi";
    
    protected void outputParts(PrintWriter writer, String[] parts)
    {
        for (int i = 0; i < parts.length; i++)
        {
            writer.print(parts[i] + ((i == parts.length - 1) ? "\n" : "\t"));
        }
    }

    public void initialize(String[] args) throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        while (args.length > argoffset && args[argoffset].matches("-([dvs8]|16|IRA|irafile)"))
        {
            if (args[argoffset].equals("-d")) { translationMapDir = args[argoffset+1]; argoffset += 2;  }
            if (args[argoffset].equals("-v")) { argoffset++; outputSolrDoc = false; }
            if (args[argoffset].equals("-IRA")) { argoffset++; outputIRAdata = true; outputSolrDoc = false;}
            if (args[argoffset].equals("-s")) { argoffset++; datasource = args[argoffset++]; }
            if (args[argoffset].equals("-8")) { argoffset++;  inputEncoding = "UTF-8"; }
            if (args[argoffset].equals("-16")) { argoffset++;  inputEncoding = "UTF-16"; }
            if (args[argoffset].equals("-irafile")) { saltyFileName = args[argoffset+1]; argoffset += 2; }
        }
        
        if (outputIRAdata) datasource = datasource + "-IRA";
        
        File file = args.length >= argoffset+1 ? new File(args[argoffset]) : null;
        InputStream input = null;
        if (file != null && file.exists() && file.canRead()) 
        	input = new FileInputStream(file); 
    	else if (file == null)
    		input = System.in;
    	else
    	{
    		System.err.println("Unable to open input file : "+ file.getAbsolutePath());
    		Runtime.getRuntime().exit(1);
    	}
        
        reader = new BufferedReader(new InputStreamReader(input, inputEncoding));
        
        File fout = args.length >= argoffset+2 ? new File(args[argoffset+1]) : null;
        OutputStream output = (fout != null) ? new FileOutputStream(fout) : System.out;
        writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"));
        
    }

    public abstract Map<String, Map<String, String>> processInputLine(String[] labels, String[] parts);

    public void process()
    {
        if (outputSolrDoc) outputStart(writer);
        int linesRead = 0;
        try
        {
            String labels[] = null;
            if (haslabels) labels = readLineAsParts(reader, -1, linesRead++);
            if (!outputSolrDoc && labels != null) { outputParts(writer, labels); }
            String parts[];
            while ((parts = readLineAsParts(reader, (labels == null) ? -1 : labels.length, linesRead++)) != null)
            {
                if (parts.length == 0) break;
                if (parts[0].equals("ID") || parts[0].contentEquals("Doc Type")) continue;
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
 

	private static String[] readLineAsParts(BufferedReader reader, int expectedNumColumns, int lineNo) throws IOException
    {
        List<String> line = new ArrayList<String>();
        boolean done = false;
        while (!done)
        {
            done = readColumn(reader, line);
        }
        if (expectedNumColumns != -1 && line.size() == expectedNumColumns - 1 )
        {
            line.add("");
        }
        if (expectedNumColumns != -1 && line.size() != expectedNumColumns && line.size() != 0)
        {
            throw new RuntimeException("Error reading line number "+ lineNo);
        }
        if (expectedNumColumns != -1 && line.size() != expectedNumColumns && line.size() == 0)
        {
            return(null); // end of file
        }
        return line.toArray(new String[0]);
    }
    
    private static boolean readColumn(BufferedReader reader, List<String> line) throws IOException
    {
    	return readColumn(reader, line, '\t');
    }
    
    private static boolean readColumn(BufferedReader reader, List<String> line, char columnSep) throws IOException
    {
        int c = reader.read();
        if (c == -1)
        {
            return(true);
        }
        else if (c == '"') 
        {
            return readQuotedColumn(reader, line);
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            while (c != columnSep && c != '\r' && c != '\n' && c != -1)
            {
                sb.append((char)c);
                c = reader.read();
            }
            line.add(sb.toString());
            if (c == columnSep) return(false); // not final column
            else if (c == '\r') 
            {
                reader.mark(10);
                c = reader.read();
                if (c != '\n') reader.reset();
            }
            return(true);                 // is final column
        }
    }

    private static boolean readQuotedColumn(BufferedReader reader, List<String> line) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int c = -1, c1 = -1; 
        boolean done = false;
        boolean prevWasSpace = false;
        while (!done)
        {
            c = reader.read(); 
            if (c == '"')
            {
                c1 = reader.read();
                if (c1 == '"')
                {
                    sb.append((char)c1);
                }
                else if (c1 == '\t' || c1 == '\r' || c1 == '\n')
                {
                    done = true;
                }
            }
            else if (c == '\t' || c == '\r' || c == '\n')
            {
                if (! prevWasSpace) 
                { 
                    sb.append((char)' '); 
                    prevWasSpace = true;
                }
            }
            else
            {
                sb.append((char)c);
                prevWasSpace = (c == ' ');
            }
        }
        line.add(sb.toString());
        if (c1 == '\t') return(false); // not final column
        else if (c1 == '\r') 
        {
            reader.mark(10);
            c = reader.read();
            if (c != '\n') reader.reset();
        }
        return(true);                  // is final column
    }

    protected void outputStart(PrintWriter writer)
    {
        writer.println("<add>");        
    }

    protected void outputEnd(PrintWriter writer)
    {
        writer.println("</add>");        
    }

    protected void outputMap(PrintWriter writer, Map<String, Map<String, String>> result)
    {
        ArrayList<String> keys = new ArrayList<String>();
        keys.addAll(result.keySet());
        Collections.sort(keys);
        boolean hasAny = false;
        for (String key : keys)
        {
            Map<String, String> values = result.get(key);
            if (values.isEmpty()) continue;
            for (String value : values.values())
            {
                if (value.length() == 0) continue;
                if (!hasAny)
                {
                    writer.println("    <doc>");
                    hasAny = true;
                }
                writer.println("        <field name=\"" + key + "\">" + xmlEncode(value) + "</field>");
            }
        }
        if (hasAny)
        {
            writer.println("    </doc>"); 
            writer.flush();
        }
        else
        {
            writer.flush();            
        }
    }

    protected String xmlEncode(String value)
    {
        String result = value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        
        return result;
    }

    protected void showAllParts(String parts[])
    {
        for (int i = 0; i < parts.length; i++)
        {
            System.err.println(i + " : " + parts[i]);
        }
    }
    
    protected String fixSubject(String subject)
    {
        String subParts[] = subject.split("--");
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String subPart : subParts)
        {
            if (first) first = false;
            else       sb.append(" -- ");
            subPart = DataUtil.cleanData(subPart);
            subPart = DataUtil.toTitleCase(subPart);
            subPart = subPart.replaceAll("\\b(And)\\b", "and" );
            subPart = subPart.replaceAll("\\b(The)\\b", "the" );
            subPart = subPart.replaceAll("\\b(For)\\b", "for" );
            subPart = subPart.replaceAll("\\b(Of)\\b", "of" );
            subPart = subPart.replaceAll("\\b(In)\\b", "in" );
            sb.append(subPart);
        }
        return sb.toString();
    }

    public String getUniquishLCCallNumber(String lcCallNum, String bestAuthorCutter, String bestDate)
    {
        String callnum = lcCallNum;
        if (callnum == null) return (null);
        if (bestAuthorCutter != null && !callnum.contains(bestAuthorCutter) && callnum.matches(".*[A-Z][0-9]+")) 
        {
            callnum = callnum + "." + bestAuthorCutter;
        }
        if (bestDate != null && !callnum.contains(bestDate))
        {
            callnum = callnum + " " + bestDate;
        }
        return(callnum);
    }
    
    public String getUniquishLCShelfKey(String lcCallNum, String bestAuthorCutter, String bestDate, String id)
    {
        String callnum = getUniquishLCCallNumber(lcCallNum, bestAuthorCutter, bestDate);
        if (callnum == null) return (null);
        if (id != null) 
        {
            callnum = callnum + " " + id;
        }
        String result = null;
        if (CallNumUtils.isValidLC(callnum))
        {
            LCCallNumber callNum = new LCCallNumber(callnum);
            result = callNum.getPaddedShelfKey();
        }
        return (result);
    }

    public String getUniquishReverseLCShelfKey(String lcCallNum, String bestAuthorCutter, String bestDate, String id)
    {
        String shelfKey = getUniquishLCShelfKey(lcCallNum, bestAuthorCutter, bestDate, id);
        if (shelfKey == null) return (shelfKey);
        String revShelfKey = CallNumUtils.getReverseShelfKey(shelfKey);
        return (revShelfKey);
    }

    public String mapEntry(String key, String mapName, String part) throws Exception
    {
        Properties mapProps = loadPropertyMap(mapName, part);
        if (key != null && mapProps.containsKey(key))
        {
        	String lookup = mapProps.getProperty(key);
        	if (lookup.trim().length() == 0)
        	{
        		return(key);
        	}
        	return(lookup);
        }
        else 
        {
        	return(key);
        }
    }

    protected String getCallNumberPrefixNew(String id, String bestSingleCallNumber, String mapName, String part) throws Exception
    {
        Properties mapProps = loadPropertyMap(mapName, part);
    
        String val = bestSingleCallNumber;
        String result = null;
        if (val == null || val.length() == 0)
        {
            return (null);
        }
        String vals[] = bestSingleCallNumber.split("[^A-Za-z]+", 2);
        String prefix = vals[0];
    
        if (vals.length == 0 || vals[0] == null || vals[0].length() == 0 || vals[0].length() > 3 || !vals[0].toUpperCase().equals(vals[0]))
        {
            return (null);
        }
        else
        {
            while (result == null && prefix.length() > 0)
            {
                result = mapProps.getProperty(prefix);
                if (result == null && prefix.length() == 1)
                {
                    break;
                }
                if (result == null)
                {
                    prefix = prefix.substring(0, prefix.length() - 1);
                }
            }
        }
        if (mapName.endsWith("callnumber_map.properties"))
        {
            int partNum = Utils.isNumber(part) ? Integer.parseInt(part) : 0;
            if (result == null) return (result);
            if (partNum == 0) return (prefix + " - " + result.replaceAll("[|]", " - "));
            String resultParts[] = result.split("[|]");
            if (partNum - 1 >= resultParts.length) return (null);
            return (prefix.substring(0, 1) + " - " + resultParts[partNum - 1]);
        }
        else // detailed call number map
        {
            if (result == null) return (result);
            if (result.startsWith("{"))
            {
                String shelfKey = CallNumUtils.getLCShelfkey(val, id);
                String keyDigits = shelfKey.substring(4, 8);
                String ranges[] = result.replaceAll("[{]", "").split("[}]");
                for (String range : ranges)
                {
                    String rangeParts[] = range.split("[-=]", 3);
                    if (keyDigits.compareTo(rangeParts[0]) >= 0 && keyDigits.compareTo(rangeParts[1]) <= 0)
                    {
                        return (prefix + rangeParts[0].replaceFirst("^0+", "") + "-" + prefix + rangeParts[1].replaceFirst("^0+", "") + " - " + rangeParts[2]);
                    }
                }
                return (null);
            }
            else if (result.startsWith(prefix.substring(0, 1)) && result.matches("[" + prefix.substring(0, 1) + "][A-Z]-[" + prefix.substring(0, 1) + "][A-Z] - .*"))
            {
                return (result);
            }
            else
            {
                return (prefix + " - " + result);
            }
    
        }
    }
    
    protected Properties loadPropertyMap(String mapName, String part) throws Exception
    {
        Properties mapProps = null;
        if (propertiesMap.containsKey(mapName)) 
        {
            mapProps = propertiesMap.get(mapName);
        }
        else
        {
            mapProps = new Properties();
            File defDir = new File(this.translationMapDir);

            File mapFile = new File(mapName);
//            System.err.println("mapfile = " + mapFile.getAbsolutePath());
            if (!mapFile.isAbsolute() && !mapFile.exists())
            {
                mapFile = new File(defDir, mapName);
//                System.err.println("mapfile = " + mapFile.getAbsolutePath());
            }
            if (mapFile.exists() && mapFile.canRead() && mapFile.getAbsolutePath().endsWith(".properties"))
            {
                mapProps.load(new FileInputStream(mapFile));
//                System.err.println("mapfile = " + mapFile.getAbsolutePath());
            }
            else if (mapFile.exists() && mapFile.canRead() && mapFile.getAbsolutePath().endsWith(".xml"))
            {
            	mapProps.loadFromXML(new FileInputStream(mapFile));
            }
            else 
            {
            	System.err.println("Can't find or read specified file: "+mapFile.getAbsolutePath());
            }
            propertiesMap.put(mapName, mapProps);
        }
        return mapProps;
    }

    public ReadSpreadsheetData()
    {
        super();
    }

    public void addResult(Map<String, Map<String,String>> result, String key, String value)
    {
        if (value == null || value.isEmpty()) return;
        if (value.contains("@"))
		{
        	String valueParts[] = value.split("@");
        	for (String v : valueParts)
        	{
        		addResult(result, key, v);
        	}
		}
        else if (result.containsKey(key)) 
        {
            Map<String,String> valueMap = result.get(key);
            String normedValue = value.toLowerCase().replaceAll("( |\\p{Punct})+", " ").trim();
            if (!valueMap.containsKey(normedValue))
            {
                valueMap.put(normedValue, value);
            }
            else
            {
                result.put(key, valueMap);
            }
        }
        else
        {
            Map<String,String> valueMap = new LinkedHashMap<String, String>();
            String normedValue = value.toLowerCase().replaceAll("( |\\p{Punct})+", " ").trim();
            valueMap.put(normedValue, value);
            result.put(key, valueMap);
        }
    }
    
    public void addResult(Map<String, Map<String,String>> result, String key, Collection<String> values)
    {
        if (values.size() == 0) return;
        for (String value : values)
        {
            addResult(result, key, value);
        }
    }

    public void handleLCNum(Map<String, Map<String,String>> result, String lcnum, String id, String firstAuthor, String year)
    {
        addResult(result, "call_number_text", lcnum);
        addResult(result, "call_number_display", lcnum);
        if (!lcnum.startsWith("Audio") && !lcnum.startsWith("audio") && !lcnum.startsWith("Video"))
        {
            String firstAuthorCutter = (firstAuthor == null) ? null : org.solrmarc.callnum.Utils.getCutterFromAuthor(firstAuthor);
            String uniquishLCNum = getUniquishLCCallNumber(lcnum, firstAuthorCutter, year);
            addResult(result, "lc_call_number_display", uniquishLCNum);
            try
            {
                String call_numFacet = getCallNumberPrefixNew(id, lcnum, "translation_maps/call_number_detail_map.properties", "0");
                if (call_numFacet != null)
                {
                    addResult(result, "call_number_facet", call_numFacet);
                }
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                
                String shelfkey = getUniquishLCShelfKey(uniquishLCNum, null,  null, id);
                String reverse_shelfkey = CallNumUtils.getReverseShelfKey(shelfkey);
                addResult(result, "shelfkey", shelfkey);
                addResult(result, "reverse_shelfkey", reverse_shelfkey);
            }
            catch (IllegalArgumentException iae)
            {
                System.err.println("shelfkey exception: "+ iae.getMessage());
            }
        }
    }

    /**
     *   Find the location of where this class is running from
     *   When run normally this would be the main solrmarc jar
     *   when run from classdirs in eclipse, is is the project location
     *
     *   @return  String - location of where this class is running from.  Used
     *                      as default search location for local configuration
     *                      files (As a side effect, sets System Property
     *                      solrmarc.jar.dir to this same value so it can be
     *                      referenced in log4j.properties)
     */
    public static String getDefaultHomeDir()
    {
        CodeSource codeSource = Boot.class.getProtectionDomain().getCodeSource();
        String jarDir;
        File jarFile = null;
        try
        {
            jarFile = new File(codeSource.getLocation().toURI().getPath());
//            System.err.println("jarfile: "+ jarFile.getAbsolutePath());

        }
        catch (URISyntaxException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (jarFile.getName().endsWith(".jar"))
        {
            jarDir = jarFile.getParentFile().getPath();
//            System.err.println("jardir: "+ jarDir);
        }
        else
        {
            // Not running from a jar. Probably running from eclipse or other
            // IDE
            jarDir = new File(".").getAbsoluteFile().getParentFile().getAbsolutePath();
//            System.err.println("else jardir: "+ jarDir);

        }
        System.setProperty("jar.dir", jarDir);
        return(jarDir);
    }

}