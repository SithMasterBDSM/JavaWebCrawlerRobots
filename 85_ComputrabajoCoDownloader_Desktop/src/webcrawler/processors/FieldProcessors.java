//===========================================================================
package webcrawler.processors;

// Basic Java classes
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

// MongoDB classes
import com.mongodb.DBObject;

// Application specific classes
import databaseMongo.model.GeographicAdministrativeRegion;
import databaseMongo.model.HtmlExtraInformation;
import webcrawler.ComputrabajoTagSegment;
import webcrawler.ComputrabajoTaggedHtml;

/**
*/
public class FieldProcessors {
    /**
    @param pageProcessor
    @param h
    @param elementCount
    @param id
     */
    public static void processHtmlContent(
        ComputrabajoTaggedHtml pageProcessor, 
        HtmlExtraInformation h,
        int elementCount, 
        String id) 
    {
        if (pageProcessor.segmentList2 == null) {
            System.out.println("Warning: empty page");
            return;
        }
        ComputrabajoTagSegment ts;
        int i;
        int j;
        String n;
        String v;
        boolean nextH2 = false;
        for (i = 0; i < pageProcessor.segmentList2.size(); i++) {
            ts = pageProcessor.segmentList2.get(i);
            if (ts == null) {
                continue;
            }
            if (!ts.insideTag) {
                String trimmedContent = ComputrabajoTaggedHtml.trimSpaces(ts.getContent());
                if (nextH2) {
                    nextH2 = false;
                    h.processH2(trimmedContent);
                }
            }
            String tn = ts.getTagName();
            if (tn == null || tn.isEmpty()) {
                continue;
            }
            if (tn.equals("UL")) {
            } else if (tn.equals("/UL")) {
            } else if (tn.equals("LI")) {
            } else if (tn.equals("/LI")) {
            } else if (tn.equals("DIV")) {
            } else if (tn.equals("/DIV")) {
            } else if (tn.equals("HR")) {
            } else if (tn.equals("P")) {
            } else if (tn.equals("/P")) {
            } else if (tn.equals("SPAN")) {
            } else if (tn.equals("/SPAN")) {
            } else if (tn.equals("H1")) {
            } else if (tn.equals("/H1")) {
            } else if (tn.equals("H2")) {
                h.setNh2(h.getNh2() + 1);
                nextH2 = true;
            } else if (tn.equals("/H2")) {
            } else if (tn.equals("H3")) {
            } else if (tn.equals("/H3")) {
            } else if (tn.equals("H4")) {
            } else if (tn.equals("/H4")) {
            } else if (tn.equals("H5")) {
            } else if (tn.equals("/H5")) {
            } else if (tn.equals("H6")) {
            } else if (tn.equals("/H6")) {
            } else if (tn.equals("FONT")) {
            } else if (tn.equals("/FONT")) {
            } else if (tn.equals("STRONG")) {
            } else if (tn.equals("/STRONG")) {
            } else if (tn.equals("U")) {
            } else if (tn.equals("/U")) {
            } else if (tn.equals("BR")) {
            } else if (tn.equals("BR/")) {
            } else if (tn.equals("B")) {
            } else if (tn.equals("/B")) {
            } else if (tn.equals("B/")) {
            } else if (tn.equals("I")) {
            } else if (tn.equals("/I")) {
            } else if (tn.equals("OL")) {
            } else if (tn.equals("/OL")) {
            } else if (tn.equals("EM")) {
            } else if (tn.equals("/EM")) {
            } else if (tn.equals("BIG")) {
            } else if (tn.equals("/BIG")) {
            } else if (tn.equals("SUP")) {
            } else if (tn.equals("/SUP")) {
            } else if (tn.equals("SMALL")) {
            } else if (tn.equals("/SMALL")) {
            } else if (tn.equals("ST1:PLACENAME")) {
            } else if (tn.equals("/ST1:PLACENAME")) {
            } else if (tn.equals("ST1:PLACETYPE")) {
            } else if (tn.equals("/ST1:PLACETYPE")) {
            } else if (tn.equals("ST1:PLACE")) {
            } else if (tn.equals("/ST1:PLACE")) {
            } else if (tn.equals("ST1:STATE")) {
            } else if (tn.equals("/ST1:STATE")) {
            } else if (tn.equals("ST1:COUNTRY-REGION")) {
            } else if (tn.equals("/ST1:COUNTRY-REGION")) {
            } else if (tn.equals("BLOCKQUOTE")) {
            } else if (tn.equals("/BLOCKQUOTE")) {
            } else if (tn.equals("A")) {
            } else if (tn.equals("/A")) {
            } else if (tn.equals("TABLE")) {
            } else if (tn.equals("/TABLE")) {
            } else if (tn.equals("TR")) {
            } else if (tn.equals("/TR")) {
            } else if (tn.equals("TH")) {
            } else if (tn.equals("/TH")) {
            } else if (tn.equals("TD")) {
            } else if (tn.equals("/TD")) {
            } else if (tn.equals("DT")) {
            } else if (tn.equals("/DT")) {
            } else if (tn.equals("DL")) {
            } else if (tn.equals("/DL")) {
            } else if (tn.equals("DIR")) {
            } else if (tn.equals("/DIR")) {
            } else if (tn.equals("IMG")) {
            } else if (tn.equals("/IMG")) {
            } else if (tn.equals("CENTER")) {
            } else if (tn.equals("/CENTER")) {
            } else if (tn.equals("FORM")) {
            } else if (tn.equals("/FORM")) {
            } else if (tn.equals("SUM")) {
            } else if (tn.equals("/SUM")) {
            } else if (tn.equals("INPUT")) {
            } else if (tn.equals("/INPUT")) {
            } else if (tn.equals("SELECT")) {
            } else if (tn.equals("/SELECT")) {
            } else if (tn.equals("TEXTAREA")) {
            } else if (tn.equals("/TEXTAREA")) {
            } else if (tn.equals("TT")) {
            } else if (tn.equals("/TT")) {
            } else if (tn.equals("STYLE")) {
            } else if (tn.equals("/STYLE")) {
            } else if (tn.equals("OPTION")) {
            } else if (tn.equals("/OPTION")) {
            } else if (tn.equals("!COMMENT")) {
            } else if (tn.equals("WBR")) {
            } else if (tn.equals("V:TEXTBOX")) {
            } else if (tn.equals("/V:TEXTBOX")) {
            } else if (tn.equals("V:OVAL")) {
            } else if (tn.equals("/V:OVAL")) {
            } else if (tn.equals("W:ANCHORLOCK")) {
            } else if (tn.equals("/W:ANCHORLOCK")) {
            } else if (tn.equals("NOSCRIPT")) {
            } else if (tn.equals("/NOSCRIPT")) {
            } else if (tn.equals("V:SHADOW")) {
            } else if (tn.equals("/V:SHADOW")) {
            } else if (tn.equals("V:H")) {
            } else if (tn.equals("/V:H")) {
            } else if (tn.equals("V:FORMULAS")) {
            } else if (tn.equals("/V:FORMULAS")) {
            } else if (tn.equals("V:SHAPE")) {
            } else if (tn.equals("/V:SHAPE")) {
            } else if (tn.equals("TBODY")) {
            } else if (tn.equals("/TBODY")) {
            } else if (tn.equals("V:PATH")) {
            } else if (tn.equals("/V:PATH")) {
            } else if (tn.equals("V:LINE")) {
            } else if (tn.equals("/V:LINE")) {
            } else if (tn.equals("V:F")) {
            } else if (tn.equals("/V:F")) {
            } else if (tn.equals("V:HANDLES")) {
            } else if (tn.equals("/V:HANDLES")) {
            } else if (tn.equals("W:SDT")) {
            } else if (tn.equals("/W:SDT")) {
            } else if (tn.equals("W:WRAP")) {
            } else if (tn.equals("/W:WRAP")) {
            } else if (tn.equals("?XML:NAMESPACE")) {
            } else if (tn.equals("O:P")) {
            } else if (tn.equals("/O:P")) {
            } else if (tn.equals("O:WRAPBLOCK")) {
            } else if (tn.equals("/O:WRAPBLOCK")) {
            } else if (tn.equals("O:WRAPLOCK")) {
            } else if (tn.equals("/O:WRAPLOCK")) {
            } else if (tn.equals("O:LOCK")) {
            } else if (tn.equals("/O:LOCK")) {
            } else if (tn.equals("O:TOP")) {
            } else if (tn.equals("/O:TOP")) {
            } else if (tn.equals("O:RIGHT")) {
            } else if (tn.equals("/O:RIGHT")) {
            } else if (tn.equals("O:LEFT")) {
            } else if (tn.equals("/O:LEFT")) {
            } else if (tn.equals("O:BOTTOM")) {
            } else if (tn.equals("/O:BOTTOM")) {
            } else if (tn.equals("O:COLUMN")) {
            } else if (tn.equals("/O:COLUMN")) {
            } else if (tn.equals("V:SHAPETYPE")) {
            } else if (tn.equals("/V:SHAPETYPE")) {
            } else if (tn.equals("V:RECT")) {
            } else if (tn.equals("/V:RECT")) {
            } else if (tn.equals("V:SHADOW")) {
            } else if (tn.equals("/V:SHADOW")) {
            } else if (tn.equals("V:TEXTPATH")) {
            } else if (tn.equals("/V:TEXTPATH")) {
            } else if (tn.equals("V:IMAGEDATA")) {
            } else if (tn.equals("/V:IMAGEDATA")) {
            } else if (tn.equals("V:STROKE")) {
            } else if (tn.equals("/V:STROKE")) {
            } else if (tn.equals("V:FILL")) {
            } else if (tn.equals("/V:FILL")) {
            } else if (tn.equals("V:H")) {
            } else if (tn.equals("/V:H")) {
            } else if (tn.equals("ST1:PERSONNAME")) {
            } else if (tn.equals("/ST1:PERSONNAME")) {
            } else if (tn.equals("ST1:METRICCONVERTER")) {
            } else if (tn.equals("/ST1:METRICCONVERTER")) {
            } else if (tn.equals("-")) {
            } else if (tn.equals("ESTUDIOS")) {
            } else if (tn.equals("COLGROUP")) {
            } else if (tn.equals("COL")) {
            } else if (tn.equals("COLOR")) {
            } else if (tn.equals("COLOR,")) {
            } else if (tn.equals("/COLOR")) {
            } else if (tn.equals("INS")) {
            } else if (tn.equals("/INS")) {
            } else if (tn.equals("NOBR")) {
            } else if (tn.equals("/NOBR")) {
            } else if (tn.equals("BODY")) {
            } else if (tn.equals("/BODY")) {
            } else if (tn.equals("ALEXIS")) {
            } else if (tn.equals("SKYPE:SPAN")) {
            } else if (tn.equals("/SKYPE:SPAN")) {
            } else if (tn.equals("ATOMICELEMENT")) {
            } else if (tn.equals("/ATOMICELEMENT")) {
            } else if (tn.equals("DISPONIBILIDAD")) {
            } else if (tn.equals("BARRIO")) {
            } else if (tn.equals("EXPERIENCIA")) {
            } else {
                System.out.println("WARNING: NO TAG REGISTERED -> " + tn);
                System.out.println("  - i: " + elementCount);
                System.out.println("  - id: " + id);
            }
        }
    }


    private static void downloadImage(String url, String filename) 
    {
        File fd;

        fd = new File(filename);

        if ( fd.exists() ) {
            return;
        }

        Process p;
        String arr[] = {"/usr/local/bin/wget", "-O", filename, url};
        try {
            p = Runtime.getRuntime().exec(arr); 
            p.waitFor();
        }
	catch ( IOException ioe ) {
            
        }
        catch ( InterruptedException ex ) {

        }
    }

    private static String getExtension(String name)
    {
        String m = "";
        
        int i;
        int n = name.length();
        for ( i = n - 1; i >= 0; i-- ) {
            char c = name.charAt(i);
            if ( c == '.' ) {
                return m;
            }
            m = "" + c + m;
        }
        
        return "";
    }

    private static void processProfilePictureUrl(DBObject o, String id, int i) 
    {
        try {
            String p = o.get("profilePictureUrl").toString();
            if (p.equals("null")) {
                return;
            }
            String ext = getExtension(p);
            String filename = "./output/profilePictures/" + id + "." + ext;
            downloadImage(p, filename);
        }
	catch (Exception ex) {

        }
    }

    private static void processHtmlContent(DBObject o, int elementCount) 
    {
        String html = o.get("htmlContent").toString();
        String id = o.get("_id").toString();
        if (html == null || html.equals("null") || html.isEmpty()) {
            System.out.println("Empty HTML - skipping id " + id);
            return;
        }
        ComputrabajoTaggedHtml page;
        page = new ComputrabajoTaggedHtml();
        InputStream is;
        is = new ByteArrayInputStream(html.getBytes());
        page.importDataFromHtml(is);
        HtmlExtraInformation h;
        h = new HtmlExtraInformation();
        processHtmlContent(page, h, elementCount, id);
        if (h.getNh2() != 4) {
            System.out.println("***** WARNING: H2 apareances: " + h.getNh2());
            System.out.println("  - HTML: " + html);
        }
    }    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
